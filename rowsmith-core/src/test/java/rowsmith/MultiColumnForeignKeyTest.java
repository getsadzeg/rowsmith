package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultiColumnForeignKeyTest {

    static DataSource ds;

    @BeforeAll
    static void schema() {
        ds = Pg.withSchema("""
                CREATE TABLE appointment (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  status text NOT NULL DEFAULT 'booked' CHECK (status IN ('booked', 'completed', 'cancelled', 'no_show')),
                  starts_at timestamptz NOT NULL,
                  CONSTRAINT appointment_id_status_key UNIQUE (id, status));
                CREATE TABLE invoice (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  appointment_id bigint NOT NULL, appointment_status text NOT NULL, amount numeric(10,2) NOT NULL,
                  CONSTRAINT invoice_appointment_id_key UNIQUE (appointment_id),
                  CONSTRAINT invoice_appointment_completed_check CHECK (appointment_status = 'completed'),
                  CONSTRAINT invoice_appointment_fkey FOREIGN KEY (appointment_id, appointment_status) REFERENCES appointment (id, status),
                  CONSTRAINT invoice_amount_check CHECK (amount > 0));
                CREATE TABLE review (id serial PRIMARY KEY, appointment_id bigint NOT NULL, appointment_status text NOT NULL,
                  FOREIGN KEY (appointment_id, appointment_status) REFERENCES appointment (id, status));
                CREATE TABLE patient (id serial PRIMARY KEY);
                CREATE TABLE booking_note (id serial PRIMARY KEY, patient_id int NOT NULL REFERENCES patient,
                  appointment_id bigint NOT NULL, appointment_status text NOT NULL,
                  FOREIGN KEY (appointment_id, appointment_status) REFERENCES appointment (id, status));
                CREATE TABLE visit (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, kind text NOT NULL CHECK (kind = 'walk_in'),
                  UNIQUE (id, kind));
                CREATE TABLE visit_note (id serial PRIMARY KEY, visit_id bigint NOT NULL, visit_kind text NOT NULL,
                  CONSTRAINT visit_note_scheduled_check CHECK (md5(visit_kind) = md5('scheduled')),
                  FOREIGN KEY (visit_id, visit_kind) REFERENCES visit (id, kind));
                """);
    }

    @Test
    void theParentIsGeneratedToSatisfyTheChildsCheckOnTheSharedColumns() {
        Row invoice = Rowsmith.of(ds).create("invoice");

        assertThat(invoice.getString("appointment_status")).isEqualTo("completed");
        assertThat(invoice.parent("appointment").getString("status")).isEqualTo("completed");
        assertThat(invoice.parent("appointment").id()).isEqualTo(invoice.get("appointment_id"));
    }

    @Test
    void aValueSetOnPartOfTheKeyIsCopiedToANewParent() {
        Rowsmith rs = Rowsmith.of(ds);
        Row first = rs.create("review", r -> r.set("appointment_status", "cancelled"));
        Row second = rs.create("review", r -> r.set("appointment_status", "completed"));
        Row invoice = rs.create("invoice", i -> i.set("appointment_status", "completed"));

        assertThat(first.parent("appointment").getString("status")).isEqualTo("cancelled");
        assertThat(first.parent("appointment").id()).isEqualTo(first.get("appointment_id"));
        assertThat(second.parent("appointment").getString("status")).isEqualTo("completed");
        assertThat(invoice.parent("appointment").id()).isEqualTo(invoice.get("appointment_id"));
    }

    @Test
    void aUsedParentIsLinkedWhenItMatchesTheValuesSetOnTheKey() {
        Rowsmith rs = Rowsmith.of(ds);
        Row cancelled = rs.create("appointment", a -> a.set("status", "cancelled"));
        Row review = rs.create("review", r -> r.use(cancelled).set("appointment_status", "cancelled"));

        assertThat(review.get("appointment_id")).isEqualTo(cancelled.id());
    }

    @Test
    void aUsedParentThatContradictsAValueSetOnTheKeyFails() {
        Rowsmith rs = Rowsmith.of(ds);
        Row booked = rs.create("appointment");

        assertThatThrownBy(() -> rs.create("invoice", i -> i.use(booked).set("appointment_status", "completed")))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.INVALID_RELATIONSHIP);
                    assertThat(e.getMessage()).isEqualTo("Rowsmith: use(appointment#" + booked.id() + ") has status = booked, but invoice.appointment_status"
                            + " is set to completed; drop one of them (MultiColumnForeignKeyTest:77)");
                });
    }

    @Test
    void anAutoSharedParentIsSharedOnlyWhenItMatchesTheValuesSetOnTheKey() {
        Plan plan = Rowsmith.of(ds).plan("patient", p -> p
                .with("booking_note", n -> n.set("appointment_status", "cancelled"))
                .with("booking_note", n -> n.set("appointment_status", "cancelled"))
                .with("booking_note", n -> n.set("appointment_status", "completed")));

        assertThat(plan.rows()).filteredOn(r -> r.table().equals("appointment")).hasSize(2);
    }

    @Test
    void settingTheWholeKeyStillUsesTheGivenValuesWithoutCreatingAParent() {
        Rowsmith rs = Rowsmith.of(ds);
        Row appointment = rs.create("appointment", a -> a.set("status", "completed"));
        Plan plan = rs.plan("invoice", i -> i.set("appointment_id", appointment.id()).set("appointment_status", "completed"));

        assertThat(plan.rows()).extracting(Plan.PlannedRow::table).containsExactly("invoice");
        Row invoice = rs.create("invoice", i -> i.set("appointment_id", appointment.id()).set("appointment_status", "completed"));
        assertThat(invoice.get("appointment_id")).isEqualTo(appointment.id());
    }

    @Test
    void aCheckTheParentCannotBeGeneratedForSaysTheValueComesFromTheParent() {
        assertThatThrownBy(() -> Rowsmith.of(ds).create("visit_note"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.CHECK_VIOLATION);
                    assertThat(e.getMessage()).contains("visit_note.visit_kind = \"walk_in\" violates check visit_note_scheduled_check")
                            .contains("visit_kind comes from the parent visit.kind")
                            .contains("set kind on the visit")
                            .contains("use(...)");
                });
    }
}

package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
                CREATE TABLE bundle (id serial PRIMARY KEY, review_id int NOT NULL, invoice_id bigint NOT NULL,
                  CONSTRAINT bundle_a_review_fkey FOREIGN KEY (review_id) REFERENCES review,
                  CONSTRAINT bundle_b_invoice_fkey FOREIGN KEY (invoice_id) REFERENCES invoice);
                CREATE TABLE emp (id serial PRIMARY KEY, dept text NOT NULL, mgr_id int NOT NULL, mgr_dept text NOT NULL,
                  UNIQUE (id, dept), FOREIGN KEY (mgr_id, mgr_dept) REFERENCES emp (id, dept) DEFERRABLE INITIALLY DEFERRED);
                CREATE TABLE shift (id serial PRIMARY KEY, team text NOT NULL, roster_id int NOT NULL, UNIQUE (id, team));
                CREATE TABLE roster (id serial PRIMARY KEY, team text NOT NULL, shift_id int NOT NULL, UNIQUE (id, team),
                  FOREIGN KEY (shift_id, team) REFERENCES shift (id, team));
                CREATE TABLE followup (id serial PRIMARY KEY, main_appointment_id bigint NOT NULL REFERENCES appointment,
                  appointment_id bigint NOT NULL, appointment_status text NOT NULL,
                  FOREIGN KEY (appointment_id, appointment_status) REFERENCES appointment (id, status));
                ALTER TABLE shift ADD FOREIGN KEY (roster_id, team) REFERENCES roster (id, team) DEFERRABLE INITIALLY DEFERRED;
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
                    assertThat(e.getMessage()).startsWith("Rowsmith: use(appointment#" + booked.id() + ") has status = \"booked\", but invoice.appointment_status"
                            + " is set to \"completed\"; drop one of them (MultiColumnForeignKeyTest:");
                });
    }

    @Test
    void aUseConflictNamesTheOtherForeignKeyToTheSameTable() {
        Rowsmith rs = Rowsmith.of(ds);
        Row booked = rs.create("appointment");

        assertThatThrownBy(() -> rs.plan("followup", f -> f.use(booked).set("appointment_status", "completed")))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.INVALID_RELATIONSHIP);
                    assertThat(e.detail()).startsWith("use(appointment#" + booked.id() + ") has status = \"booked\", but followup.appointment_status"
                            + " is set to \"completed\"; drop one of them, or pass the row for the other key with use(\"main_appointment_id\", row)");
                });
    }

    @Test
    void aParentForAPartlySetKeyIsNeverShared() {
        Plan plan = Rowsmith.of(ds).plan("patient", p -> p
                .with("booking_note", n -> n.set("appointment_status", "cancelled"))
                .with("booking_note", n -> n.set("appointment_status", "cancelled"))
                .with("booking_note", n -> n.set("appointment_status", "completed")));

        assertThat(plan.rows()).filteredOn(r -> r.table().equals("appointment")).hasSize(3);
    }

    @Test
    void aParentIsNotSharedWhenTheChildHasChecksOnTheKey() {
        Row bundle = Rowsmith.of(ds).create("bundle");

        Row invoice = bundle.parent("invoice");
        assertThat(invoice.parent("appointment").getString("status")).isEqualTo("completed");
        assertThat(invoice.parent("appointment").id()).isNotEqualTo(bundle.parent("review").get("appointment_id"));
    }

    @Test
    void settingOnlyTheParentsIdFails() {
        Rowsmith rs = Rowsmith.of(ds);
        Row existing = rs.create("appointment");

        assertPartOfKey(() -> rs.plan("review", r -> r.set("appointment_id", existing.id())),
                "review sets only appointment_id of key (appointment_id, appointment_status); set the whole key, or use(...) a matching appointment");
    }

    @Test
    void aWithChildThatSetsPartOfTheKeyFails() {
        assertPartOfKey(() -> Rowsmith.of(ds).plan("appointment", a -> a.with("invoice", 1, i -> i.set("appointment_status", "completed"))),
                "invoice sets only appointment_status of key (appointment_id, appointment_status), which with(...) fills from appointment#1;"
                        + " set status on the appointment instead");
    }

    @Test
    void aRowForTheKeyColumnWithASetValueOnTheSameKeyFails() {
        Rowsmith rs = Rowsmith.of(ds);
        Row booked = rs.create("appointment");
        String expected = "review sets only appointment_status of key (appointment_id, appointment_status); set the whole key, or use(...) a matching appointment";

        assertPartOfKey(() -> rs.plan("review", r -> r.set("appointment_id", booked).set("appointment_status", "booked")), expected);
        assertPartOfKey(() -> rs.plan("review", r -> r.use("appointment_id", booked).set("appointment_status", "booked")), expected);
    }

    @Test
    void aSelfReferenceWithPartOfTheKeySetFails() {
        assertPartOfKey(() -> Rowsmith.of(ds).plan("emp", e -> e.set("mgr_dept", "sales")),
                "emp sets only mgr_dept of key (mgr_id, mgr_dept); set the whole key, or use(...) a matching emp");
    }

    @Test
    void aCycleBackToARowInProgressWithPartOfTheKeySetFails() {
        assertPartOfKey(() -> Rowsmith.of(ds).plan("shift", s -> s.set("team", "red")),
                "roster sets only team of key (shift_id, team); set the whole key, or use(...) a matching shift");
    }

    private static void assertPartOfKey(ThrowingCallable call, String message) {
        assertThatThrownBy(call).isInstanceOfSatisfying(RowsmithException.class, e -> {
            assertThat(e.code()).isEqualTo(ErrorCode.INVALID_RELATIONSHIP);
            assertThat(e.detail()).startsWith(message + " (MultiColumnForeignKeyTest:");
        });
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
                            .contains("[visit_kind comes from visit.kind; set it there or use(...) a matching visit]");
                });
    }

    @Test
    void aCheckOnAValueCopiedFromAWithParentSaysOnlyWhereTheValueComesFrom() {
        assertThatThrownBy(() -> Rowsmith.of(ds).plan("appointment", a -> a.set("status", "booked").with("invoice", 1)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.CHECK_VIOLATION);
                    assertThat(e.detail()).startsWith("invoice.appointment_status = \"booked\" violates check invoice_appointment_completed_check")
                            .contains("[appointment_status comes from appointment.status; set it there or use(...) a matching appointment]")
                            .doesNotContain("set appointment_status here");
                });
    }
}

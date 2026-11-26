package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import javax.sql.DataSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidationTest {

    static DataSource ds;
    static Rowsmith rs;

    @BeforeAll
    static void schema() {
        ds = Pg.withResource("lending.sql");
        rs = Rowsmith.builder(ds).build();
    }

    private static void fails(ThrowingCallable call, ErrorCode code, String... fragments) {
        long loans = Pg.count(ds, "loan");
        assertThatThrownBy(call).isInstanceOfSatisfying(RowsmithException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.getMessage()).startsWith("Rowsmith: ").contains(fragments).contains("(ValidationTest:");
        });
        assertThat(Pg.count(ds, "loan")).as("nothing inserted").isEqualTo(loans);
    }

    @Test
    void checkViolationNamesConstraintAndAllowedValues() {
        fails(() -> rs.create("dispute", d -> d.set("status", "OPN")), ErrorCode.CHECK_VIOLATION,
                "dispute.status = \"OPN\" violates check dispute_status_chk; allowed: OPEN, CLOSED");
    }

    @Test
    void enumViolation() {
        fails(() -> rs.create("loan", l -> l.set("status", "ACTIV")), ErrorCode.NOT_IN_ENUM,
                "loan.status = \"ACTIV\"", "allowed: PENDING, ACTIVE, PAID_OFF, CHARGED_OFF");
    }

    @Test
    void unknownTableAndColumnListTheNames() {
        fails(() -> rs.create("loans"), ErrorCode.UNKNOWN_TABLE, "unknown table 'loans'", "Tables: ", "installment, loan,");
        fails(() -> rs.create("loan", l -> l.set("princpal", 10)), ErrorCode.UNKNOWN_COLUMN, "loan.princpal", "Columns: id, customer_id, store_id, principal");
    }

    @Test
    void typeLengthAndPrecision() {
        fails(() -> rs.create("loan", l -> l.set("term_months", "twelve")), ErrorCode.INVALID_VALUE, "loan.term_months = \"twelve\": invalid input syntax for type integer");
        fails(() -> rs.create("customer", c -> c.set("email", "x".repeat(121))), ErrorCode.VALUE_OUT_OF_RANGE, "value too long for type character varying(120)");
        fails(() -> rs.create("loan", l -> l.set("principal", 1e12)), ErrorCode.VALUE_OUT_OF_RANGE, "numeric field overflow");
    }

    @Test
    void domainAndRangeChecks() {
        fails(() -> rs.create("loan", l -> l.set("principal", -5)), ErrorCode.CHECK_VIOLATION, "loan.principal = -5 violates check");
        fails(() -> rs.create("loan", l -> l.set("term_months", 61)), ErrorCode.CHECK_VIOLATION, "loan.term_months = 61", "must be >= 1 and <= 60");
    }

    @Test
    void crossColumnCheckWithFixedValues() {
        fails(() -> rs.create("installment", i -> i.set("amount", 10).set("paid_amount", 20)), ErrorCode.CHECK_VIOLATION,
                "paid_amount = 20", "amount = 10");
    }

    @Test
    void notNullAndGeneratedColumns() {
        fails(() -> rs.create("loan", l -> l.set("originated_on", null)), ErrorCode.NOT_NULL_VIOLATION, "loan.originated_on is NOT NULL");
        fails(() -> rs.create("installment", i -> i.set("outstanding", 1)), ErrorCode.INVALID_VALUE, "GENERATED column");
    }

    @Test
    void databaseErrorsAreTranslated() {
        fails(() -> rs.create("loan", l -> l.set("customer_id", 999999).set("status", "ACTIVE")), ErrorCode.FOREIGN_KEY_VIOLATION,
                "insert into loan", "loan_customer_id_fkey", "Hint");
    }

    @Test
    void theCallSiteIsNamedOnce() {
        assertThatThrownBy(() -> rs.create("loan", l -> l.set("term_months", 61)))
                .hasMessageContaining("(ValidationTest:").message().containsOnlyOnce("(ValidationTest:");
        assertThatThrownBy(() -> rs.create("loan", l -> l.set("customer_id", 999999).set("status", "ACTIVE")))
                .hasMessageContaining("(ValidationTest:").message().containsOnlyOnce("(ValidationTest:");
    }

    @Test
    void aRowForAColumnOutsideAnyForeignKeyIsRejected() {
        Row merchant = rs.create("merchant");
        fails(() -> rs.create("customer", c -> c.use("email", merchant)), ErrorCode.INVALID_RELATIONSHIP, "email is not a foreign key column");
    }

    @Test
    void integerMinimumsReachPostgresWhichJudgesTheRange() {
        Rowsmith nums = Rowsmith.builder(Pg.withSchema("CREATE TABLE nums (id serial PRIMARY KEY, s int2, i int4, l int8)")).build();
        Row r = nums.create("nums", n -> n.set("s", Short.MIN_VALUE).set("i", Integer.MIN_VALUE).set("l", Long.MIN_VALUE));
        assertThat(r.values()).containsEntry("s", (int) Short.MIN_VALUE).containsEntry("i", Integer.MIN_VALUE).containsEntry("l", Long.MIN_VALUE);
        assertThat(nums.create("nums", n -> n.set("i", "-2147483648")).values()).containsEntry("i", Integer.MIN_VALUE);
        assertThatThrownBy(() -> nums.create("nums", n -> n.set("s", Short.MIN_VALUE - 1)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALUE_OUT_OF_RANGE))
                .hasMessageContaining("is out of range for type smallint");
        assertThatThrownBy(() -> nums.create("nums", n -> n.set("i", "-2147483649")))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALUE_OUT_OF_RANGE))
                .hasMessageContaining("nums.i = \"-2147483649\"");
    }

    @Test
    void jsonColumnsTakeJsonTextThatPostgresValidates() {
        Rowsmith docs = Rowsmith.builder(Pg.withSchema("CREATE TABLE doc (id serial PRIMARY KEY, body jsonb NOT NULL)")).build();
        assertThat(docs.create("doc", d -> d.set("body", "{\"a\": [1, 2]}")).values().get("body")).isEqualTo("{\"a\": [1, 2]}");
        assertThatThrownBy(() -> docs.create("doc", d -> d.set("body", Map.of("a", 1))))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_VALUE))
                .hasMessageContaining("pass JSON text");
        assertThatThrownBy(() -> docs.create("doc", d -> d.set("body", "{a")))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_VALUE))
                .hasMessageContaining("invalid input syntax for type json");
    }
}

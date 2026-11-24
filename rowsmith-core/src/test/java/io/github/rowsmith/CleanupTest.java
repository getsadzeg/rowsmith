package io.github.rowsmith;

import static io.github.rowsmith.lending.Db.CUSTOMER;
import static io.github.rowsmith.lending.Db.CUSTOMER_KYC;
import static io.github.rowsmith.lending.Db.DISPUTE;
import static io.github.rowsmith.lending.Db.INSTALLMENT;
import static io.github.rowsmith.lending.Db.LOAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rowsmith.lending.Db.LoanStatus;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

class CleanupTest {

    DataSource ds;

    @BeforeEach
    void schema() {
        ds = Pg.withResource("lending.sql");
        Rowsmith.of(ds).createMany(LOAN, 2, l -> l.set(LOAN.STATUS, LoanStatus.ACTIVE));
    }

    static String fingerprint(DataSource ds) {
        return Pg.queryString(ds, """
                SELECT md5(string_agg(t, '|' ORDER BY t)) FROM (
                  SELECT 'merchant:' || coalesce((SELECT string_agg(m::text, ',' ORDER BY id) FROM merchant m), '') AS t
                  UNION ALL SELECT 'store:' || coalesce((SELECT string_agg(s::text, ',' ORDER BY id) FROM store s), '')
                  UNION ALL SELECT 'customer:' || coalesce((SELECT string_agg(c::text, ',' ORDER BY id) FROM customer c), '')
                  UNION ALL SELECT 'loan:' || coalesce((SELECT string_agg(l::text, ',' ORDER BY id) FROM loan l), '')
                  UNION ALL SELECT 'installment:' || coalesce((SELECT string_agg(i::text, ',' ORDER BY id) FROM installment i), '')
                  UNION ALL SELECT 'dispute:' || coalesce((SELECT string_agg(d::text, ',' ORDER BY id) FROM dispute d), '')
                  UNION ALL SELECT 'kyc:' || coalesce((SELECT string_agg(k::text, ',' ORDER BY customer_id) FROM customer_kyc k), '')
                ) x""");
    }

    @Test
    void cleanupRestoresExactState() {
        String before = fingerprint(ds);
        Rowsmith rs = Rowsmith.builder(ds).build();
        rs.create(LOAN, l -> l.with(INSTALLMENT, 3).with(DISPUTE, 2));
        rs.createMany(CUSTOMER, 3, c -> c.with(CUSTOMER_KYC, 1));
        assertThat(fingerprint(ds)).isNotEqualTo(before);
        rs.cleanup();
        assertThat(fingerprint(ds)).isEqualTo(before);
    }

    @Test
    void cleanupIsRefusedWhenForeignRowsReferenceOursAndDeletesNothing() {
        Rowsmith rs = Rowsmith.builder(ds).build();
        Row loan = rs.create(LOAN, l -> l.set(LOAN.STATUS, LoanStatus.ACTIVE));
        Pg.exec(ds, "INSERT INTO dispute (loan_id, status, reason) VALUES (" + loan.id() + ", 'OPEN', 'made by app code')");
        String before = fingerprint(ds);
        assertThatThrownBy(rs::cleanup)
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CLEANUP_BLOCKED))
                .hasMessageContaining("nothing was deleted")
                .hasMessageContaining("dispute");
        assertThat(fingerprint(ds)).isEqualTo(before);
    }

    @Test
    void cleanupSkipsRowsAlreadyRemovedByTheTest() {
        String before = fingerprint(ds);
        Rowsmith rs = Rowsmith.builder(ds).build();
        Row loan = rs.create(LOAN, l -> l.set(LOAN.STATUS, LoanStatus.ACTIVE).with(DISPUTE, 1));
        Pg.exec(ds, "DELETE FROM dispute WHERE loan_id = " + loan.id());
        rs.cleanup();
        assertThat(fingerprint(ds)).isEqualTo(before);
    }

    @Test
    void joinsSpringTransactionSoRollbackCleansUp() {
        String before = fingerprint(ds);
        Rowsmith rs = Rowsmith.builder(ds).build();
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.executeWithoutResult(status -> {
            rs.create(LOAN, l -> l.with(INSTALLMENT, 2));
            assertThat(fingerprint(ds)).as("other connections do not see uncommitted rows").isEqualTo(before);
            status.setRollbackOnly();
        });
        assertThat(fingerprint(ds)).isEqualTo(before);
        rs.cleanup();
        assertThat(fingerprint(ds)).isEqualTo(before);
    }

    @Test
    void aFailedCreateInsideASpringTransactionLeavesTheTransactionUsable() {
        DataSource guarded = Pg.withSchema("""
                CREATE TABLE parent (id serial PRIMARY KEY);
                CREATE TABLE child (id serial PRIMARY KEY, parent_id int NOT NULL REFERENCES parent, note text NOT NULL);
                CREATE FUNCTION refuse() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'children are refused'; END $$;
                CREATE TRIGGER refuse BEFORE INSERT ON child FOR EACH ROW WHEN (NEW.note = 'refused') EXECUTE FUNCTION refuse();
                """);
        Rowsmith rs = Rowsmith.of(guarded);
        JdbcTemplate jdbc = new JdbcTemplate(guarded);
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(guarded));
        tx.executeWithoutResult(status -> {
            Row kept = rs.create("parent");
            assertThatThrownBy(() -> rs.create("child", c -> c.set("note", "refused")))
                    .isInstanceOf(RowsmithException.class)
                    .hasMessageContaining("children are refused");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM parent", Long.class)).as("only the failed call's parent is gone").isEqualTo(1);
            rs.create("child", c -> c.set("parent_id", kept).set("note", "accepted"));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM child", Long.class)).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(Pg.count(guarded, "parent")).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = Propagation.class, names = {"NOT_SUPPORTED", "SUPPORTS"})
    void withoutAnActualSpringTransactionRowsAreCommittedAndCleanedUp(Propagation propagation) {
        String before = fingerprint(ds);
        Rowsmith rs = Rowsmith.builder(ds).build();
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setPropagationBehavior(propagation.value());
        tx.executeWithoutResult(status -> rs.create(LOAN, l -> l.with(INSTALLMENT, 2)));
        assertThat(fingerprint(ds)).isNotEqualTo(before);
        rs.cleanup();
        assertThat(fingerprint(ds)).isEqualTo(before);
    }

    @Test
    void failedCreateLeavesNothingBehind() {
        String before = fingerprint(ds);
        Rowsmith rs = Rowsmith.builder(ds).build();
        assertThatThrownBy(() -> rs.create("installment", i -> i.set("loan_id", 424242)))
                .isInstanceOf(RowsmithException.class);
        assertThat(fingerprint(ds)).isEqualTo(before);
    }

    @Test
    void childrenKeyedByTimestampsAreRecognisedAsOurs() {
        DataSource events = Pg.withSchema("""
                CREATE TABLE source (id serial PRIMARY KEY);
                CREATE TABLE event (at timestamptz, local_at timestamp, source_id int NOT NULL REFERENCES source,
                                    PRIMARY KEY (at, local_at));
                """);
        Rowsmith rs = Rowsmith.of(events);
        rs.create("event", e -> e.set("at", "2030-01-02T03:04:05.123456Z").set("local_at", "2030-01-02T03:04:05"));
        rs.createMany("event", 2);
        rs.cleanup();
        assertThat(Pg.count(events, "event")).isZero();
        assertThat(Pg.count(events, "source")).isZero();
    }

    @Test
    void cleanupInsideAnOpenTransactionIsRefusedAndDeletesNothing() {
        Rowsmith rs = Rowsmith.builder(ds).build();
        rs.create(LOAN, l -> l.with(INSTALLMENT, 1));
        String before = fingerprint(ds);
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.executeWithoutResult(status -> assertThatThrownBy(rs::cleanup)
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CLEANUP_BLOCKED))
                .hasMessageContaining("a transaction is open"));
        assertThat(fingerprint(ds)).isEqualTo(before);
        rs.cleanup();
        assertThat(fingerprint(ds)).isNotEqualTo(before);
    }

    static final String DEFERRABLE_CYCLE = """
            CREATE TABLE a (id serial PRIMARY KEY, b_id int NOT NULL);
            CREATE TABLE b (id serial PRIMARY KEY, a_id int NOT NULL REFERENCES a DEFERRABLE INITIALLY IMMEDIATE);
            ALTER TABLE a ADD FOREIGN KEY (b_id) REFERENCES b DEFERRABLE INITIALLY IMMEDIATE;
            """;

    @Test
    void constraintsDeferredForACreateAreImmediateAgainInsideASpringTransaction() {
        DataSource cycle = Pg.withSchema(DEFERRABLE_CYCLE);
        Rowsmith rs = Rowsmith.builder(cycle).build();
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(cycle));
        tx.executeWithoutResult(status -> {
            rs.create("a");
            assertThatThrownBy(() -> new JdbcTemplate(cycle).update("INSERT INTO b (a_id) VALUES (424242)"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("b_a_id_fkey");
            status.setRollbackOnly();
        });
        assertThat(Pg.count(cycle, "a")).isZero();
    }

    @Test
    void rowsCreatedThroughADeferrableCycleAreCleanedUp() {
        DataSource cycle = Pg.withSchema(DEFERRABLE_CYCLE);
        Rowsmith rs = Rowsmith.builder(cycle).build();
        rs.create("a");
        rs.create("b");
        rs.cleanup();
        assertThat(Pg.count(cycle, "a")).isZero();
        assertThat(Pg.count(cycle, "b")).isZero();
    }

    @Test
    void aFailingDeleteIsADatabaseErrorAndDeletesNothing() {
        DataSource guarded = Pg.withSchema("""
                CREATE TABLE parent (id serial PRIMARY KEY);
                CREATE TABLE child (id serial PRIMARY KEY, parent_id int NOT NULL REFERENCES parent);
                CREATE FUNCTION refuse() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'parents are kept'; END $$;
                CREATE TRIGGER keep BEFORE DELETE ON parent FOR EACH ROW EXECUTE FUNCTION refuse();
                """);
        Rowsmith rs = Rowsmith.builder(guarded).build();
        rs.create("child");
        assertThatThrownBy(rs::cleanup)
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.DATABASE_ERROR))
                .hasMessageContaining("parents are kept");
        assertThat(Pg.count(guarded, "child")).isEqualTo(1);
        assertThat(Pg.count(guarded, "parent")).isEqualTo(1);
    }
}

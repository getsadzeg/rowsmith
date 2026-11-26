package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static rowsmith.lending.Db.CUSTOMER;
import static rowsmith.lending.Db.CUSTOMER_KYC;
import static rowsmith.lending.Db.DISPUTE;
import static rowsmith.lending.Db.INSTALLMENT;
import static rowsmith.lending.Db.LOAN;
import static rowsmith.lending.Db.MERCHANT;
import static rowsmith.lending.Db.STORE;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rowsmith.lending.Db.DisputeStatus;
import rowsmith.lending.Db.LoanStatus;

class PlannerTest {

    static DataSource ds;

    @BeforeAll
    static void schema() {
        ds = Pg.withResource("lending.sql");
    }

    Rowsmith rowsmith() {
        return Rowsmith.builder(ds).build();
    }

    static void active(RowSpec loan) {
        loan.set(LOAN.STATUS, LoanStatus.ACTIVE);
    }

    @Test
    void ordersParentsBeforeChildrenDeterministically() {
        Plan plan = rowsmith().plan(INSTALLMENT);
        assertThat(plan.rows()).extracting(Plan.PlannedRow::table)
                .containsExactly("customer", "merchant", "store", "loan", "installment");
        assertThat(rowsmith().plan(INSTALLMENT).sql()).isEqualTo(plan.sql());
    }

    @Test
    void sharesParentsWithinOneCall() {
        Rowsmith rs = rowsmith();
        List<Row> loans = rs.createMany(LOAN, 3, PlannerTest::active);
        assertThat(loans).extracting(l -> l.get(LOAN.CUSTOMER_ID)).containsOnly(loans.get(0).get(LOAN.CUSTOMER_ID));
        assertThat(loans).extracting(l -> l.get(LOAN.STORE_ID)).containsOnly(loans.get(0).get(LOAN.STORE_ID));
        assertThat(loans.get(0).parent(STORE).parent(MERCHANT)).isNotNull();
    }

    @Test
    void separateCallsGetSeparateParents() {
        Rowsmith rs = rowsmith();
        Row a = rs.create(LOAN, PlannerTest::active);
        Row b = rs.create(LOAN, PlannerTest::active);
        assertThat(a.get(LOAN.CUSTOMER_ID)).isNotEqualTo(b.get(LOAN.CUSTOMER_ID));
    }

    @Test
    void reusesExistingRows() {
        Rowsmith rs = rowsmith();
        Row customer = rs.create(CUSTOMER);
        Row loan = rs.create(LOAN, l -> l.use(customer).apply(PlannerTest::active));
        assertThat(loan.get(LOAN.CUSTOMER_ID)).isEqualTo(customer.get(CUSTOMER.ID));
        Row other = rs.create(LOAN, l -> l.set(LOAN.CUSTOMER_ID, customer).apply(PlannerTest::active));
        assertThat(other.get(LOAN.CUSTOMER_ID)).isEqualTo(customer.get(CUSTOMER.ID));
        Row found = rs.find(CUSTOMER, customer.id());
        assertThat(found.get(CUSTOMER.EMAIL)).isEqualTo(customer.get(CUSTOMER.EMAIL));
    }

    @Test
    void createsChildrenAndLinksThem() {
        Row loan = rowsmith().create(LOAN, l -> l
                .with(INSTALLMENT, 3)
                .with(DISPUTE, 1, d -> d.set(DISPUTE.STATUS, DisputeStatus.OPEN)));
        assertThat(loan.children(INSTALLMENT)).hasSize(3)
                .extracting(i -> i.get(INSTALLMENT.SEQ)).containsExactly(1, 2, 3);
        assertThat(loan.child(DISPUTE).get(DISPUTE.STATUS)).isEqualTo(DisputeStatus.OPEN);
        assertThat(loan.child(DISPUTE).parent(LOAN)).isSameAs(loan);
    }

    @Test
    void partialUniqueIndexForcesFreshParents() {
        List<Row> loans = rowsmith().createMany(LOAN, 2, l -> l.set(LOAN.STATUS, LoanStatus.PENDING));
        assertThat(loans.get(0).get(LOAN.CUSTOMER_ID)).isNotEqualTo(loans.get(1).get(LOAN.CUSTOMER_ID));
    }

    @Test
    void partialUniqueViolationIsExplainedBeforeTheDatabase() {
        Rowsmith rs = rowsmith();
        Row customer = rs.create(CUSTOMER);
        assertThatThrownBy(() -> rs.createMany(LOAN, 2, l -> l.use(customer).set(LOAN.STATUS, LoanStatus.PENDING)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.UNIQUE_VIOLATION))
                .hasMessageContaining("one_pending_loan_per_customer")
                .hasMessageContaining("WHERE")
                .hasMessageContaining("PlannerTest");
    }

    @Test
    void oneToOneChildrenAreLimited() {
        assertThatThrownBy(() -> rowsmith().create(CUSTOMER, c -> c.with(CUSTOMER_KYC, 2)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.UNIQUE_VIOLATION))
                .hasMessageContaining("can have at most one customer_kyc");
        Row c = rowsmith().create(CUSTOMER, s -> s.with(CUSTOMER_KYC, 1));
        assertThat(c.child(CUSTOMER_KYC).get(CUSTOMER_KYC.CUSTOMER_ID)).isEqualTo(c.get(CUSTOMER.ID));
    }

    @Test
    void withOnUnrelatedTableExplainsWhy() {
        assertThatThrownBy(() -> rowsmith().create(MERCHANT, m -> m.with(INSTALLMENT, 1)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_RELATIONSHIP))
                .hasMessageContaining("installment has no foreign key to merchant")
                .hasMessageContaining("loan_id -> loan");
    }

    @Test
    void requiredCycleIsAClearError() {
        DataSource cyc = Pg.withResource("tricky/10_required_cycle.sql");
        assertThatThrownBy(() -> Rowsmith.builder(cyc).build().create("c"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.REQUIRED_FK_CYCLE))
                .hasMessageContaining("a -> b -> a")
                .hasMessageContaining("DEFERRABLE");
    }

    @Test
    void deferrableCycleIsInsertedInOneTransaction() {
        DataSource def = Pg.withResource("tricky/08_deferrable.sql");
        Rowsmith rs = Rowsmith.builder(def).build();
        Row team = rs.create("team");
        Row captain = team.parent("captain_id");
        assertThat(captain.get("team_id")).isEqualTo(team.id());
    }

    @Test
    void deferredKeyFromASequenceDefaultIsTakenFromTheDefault() {
        DataSource def = Pg.withSchema("""
                CREATE SEQUENCE code_seq START 500;
                CREATE TABLE board (id serial PRIMARY KEY, owner_code text NOT NULL);
                CREATE TABLE member (code text PRIMARY KEY DEFAULT 'M-' || nextval('code_seq'), board_id int NOT NULL REFERENCES board(id));
                ALTER TABLE board ADD FOREIGN KEY (owner_code) REFERENCES member(code) DEFERRABLE INITIALLY DEFERRED;
                """);
        Row board = Rowsmith.builder(def).build().create("board");
        assertThat(board.getString("owner_code")).isEqualTo("M-500");
        assertThat(board.parent("owner_code").get("board_id")).isEqualTo(board.id());
    }

    @Test
    void deferredUuidKeyIsTakenFromTheDefault() {
        DataSource def = Pg.withSchema("""
                CREATE FUNCTION member_key() RETURNS uuid VOLATILE LANGUAGE sql AS $$ SELECT '00000000-0000-0000-0000-000000000042'::uuid $$;
                CREATE TABLE board (id serial PRIMARY KEY, owner_id uuid NOT NULL);
                CREATE TABLE member (id uuid PRIMARY KEY DEFAULT member_key(), board_id int NOT NULL REFERENCES board(id));
                ALTER TABLE board ADD FOREIGN KEY (owner_id) REFERENCES member(id) DEFERRABLE INITIALLY DEFERRED;
                """);
        Row board = Rowsmith.builder(def).build().create("board");
        assertThat(board.getString("owner_id")).isEqualTo("00000000-0000-0000-0000-000000000042");
        assertThat(board.parent("owner_id").get("board_id")).isEqualTo(board.id());
    }

    @Test
    void dryRunDoesNotInsertAndMatchesCreate() {
        Rowsmith rs = rowsmith();
        long before = Pg.count(ds, "loan");
        Plan plan = rs.plan(LOAN, l -> l.apply(PlannerTest::active).with(INSTALLMENT, 2));
        assertThat(Pg.count(ds, "loan")).isEqualTo(before);
        assertThat(plan.counts()).containsEntry("installment", 2L).containsEntry("loan", 1L);
        assertThat(plan.sql().get(plan.sql().size() - 1)).startsWith("INSERT INTO \"public\".\"installment\"").contains("<loan#1.id>");
        Row loan = rs.create(LOAN, l -> l.apply(PlannerTest::active).with(INSTALLMENT, 2));
        assertThat(loan.get(LOAN.PRINCIPAL).toPlainString())
                .isEqualTo(String.valueOf(plan.rows().stream().filter(r -> r.table().equals("loan")).findFirst().orElseThrow().values().get("principal")));
    }

    @Test
    void aFailedCreateLeavesNoKeysBehind() {
        Rowsmith rs = rowsmith();
        assertThatThrownBy(() -> rs.create("customer", c -> c.set("email", "retry@example.com")
                .with("loan", l -> l.set("status", "ACTIVE").set("store_id", 999_999))))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.FOREIGN_KEY_VIOLATION));
        Row customer = rs.create("customer", c -> c.set("email", "retry@example.com"));
        assertThat(customer.getString("email")).isEqualTo("retry@example.com");
    }

    @Test
    void aKeyOfTheForeignKeyAndAnExpressionAllowsSeveralChildren() {
        DataSource db = Pg.withSchema("""
                CREATE TABLE folder (id serial PRIMARY KEY);
                CREATE TABLE note (id serial PRIMARY KEY, folder_id int NOT NULL REFERENCES folder(id), title text NOT NULL);
                CREATE UNIQUE INDEX note_title ON note (folder_id, lower(title));
                """);
        Row folder = Rowsmith.builder(db).build().create("folder", f -> f.with("note", 3));
        assertThat(folder.children("note")).hasSize(3);
    }

    @Test
    void childrenAreFoundByPlainAndQualifiedName() {
        Rowsmith rs = rowsmith();
        Row loan = rs.create(LOAN, l -> l.apply(PlannerTest::active).with("public.installment", 2));
        assertThat(loan.children("installment")).hasSize(2).isEqualTo(loan.children("public.installment")).isEqualTo(loan.children(INSTALLMENT));
        Row customer = rs.create(CUSTOMER, c -> c.with("customer_kyc", 1));
        assertThat(customer.child("public.customer_kyc")).isSameAs(customer.child("customer_kyc"));
    }

    @Test
    void childrenAreFoundByTheNamesWithAccepts() {
        DataSource db = Pg.withSchema("""
                CREATE SCHEMA other;
                CREATE TABLE other.folder (id serial PRIMARY KEY);
                CREATE TABLE other.note (id serial PRIMARY KEY, folder_id int NOT NULL REFERENCES other.folder);
                CREATE TABLE "Folder" (id serial PRIMARY KEY);
                CREATE TABLE "Note" (id serial PRIMARY KEY, folder_id int NOT NULL REFERENCES "Folder");
                """);
        Rowsmith rs = Rowsmith.of(db);
        Row outsideSearchPath = rs.create("folder", f -> f.with("note", 2));
        assertThat(outsideSearchPath.children("note")).hasSize(2).isEqualTo(outsideSearchPath.children("other.note"));
        Row quoted = rs.create("\"Folder\"", f -> f.with("\"Note\"", 1));
        assertThat(quoted.child("\"Note\"")).isSameAs(quoted.child("public.Note"));
    }

    @Test
    void plannedKeyFollowsTheParentItIsCopiedFrom() {
        DataSource db = Pg.withSchema("""
                CREATE TABLE account (id int PRIMARY KEY);
                CREATE TABLE profile (account_id int PRIMARY KEY REFERENCES account(id));
                CREATE TABLE avatar (id serial PRIMARY KEY, profile_id int NOT NULL REFERENCES profile(account_id));
                """);
        Plan plan = Rowsmith.builder(db).build().plan("avatar");
        Object accountId = plan.rows().get(0).values().get("id");
        assertThat(plan.rows()).extracting(Plan.PlannedRow::table).containsExactly("account", "profile", "avatar");
        assertThat(plan.rows().get(2).values()).containsEntry("profile_id", accountId);
    }
}

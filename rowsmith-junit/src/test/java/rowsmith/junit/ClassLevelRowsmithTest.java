package rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static rowsmith.junit.ContainerFieldTest.count;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import rowsmith.Row;
import rowsmith.Rowsmith;

@RowsmithTest
class ClassLevelRowsmithTest {

    static final PostgreSQLContainer PG = ContainerFieldTest.PG;

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PerClassInstance {

        Rowsmith rowsmith;
        final List<Rowsmith> resolved = new ArrayList<>();
        Row merchant;

        @BeforeAll
        void fixtures(Rowsmith rs) {
            resolved.add(rs);
            merchant = rs.create("merchant");
        }

        @Test
        @Order(1)
        void firstTestGetsItsOwnRowsmith() {
            assertThat(resolved).doesNotContain(rowsmith);
            resolved.add(rowsmith);
            rowsmith.create("store", s -> s.set("merchant_id", merchant));
            assertThat(count("store")).isEqualTo(1);
        }

        @Test
        @Order(2)
        void laterTestsGetAFreshRowsmithWhileTheClassFixturesStay() {
            assertThat(resolved).doesNotContain(rowsmith);
            assertThat(count("store")).isZero();
            assertThat(count("merchant")).isEqualTo(1);
            rowsmith.create("store", s -> s.set("merchant_id", merchant));
        }

        @Test
        @Order(3)
        void eachTestsRowsAreCleanedAfterIt() {
            assertThat(count("store")).isZero();
            assertThat(count("merchant")).isEqualTo(1);
        }
    }

    @AfterAll
    static void theClassFixturesWereCleanedAfterTheClass() {
        assertThat(count("store")).isZero();
        assertThat(count("merchant")).isZero();
    }
}

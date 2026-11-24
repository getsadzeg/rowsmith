package io.github.rowsmith.junit;

import static io.github.rowsmith.junit.ContainerFieldTest.count;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.rowsmith.Rowsmith;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.postgresql.PostgreSQLContainer;

@RowsmithTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConstructorInjectionTest {

    static final PostgreSQLContainer PG = ContainerFieldTest.PG;

    private final Rowsmith rowsmith;

    ConstructorInjectionTest(Rowsmith rowsmith) {
        this.rowsmith = rowsmith;
    }

    @Test
    @Order(1)
    void createsAMerchant(Rowsmith parameter) {
        assertThat(parameter).isSameAs(rowsmith);
        rowsmith.create("merchant");
        assertThat(count("merchant")).isEqualTo(1);
    }

    @Test
    @Order(2)
    void theFirstTestsRowsAreCleanedAfterIt() {
        assertThat(count("merchant")).isZero();
        rowsmith.create("merchant");
    }

    @AfterAll
    static void theSecondTestsRowsAreCleanedToo() {
        assertThat(count("merchant")).isZero();
    }
}

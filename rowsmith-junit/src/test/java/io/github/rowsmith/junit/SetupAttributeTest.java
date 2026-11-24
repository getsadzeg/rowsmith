package io.github.rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rowsmith.Column;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.RowsmithSetup;
import io.github.rowsmith.Table;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

@RowsmithTest(setup = SetupAttributeTest.Conventions.class)
class SetupAttributeTest {

    static final PostgreSQLContainer PG = ContainerFieldTest.PG;

    static final class Customer extends Table {
        final Column<String> EMAIL = column("email", String.class);
        final Column<String> SEGMENT = column("segment", String.class);

        Customer() {
            super("public", "customer");
        }
    }

    static final Customer CUSTOMER = new Customer();

    public static class Conventions implements RowsmithSetup {
        @Override
        public void configure(Config c) {
            c.value(CUSTOMER.EMAIL, n -> "customer" + n + "@example.com");
            c.value("customer", "segment", n -> n % 2 == 0 ? "SMB" : "RETAIL");
        }
    }

    @Test
    void valuePatternsFromTheSetupClass(Rowsmith rowsmith) {
        List<Row> customers = rowsmith.createMany(CUSTOMER, 2);
        assertThat(customers).extracting(c -> c.get(CUSTOMER.EMAIL)).containsExactly("customer1@example.com", "customer2@example.com");
        assertThat(customers).extracting(c -> c.get(CUSTOMER.SEGMENT)).containsExactly("RETAIL", "SMB");
    }

    @Test
    void rowNumbersStartOverInEachTest(Rowsmith rowsmith) {
        assertThat(rowsmith.create(CUSTOMER).get(CUSTOMER.EMAIL)).isEqualTo("customer1@example.com");
    }
}

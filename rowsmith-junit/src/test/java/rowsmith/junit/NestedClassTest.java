package rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static rowsmith.junit.SetupAttributeTest.CUSTOMER;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import rowsmith.Rowsmith;

@RowsmithTest(setup = SetupAttributeTest.Conventions.class)
class NestedClassTest {

    static final PostgreSQLContainer PG = ContainerFieldTest.PG;

    Rowsmith outerField;

    @Nested
    class Inner {
        @Test
        void seesTheEnclosingSettingsContainerAndField(Rowsmith rowsmith) {
            assertThat(outerField).isSameAs(rowsmith);
            assertThat(rowsmith.create(CUSTOMER).get(CUSTOMER.EMAIL)).isEqualTo("customer1@example.com");
        }
    }
}

package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class SchemaReadingTest {

    @Test
    void checkExpressionsComeWithoutTheirClauses() {
        DataSource ds = Pg.withSchema("""
            CREATE DOMAIN positive AS int CHECK (VALUE > 10);
            CREATE TABLE stock (id serial PRIMARY KEY, qty int NOT NULL, level positive NOT NULL);
            ALTER TABLE stock ADD CONSTRAINT stock_qty_check CHECK (qty > 5) NO INHERIT NOT VALID;
            """);
        Rowsmith rs = Rowsmith.of(ds);
        assertThat(rs.describe("stock")).contains("check stock_qty_check: (qty > 5)\n");
        Row row = rs.create("stock");
        assertThat(row.getInt("qty")).isGreaterThan(5);
        assertThat(row.getInt("level")).isGreaterThan(10);
    }

    @Test
    void negativeNumericScale() {
        DataSource ds = Pg.freshDatabase();
        assumeTrue(Pg.queryLong(ds, "SHOW server_version_num") >= 150000, "numeric(p,-s) needs PostgreSQL 15");
        Pg.exec(ds, "CREATE TABLE budget (id serial PRIMARY KEY, amount numeric(5,-2) NOT NULL CHECK (amount > 100))");
        for (Row row : Rowsmith.of(ds).createMany("budget", 3)) {
            BigDecimal amount = row.getBigDecimal("amount");
            assertThat(amount).isGreaterThan(BigDecimal.valueOf(100)).isLessThanOrEqualTo(BigDecimal.valueOf(9_999_900));
            assertThat(amount.remainder(BigDecimal.valueOf(100))).isZero();
        }
    }

    @Test
    void columnNamesAreExact() {
        DataSource ds = Pg.withSchema("CREATE TABLE item (id serial PRIMARY KEY, qty int NOT NULL, \"Label\" text)");
        Rowsmith rs = Rowsmith.of(ds);
        assertThat(rs.create("item", i -> i.set("Label", "x")).getString("Label")).isEqualTo("x");
        assertThatThrownBy(() -> rs.create("item", i -> i.set("QTY", 7)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_COLUMN);
                    assertThat(e.getMessage()).contains("Columns: id, qty, Label");
                });
    }
}

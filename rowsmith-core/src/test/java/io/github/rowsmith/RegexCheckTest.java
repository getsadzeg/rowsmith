package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class RegexCheckTest {

    @Test
    void semverCheckAddedByLaterMigration() {
        DataSource ds = Pg.withSchema("""
            CREATE TABLE credit_decision (
              id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              model_version varchar(16) NOT NULL CHECK (model_version ~ '^v[0-9]+\\.[0-9]+$'));
            ALTER TABLE credit_decision DROP CONSTRAINT credit_decision_model_version_check;
            ALTER TABLE credit_decision ADD CONSTRAINT credit_decision_model_version_chk
              CHECK (model_version ~ '^v[0-9]+\\.[0-9]+\\.[0-9]+$') NOT VALID;
            """);
        Rowsmith rs = Rowsmith.of(ds);
        for (Row row : rs.createMany("credit_decision", 20, d -> {})) {
            assertThat((String) row.get("model_version")).matches("v[0-9]+\\.[0-9]+\\.[0-9]+");
        }
    }

    @Test
    void theRowNumberChoosesTheMatch() {
        DataSource ds = Pg.withSchema("""
            CREATE TABLE voucher (
              id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              code text NOT NULL CHECK (code ~ '^[A-Z]{4}-[0-9]{4}$'));
            """);
        List<String> first = Rowsmith.of(ds).planMany("voucher", 2, v -> {}).sql();
        assertThat(Rowsmith.of(ds).planMany("voucher", 2, v -> {}).sql()).isEqualTo(first);
        assertThat(first.get(0)).isNotEqualTo(first.get(1));
    }

    @Test
    void likePatternsFitTheColumn() {
        DataSource ds = Pg.withSchema("""
            CREATE TABLE code (
              id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              short varchar(1) NOT NULL CHECK (short LIKE 'r%'),
              padded text NOT NULL CHECK (padded LIKE 'INV-%' AND length(padded) = 10));
            """);
        Row row = Rowsmith.of(ds).create("code");
        assertThat(row.get("short")).isEqualTo("r");
        assertThat(row.get("padded")).isEqualTo("INV-000001");
    }

    @Test
    void commonFormats() {
        DataSource ds = Pg.withSchema("""
            CREATE TABLE account (
              id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              iban_like text NOT NULL CHECK (iban_like ~ '^[A-Z]{2}[0-9]{2}[A-Z0-9]{8,20}$'),
              code char(6) NOT NULL CHECK (code ~ '^[A-Z]{3}-[0-9]{2}$'),
              phone varchar(20) NOT NULL CHECK (phone ~ '^\\+[1-9][0-9]{7,14}$'),
              email text NOT NULL CHECK (email ~* '^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$'));
            """);
        Rowsmith.of(ds).createMany("account", 20, a -> {});
        assertThat(Pg.count(ds, "account")).isEqualTo(20);
    }

    @Test
    void aSubstringFarIntoTheValueIsMatched() {
        DataSource ds = Pg.withSchema("""
            CREATE TABLE ref_code (
              id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              code text NOT NULL CHECK (substring(code, 80, 2) = 'ZZ'));
            """);
        assertThat(Rowsmith.of(ds).create("ref_code").getString("code").substring(79, 81)).isEqualTo("ZZ");
    }
}

package io.github.rowsmith.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rowsmith.internal.sql.SqlExpr;
import io.github.rowsmith.internal.sql.SqlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SqlExpressionTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "((amount > (0)::numeric))",
        "((status = ANY (ARRAY['OPEN'::text, 'CLOSED'::text])))",
        "(((status)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying])::text[])))",
        "(((term_months >= 1) AND (term_months <= 60)))",
        "((length(TRIM(BOTH FROM name)) > 0))",
        "(((paid_at IS NULL) OR (status = 'PAID'::text)))",
        "((num_nonnulls(card_token, iban) = 1))",
        "((ends_at > starts_at))",
        "((email ~~* '%@%'::text))",
        "((code)::text ~ '^[A-Z]+$'::text)",
        "((VALUE >= 1901) AND (VALUE <= 2155))",
        "((launched_on >= '2020-01-01'::date))",
        "((upper((code)::text) = (code)::text))",
        "(NOT ((a IS NULL) AND (b IS NULL)))",
        "nextval('invoice_id_seq'::regclass)",
        "'{}'::jsonb",
        "(now() + '30 days'::interval)",
        "((x)::double precision >= (0.1)::double precision)",
        "(created_at <= CURRENT_TIMESTAMP)",
        "((color <> ALL (ARRAY[''::text, 'none'::text])))"
    })
    void parsesPostgresOutput(String text) {
        assertThat(SqlParser.tryParse(text)).as(text).isNotNull();
    }

    @Test
    void unsupportedSyntaxIsOpaqueNotFatal() {
        assertThat(SqlParser.tryParse("((data ? 'tags'::text))")).isNull();
    }

    @Test
    void printsWhatItParsedAsSqlPostgresReadsTheSameWay() {
        assertThat(SqlExpr.sql(SqlParser.parse("((sku)::character varying(10) ~~ 'SKU-%'::text)")))
                .isEqualTo("((\"sku\")::character varying(10)) ~~ ((E'SKU-%')::text)");
        assertThat(SqlExpr.sql(SqlParser.parse("(app.valid_tag(name) AND (d <= CURRENT_DATE))")))
                .isEqualTo("(app.valid_tag((\"name\"))) AND ((\"d\") <= (CURRENT_DATE))");
        assertThat(SqlExpr.sql(SqlParser.parse("((VALUE)[1] < (\"substring\"((VALUE)::text, 2, 1))::integer)")))
                .isEqualTo("((\"value\")[1]) < ((substring(((\"value\")::text), (2), (1)))::integer)");
    }
}

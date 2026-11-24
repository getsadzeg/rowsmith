package io.github.rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rowsmith.RelativeTime;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@SpringJUnitConfig(SpringTransactionalTest.Config.class)
@Transactional
@RowsmithTest
class SpringTransactionalTest {

    static final LocalDate APP_TODAY = LocalDate.of(2030, 6, 15);

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setURL(ContainerFieldTest.PG.getJdbcUrl());
            ds.setUser(ContainerFieldTest.PG.getUsername());
            ds.setPassword(ContainerFieldTest.PG.getPassword());
            return ds;
        }

        @Bean
        PlatformTransactionManager tx(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }

        @Bean
        JdbcTemplate jdbc(DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(APP_TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void rowsAreVisibleInsideTheTestTransactionAndUseTheAppClock(Rowsmith rowsmith) {
        assertRowsAreVisibleAndUseTheAppClock(rowsmith);
    }

    private void assertRowsAreVisibleAndUseTheAppClock(Rowsmith rowsmith) {
        Row inst = rowsmith.create("installment", i -> i.set("due_date", RelativeTime.today().minusDays(11)));
        assertThat(inst.getLocalDate("due_date")).isEqualTo(APP_TODAY.minusDays(11));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM installment", Long.class)).isEqualTo(1);
    }

    @Nested
    class InNestedClass {
        @Test
        void usesTheEnclosingContextTransactionAndClock(Rowsmith rowsmith) {
            assertRowsAreVisibleAndUseTheAppClock(rowsmith);
        }
    }

    @AfterAll
    static void rolledBack() {
        assertThat(ContainerFieldTest.count("installment")).isZero();
    }
}

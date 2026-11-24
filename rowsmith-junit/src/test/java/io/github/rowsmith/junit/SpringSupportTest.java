package io.github.rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowsmithException;
import java.time.Clock;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

class SpringSupportTest {

    @Configuration
    static class TwoDataSources {
        @Bean
        DataSource orders() {
            return new PGSimpleDataSource();
        }

        @Bean
        DataSource billing() {
            return new PGSimpleDataSource();
        }
    }

    @Configuration
    static class PrimaryDataSource {
        @Bean
        @Primary
        DataSource orders() {
            return new PGSimpleDataSource();
        }

        @Bean
        DataSource billing() {
            return new PGSimpleDataSource();
        }
    }

    @Configuration
    static class TwoClocks {
        @Bean
        Clock utc() {
            return Clock.system(ZoneOffset.UTC);
        }

        @Bean
        Clock local() {
            return Clock.systemDefaultZone();
        }
    }

    @Configuration
    static class PrimaryClock {
        @Bean
        @Primary
        Clock utc() {
            return Clock.system(ZoneOffset.UTC);
        }

        @Bean
        Clock local() {
            return Clock.systemDefaultZone();
        }
    }

    @Test
    void severalDataSourcesWithoutPrimaryNameTheBeans() {
        try (var ctx = new AnnotationConfigApplicationContext(TwoDataSources.class)) {
            assertThatThrownBy(() -> SpringSupport.dataSource(ctx)).isInstanceOfSatisfying(RowsmithException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCode.NO_DATASOURCE)).hasMessageContaining("orders, billing");
        }
    }

    @Test
    void thePrimaryDataSourceWins() {
        try (var ctx = new AnnotationConfigApplicationContext(PrimaryDataSource.class)) {
            assertThat(SpringSupport.dataSource(ctx)).containsSame(ctx.getBean("orders", DataSource.class));
        }
    }

    @Test
    void severalClocksWithoutPrimaryNameTheBeans() {
        try (var ctx = new AnnotationConfigApplicationContext(TwoClocks.class)) {
            assertThatThrownBy(() -> SpringSupport.clock(ctx)).isInstanceOfSatisfying(RowsmithException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_CONFIG)).hasMessageContaining("utc, local");
        }
    }

    @Test
    void thePrimaryClockWins() {
        try (var ctx = new AnnotationConfigApplicationContext(PrimaryClock.class)) {
            assertThat(SpringSupport.clock(ctx)).containsSame(ctx.getBean("utc", Clock.class));
        }
    }

    @Test
    void noClockBeanMeansNoClock() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.refresh();
            assertThat(SpringSupport.clock(ctx)).isEmpty();
        }
    }
}

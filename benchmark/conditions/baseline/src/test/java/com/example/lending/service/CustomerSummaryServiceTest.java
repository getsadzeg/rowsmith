package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lending.TestcontainersConfiguration;
import com.example.lending.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CustomerSummaryServiceTest {

    @Autowired CustomerSummaryService service;
    @Autowired JdbcTemplate jdbc;
    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc);
    }

    @Test
    void summarisesOpenLoansAddressAndKyc() {
        long customer = data.customer();
        data.address(customer, "Austin", true);
        data.kyc(customer, "APPROVED");
        long store = data.store(data.merchant());
        for (int l = 0; l < 2; l++) {
            long loan = data.loan(customer, store, "ACTIVE", new BigDecimal("1200.00"), 12, LocalDate.now().minusMonths(1));
            data.unpaidInstallment(loan, 1, LocalDate.now().plusMonths(1));
            data.unpaidInstallment(loan, 2, LocalDate.now().plusMonths(2));
        }

        CustomerSummaryService.Summary s = service.summary(customer);

        assertThat(s.openLoans()).isEqualTo(2);
        assertThat(s.outstanding()).isEqualByComparingTo("400.00");
        assertThat(s.primaryCity()).isEqualTo("Austin");
        assertThat(s.kycStatus()).isEqualTo("APPROVED");
    }

    @Test
    void customerWithoutLoansOrKyc() {
        long customer = data.customer();

        CustomerSummaryService.Summary s = service.summary(customer);

        assertThat(s.openLoans()).isZero();
        assertThat(s.outstanding()).isEqualByComparingTo("0");
        assertThat(s.primaryCity()).isNull();
        assertThat(s.kycStatus()).isNull();
    }
}

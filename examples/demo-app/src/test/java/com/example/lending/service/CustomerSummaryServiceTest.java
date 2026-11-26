package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import rowsmith.Row;
import rowsmith.Rowsmith;
import rowsmith.junit.RowsmithTest;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class CustomerSummaryServiceTest {

    @Autowired CustomerSummaryService service;
    Rowsmith rowsmith;

    @Test
    void summarisesOpenLoansAddressAndKyc() {
        Row customer = rowsmith.create("customer", c -> c
                .with("address", 1, a -> a.apply(Presets::primaryAddress).set("city", "Austin"))
                .with("kyc_check", 1, k -> k.apply(Presets::approvedKyc))
                .with("loan", 2, l -> l.apply(Presets::activeLoan).set("principal", 1200).set("term_months", 12)
                        .with("installment", 2, i -> i.set("amount", 100))));

        CustomerSummaryService.Summary s = service.summary(customer.getLong("id"));

        assertThat(s.openLoans()).isEqualTo(2);
        assertThat(s.outstanding()).isEqualByComparingTo("400.00"); // 2 loans x 2 installments x 100.00
        assertThat(s.primaryCity()).isEqualTo("Austin");
        assertThat(s.kycStatus()).isEqualTo("APPROVED");
    }

    @Test
    void customerWithoutLoansOrKyc() {
        Row customer = rowsmith.create("customer");

        CustomerSummaryService.Summary s = service.summary(customer.getLong("id"));

        assertThat(s.openLoans()).isZero();
        assertThat(s.outstanding()).isEqualByComparingTo("0");
        assertThat(s.primaryCity()).isNull();
        assertThat(s.kycStatus()).isNull();
    }
}

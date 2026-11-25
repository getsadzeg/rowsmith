package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.junit.RowsmithTest;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Reference solution for benchmark task t5. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class RefundServiceTest {

    @Autowired RefundService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    Row paymentOnPartiallyPaidInstallment() {
        Row installment = rowsmith.create("installment", i -> i.set("amount", 100).set("paid_amount", 60).apply(Presets::partiallyPaidInstallment));
        return rowsmith.create("payment", p -> p.use(installment).set("loan_id", installment.parent("loan")).set("amount", 60));
    }

    Map<String, Object> installment(Row payment) {
        return jdbc.queryForMap("SELECT paid_amount, status FROM installment WHERE id = ?", payment.get("installment_id"));
    }

    String paymentStatus(Row payment) {
        return jdbc.queryForObject("SELECT status FROM payment WHERE id = ?", String.class, payment.getLong("id"));
    }

    @Test
    void partialRefundReducesPaidAmount() {
        Row payment = paymentOnPartiallyPaidInstallment();
        service.refund(payment.getLong("id"), new BigDecimal("20.00"), "goodwill");
        assertThat((BigDecimal) installment(payment).get("paid_amount")).isEqualByComparingTo("40.00");
        assertThat(installment(payment).get("status")).isEqualTo("PARTIALLY_PAID");
        assertThat(paymentStatus(payment)).isEqualTo("PARTIALLY_REFUNDED");
    }

    @Test
    void fullRefundMakesInstallmentUnpaid() {
        Row payment = paymentOnPartiallyPaidInstallment();
        service.refund(payment.getLong("id"), new BigDecimal("60.00"), "error");
        assertThat((BigDecimal) installment(payment).get("paid_amount")).isEqualByComparingTo("0");
        assertThat(installment(payment).get("status")).isEqualTo("UNPAID");
        assertThat(paymentStatus(payment)).isEqualTo("REFUNDED");
    }

    @Test
    void overRefundIsRejected() {
        Row payment = paymentOnPartiallyPaidInstallment();
        service.refund(payment.getLong("id"), new BigDecimal("50.00"), "first");
        assertThatThrownBy(() -> service.refund(payment.getLong("id"), new BigDecimal("10.01"), "too much"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

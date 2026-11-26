package com.example.lending.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "late_fee")
public class LateFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "installment_id")
    private Long installmentId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "assessed_on")
    private LocalDate assessedOn;

    @Column(name = "waived")
    private boolean waived;

    public Long getId() { return id; }

    public Long getInstallmentId() { return installmentId; }
    public void setInstallmentId(Long installmentId) { this.installmentId = installmentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getAssessedOn() { return assessedOn; }
    public void setAssessedOn(LocalDate assessedOn) { this.assessedOn = assessedOn; }
    public boolean isWaived() { return waived; }
    public void setWaived(boolean waived) { this.waived = waived; }
}

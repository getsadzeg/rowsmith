package com.example.lending;

import static io.github.rowsmith.RelativeTime.now;
import static io.github.rowsmith.RelativeTime.today;

import io.github.rowsmith.RowSpec;

/** Reusable row presets for tests: plain methods, applied with {@code apply(Presets::activeLoan)}. */
public final class Presets {

    private Presets() {}

    public static void activeLoan(RowSpec loan) {
        loan.set("status", "ACTIVE").set("originated_on", today().minusMonths(3));
    }

    public static void paidOffLoan(RowSpec loan) {
        loan.set("status", "PAID_OFF").set("originated_on", today().minusMonths(12)).set("closed_on", today().minusMonths(1));
    }

    public static void overdueInstallment(RowSpec installment) {
        installment.set("status", "UNPAID").set("due_date", today().minusDays(15));
    }

    public static void paidInstallment(RowSpec installment) {
        installment.set("status", "PAID").set("paid_at", now().minusDays(1));
    }

    public static void partiallyPaidInstallment(RowSpec installment) {
        installment.set("status", "PARTIALLY_PAID");
    }

    public static void primaryAddress(RowSpec address) {
        address.set("is_primary", true);
    }

    public static void approvedKyc(RowSpec kyc) {
        kyc.set("status", "APPROVED");
    }

    public static void openDispute(RowSpec dispute) {
        dispute.set("status", "OPEN");
    }

    public static void disputeUnderReview(RowSpec dispute) {
        dispute.set("status", "UNDER_REVIEW");
    }

    public static void resolvedDispute(RowSpec dispute) {
        dispute.set("status", "RESOLVED").set("resolved_at", now().minusDays(1));
    }
}

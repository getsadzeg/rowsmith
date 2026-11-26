package com.example.lending.web;

import com.example.lending.service.CustomerSummaryService;
import com.example.lending.service.OverdueReportService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final OverdueReportService overdue;
    private final CustomerSummaryService customers;
    private final Clock clock;

    public ReportController(OverdueReportService overdue, CustomerSummaryService customers, Clock clock) {
        this.overdue = overdue;
        this.customers = customers;
        this.clock = clock;
    }

    @GetMapping("/reports/overdue")
    public List<OverdueReportService.MerchantOverdue> overdue() {
        return overdue.report(LocalDate.now(clock));
    }

    @GetMapping("/customers/{id}/summary")
    public CustomerSummaryService.Summary summary(@PathVariable long id) {
        return customers.summary(id);
    }
}

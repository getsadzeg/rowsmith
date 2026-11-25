package com.example.lending.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LateFeeRepository extends JpaRepository<LateFee, Long> {
}

package com.example.lending.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KycCheckRepository extends JpaRepository<KycCheck, Long> {
}

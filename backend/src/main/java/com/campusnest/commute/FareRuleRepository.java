package com.campusnest.commute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FareRuleRepository extends JpaRepository<FareRule, Long> {

    List<FareRule> findAllByOrderByModeAscUpToKmAsc();
}

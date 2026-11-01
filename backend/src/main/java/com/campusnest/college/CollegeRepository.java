package com.campusnest.college;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {

    List<College> findAllByOrderByNameAsc();
}

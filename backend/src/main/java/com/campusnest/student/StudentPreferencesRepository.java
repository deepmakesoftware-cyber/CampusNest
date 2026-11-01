package com.campusnest.student;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPreferencesRepository extends JpaRepository<StudentPreferences, Long> {
}

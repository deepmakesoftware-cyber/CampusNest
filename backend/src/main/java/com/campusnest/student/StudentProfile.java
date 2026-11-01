package com.campusnest.student;

import com.campusnest.auth.UserAccount;
import com.campusnest.college.College;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Student-only details. Shares its primary key with the user account (one-to-one). */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    // @MapsId: this profile's id IS the user's id.
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Column(length = 80)
    private String course;

    @Column(name = "year_of_study")
    private Short yearOfStudy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    protected StudentProfile() {
    }

    public StudentProfile(UserAccount user) {
        this.user = user;
    }

    public void update(College college, String course, Short yearOfStudy, Gender gender) {
        this.college = college;
        this.course = (course == null || course.isBlank()) ? null : course.trim();
        this.yearOfStudy = yearOfStudy;
        this.gender = gender;
    }

    public Long getUserId() {
        return userId;
    }

    public College getCollege() {
        return college;
    }

    public String getCourse() {
        return course;
    }

    public Short getYearOfStudy() {
        return yearOfStudy;
    }

    public Gender getGender() {
        return gender;
    }
}

package com.campusnest.student;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.college.College;
import com.campusnest.college.CollegeRepository;
import com.campusnest.common.error.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * The logged-in student's own profile. Only STUDENT accounts reach this controller
 * (rule "/api/v1/student/**" in SecurityConfig), and it only ever loads the caller's own row,
 * so one student can never read or change another student's profile.
 */
@RestController
@RequestMapping("/api/v1/student/profile")
public class StudentProfileController {

    private final StudentProfileRepository profiles;
    private final CollegeRepository colleges;

    public StudentProfileController(StudentProfileRepository profiles, CollegeRepository colleges) {
        this.profiles = profiles;
        this.colleges = colleges;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public StudentProfileResponse get(@AuthenticationPrincipal AppUserPrincipal me) {
        return StudentProfileResponse.from(loadOwnProfile(me));
    }

    @PutMapping
    @Transactional
    public StudentProfileResponse update(@AuthenticationPrincipal AppUserPrincipal me,
            @Valid @RequestBody StudentProfileRequest request) {
        College college = null;
        if (request.collegeId() != null) {
            college = colleges.findById(request.collegeId())
                    .orElseThrow(() -> ApiException.badRequest("UNKNOWN_COLLEGE", "The selected college does not exist."));
        }
        StudentProfile profile = loadOwnProfile(me);
        profile.update(college, request.course(), request.yearOfStudy(), request.gender());
        return StudentProfileResponse.from(profile);
    }

    private StudentProfile loadOwnProfile(AppUserPrincipal me) {
        return profiles.findById(me.getId())
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "Student profile not found."));
    }

    public record StudentProfileRequest(
            Long collegeId,
            @Size(max = 80) String course,
            @Min(1) @Max(6) Short yearOfStudy,
            Gender gender) {
    }

    public record StudentProfileResponse(Long collegeId, String collegeName, String course, Short yearOfStudy,
            Gender gender) {

        static StudentProfileResponse from(StudentProfile p) {
            College c = p.getCollege();
            return new StudentProfileResponse(c == null ? null : c.getId(), c == null ? null : c.getName(),
                    p.getCourse(), p.getYearOfStudy(), p.getGender());
        }
    }
}

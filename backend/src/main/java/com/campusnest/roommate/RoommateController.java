package com.campusnest.roommate;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.roommate.RoommateDtos.ConnectionView;
import com.campusnest.roommate.RoommateDtos.MatchView;
import com.campusnest.roommate.RoommateDtos.ProfileRequest;
import com.campusnest.roommate.RoommateDtos.ProfileView;
import com.campusnest.roommate.RoommateDtos.RequestView;
import com.campusnest.roommate.RoommateDtos.SendRequest;

import jakarta.validation.Valid;

/** Roommate matching. STUDENT area only (/api/v1/student/**). */
@RestController
@RequestMapping("/api/v1/student/roommates")
public class RoommateController {

    private final RoommateService service;

    public RoommateController(RoommateService service) {
        this.service = service;
    }

    /** 204 if the student has no roommate profile yet. */
    @GetMapping("/profile")
    public ResponseEntity<ProfileView> getProfile(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.getProfile(me.getId()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/profile")
    public ProfileView saveProfile(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody ProfileRequest request) {
        return service.saveProfile(me.getId(), request);
    }

    @GetMapping("/matches")
    public List<MatchView> matches(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.matches(me.getId());
    }

    @GetMapping("/requests")
    public List<RequestView> requests(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.pendingRequests(me.getId());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView send(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody SendRequest request) {
        return service.sendRequest(me.getId(), request.toUserId(), request.message());
    }

    /** action = accept | decline (recipient) or withdraw (sender). */
    @PostMapping("/requests/{id}/{action}")
    public RequestView respond(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id, @PathVariable String action) {
        return service.respond(id, me.getId(), action);
    }

    @GetMapping("/connections")
    public List<ConnectionView> connections(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.connections(me.getId());
    }

    @PostMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long userId) {
        service.block(me.getId(), userId);
    }
}

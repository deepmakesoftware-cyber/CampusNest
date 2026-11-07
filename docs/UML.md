# CampusNest — UML and System Diagrams

All diagrams describe the system **as implemented** (end of Phase 10) and are written in Mermaid, which GitHub
renders automatically. To export images for the report, paste a diagram into <https://mermaid.live> and download
PNG/SVG.

The **component** and **deployment** diagrams use strict UML 2 notation (interfaces, ports, nodes, artifacts) and
are drawn as SVG with ready-to-submit PNG exports in [docs/diagrams/](diagrams/).

Contents: [1 Use cases](#1-use-case-diagram) · [2 Architecture](#2-architecture--component-diagram) ·
[3 Class diagram](#3-class-diagram-domain-model) · [4 ER diagram](#4-entity-relationship-diagram) ·
[5 State machines](#5-state-machine-diagrams) · [6 Sequence diagrams](#6-sequence-diagrams) ·
[7 Activity diagrams](#7-activity-diagrams) ·
[8 Deployment diagram](#8-deployment-diagram)

---

## 1. Use case diagram

Mermaid has no native use-case notation, so actors are drawn as rounded nodes and use cases as ellipses (stadium
shapes). Every use case below is implemented.

```mermaid
flowchart LR
    Visitor([Visitor])
    Student([Student])
    Owner([Owner])
    Admin([Administrator])

    subgraph CampusNest
        UC1((Browse & search listings))
        UC2((View listing details & reviews))
        UC3((Register / log in))
        UC4((Set preferences))
        UC5((Get ranked recommendations<br/>with score breakdown))
        UC6((Compare cost vs commute))
        UC7((Save listings))
        UC8((Message owner))
        UC9((Review a listing))
        UC10((Find & connect with roommates))
        UC11((Report listing / review / user))
        UC12((Create, edit & submit listing))
        UC13((Update availability))
        UC14((Request identity verification))
        UC15((Review listings: approve / request changes /<br/>reject / suspend / reinstate))
        UC16((Verify owners))
        UC17((Resolve reports, hide reviews))
        UC18((Suspend / reinstate users))
        UC19((Maintain fare rules))
    end

    Visitor --- UC1
    Visitor --- UC2
    Visitor --- UC3
    Student --- UC4
    Student --- UC5
    Student --- UC6
    Student --- UC7
    Student --- UC8
    Student --- UC9
    Student --- UC10
    Student --- UC11
    Owner --- UC12
    Owner --- UC13
    Owner --- UC14
    Owner --- UC8
    Owner --- UC11
    Admin --- UC15
    Admin --- UC16
    Admin --- UC17
    Admin --- UC18
    Admin --- UC19
```

Students and owners can also do everything a visitor can (generalisation, not drawn to keep the diagram readable).

---

## 2. Architecture / component diagram

### 2.1 UML component diagram

![CampusNest UML component diagram](diagrams/component-diagram.png)

Source: [component-diagram.svg](diagrams/component-diagram.svg). The frontend *requires* the REST API that the
backend *provides* (ball-and-socket assembly through ports); the backend requires PostgreSQL (JDBC) and the photo
folder; the map view requires OpenStreetMap tiles.

Direct dependencies between backend modules, from the Java `import` statements (excluding `common`, and `auth`
used for the current user). The diagram draws only the arrows not already implied by other arrows:

| Module | Depends on |
|---|---|
| recommendation | student, engagement, listing, commute, owner, college |
| moderation | engagement, listing, owner |
| roommate | student |
| student | commute, listing (amenities, room types), college |
| engagement | listing |
| commute | listing, college |
| listing | owner, college (the demo-data seeder also uses student, roommate, commute) |
| auth | student, owner (registration creates the profile) |
| owner, college | — |

### 2.2 Request flow overview

```mermaid
flowchart TB
    subgraph Browser
        UI[Next.js pages<br/>React + TypeScript]
    end
    subgraph "Next.js server (port 3000)"
        Rewrites["/api/* rewrite (proxy)"]
    end
    subgraph "Spring Boot API (port 8080) — modular monolith"
        Sec[Security filter chain<br/>session, CSRF, rate limit,<br/>active-account check, role rules]
        Auth[auth]
        Student[student]
        Owner[owner]
        Listing[listing<br/>state machine]
        Eng[engagement<br/>saved, enquiries,<br/>reviews, reports]
        Rec[recommendation<br/>SuitabilityScorer,<br/>comparison]
        Comm[commute<br/>stations, fares]
        Room[roommate<br/>CompatibilityScorer]
        Mod[moderation<br/>admin actions,<br/>verification]
    end
    DB[(PostgreSQL 18<br/>Flyway V1–V7)]
    Files[[Local disk:<br/>listing photos]]
    OSM[OpenStreetMap tiles]

    UI -->|same-origin requests| Rewrites --> Sec
    Sec --> Auth & Student & Owner & Listing & Eng & Rec & Room & Mod
    Rec --> Comm
    Rec --> Eng
    Auth & Student & Owner & Listing & Eng & Rec & Comm & Room & Mod --> DB
    Listing --> Files
    UI -.->|map images only| OSM
```

---

## 3. Class diagram (domain model)

Only the main fields and methods are shown. Arrows are JPA associations.

```mermaid
classDiagram
    class UserAccount {
        Long id
        String email
        String passwordHash
        Role role
        AccountStatus status
        String displayName
        String phone
        suspend(reason)
        reinstate()
    }
    class StudentProfile {
        College college
        String course
        Short yearOfStudy
        Gender gender
    }
    class OwnerProfile {
        OwnerType ownerType
        String businessName
        VerificationStatus verificationStatus
        requestVerification()
        markVerified(admin)
        rejectVerification()
        revokeVerification()
    }
    class StudentPreferences {
        int budgetMax
        int maxCommuteMinutes
        TravelMode commuteMode
        List~String~ roomChoices
        List~String~ mustHaveAmenities
        List~String~ niceToHaveAmenities
    }
    class College {
        String name
        String shortName
        BigDecimal latitude
        BigDecimal longitude
    }
    class Listing {
        Long id
        String title
        PropertyType propertyType
        GenderPolicy genderPolicy
        BigDecimal latitude
        BigDecimal longitude
        ListingStatus status
        Instant lastConfirmedAt
        boolean sampleData
        long version
        updateDetails() bool material
        totalBedsAvailable()
    }
    class RoomOption {
        RoomType roomType
        int monthlyRent
        int bedsTotal
        int bedsAvailable
    }
    class ListingCharge {
        ChargeType chargeType
        int amount
        ChargeFrequency frequency
        boolean mandatory
    }
    class ListingAmenity {
        Amenity amenity
        boolean available
    }
    class ListingImage {
        String storageKey
        String contentType
    }
    class ListingStatusChange {
        ListingStatus fromStatus
        ListingStatus toStatus
        String reason
    }
    class ListingLifecycle {
        <<utility>>
        canTransition(from, to)$
        requireTransition(from, to)$
    }
    class Enquiry {
        boolean studentSharesContact
        boolean ownerSharesContact
        involves(userId)
    }
    class EnquiryMessage {
        String body
    }
    class Review {
        int rating
        String comment
        String status
    }
    class Report {
        TargetType targetType
        Long targetId
        Reason reason
        Status status
    }
    class RoommateProfile {
        boolean discoverable
        int budgetMin
        int budgetMax
        SleepSchedule sleepSchedule
        Smoking smoking
        int cleanliness
    }
    class RoommateRequest {
        RequestStatus status
    }
    class SuitabilityScorer {
        <<pure function>>
        score(Preferences, ListingFacts, now)$ Result
    }
    class CompatibilityScorer {
        <<pure function>>
        score(Side a, Side b)$ Result
    }

    UserAccount "1" -- "0..1" StudentProfile
    UserAccount "1" -- "0..1" OwnerProfile
    UserAccount "1" -- "0..1" StudentPreferences
    UserAccount "1" -- "0..1" RoommateProfile
    StudentProfile "*" --> "0..1" College
    UserAccount "1" -- "*" Listing : owns
    Listing "1" *-- "1..*" RoomOption
    Listing "1" *-- "*" ListingCharge
    Listing "1" *-- "*" ListingAmenity
    Listing "1" *-- "*" ListingImage
    Listing "1" -- "*" ListingStatusChange
    Listing ..> ListingLifecycle : status changes checked by
    Listing "1" -- "*" Enquiry
    Enquiry "1" *-- "*" EnquiryMessage
    Listing "1" -- "*" Review
    UserAccount "1" -- "*" Report : files
    UserAccount "1" -- "*" RoommateRequest : sends/receives
    SuitabilityScorer ..> Listing : facts from
    CompatibilityScorer ..> RoommateProfile : facts from
```

---

## 4. Entity relationship diagram

The full, current schema (Flyway V1–V7). Column details are in [DATABASE.md](DATABASE.md).

```mermaid
erDiagram
    users ||--o| student_profiles : "has"
    users ||--o| owner_profiles : "has"
    users ||--o| student_preferences : "sets"
    users ||--o| roommate_profiles : "opts in"
    colleges ||--o{ student_profiles : "attended by"
    users ||--o{ listings : "owns"
    listings ||--|{ room_options : "offers"
    listings ||--o{ listing_charges : "discloses"
    listings ||--o{ listing_amenities : "answers"
    amenities ||--o{ listing_amenities : "used in"
    listings ||--o{ listing_images : "shows"
    listings ||--o{ listing_status_history : "audited by"
    users ||--o{ saved_listings : "saves"
    listings ||--o{ saved_listings : "saved as"
    listings ||--o{ enquiries : "receives"
    users ||--o{ enquiries : "starts"
    enquiries ||--|{ enquiry_messages : "contains"
    listings ||--o{ reviews : "receives"
    users ||--o{ reviews : "writes"
    users ||--o{ reports : "files"
    users ||--o{ roommate_requests : "sends / receives"
    users ||--o{ roommate_blocks : "blocks"
    users ||--o{ verification_records : "owner checked"
    stations }o--o{ colleges : "nearest station (computed)"
    fare_rules }o--o{ listings : "cost estimates (computed)"
```

---

## 5. State machine diagrams

### 5.1 Listing lifecycle (implemented in `ListingLifecycle.java`)

```mermaid
stateDiagram-v2
    [*] --> DRAFT : owner creates
    DRAFT --> PENDING_VERIFICATION : owner submits (checklist complete)
    PENDING_VERIFICATION --> PUBLISHED_AVAILABLE : admin approves, beds free
    PENDING_VERIFICATION --> PUBLISHED_UNAVAILABLE : admin approves, no beds
    PENDING_VERIFICATION --> CHANGES_REQUESTED : admin (reason)
    PENDING_VERIFICATION --> REJECTED : admin (reason)
    CHANGES_REQUESTED --> PENDING_VERIFICATION : owner resubmits
    PUBLISHED_AVAILABLE --> PUBLISHED_UNAVAILABLE : owner sets free beds = 0
    PUBLISHED_UNAVAILABLE --> PUBLISHED_AVAILABLE : owner frees a bed
    PUBLISHED_AVAILABLE --> PENDING_VERIFICATION : owner material edit
    PUBLISHED_UNAVAILABLE --> PENDING_VERIFICATION : owner material edit
    PUBLISHED_AVAILABLE --> SUSPENDED : admin (reason)
    PUBLISHED_UNAVAILABLE --> SUSPENDED : admin (reason)
    SUSPENDED --> PUBLISHED_UNAVAILABLE : admin reinstates
    DRAFT --> [*] : owner deletes draft
    REJECTED --> [*]
```

### 5.2 Owner identity verification

```mermaid
stateDiagram-v2
    [*] --> UNVERIFIED
    UNVERIFIED --> PENDING : owner requests
    REJECTED --> PENDING : owner requests again
    PENDING --> VERIFIED : admin (method + notes)
    PENDING --> REJECTED : admin (method + notes)
    UNVERIFIED --> VERIFIED : admin verifies directly
    REJECTED --> VERIFIED : admin verifies after a new check
    VERIFIED --> UNVERIFIED : admin revokes (method + notes)
```

### 5.3 Roommate request

```mermaid
stateDiagram-v2
    [*] --> PENDING : student sends (compatible, < 10/day)
    PENDING --> ACCEPTED : recipient accepts → contact details shared
    PENDING --> DECLINED : recipient declines, or recipient blocks
    PENDING --> WITHDRAWN : sender withdraws, or sender blocks
    ACCEPTED --> WITHDRAWN : original sender blocks
    ACCEPTED --> DECLINED : original recipient blocks
    ACCEPTED --> [*]
    DECLINED --> [*]
    WITHDRAWN --> [*]
```

### 5.4 Report

```mermaid
stateDiagram-v2
    [*] --> OPEN : user reports
    OPEN --> UPHELD : admin (note) — listing loses trust points for 90 days
    OPEN --> DISMISSED : admin (note)
    UPHELD --> [*]
    DISMISSED --> [*]
```

---

## 6. Sequence diagrams

### 6.1 Login (Spring Security form login, session cookie, CSRF)

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Next.js (browser)
    participant SEC as Spring Security filters
    participant UDS as AppUserDetailsService
    participant DB as PostgreSQL
    U->>FE: enter email + password
    FE->>SEC: GET /api/v1/auth/csrf
    SEC-->>FE: Set-Cookie XSRF-TOKEN
    FE->>SEC: POST /api/v1/auth/login (form) + X-XSRF-TOKEN header
    SEC->>SEC: CSRF check, LoginRateLimitFilter (≤ 5 failures / 15 min)
    SEC->>UDS: loadUserByUsername(email)
    UDS->>DB: select user by lower-case email
    DB-->>UDS: user row (BCrypt hash, role, status)
    UDS-->>SEC: AppUserPrincipal
    SEC->>SEC: BCrypt check, suspended? → reject
    SEC->>SEC: new session id (fixation protection), rotate CSRF token
    SEC-->>FE: 200 user JSON + Set-Cookie JSESSIONID (HttpOnly)
    FE-->>U: redirect to role dashboard
```

### 6.2 Ranked recommendations (FR-1)

```mermaid
sequenceDiagram
    actor S as Student
    participant FE as Next.js
    participant RC as RecommendationController
    participant RS as RecommendationService
    participant CS as CommuteService
    participant TS as TrustSignals
    participant SC as SuitabilityScorer
    participant DB as PostgreSQL
    S->>FE: open "Recommended"
    FE->>RC: GET /api/v1/student/recommendations
    RC->>RS: recommend(studentId)
    RS->>DB: profile (college), preferences
    RS->>CS: context() — stations + fare rules
    RS->>TS: snapshot() — reviews & upheld reports (2 grouped queries)
    RS->>DB: published listings of active owners
    loop each listing
        RS->>CS: estimate(listing, college, mode)
        RS->>SC: score(preferences, facts)
        SC-->>RS: eligible? + 5 part scores + reasons
    end
    RS->>RS: drop excluded (count reasons), sort by score
    RS-->>FE: ranked items + breakdowns + excluded counts
    FE-->>S: list with score badges and "Why this score?"
```

### 6.3 Owner submits, admin reviews (FR-3)

```mermaid
sequenceDiagram
    actor O as Owner
    actor A as Admin
    participant OLS as OwnerListingService
    participant LCS as ListingLifecycleService
    participant ALS as AdminListingService
    participant DB as PostgreSQL
    O->>OLS: POST /owner/listings/{id}/submit
    OLS->>DB: load listing WHERE id AND owner_id (anti-IDOR)
    OLS->>OLS: SubmissionChecklist — missing items? → 400 with list
    OLS->>LCS: transition(DRAFT → PENDING_VERIFICATION)
    LCS->>DB: update status + insert history row
    A->>ALS: POST /admin/listings/{id}/request-changes {reason}
    ALS->>LCS: transition(PENDING → CHANGES_REQUESTED, reason)
    O->>OLS: PUT listing (fix) then submit again
    A->>ALS: POST /admin/listings/{id}/approve
    ALS->>LCS: approve → PUBLISHED_AVAILABLE (beds free)
    LCS->>DB: status + published_at + history row
```

### 6.4 Roommate request with privacy (FR-2)

```mermaid
sequenceDiagram
    actor A as Student A
    actor B as Student B
    participant RS as RoommateService
    participant CS as CompatibilityScorer
    A->>RS: GET /student/roommates/matches
    RS->>CS: score(A, each discoverable student)
    RS-->>A: short names, habits, %, matches/differences (no contacts)
    A->>RS: POST /student/roommates/requests {toUserId: B}
    RS->>RS: B discoverable? not blocked? no dealbreaker? < 10/day? none active?
    RS-->>A: 201 PENDING
    B->>RS: POST /student/roommates/requests/{id}/accept
    RS-->>B: ACCEPTED
    A->>RS: GET /student/roommates/connections
    RS-->>A: B's full name, email, phone (only now)
```

---

## 7. Activity diagrams

### 7.1 Student finds accommodation

```mermaid
flowchart TD
    A([Start]) --> B[Register as student]
    B --> C[Add college in Account]
    C --> D[Set preferences:<br/>budget, commute, rooms, amenities]
    D --> E[Open Recommended]
    E --> F{Any listings?}
    F -- no --> G[Relax preferences] --> E
    F -- yes --> H[Read score breakdown]
    H --> I[+ Compare 2–4 listings]
    I --> J[Compare cost vs commute]
    J --> K{Shortlist?}
    K -- yes --> L[Save listing]
    L --> M[Message owner<br/>optionally share contact]
    M --> N[Visit in person]
    N --> O[Review the listing]
    O --> P([End])
    K -- no --> E
```

### 7.2 Owner lists a property

```mermaid
flowchart TD
    A([Start]) --> B[Register as owner]
    B --> C[Create draft with title]
    C --> D[Fill details, location pin,<br/>rooms, charges, amenities]
    D --> E[Upload photos]
    E --> F{Checklist complete?}
    F -- no --> D
    F -- yes --> G[Submit for review]
    G --> H{Admin decision}
    H -- changes requested --> I[Read admin message] --> D
    H -- rejected --> Z([End: locked])
    H -- approved --> J[Published]
    J --> K[Confirm availability regularly]
    K --> L{Material edit?}
    L -- yes --> G
    L -- no --> K
```

---

## 8. Deployment diagram

![CampusNest UML deployment diagram](diagrams/deployment-diagram.png)

Source: [deployment-diagram.svg](diagrams/deployment-diagram.svg). Shows the demonstration setup: the browser on the
user's device talks HTTP to the Next.js server (port 3000), which forwards `/api/*` to the Spring Boot jar on the
JVM (port 8080); the backend reaches PostgreSQL in a Docker container over JDBC (port 5432, bound to 127.0.0.1)
and stores photos in the `uploads/` folder. Production changes are listed in [DEPLOYMENT.md](DEPLOYMENT.md).

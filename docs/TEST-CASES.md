# CampusNest — Test Cases

Every row below is an **automated test that exists and passes** (last full run 2026-11-02: **165 tests, 0 failures**,
`cd backend && .\mvnw.cmd test`). Integration tests run against a real PostgreSQL 18 in Docker (Testcontainers).

Types: **N** normal · **B** boundary · **I** invalid input · **U** unauthorized / forbidden · **P** privacy · **NF** non-functional.

## FR-1 — Accommodation Suitability Score

| ID | Scenario | Type | Expected | Test |
|---|---|---|---|---|
| S-01 | Cost within budget | N | Budget part 100, explanation "within your ₹… budget" | `SuitabilityScorerTest.excellentBudgetMatchScoresFullBudgetPoints` |
| S-02 | 15% over budget | N | Budget part 50 | `PoorBudget.fifteenPercentOverBudgetLosesHalfTheBudgetPoints` |
| S-03 | Mandatory charges added to rent | N | ₹9,500 + ₹800 = ₹10,300 → 90 | `PoorBudget.mandatoryChargesCountTowardsTheBudget` |
| S-04 | Exactly 30% over | B | Shown, budget part 0 | `PoorBudget.exactlyThirtyPercentOverIsShownWithZeroBudgetPoints` |
| S-05 | 30% + ₹1 over | B | Excluded: OVER_BUDGET | `PoorBudget.moreThanThirtyPercentOverIsExcluded` |
| S-06 | Commute ≤ half the limit | N | Commute part 100 | `commuteWithinHalfTheLimitIsExcellent` |
| S-07 | Commute curve 20/30/40/50/60/90 min (limit 40) | B | 100/85/70/35/0/0 | `commuteCurveBoundaries` |
| S-08 | Room type not wanted / 2nd choice | N | 0 / 70 | `roomTypeMismatchScoresZeroAndSecondChoiceSeventy` |
| S-09 | Several room types | N | Best-scoring room chosen | `bestRoomIsChosenWhenAListingHasSeveral` |
| S-10 | Amenities not specified | N | Half credit; completeness 38% | `unknownAmenitiesGetHalfCreditAndLowerDataCompleteness` |
| S-11 | Amenities confirmed missing | N | No credit, listed as "Not available" | `amenitiesConfirmedMissingScoreZeroForThatItem` |
| S-12 | Unverified owner | N | Trust 35 points lower | `unverifiedOwnerLosesTheVerificationTrustPoints` |
| S-13 | Availability 20 / 45 days old / never | B | Freshness 15 / 0 / 0 | `staleOrMissingAvailabilityLosesFreshnessPoints` |
| S-14 | No free beds | N | Excluded: FULL | `fullListingsAreExcluded` |
| S-15 | Gender policy conflict / unknown gender | N | Excluded / shown with note | `genderRestrictedListingsAreExcludedOnlyWhenGendersConflict` |
| S-16 | Total equals weighted sum of shown parts | N | 99 = Σ(w × s) / 100 | `totalIsTheWeightedSumOfTheDisplayedParts` |
| S-17 | One 5★ review | N | Rating 18/25 (Bayesian) | `reviewsMoveTheRatingButOneReviewCannotDominate` |
| S-18 | Band thresholds | B | 85/84/70/69/50/49 | `bandsMatchTheDocumentedThresholds` |
| S-19 | API: no college / no preferences | I | 409 COLLEGE_REQUIRED / PREFERENCES_REQUIRED | `RecommendationApiTest.recommendationsNeedCollegeAndPreferences` |
| S-20 | API: real listings ranked, totals hand-checked (90 / 65), over-budget explained | N | Correct order and numbers | `RecommendationApiTest.listingsAreScoredRankedAndExplained` |
| S-21 | API: owner / anonymous | U | 403 / 401 | `RecommendationApiTest.onlyStudentsGetRecommendations` |
| S-22 | Invalid preferences | I | 400 with field errors; unknown amenity | `RecommendationApiTest.invalidPreferencesAreRejected` |

## FR-2 — Roommate matching

| ID | Scenario | Type | Expected | Test |
|---|---|---|---|---|
| R-01 | Identical habits | N | 100%, "Strong match", no differences | `CompatibilityScorerTest.identicalPreferencesAreAHundredPercentMatch` |
| R-02 | Partially compatible | N | 45% (hand-calculated), matches + differences | `partiallyCompatibleStudentsGetAMixedScoreWithReasons` |
| R-03 | Budgets don't overlap | N | Ruled out: BUDGET | `budgetsThatDoNotOverlapAreRuledOut` |
| R-04 | Smoking dealbreaker either way; outdoor smoker without dealbreaker | N | Ruled out / 50 points | `smokingDealbreakerRespectsEitherStudent` |
| R-05 | Same-gender-only with other / unknown gender | N | Ruled out | `sameGenderOnlyNeedsBothGendersKnownAndEqual` |
| R-06 | Optional answer missing | B | Factor skipped, weights rebalanced | `missingOptionalAnswerIsSkippedAndWeightsRebalanced` |
| R-07 | Symmetry | N | Same % both ways | `scoreIsTheSameWhicheverStudentIsViewing` |
| R-08 | Name shortening | P | "Asha K." | `namesAreShortenedForPrivacy` |
| R-09 | No / hidden own profile | I | 409 ROOMMATE_PROFILE_REQUIRED | `RoommateApiTest.matchesNeedYourOwnVisibleProfile` |
| R-10 | Match shows no surname, email or phone | P | Absent from response | `compatibleStudentsSeeAnExplainedMatchWithoutPrivateDetails` |
| R-11 | Dealbreaker blocks match and request | N | Hidden; 409 NOT_COMPATIBLE | `dealbreakersHideMatchesAndBlockRequests` |
| R-12 | Contacts only after acceptance; duplicates; only recipient accepts | P/U | As described | `contactDetailsAreSharedOnlyAfterTheRequestIsAccepted` |
| R-13 | Decline, withdraw, outsiders | U | Statuses; 404 for outsiders; 409 when closed | `requestsCanBeDeclinedOrWithdrawnAndOutsidersCannotTouchThem` |
| R-14 | Block | P | Hidden both ways, requests stopped | `blockingHidesBothWaysAndStopsRequests` |
| R-15 | Invalid profile, self-request, owner | I/U | 400 / 400 / 403 | `invalidProfilesAndWrongRolesAreRejected` |

## FR-3 — Listing verification and trust

| ID | Scenario | Type | Expected | Test |
|---|---|---|---|---|
| L-01 | 12 allowed state transitions | N | Allowed | `ListingLifecycleTest.allowedTransitions` |
| L-02 | 8 forbidden transitions (e.g. draft → published) | I | Forbidden, 409 | `ListingLifecycleTest.forbiddenTransitions`, `forbiddenTransitionThrowsConflict` |
| L-03 | Rejected is final; owner edit rules | B | No exits; editable only outside review | `rejectedIsTerminal`, `ownerCanEditOnlyOutsideReviewAndModeration` |
| L-04 | Draft shows checklist; incomplete submit; outside Mumbai | I | 400 LISTING_INCOMPLETE with fields | `ListingWorkflowTest` (3 tests) |
| L-05 | Submit → locked → approve → public photo; history of 3 | N | As described | `submitThenApprovePublishesListingAndRecordsHistory` |
| L-06 | Approve with no free beds | B | PUBLISHED_UNAVAILABLE | `listingWithNoFreeBedsIsPublishedAsUnavailable` |
| L-07 | Request changes → owner sees reason → resubmit | N | As described | `requestChangesShowsReasonToOwnerWhoCanFixAndResubmit` |
| L-08 | Admin decision without reason | I | 400 | `adminDecisionsThatAffectTheOwnerRequireAReason` |
| L-09 | Rejected listing | N | Owner edit / submit → 409 | `rejectedListingIsLockedForever` |
| L-10 | Admin can't see drafts; approve twice | U/I | 404 / 409 | `adminCannotSeeDraftsOrApproveTwice` |
| L-11 | Availability updates switch status, freshness updated, beds > total | N/I | As described / 400 | `availabilityUpdatesSwitchBetweenAvailableAndUnavailable` |
| L-12 | Rent change vs title change after publication | N | Stays published / back to review | `rentChangeStaysPublishedButTitleChangeNeedsNewReview` |
| L-13 | Suspend → photos hidden → reinstate as unavailable | N | As described | `suspendedListingHidesPhotosAndReturnsAsUnavailableWhenReinstated` |
| L-14 | Reviews only after enquiry; labelled "stay not verified"; one per student | N/U | As described | `TrustApiTest.onlyStudentsWhoEnquiredCanReviewAndReviewsAreLabelledUnverified` |
| L-15 | Hidden review stays hidden after edit | N | count 0 | `hiddenReviewsDisappearAndEditingDoesNotUnhideThem` |
| L-16 | Reviews and upheld report change trust | N | 56 → 58 → 48 (hand-calculated) | `reviewsAndUpheldReportsChangeTheTrustPartOfTheScore` |
| L-17 | Report validation, duplicates, admin resolution once | I/U | 404 / 400 / 409 / 403 | `reportsAreValidatedDeduplicatedAndResolvedOnceByAdmins` |
| L-18 | Owner verification request → decide → public status → revoke | N/U/I | As described | `ownerVerificationIsRequestedDecidedRecordedAndShownPublicly` |
| L-19 | Admin overview | N/U | Numbers / 403 for student | `adminOverviewCountsWhatNeedsAttention` |

## FR-4 — Cost versus commute

| ID | Scenario | Type | Expected | Test |
|---|---|---|---|---|
| C-01 | Auto fare minimum (0.5 / 1.5 km) | B | ₹27 | `FareCalculatorTest.autoMinimumFareCoversFirstOneAndAHalfKilometres` |
| C-02 | Auto per-km (3 km, 8.2 km) | N | ₹54, ₹149 | `autoChargesPerKilometreAfterTheMinimum` |
| C-03 | Bus slab edges (5 / 5.1 / 25 / 25.1 km) | B | ₹10 / ₹15 / ₹35 / not available | `busUsesTheSmallestSlabThatCoversTheDistance` |
| C-04 | Train pass without/with entered fare | N | Not available / slab price | `trainPassIsNotAvailableUntilAFareIsEntered` |
| C-05 | No rules at all | B | Not available (never guessed) | `noRulesMeansNoFares` |
| C-06 | Train legs explained; same station; one-stop hop; too far to walk | N/B | As described | `CommuteModelTest` (6) |
| C-07 | Near vs far listing: walk suggested vs auto; train cost unknown; totals hand-checked | N | As described | `ComparisonApiTest.nearbyAndFarListingsShowTheCostVersusCommuteTradeOff` |
| C-08 | Suggestion with unknown cost | B | Total hidden ("Incomplete") | `totalIsHiddenWhenTheChosenModeHasNoKnownCost` |
| C-09 | 1 / duplicate / draft / 40 days / owner / no college | I/U | 400 / 400 / 404 / 400 / 403 / 409 | `compareNeedsTwoToFourVisibleListingsACollegeAndAStudent` |
| C-10 | Fare rules public; admin-only changes; invalid rule | U/I | 403 / 400 | `fareRulesArePublicAndOnlyAdminsCanChangeThem` |

## FR-5 — Privacy and role-based access

| ID | Scenario | Type | Expected | Test |
|---|---|---|---|---|
| A-01 | Register student/owner; BCrypt hash; no password in response | N/P | 201, `{bcrypt}` hash | `AuthFlowTest.registerStudentCreatesAccountAndProfileWithHashedPassword` |
| A-02 | Duplicate email (case-insensitive); register as admin; bad fields; unknown role | I | 409 / 400 / 400 / 400 | `AuthFlowTest` (4 tests) |
| A-03 | Request without CSRF token | U | 403 CSRF_TOKEN_INVALID | `stateChangingRequestWithoutCsrfTokenIsForbidden` |
| A-04 | Login → me → logout → me | N | 200 / 200 / 204 / 401 | `loginThenMeThenLogout` |
| A-05 | Wrong password vs unknown email | P | Identical 401 responses | `wrongPasswordAndUnknownEmailGetTheSameAnswer` |
| A-06 | 5 failed logins | NF | 6th attempt 429 | `loginIsBlockedAfterFiveFailedAttempts`, `LoginAttemptServiceTest` (5) |
| A-07 | Role access matrix (anonymous / student / owner / admin × 3 areas) | U | 401 / 403 / 200 | `RoleAccessTest` (4 tests) |
| A-08 | Student calls admin suspend directly | U | 403 | `studentCannotSuspendUsersByCallingAdminEndpointDirectly` |
| A-09 | Owner tries to self-verify | U | Status stays UNVERIFIED | `ownerCannotMarkThemselvesVerified` |
| A-10 | Suspended user: session ends, login refused, reinstate works | U | 401 ACCOUNT_SUSPENDED | `suspendedUserIsLoggedOutImmediatelyAndCannotLogInUntilReinstated` |
| A-11 | Owner B touches owner A's listing (get/edit/submit/delete/upload) | U | 404 | `ownersCannotTouchEachOthersListings` |
| A-12 | Draft photos | P | Owner only | `draftPhotosAreVisibleOnlyToTheirOwner` |
| A-13 | Public listing has no owner phone/email | P | Absent | `publicDetailNeverExposesOwnerContactDetails` |
| A-14 | Enquiry outsiders (other student, other owner, admin) | P/U | 404 | `outsidersCannotReadOrWriteAThread` |
| A-15 | Contact sharing opt-in in enquiries | P | Email only after opt-in | `enquiryThreadWithOptInContactSharing` |
| A-16 | Upload non-image; EXIF/GPS stripping; size and dimension limits | I/P | 400 / metadata removed | `ImageProcessorTest` (6), `nonImageUploadIsRejected` |

## Discovery, engagement and non-functional

| ID | Scenario | Type | Expected | Test |
|---|---|---|---|---|
| D-01 | Only published listings; suspended owners hidden | N/P | As described | `PublicDiscoveryTest` (2 tests) |
| D-02 | 11 filter combinations incl. distance and free beds | N/B | Counts as expected | `filtersNarrowResultsCorrectly` |
| D-03 | Saved listings idempotent; suspended kept but marked; roles | N/U | As described | `EngagementTest` (3 tests) |
| D-04 | Enquiry needs published listing and message; 15/day limit | I/NF | 404 / 400 / 429 | `EngagementTest` (2 tests) |
| D-05 | Haversine maths | N | Known distances | `GeoDistanceTest` (5) |
| NF-01 | Health endpoint and deny-by-default | N/U | 200 / 401 | `HealthControllerTest` (2) |
| NF-02 | Search with 1,000 listings | NF | < 1 s (measured ~115 ms) | `PerformanceTest.searchWithAThousandListingsIsUnderOneSecond` |
| NF-03 | Recommendations with 1,000 listings | NF | < 1 s (measured ~210 ms) | `PerformanceTest.recommendationsForAThousandListingsAreUnderOneSecond` |
| NF-04 | Whole demonstration journey | N | Passes end to end | `FullJourneyTest.theDemonstrationJourneyWorksEndToEnd` |

## Manual / scripted checks

| ID | Check | How | Last result |
|---|---|---|---|
| M-01 | 22 pages, 29 API calls, 3 cross-role checks through the Next.js proxy | `bash scripts/smoke-test.sh` | 0 failures (2026-11-02) |
| M-02 | Frontend type safety and lint | `npx tsc --noEmit`, `npm run lint` | Clean |
| M-03 | Production build of all pages | `npm run build` | 27 routes built |
| M-04 | Clicking through the UI in a browser | Demo script ([DEMO-SCRIPT.md](DEMO-SCRIPT.md)) | Done by Deep (2026-11-02) up to Phase 7; repeat before the review |

import type { FoodPreference, SharingSize, SleepSchedule, Smoking, StudyEnvironment } from "./types";

export const SLEEP_LABELS: Record<SleepSchedule, string> = {
  EARLY: "Early (asleep by ~10:30 pm)",
  REGULAR: "Regular (around midnight)",
  LATE: "Late (after 1 am)",
};

export const SMOKING_LABELS: Record<Smoking, string> = {
  NON_SMOKER: "I don't smoke",
  OUTDOOR_SMOKER: "I smoke, but only outdoors",
  SMOKER: "I smoke",
};

export const STUDY_LABELS: Record<StudyEnvironment, string> = {
  QUIET: "Quiet",
  MODERATE: "Some noise is fine",
  LIVELY: "Lively, music and chat",
};

export const FOOD_LABELS: Record<FoodPreference, string> = {
  VEG: "Vegetarian",
  EGGETARIAN: "Eggetarian",
  NON_VEG: "Non-vegetarian",
  VEGAN: "Vegan",
  NO_PREFERENCE: "No preference",
};

export const SHARING_LABELS: Record<SharingSize, string> = {
  ONE_ROOMMATE: "One roommate",
  TWO_OR_MORE: "Two or more roommates",
  EITHER: "Either is fine",
};

/** Areas near VJTI students commonly consider. Students can choose several. */
export const LOCALITIES = [
  "Matunga", "King's Circle", "Dadar", "Wadala", "Sion", "Mahim", "Parel", "Chembur", "Kurla", "Ghatkopar",
  "Bandra", "Andheri", "Thane",
];

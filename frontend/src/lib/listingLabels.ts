import type { ChargeFrequency, ChargeType, GenderPolicy, ListingStatus, PropertyType, RoomType } from "./types";

// Human-readable labels for the listing enums. One place, so every page uses the same words.

export const PROPERTY_TYPE_LABELS: Record<PropertyType, string> = {
  PG: "PG (paying guest)",
  HOSTEL: "Private hostel",
  FLAT: "Shared flat",
  ROOM: "Single room",
};

export const GENDER_POLICY_LABELS: Record<GenderPolicy, string> = {
  ANY: "Any gender",
  FEMALE_ONLY: "Women only",
  MALE_ONLY: "Men only",
};

export const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  SINGLE: "Single room",
  DOUBLE: "Double sharing",
  TRIPLE: "Triple sharing",
  DORMITORY: "Dormitory",
};

export const CHARGE_TYPE_LABELS: Record<ChargeType, string> = {
  ELECTRICITY: "Electricity",
  MAINTENANCE: "Maintenance",
  FOOD: "Food / mess",
  WIFI: "Wi-Fi",
  WATER: "Water",
  OTHER: "Other",
};

export const CHARGE_FREQUENCY_LABELS: Record<ChargeFrequency, string> = {
  MONTHLY: "per month",
  ONE_TIME: "one-time",
};

export const STATUS_LABELS: Record<ListingStatus, string> = {
  DRAFT: "Draft",
  PENDING_VERIFICATION: "Under review",
  CHANGES_REQUESTED: "Changes requested",
  PUBLISHED_AVAILABLE: "Published · available",
  PUBLISHED_UNAVAILABLE: "Published · full",
  SUSPENDED: "Suspended",
  REJECTED: "Rejected",
};

/** What each status means for the owner. */
export const STATUS_EXPLANATIONS: Record<ListingStatus, string> = {
  DRAFT: "Only you can see this listing. Complete it and submit it for review.",
  PENDING_VERIFICATION: "A CampusNest administrator is reviewing this listing. You can't edit it until the review is done.",
  CHANGES_REQUESTED: "An administrator asked for changes. Update the listing and submit it again.",
  PUBLISHED_AVAILABLE: "Students can see this listing. Keep availability up to date.",
  PUBLISHED_UNAVAILABLE: "Students can see this listing, marked as having no free beds.",
  SUSPENDED: "This listing has been hidden from students by an administrator.",
  REJECTED: "This listing was not approved and can't be edited or resubmitted.",
};

export const STATUS_BADGE_CLASSES: Record<ListingStatus, string> = {
  DRAFT: "bg-slate-100 text-slate-700",
  PENDING_VERIFICATION: "bg-amber-50 text-amber-800",
  CHANGES_REQUESTED: "bg-orange-50 text-orange-800",
  PUBLISHED_AVAILABLE: "bg-green-50 text-green-800",
  PUBLISHED_UNAVAILABLE: "bg-sky-50 text-sky-800",
  SUSPENDED: "bg-red-50 text-red-800",
  REJECTED: "bg-red-50 text-red-800",
};

const rupees = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

export function formatRupees(amount: number): string {
  return rupees.format(amount);
}

export function formatDate(iso: string | null): string {
  return iso ? new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "—";
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });
}

export function optionsFrom<T extends string>(labels: Record<T, string>): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));
}

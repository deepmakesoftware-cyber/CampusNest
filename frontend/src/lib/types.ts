// TypeScript shapes of the backend's JSON. Keep in sync with the Java records they mirror.

export type Role = "STUDENT" | "OWNER" | "ADMIN";
export type AccountStatus = "ACTIVE" | "SUSPENDED";

/** AccountDtos.UserResponse */
export type CurrentUser = {
  id: number;
  email: string;
  displayName: string;
  phone: string | null;
  role: Role;
};

/** CollegeController.CollegeResponse */
export type College = {
  id: number;
  name: string;
  shortName: string;
  address: string;
  city: string;
  latitude: number;
  longitude: number;
};

export type Gender = "FEMALE" | "MALE" | "OTHER";

/** StudentProfileController.StudentProfileResponse */
export type StudentProfile = {
  collegeId: number | null;
  collegeName: string | null;
  course: string | null;
  yearOfStudy: number | null;
  gender: Gender | null;
};

export type OwnerType = "INDIVIDUAL" | "PG_OPERATOR" | "HOSTEL_OPERATOR";
export type VerificationStatus = "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED";

/** OwnerProfileController.OwnerProfileResponse */
export type OwnerProfile = {
  ownerType: OwnerType;
  businessName: string | null;
  verificationStatus: VerificationStatus;
};

/** AdminUserService.AdminUserView */
export type AdminUser = {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  status: AccountStatus;
  statusReason: string | null;
  createdAt: string;
  lastLoginAt: string | null;
};

/** common.web.PageResponse */
export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

// ---------------- Listings (ListingDtos.java) ----------------

export type ListingStatus =
  | "DRAFT"
  | "PENDING_VERIFICATION"
  | "CHANGES_REQUESTED"
  | "PUBLISHED_AVAILABLE"
  | "PUBLISHED_UNAVAILABLE"
  | "SUSPENDED"
  | "REJECTED";
export type PropertyType = "PG" | "HOSTEL" | "FLAT" | "ROOM";
export type GenderPolicy = "ANY" | "FEMALE_ONLY" | "MALE_ONLY";
export type RoomType = "SINGLE" | "DOUBLE" | "TRIPLE" | "DORMITORY";
export type ChargeType = "ELECTRICITY" | "MAINTENANCE" | "FOOD" | "WIFI" | "WATER" | "OTHER";
export type ChargeFrequency = "MONTHLY" | "ONE_TIME";

export type RoomView = {
  id: number;
  roomType: RoomType;
  sharingCapacity: number;
  monthlyRent: number;
  securityDeposit: number | null;
  bedsTotal: number;
  bedsAvailable: number;
  availabilityUpdatedAt: string | null;
};

export type ChargeView = {
  chargeType: ChargeType;
  description: string | null;
  amount: number;
  frequency: ChargeFrequency;
  mandatory: boolean;
};

/** available: null = the owner has not said. */
export type AmenityView = { amenityId: number; code: string; name: string; core: boolean; available: boolean | null };
export type ImageView = { id: number; url: string; width: number; height: number };
export type StatusChangeView = {
  fromStatus: ListingStatus | null;
  toStatus: ListingStatus;
  reason: string | null;
  at: string;
  actorRole: Role | null;
};

export type ListingDetail = {
  id: number;
  title: string;
  description: string | null;
  propertyType: PropertyType;
  genderPolicy: GenderPolicy;
  addressLine: string | null;
  locality: string | null;
  city: string;
  pincode: string | null;
  latitude: number | null;
  longitude: number | null;
  status: ListingStatus;
  statusReason: string | null;
  submittedAt: string | null;
  publishedAt: string | null;
  lastConfirmedAt: string | null;
  sampleData: boolean;
  rooms: RoomView[];
  charges: ChargeView[];
  amenities: AmenityView[];
  images: ImageView[];
  missingForSubmission: string[];
  ownerCanEdit: boolean;
  history: StatusChangeView[];
};

export type ListingSummary = {
  id: number;
  title: string;
  locality: string | null;
  status: ListingStatus;
  statusReason: string | null;
  minimumRent: number | null;
  roomTypes: number;
  images: number;
  updatedAt: string;
};

export type AdminListingSummary = {
  id: number;
  title: string;
  locality: string | null;
  status: ListingStatus;
  ownerName: string;
  ownerVerification: VerificationStatus;
  minimumRent: number | null;
  submittedAt: string | null;
  updatedAt: string;
};

export type OwnerInfo = {
  id: number;
  displayName: string;
  email: string;
  phone: string | null;
  ownerType: OwnerType | null;
  businessName: string | null;
  verificationStatus: VerificationStatus;
};

export type AdminListingDetail = { listing: ListingDetail; owner: OwnerInfo };

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: "Student",
  OWNER: "Property owner",
  ADMIN: "Administrator",
};

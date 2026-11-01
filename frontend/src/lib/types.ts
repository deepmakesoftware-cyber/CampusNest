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

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: "Student",
  OWNER: "Property owner",
  ADMIN: "Administrator",
};

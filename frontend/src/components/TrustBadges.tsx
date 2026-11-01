import { FRESHNESS_CLASSES, FRESHNESS_LABELS } from "@/lib/listingLabels";
import type { Freshness, VerificationStatus } from "@/lib/types";

// Small, honest trust indicators. The wording is deliberate: "reviewed" and "identity checked",
// never "safe" or "guaranteed" (see docs/ALGORITHMS.md, FR-3).

export function SampleBadge() {
  return (
    <span className="inline-block rounded bg-purple-50 px-1.5 py-0.5 text-xs font-medium text-purple-800" title="Created for demonstration. Not a real property.">
      Sample listing
    </span>
  );
}

export function OwnerCheckedBadge({ status }: { status: VerificationStatus }) {
  if (status !== "VERIFIED") return null;
  return (
    <span
      className="inline-block rounded bg-teal-50 px-1.5 py-0.5 text-xs font-medium text-teal-800"
      title="A CampusNest administrator checked this owner's identity. This is not a guarantee of the property's condition or safety."
    >
      Owner identity checked
    </span>
  );
}

export function FreshnessNote({ freshness }: { freshness: Freshness }) {
  return <span className={`text-xs ${FRESHNESS_CLASSES[freshness]}`}>{FRESHNESS_LABELS[freshness]}</span>;
}

import { STATUS_BADGE_CLASSES, STATUS_LABELS } from "@/lib/listingLabels";
import type { ListingStatus } from "@/lib/types";

export default function ListingStatusBadge({ status }: { status: ListingStatus }) {
  return (
    <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${STATUS_BADGE_CLASSES[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  );
}

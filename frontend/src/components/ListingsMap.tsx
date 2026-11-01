"use client";

import "leaflet/dist/leaflet.css";
import { useEffect, useRef } from "react";
import type { LayerGroup, Map as LeafletMap } from "leaflet";
import { formatRupees } from "@/lib/listingLabels";
import type { ListingCard } from "@/lib/types";

// Map of search results: one dot per listing, plus the college as a square marker.
// Popup content is built with DOM nodes and textContent (never HTML strings), so a listing title can
// never inject HTML/JavaScript into the page (XSS protection).

const TILE_URL = process.env.NEXT_PUBLIC_MAP_TILE_URL ?? "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

type Props = {
  listings: ListingCard[];
  college: { name: string; latitude: number; longitude: number } | null;
};

export default function ListingsMap({ listings, college }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const layerRef = useRef<LayerGroup | null>(null);

  useEffect(() => {
    let cancelled = false;
    import("leaflet").then((mod) => {
      const L = mod.default ?? mod;
      if (cancelled || !containerRef.current) return;
      if (!mapRef.current) {
        mapRef.current = L.map(containerRef.current).setView([19.0222, 72.8562], 12);
        L.tileLayer(TILE_URL, { attribution: ATTRIBUTION, maxZoom: 19 }).addTo(mapRef.current);
      }
      const map = mapRef.current;
      layerRef.current?.remove();
      const layer = L.layerGroup().addTo(map);
      layerRef.current = layer;

      const points: [number, number][] = [];
      if (college) {
        const p: [number, number] = [college.latitude, college.longitude];
        points.push(p);
        L.rectangle([[p[0] - 0.0012, p[1] - 0.0012], [p[0] + 0.0012, p[1] + 0.0012]], { color: "#1e293b", weight: 2, fillOpacity: 0.9 })
          .bindTooltip(college.name)
          .addTo(layer);
      }
      for (const l of listings) {
        const p: [number, number] = [l.latitude, l.longitude];
        points.push(p);
        const popup = document.createElement("div");
        const link = document.createElement("a");
        link.href = `/listings/${l.id}`;
        link.textContent = l.title;
        link.style.fontWeight = "600";
        const info = document.createElement("div");
        info.textContent = `from ${formatRupees(l.fromRent)}/month${l.distanceKm !== null ? ` · ${l.distanceKm} km` : ""}`;
        popup.append(link, info);
        L.circleMarker(p, {
          radius: 8,
          color: l.bedsAvailable > 0 ? "#0f766e" : "#64748b",
          fillColor: l.bedsAvailable > 0 ? "#14b8a6" : "#94a3b8",
          fillOpacity: 0.85,
        })
          .bindPopup(popup)
          .addTo(layer);
      }
      if (points.length > 1) {
        map.fitBounds(L.latLngBounds(points), { padding: [30, 30], maxZoom: 15 });
      } else if (points.length === 1) {
        map.setView(points[0], 14);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [listings, college]);

  useEffect(
    () => () => {
      mapRef.current?.remove();
      mapRef.current = null;
    },
    [],
  );

  return (
    <div
      ref={containerRef}
      role="img"
      aria-label={`Map of ${listings.length} listings${college ? ` around ${college.name}` : ""}`}
      className="relative z-0 h-96 w-full overflow-hidden rounded-lg border border-slate-300"
    />
  );
}

"use client";

import "leaflet/dist/leaflet.css";
import { useEffect, useRef, useState } from "react";
import type { CircleMarker, Map as LeafletMap } from "leaflet";

// A small OpenStreetMap map built directly on Leaflet (BSD-licensed).
// - Editable mode: click the map to set the location (onChange).
// - Read-only mode: just shows the point.
// OSM tile policy: attribution must stay visible; the tile URL is configurable (NEXT_PUBLIC_MAP_TILE_URL)
// so we can switch providers without code changes. Tiles are loaded only for the area being viewed.

const TILE_URL = process.env.NEXT_PUBLIC_MAP_TILE_URL ?? "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const VJTI: [number, number] = [19.022172, 72.856204];

type Props = {
  latitude: number | null;
  longitude: number | null;
  onChange?: (latitude: number, longitude: number) => void;
  /** Accessible description of the map for screen readers. */
  label: string;
};

const round6 = (n: number) => Math.round(n * 1e6) / 1e6;

export default function LocationMap({ latitude, longitude, onChange, label }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const markerRef = useRef<CircleMarker | null>(null);
  const onChangeRef = useRef(onChange);
  const initialView = useRef<[number, number]>(latitude !== null && longitude !== null ? [latitude, longitude] : VJTI);
  const [ready, setReady] = useState(false);
  const editable = Boolean(onChange);

  useEffect(() => {
    onChangeRef.current = onChange;
  });

  // Create the map once. Leaflet needs the browser's window, so it is imported only here (never on the server).
  useEffect(() => {
    let cancelled = false;
    import("leaflet").then((mod) => {
      const L = mod.default ?? mod;
      if (cancelled || !containerRef.current || mapRef.current) return;
      const map = L.map(containerRef.current).setView(initialView.current, 15);
      L.tileLayer(TILE_URL, { attribution: ATTRIBUTION, maxZoom: 19 }).addTo(map);
      if (editable) {
        map.on("click", (e) => onChangeRef.current?.(round6(e.latlng.lat), round6(e.latlng.lng)));
      }
      mapRef.current = map;
      setReady(true);
    });
    return () => {
      cancelled = true;
      mapRef.current?.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, [editable]);

  // Keep the marker in sync with the latitude/longitude props.
  useEffect(() => {
    const map = mapRef.current;
    if (!ready || !map) return;
    import("leaflet").then((mod) => {
      const L = mod.default ?? mod;
      if (latitude === null || longitude === null) {
        markerRef.current?.remove();
        markerRef.current = null;
        return;
      }
      if (!markerRef.current) {
        markerRef.current = L.circleMarker([latitude, longitude], {
          radius: 9,
          color: "#0f766e",
          fillColor: "#14b8a6",
          fillOpacity: 0.8,
        }).addTo(map);
      } else {
        markerRef.current.setLatLng([latitude, longitude]);
      }
      if (!map.getBounds().contains([latitude, longitude])) {
        map.panTo([latitude, longitude]);
      }
    });
  }, [ready, latitude, longitude]);

  return (
    <div
      ref={containerRef}
      role="img"
      aria-label={label}
      className={`relative z-0 h-72 w-full overflow-hidden rounded-lg border border-slate-300 ${editable ? "cursor-crosshair" : ""}`}
    />
  );
}

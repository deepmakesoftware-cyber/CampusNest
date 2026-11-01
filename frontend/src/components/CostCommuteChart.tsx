"use client";

import { useState } from "react";
import { formatRupees } from "@/lib/listingLabels";

// Cost-versus-commute scatter: one dot per compared listing (max 4), x = estimated one-way minutes,
// y = estimated monthly total. One series -> one colour, direct name labels, no legend box.
// The comparison table below the chart is the text alternative.

export type ChartPoint = { id: number; label: string; minutes: number; monthly: number; modeLabel: string };

const W = 640;
const H = 300;
const PAD = { top: 20, right: 24, bottom: 44, left: 72 };
const INK_MUTED = "#64748b"; // slate-500
const GRID = "#e2e8f0"; // slate-200
const MARK = "#0f766e"; // teal-700
const SURFACE = "#ffffff";

function niceMax(value: number, step: number) {
  return Math.max(step, Math.ceil(value / step) * step);
}

export default function CostCommuteChart({ points }: { points: ChartPoint[] }) {
  const [hover, setHover] = useState<number | null>(null);
  if (points.length === 0) return null;

  const xMax = niceMax(Math.max(...points.map((p) => p.minutes)) * 1.15, 10);
  const yMax = niceMax(Math.max(...points.map((p) => p.monthly)) * 1.15, 2000);
  const x = (m: number) => PAD.left + (m / xMax) * (W - PAD.left - PAD.right);
  const y = (r: number) => H - PAD.bottom - (r / yMax) * (H - PAD.top - PAD.bottom);
  const xTicks = Array.from({ length: 5 }, (_, i) => Math.round((xMax / 4) * i));
  const yTicks = Array.from({ length: 5 }, (_, i) => Math.round((yMax / 4) * i));
  const active = points.find((p) => p.id === hover) ?? null;

  return (
    <figure className="relative">
      <svg viewBox={`0 0 ${W} ${H}`} className="h-auto w-full" role="img" aria-label="Estimated monthly cost against estimated commute time for each compared listing">
        {/* recessive grid and axes */}
        {yTicks.map((t) => (
          <g key={`y${t}`}>
            <line x1={PAD.left} x2={W - PAD.right} y1={y(t)} y2={y(t)} stroke={GRID} strokeWidth={1} />
            <text x={PAD.left - 8} y={y(t)} textAnchor="end" dominantBaseline="middle" fontSize={11} fill={INK_MUTED}>
              {formatRupees(t)}
            </text>
          </g>
        ))}
        {xTicks.map((t) => (
          <text key={`x${t}`} x={x(t)} y={H - PAD.bottom + 18} textAnchor="middle" fontSize={11} fill={INK_MUTED}>
            {t} min
          </text>
        ))}
        <text x={(PAD.left + W - PAD.right) / 2} y={H - 6} textAnchor="middle" fontSize={11} fill={INK_MUTED}>
          Estimated one-way commute
        </text>

        {points.map((p) => (
          <g key={p.id}>
            {/* larger invisible hit target than the mark */}
            <circle cx={x(p.minutes)} cy={y(p.monthly)} r={16} fill="transparent" onMouseEnter={() => setHover(p.id)} onMouseLeave={() => setHover(null)} onFocus={() => setHover(p.id)} onBlur={() => setHover(null)} tabIndex={0} aria-label={`${p.label}: ${p.minutes} minutes, ${formatRupees(p.monthly)} per month`} />
            <circle cx={x(p.minutes)} cy={y(p.monthly)} r={hover === p.id ? 7 : 5} fill={MARK} stroke={SURFACE} strokeWidth={2} pointerEvents="none" />
            <text x={x(p.minutes) + 10} y={y(p.monthly) - 8} fontSize={12} fill="#0f172a" pointerEvents="none">
              {p.label}
            </text>
          </g>
        ))}
      </svg>
      {active && (
        <div
          className="pointer-events-none absolute rounded-md border border-slate-200 bg-white px-3 py-2 text-xs shadow-md"
          style={{ left: `${(x(active.minutes) / W) * 100}%`, top: `${(y(active.monthly) / H) * 100}%`, transform: "translate(12px, 12px)" }}
        >
          <div className="font-medium text-slate-900">{active.label}</div>
          <div className="text-slate-600">≈ {active.minutes} min by {active.modeLabel}</div>
          <div className="text-slate-600">≈ {formatRupees(active.monthly)} per month (estimated)</div>
        </div>
      )}
      <figcaption className="mt-1 text-xs text-slate-500">
        Lower-left is cheaper and closer. Both axes are estimates; listings without a known total are not plotted.
      </figcaption>
    </figure>
  );
}

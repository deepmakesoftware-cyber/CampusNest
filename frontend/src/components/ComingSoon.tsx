// Honest placeholder for features that are planned but not built yet.
// Never present these as working functionality.

export default function ComingSoon({ items }: { items: { title: string; phase: string; description: string }[] }) {
  return (
    <section className="rounded-lg border border-dashed border-slate-300 bg-white p-5 sm:p-6">
      <h2 className="text-lg font-semibold text-slate-900">Coming in upcoming releases</h2>
      <p className="mt-1 text-sm text-slate-600">These features are planned and not available yet.</p>
      <ul className="mt-4 space-y-3">
        {items.map((item) => (
          <li key={item.title} className="flex flex-col gap-0.5 sm:flex-row sm:items-baseline sm:gap-3">
            <span className="shrink-0 rounded bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-slate-600">{item.phase}</span>
            <span>
              <span className="font-medium text-slate-900">{item.title}</span>
              <span className="text-slate-600"> — {item.description}</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

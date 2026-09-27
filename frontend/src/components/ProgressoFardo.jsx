export default function ProgressoFardo({ reservado, total, fechado }) {
  const slots = Array.from({ length: total }, (_, i) => i < reservado);
  const pct = Math.round((reservado / total) * 100);

  return (
    <div>
      <div className="flex items-baseline justify-between mb-1.5">
        <span className="text-[11px] tracking-widest2 uppercase text-kraft-200/60">
          Unidades do fardo
        </span>
        <span
          className={`text-sm font-semibold tabular-nums ${
            fechado ? "text-moss" : "text-amber-crate"
          }`}
        >
          {reservado}/{total}
        </span>
      </div>
      <div className="flex gap-[3px]">
        {slots.map((filled, i) => (
          <div
            key={i}
            className={`h-3 flex-1 rounded-[2px] transition-colors duration-300 ${
              filled
                ? fechado
                  ? "bg-moss"
                  : "bg-amber-crate"
                : "bg-cargo-700"
            }`}
            style={{ transitionDelay: `${i * 25}ms` }}
          />
        ))}
      </div>
      <div className="mt-1 text-[11px] text-kraft-200/40">
        {fechado ? "Fardo completo — fechado" : `${pct}% reservado`}
      </div>
    </div>
  );
}

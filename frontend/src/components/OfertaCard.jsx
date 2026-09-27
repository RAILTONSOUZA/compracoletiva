import { useState } from "react";
import ProgressoFardo from "./ProgressoFardo";

export default function OfertaCard({ oferta, onReservar }) {
  const [qtd, setQtd] = useState(1);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  const fechado = oferta.status === "fechada";

  async function confirmar() {
    setErro("");
    setEnviando(true);
    try {
      await onReservar(oferta.id, Number(qtd));
      setQtd(1);
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div
      className={`border rounded-md p-4 bg-cargo-900 ${
        fechado ? "border-moss/40" : "border-cargo-700"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold text-kraft-100 leading-tight">{oferta.produto}</h3>
          {oferta.descricao && (
            <p className="text-xs text-kraft-200/50 mt-0.5">{oferta.descricao}</p>
          )}
        </div>
        <span className="text-xs text-kraft-200/60 whitespace-nowrap tabular-nums">
          R$ {oferta.preco_unitario.toFixed(2)} / un
        </span>
      </div>

      <div className="mt-3">
        <ProgressoFardo
          reservado={oferta.unidades_reservadas}
          total={oferta.unidades_por_fardo}
          fechado={fechado}
        />
      </div>

      {!fechado && (
        <div className="mt-3 flex items-center gap-2">
          <input
            type="number"
            min={1}
            max={oferta.unidades_restantes}
            value={qtd}
            onChange={(e) => setQtd(e.target.value)}
            className="w-20 bg-cargo-800 border border-cargo-600 rounded px-2 py-1.5 text-sm text-kraft-100 focus:outline-none focus:ring-2 focus:ring-amber-crate"
          />
          <button
            onClick={confirmar}
            disabled={enviando || qtd < 1 || qtd > oferta.unidades_restantes}
            className="flex-1 bg-amber-crate text-cargo-950 font-semibold text-sm py-1.5 rounded hover:brightness-110 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            {enviando ? "Reservando..." : `Reservar (R$ ${(qtd * oferta.preco_unitario).toFixed(2)})`}
          </button>
        </div>
      )}
      {erro && <p className="mt-2 text-xs text-rust">{erro}</p>}
    </div>
  );
}

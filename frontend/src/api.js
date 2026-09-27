// 🔗 Cole aqui a URL exata gerada no Google Apps Script (Implantar > App da Web)
const BASE_URL = "https://script.google.com/macros/s/AKfycbyC5_xk7gEklhJ8IsNM8rLkl1zzCWDLe2FFlLPUf1LwW3J0DNTAB0w1SJlTRYVzl4RS/exec";

async function request(action, options = {}) {
  // Prepara a URL enviando a 'action' via query param
  const url = `${BASE_URL}?action=${action}`;
  
  const defaultHeaders = {
    // text/plain previne bloqueios de CORS no envio de payload ao Google Apps Script
    "Content-Type": "text/plain;charset=utf-8", 
  };

  const res = await fetch(url, {
    headers: defaultHeaders,
    ...options,
  });

  const data = await res.json().catch(() => null);

  if (!res.ok || data?.status === "erro") {
    throw new Error(data?.mensagem || data?.detail || "Erro na requisição");
  }

  return data;
}

export const api = {
  // 👥 FUNCIONÁRIOS
  listarFuncionarios: () => request("listarFuncionarios"),
  criarFuncionario: (nome, matricula) =>
    request("criarFuncionario", {
      method: "POST",
      body: JSON.stringify({ nome, matricula }),
    }),

  // 📦 PEDIDOS
  listarPedidos: () => request("listarPedidos"),
  criarPedido: (titulo) =>
    request("criarPedido", {
      method: "POST",
      body: JSON.stringify({ titulo }),
    }),
  detalharPedido: (id) => request(`detalharPedido&id=${id}`),
  fecharPedido: (id) =>
    request("fecharPedido", {
      method: "POST",
      body: JSON.stringify({ id }),
    }),

  // 🏷️ OFERTAS
  listarOfertas: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request(`ofertas${qs ? `&${qs}` : ""}`);
  },
  detalharOferta: (id) => request(`detalharOferta&id=${id}`),
  criarOferta: (pedidoId, payload) =>
    request("criarOferta", {
      method: "POST",
      body: JSON.stringify({ pedido_id: pedidoId, ...payload }),
    }),

  // 🛒 RESERVAS / COMPRAS
  reservar: (ofertaId, funcionarioId, quantidade) =>
    request("reservar", {
      method: "POST",
      body: JSON.stringify({
        oferta_id: ofertaId,
        funcionario_id: funcionarioId,
        quantidade: quantidade,
      }),
    }),
  cancelarReserva: (reservaId) =>
    request("cancelarReserva", {
      method: "POST",
      body: JSON.stringify({ reserva_id: reservaId }),
    }),
  marcarPago: (reservaId) =>
    request("marcarPago", {
      method: "POST",
      body: JSON.stringify({ reserva_id: reservaId }),
    }),

  minhasReservas: (funcionarioId) =>
    request(`minhasReservas&funcionario_id=${funcionarioId}`),
};
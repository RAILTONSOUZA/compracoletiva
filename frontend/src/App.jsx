import React, { useState, useEffect } from 'react';
import { ShoppingBag, PlusCircle, Users, CheckCircle2, Clock, UserCheck, PackageCheck, Download, FileText, ChevronDown, ChevronRight, FileCheck, Tag, Search, Check, ShieldCheck, LogOut, Flame, Lock, Filter, CheckSquare, XCircle } from 'lucide-react';
import { API_URL } from './config';

export default function App() {
  const [telaAtiva, setTelaAtiva] = useState('comprar');
  const [ofertas, setOfertas] = useState([]);
  const [produtosTabela, setProdutosTabela] = useState([]);
  const [pedidosConsolidados, setPedidosConsolidados] = useState([]);

  // AUTENTICAÇÃO DO CLIENTE / FUNCIONÁRIO (CPF/CNPJ)
  const [cpfInput, setCpfInput] = useState('');
  const [usuarioAutenticado, setUsuarioAutenticado] = useState(null);
  const [nomeExibicao, setNomeExibicao] = useState('');
  const [erroAuth, setErroAuth] = useState('');

  // AUTENTICAÇÃO ADMIN (admin1)
  const [adminUser, setAdminUser] = useState('');
  const [adminAutenticado, setAdminAutenticado] = useState(false);
  const [erroAdmin, setErroAdmin] = useState('');

  // FILTROS DE CONSULTA ADMIN
  const [filtroDataInicio, setFiltroDataInicio] = useState('');
  const [filtroDataFim, setFiltroDataFim] = useState('');
  const [filtroStatus, setFiltroStatus] = useState('TODOS');

  // Cadastro de Oferta (Admin)
  const [buscaProduto, setBuscaProduto] = useState('');
  const [menuBuscaAberto, setMenuBuscaAberto] = useState(false);
  const [produtoSelecionado, setProdutoSelecionado] = useState(null);
  const [novaOferta, setNovaOferta] = useState({ codprod: '', desconto_percentual: 0, data_limite: '' });

  const [qtds, setQtds] = useState({});
  const [meusPedidos, setMeusPedidos] = useState([]);

  // Accordion Expansíveis
  const [pedidoAberto, setPedidoAberto] = useState(null);
  const [itemAberto, setItemAberto] = useState(null);

  const carregarDados = async () => {
    try {
      const resProdutos = await fetch(`${API_URL}/produtos/`);
      const dataProdutos = await resProdutos.json();
      setProdutosTabela(dataProdutos);

      const resOfertas = await fetch(`${API_URL}/ofertas/`);
      const dataOfertas = await resOfertas.json();
      setOfertas(dataOfertas);

      const resPedidosConsolidados = await fetch(`${API_URL}/pedidos-consolidados/`);
      const dataConsolidados = await resPedidosConsolidados.json();
      setPedidosConsolidados(dataConsolidados);

      if (usuarioAutenticado) {
        const idBusca = nomeExibicao || usuarioAutenticado.cpfcnpj;
        const resPedidos = await fetch(`${API_URL}/meus-pedidos/${encodeURIComponent(idBusca)}`);
        const dataPedidos = await resPedidos.json();
        setMeusPedidos(dataPedidos);
      }
    } catch (err) {
      console.error("Erro ao carregar dados:", err);
    }
  };

  useEffect(() => {
    carregarDados();
  }, [usuarioAutenticado, nomeExibicao]);

  // LOGIN CLIENTE VIA CPF/CNPJ
  const handleLoginCPF = async (e) => {
    e.preventDefault();
    setErroAuth('');

    try {
      const res = await fetch(`${API_URL}/login-cpf/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cpfcnpj: cpfInput })
      });

      const data = await res.json();
      if (!res.ok) {
        setErroAuth(data.detail || "Erro ao autenticar CPF.");
      } else {
        setUsuarioAutenticado(data);
        setNomeExibicao(`Cliente #${data.codigo}`);
      }
    } catch (err) {
      setErroAuth("Erro de conexão com o servidor.");
    }
  };

  // FATURAR PEDIDO
  const handleFaturarPedido = async (codigoPedido) => {
    if (!window.confirm(`Confirma o FATURAMENTO do pedido ${codigoPedido}?`)) return;

    try {
      const res = await fetch(`${API_URL}/pedidos/faturar/${codigoPedido}`, { method: 'PUT' });
      if (res.ok) {
        alert(`Pedido ${codigoPedido} faturado com sucesso!`);
        carregarDados();
      }
    } catch (err) {
      alert("Erro ao faturar pedido.");
    }
  };

  // CANCELAR PEDIDO
  const handleCancelarPedido = async (codigoPedido) => {
    if (!window.confirm(`Deseja CANCELAR o pedido ${codigoPedido}? Ele não será faturado.`)) return;

    try {
      const res = await fetch(`${API_URL}/pedidos/cancelar/${codigoPedido}`, { method: 'PUT' });
      if (res.ok) {
        alert(`Pedido ${codigoPedido} cancelado!`);
        carregarDados();
      }
    } catch (err) {
      alert("Erro ao cancelar pedido.");
    }
  };

  const handleLoginAdmin = (e) => {
    e.preventDefault();
    setErroAdmin('');
    if (adminUser.trim().toLowerCase() === 'admin1') {
      setAdminAutenticado(true);
    } else {
      setErroAdmin('Usuário incorreto! Utilize admin1');
    }
  };

  const handleLogoutAdmin = () => {
    setAdminAutenticado(false);
    setAdminUser('');
  };

  const handleLogout = () => {
    setUsuarioAutenticado(null);
    setCpfInput('');
    setNomeExibicao('');
    setMeusPedidos([]);
  };

  // BUSCA E CADASTRO DE OFERTAS
  const produtosFiltrados = produtosTabela.filter(p => {
    const termo = buscaProduto.toLowerCase().trim();
    if (!termo) return true;
    return String(p.CODPROD).toLowerCase().includes(termo) || String(p.DESCRICAO).toLowerCase().includes(termo);
  }).slice(0, 30);

  const handleSelecionarProduto = (prod) => {
    setProdutoSelecionado(prod);
    setNovaOferta({ ...novaOferta, codprod: prod.CODPROD });
    setBuscaProduto(`${prod.CODPROD} - ${prod.DESCRICAO}`);
    setMenuBuscaAberto(false);
  };

  const handleCriarOferta = async (e) => {
    e.preventDefault();
    if (!novaOferta.codprod) {
      alert("Selecione um produto válido da lista!");
      return;
    }

    await fetch(`${API_URL}/ofertas/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        codprod: Number(novaOferta.codprod),
        desconto_percentual: Number(novaOferta.desconto_percentual),
        data_limite: novaOferta.data_limite
      })
    });

    setNovaOferta({ codprod: '', desconto_percentual: 0, data_limite: '' });
    setProdutoSelecionado(null);
    setBuscaProduto('');
    carregarDados();
    alert('Oferta cadastrada com sucesso!');
  };

  const handleComprar = async (ofertaId) => {
    if (!usuarioAutenticado) {
      alert("Por favor, faça a validação do seu CPF para comprar.");
      return;
    }

    const qtd = qtds[ofertaId] || 1;
    const identificador = nomeExibicao || usuarioAutenticado.cpfcnpj;

    const res = await fetch(`${API_URL}/comprar/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        oferta_id: ofertaId,
        usuario_nome: identificador,
        qtd_desejada: Number(qtd)
      })
    });

    if (!res.ok) {
      const err = await res.json();
      alert(err.detail || "Erro ao realizar compra");
    } else {
      carregarDados();
      alert("Compra realizada com sucesso!");
    }
  };

  const formatarDataHora = (dataStr) => {
    if (!dataStr) return '';
    const d = new Date(dataStr);
    return d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
  };

  const ofertasAtivas = ofertas.filter(o => {
    const dataLimitePassou = new Date(o.data_limite) <= new Date();
    return o.status === 'ATIVA' && !dataLimitePassou;
  });

  // FILTRO DE CONSULTA DE PEDIDOS NO ADMIN
  const pedidosConsultaFiltrados = pedidosConsolidados.filter(p => {
    let valido = true;
    const dataPed = new Date(p.data_encerramento);

    if (filtroStatus !== 'TODOS' && p.status_pedido !== filtroStatus) {
      valido = false;
    }
    if (filtroDataInicio) {
      const dtInicio = new Date(filtroDataInicio);
      if (dataPed < dtInicio) valido = false;
    }
    if (filtroDataFim) {
      const dtFim = new Date(filtroDataFim);
      dtFim.setHours(23, 59, 59);
      if (dataPed > dtFim) valido = false;
    }
    return valido;
  });

  const precoTabelaFardo = produtoSelecionado ? produtoSelecionado.CXPTABELA : 0;
  const qtdFardo = produtoSelecionado ? produtoSelecionado.QTUNITCX : 0;
  const precoFardoComDesconto = precoTabelaFardo * (1 - (novaOferta.desconto_percentual / 100));
  const precoUnidadeComDesconto = qtdFardo > 0 ? precoFardoComDesconto / qtdFardo : 0;

  return (
    <div className="min-h-screen bg-slate-50 font-sans">
      
      {/* HEADER DESCONTO DO DIA */}
      <header className="bg-slate-950 text-white shadow-md border-b-4 border-[#6BBF4E]">
        <div className="max-w-6xl mx-auto px-6 py-3.5 flex flex-col md:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="bg-[#0D0618] p-1.5 rounded-xl border border-[#6BBF4E]/40 shadow-sm flex items-center justify-center">
              <img src="/logo.svg" alt="Desconto do Dia" className="h-11 w-auto" />
            </div>
            <div>
              <h1 className="text-xl font-black leading-tight tracking-wide text-white">DESCONTO DO DIA</h1>
              <p className="text-xs text-[#6BBF4E] font-bold">Compra Coletiva & Atacado Inteligente</p>
            </div>
          </div>

          <nav className="flex bg-slate-900 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setTelaAtiva('comprar')}
              className={`flex items-center gap-2 px-5 py-2 rounded-lg font-bold text-sm transition-all ${
                telaAtiva === 'comprar' ? 'bg-[#6BBF4E] text-slate-950 shadow-sm' : 'text-slate-300 hover:text-white'
              }`}
            >
              <Users size={18} /> Área de Ofertas
            </button>

            <button
              onClick={() => setTelaAtiva('admin')}
              className={`flex items-center gap-2 px-5 py-2 rounded-lg font-bold text-sm transition-all ${
                telaAtiva === 'admin' ? 'bg-[#6C3FC4] text-white shadow-sm' : 'text-slate-300 hover:text-white'
              }`}
            >
              <PlusCircle size={18} /> Painel Admin
            </button>
          </nav>
        </div>
      </header>

      {/* CONTEÚDO PRINCIPAL */}
      <main className="max-w-6xl mx-auto p-6">

        {/* ================= TELA 1: ÁREA DE OFERTAS & LOGIN ================= */}
        {telaAtiva === 'comprar' && (
          <div className="space-y-8">
            
            {!usuarioAutenticado ? (
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 max-w-md mx-auto">
                <div className="flex items-center gap-2 text-comabel-blue font-bold text-lg mb-2">
                  <ShieldCheck size={22} className="text-comabel-red" />
                  Identificação de Acesso
                </div>
                <p className="text-xs text-slate-500 mb-4">
                  Digite seu CPF ou CNPJ cadastrado para participar das ofertas coletivas.
                </p>

                <form onSubmit={handleLoginCPF} className="space-y-3">
                  <div>
                    <input
                      type="text"
                      placeholder="Digite seu CPF (Ex: 01740108371)..."
                      value={cpfInput}
                      onChange={(e) => setCpfInput(e.target.value)}
                      className="w-full border border-slate-300 rounded-xl p-3 text-sm font-bold text-slate-800 focus:outline-comabel-blue"
                      required
                    />
                  </div>

                  {erroAuth && (
                    <p className="text-xs font-bold text-comabel-red">{erroAuth}</p>
                  )}

                  <button
                    type="submit"
                    className="w-full bg-comabel-blue hover:bg-comabel-blueDark text-white font-bold py-3 rounded-xl transition shadow-md text-sm"
                  >
                    Validar e Acessar Ofertas
                  </button>
                </form>
              </div>
            ) : (
              <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <div>
                  <h2 className="text-lg font-bold text-comabel-blue">Ofertas Disponíveis para Agrupamento</h2>
                  <p className="text-xs text-slate-500">Junte-se a outros compradores para fechar os fardos.</p>
                </div>
                
                <div className="flex items-center gap-3">
                  <div className="bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200 flex items-center gap-2">
                    <UserCheck size={16} className="text-comabel-blue" />
                    <span className="text-xs text-slate-500">ID/CPF:</span>
                    <input
                      type="text"
                      value={nomeExibicao}
                      onChange={(e) => setNomeExibicao(e.target.value)}
                      className="bg-white border rounded px-2 py-0.5 text-xs font-bold text-slate-800 focus:outline-comabel-blue"
                    />
                  </div>
                  
                  <button
                    onClick={handleLogout}
                    className="text-slate-400 hover:text-comabel-red p-1 rounded-lg transition"
                    title="Sair"
                  >
                    <LogOut size={18} />
                  </button>
                </div>
              </div>
            )}

            {/* Vitrine de Ofertas Ativas */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {ofertasAtivas.length === 0 ? (
                <div className="col-span-full bg-white p-8 text-center rounded-2xl text-slate-500 text-sm border">
                  Nenhuma oferta ativa no momento.
                </div>
              ) : (
                ofertasAtivas.map((oferta) => {
                  const fardoAtivo = oferta.fardos.find(f => f.status === 'EM_ANDAMENTO') || { unidades_reservadas: 0 };
                  const percentual = Math.min(100, Math.round((fardoAtivo.unidades_reservadas / oferta.qtd_por_fardo) * 100));
                  const faltam = oferta.qtd_por_fardo - fardoAtivo.unidades_reservadas;
                  const fardosCompletos = oferta.fardos.filter(f => f.status === 'CONCLUIDO').length;

                  return (
                    <div key={oferta.id} className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 flex flex-col justify-between hover:border-comabel-blue transition">
                      <div>
                        <div className="flex justify-between items-start">
                          <span className="text-xs font-bold text-comabel-blue bg-blue-50 px-2.5 py-1 rounded-md border border-blue-100">
                            R$ {oferta.preco_unidade.toFixed(2)} / un
                          </span>
                          {fardosCompletos > 0 && (
                            <span className="flex items-center gap-1 text-[11px] font-bold text-comabel-red bg-red-50 px-2 py-1 rounded-md border border-red-100">
                              <CheckCircle2 size={12} /> {fardosCompletos} fardo(s) fechado(s)
                            </span>
                          )}
                        </div>

                        {/* NOME E CÓDIGO DO PRODUTO DESTACADOS */}
                        <h3 className="font-bold text-slate-800 text-lg mt-3">{oferta.produto_nome}</h3>
                        <p className="text-xs text-slate-500">Fardo com {oferta.qtd_por_fardo} un (R$ {oferta.preco_fardo.toFixed(2)})</p>

                        <div className="mt-2 text-xs flex items-center gap-1 font-semibold text-slate-600">
                          <Clock size={13} className="text-comabel-red" />
                          <span>Encerra em: {formatarDataHora(oferta.data_limite)}</span>
                        </div>

                        <div className="mt-4 bg-slate-50 p-3.5 rounded-xl border border-slate-100">
                          <div className="flex justify-between text-xs font-semibold text-slate-700 mb-1.5">
                            <span>Fardo em Formação</span>
                            <span>{fardoAtivo.unidades_reservadas} / {oferta.qtd_por_fardo} un</span>
                          </div>
                          <div className="w-full bg-slate-200 h-3 rounded-full overflow-hidden">
                            <div className="bg-comabel-blue h-full transition-all duration-300" style={{ width: `${percentual}%` }} />
                          </div>
                          <p className="text-[11px] text-slate-500 mt-2 font-medium">
                            {faltam === 0 ? "Fardo completo! Criando novo..." : `Faltam ${faltam} un para fechar este fardo`}
                          </p>
                        </div>
                      </div>

                      <div className="mt-6 flex gap-2">
                        <input
                          type="number"
                          min="1"
                          max={oferta.qtd_por_fardo - 1}
                          value={qtds[oferta.id] || 1}
                          onChange={(e) => setQtds({ ...qtds, [oferta.id]: e.target.value })}
                          className="w-16 border rounded-xl text-center font-bold text-slate-800 text-sm focus:outline-comabel-blue"
                        />
                        <button
                          onClick={() => handleComprar(oferta.id)}
                          className="flex-1 bg-comabel-blue hover:bg-comabel-blueDark text-white font-bold py-2.5 rounded-xl text-sm transition shadow-sm"
                        >
                          Juntar e Comprar
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* SEÇÃO "MEUS PEDIDOS" DO COMPRADOR */}
            {usuarioAutenticado && (
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <h3 className="text-md font-bold text-comabel-blue mb-4 flex items-center gap-2">
                  <PackageCheck className="text-comabel-red" size={20} /> Meus Pedidos
                </h3>

                {meusPedidos.length === 0 ? (
                  <p className="text-xs text-slate-500">Nenhum pedido faturado no seu histórico no momento.</p>
                ) : (
                  <div className="space-y-4">
                    {meusPedidos.map((ped) => (
                      <div key={ped.codigo_pedido} className="border border-slate-200 rounded-xl overflow-hidden bg-white shadow-sm">
                        
                        <div className="bg-comabel-blue text-white p-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-2">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="bg-comabel-red text-white font-extrabold text-xs px-2.5 py-0.5 rounded-md">
                                {ped.codigo_pedido}
                              </span>
                              <span className="bg-emerald-500 text-white font-extrabold text-[10px] px-2 py-0.5 rounded-md uppercase">
                                {ped.status_pedido}
                              </span>
                              <span className="text-xs text-blue-100">
                                Encerramento: {formatarDataHora(ped.data_encerramento)}
                              </span>
                            </div>
                          </div>

                          <div className="flex items-center gap-4">
                            <span className="text-base font-bold text-white">
                              Total: R$ {ped.valor_total_usuario.toFixed(2)}
                            </span>
                            <a
                              href={`${API_URL}/pdf/comprovante-funcionario/${ped.codigo_pedido}/${encodeURIComponent(nomeExibicao || usuarioAutenticado.cpfcnpj)}`}
                              target="_blank"
                              rel="noreferrer"
                              className="flex items-center gap-1.5 bg-comabel-red hover:bg-comabel-redDark text-white font-bold px-3 py-1.5 rounded-lg text-xs transition shadow-sm"
                            >
                              <Download size={14} /> Imprimir Comprovante
                            </a>
                          </div>
                        </div>

                        <div className="p-4 bg-slate-50">
                          <table className="w-full text-left text-xs border-collapse">
                            <thead>
                              <tr className="border-b text-slate-500 font-semibold">
                                <th className="pb-2">Código / Descrição do Produto</th>
                                <th className="pb-2">Quantidade</th>
                                <th className="pb-2">Preço Unid.</th>
                                <th className="pb-2 text-right">Subtotal</th>
                              </tr>
                            </thead>
                            <tbody>
                              {ped.itens.map((item, idx) => (
                                <tr key={idx} className="border-b last:border-0">
                                  <td className="py-2 font-bold text-slate-800">{item.produto_nome}</td>
                                  <td className="py-2 text-slate-700">{item.qtd_unidades} un</td>
                                  <td className="py-2 text-slate-600">R$ {item.preco_unidade.toFixed(2)}</td>
                                  <td className="py-2 font-bold text-comabel-blue text-right">R$ {item.valor_total.toFixed(2)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

          </div>
        )}

        {/* ================= TELA 2: PAINEL ADMINISTRAÇÃO ================= */}
        {telaAtiva === 'admin' && (
          <div>
            {!adminAutenticado ? (
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 max-w-md mx-auto my-10">
                <div className="flex items-center gap-2 text-comabel-blue font-bold text-lg mb-2">
                  <Lock size={22} className="text-comabel-red" />
                  Acesso Restrito - Painel Admin
                </div>
                <p className="text-xs text-slate-500 mb-4">
                  Digite seu nome de usuário administrador para gerenciar o sistema.
                </p>

                <form onSubmit={handleLoginAdmin} className="space-y-3">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Usuário Administrador</label>
                    <input
                      type="text"
                      placeholder="Ex: admin1"
                      value={adminUser}
                      onChange={(e) => setAdminUser(e.target.value)}
                      className="w-full border border-slate-300 rounded-xl p-3 text-sm font-bold text-slate-800 focus:outline-comabel-blue"
                      required
                    />
                  </div>

                  {erroAdmin && (
                    <p className="text-xs font-bold text-comabel-red">{erroAdmin}</p>
                  )}

                  <button
                    type="submit"
                    className="w-full bg-comabel-red hover:bg-comabel-redDark text-white font-bold py-3 rounded-xl transition shadow-md text-sm"
                  >
                    Entrar no Painel Admin
                  </button>
                </form>
              </div>
            ) : (
              <div className="space-y-8">
                
                <div className="bg-white p-4 rounded-xl border flex justify-between items-center">
                  <span className="text-sm font-bold text-slate-700">Conectado como: <strong className="text-comabel-blue">admin1</strong></span>
                  <button
                    onClick={handleLogoutAdmin}
                    className="text-xs font-bold text-comabel-red hover:underline flex items-center gap-1"
                  >
                    <LogOut size={14} /> Sair do Painel
                  </button>
                </div>

                {/* OFERTAS ATIVAS NO MOMENTO */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                  <h2 className="text-lg font-bold text-comabel-blue mb-1 flex items-center gap-2">
                    <Flame size={20} className="text-comabel-red" /> Ofertas Ativas no Momento ({ofertasAtivas.length})
                  </h2>
                  <p className="text-xs text-slate-500 mb-4">Acompanhe o andamento das compras em tempo real.</p>

                  {ofertasAtivas.length === 0 ? (
                    <p className="text-xs text-slate-500 bg-slate-50 p-4 rounded-xl text-center border">Nenhuma oferta ativa no momento.</p>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {ofertasAtivas.map((o) => {
                        const fardosCompletos = o.fardos.filter(f => f.status === 'CONCLUIDO').length;
                        const fardoAtivo = o.fardos.find(f => f.status === 'EM_ANDAMENTO') || { unidades_reservadas: 0 };
                        
                        return (
                          <div key={o.id} className="border border-slate-200 rounded-xl p-4 bg-slate-50">
                            <div className="flex justify-between items-start mb-2">
                              <h4 className="font-bold text-sm text-slate-800">{o.produto_nome}</h4>
                              <span className="text-[11px] font-bold text-comabel-blue bg-blue-100 px-2 py-0.5 rounded">
                                {fardosCompletos} Fardo(s) Fechado(s)
                              </span>
                            </div>
                            <p className="text-xs text-slate-600">Preço Fardo: <strong>R$ {o.preco_fardo.toFixed(2)}</strong></p>
                            <p className="text-xs text-slate-600 mb-3">Encerra: {formatarDataHora(o.data_limite)}</p>
                            
                            <div className="bg-white p-2.5 rounded-lg border text-xs">
                              <div className="flex justify-between font-bold text-slate-700 mb-1">
                                <span>Fardo Atual em Formação:</span>
                                <span>{fardoAtivo.unidades_reservadas} / {o.qtd_por_fardo} un</span>
                              </div>
                              <div className="w-full bg-slate-200 h-2.5 rounded-full overflow-hidden">
                                <div 
                                  className="bg-comabel-red h-full transition-all" 
                                  style={{ width: `${Math.min(100, (fardoAtivo.unidades_reservadas / o.qtd_por_fardo) * 100)}%` }} 
                                />
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                  
                  {/* FORMULÁRIO DE CADASTRO DE OFERTA */}
                  <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-fit">
                    <h2 className="text-lg font-bold text-comabel-blue mb-1 flex items-center gap-2">
                      <Tag className="text-comabel-red" size={20} /> Cadastrar Ciclo de Oferta
                    </h2>
                    <p className="text-xs text-slate-500 mb-5">Pesquise o produto por código ou nome.</p>

                    <form onSubmit={handleCriarOferta} className="space-y-4">
                      
                      <div className="relative">
                        <label className="block text-xs font-bold text-slate-700 mb-1">Buscar Produto (Código ou Nome)</label>
                        
                        <div className="relative">
                          <input
                            type="text"
                            placeholder="Ex: 1402 ou ABSORVENTE..."
                            value={buscaProduto}
                            onFocus={() => setMenuBuscaAberto(true)}
                            onChange={(e) => {
                              setBuscaProduto(e.target.value);
                              setMenuBuscaAberto(true);
                              setProdutoSelecionado(null);
                              setNovaOferta({ ...novaOferta, codprod: '' });
                            }}
                            className="w-full border border-slate-300 rounded-lg p-2.5 pl-9 text-sm font-semibold text-slate-800 focus:outline-comabel-blue"
                          />
                          <Search className="absolute left-3 top-3 text-slate-400" size={16} />
                        </div>

                        {menuBuscaAberto && (
                          <div className="absolute z-50 w-full mt-1 bg-white border border-slate-200 rounded-xl shadow-xl max-h-60 overflow-y-auto">
                            {produtosFiltrados.length === 0 ? (
                              <div className="p-3 text-xs text-slate-500 text-center">Nenhum produto encontrado.</div>
                            ) : (
                              produtosFiltrados.map((prod) => (
                                <div
                                  key={prod.CODPROD}
                                  onClick={() => handleSelecionarProduto(prod)}
                                  className="p-3 hover:bg-blue-50 border-b border-slate-100 last:border-0 cursor-pointer flex justify-between items-center text-xs transition"
                                >
                                  <div>
                                    <span className="font-extrabold text-comabel-blue bg-blue-50 px-1.5 py-0.5 rounded border border-blue-100 mr-2">
                                      #{prod.CODPROD}
                                    </span>
                                    <span className="font-bold text-slate-800">{prod.DESCRICAO}</span>
                                    <span className="text-slate-400 block text-[10px] mt-0.5">
                                      Caixa c/ {prod.QTUNITCX} un | Dep: {prod.DEPARTAMENTO}
                                    </span>
                                  </div>
                                  <span className="font-bold text-slate-700 bg-slate-100 px-2 py-1 rounded">
                                    R$ {prod.CXPTABELA.toFixed(2)}
                                  </span>
                                </div>
                              ))
                            )}
                          </div>
                        )}
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-slate-700 mb-1">% Desconto (para Funcionários/Clientes)</label>
                        <div className="relative">
                          <input
                            type="number"
                            step="0.1"
                            min="0"
                            max="100"
                            required
                            value={novaOferta.desconto_percentual}
                            onChange={(e) => setNovaOferta({ ...novaOferta, desconto_percentual: e.target.value })}
                            className="w-full border border-slate-300 rounded-lg p-2.5 pl-8 text-sm font-bold text-comabel-red focus:outline-comabel-blue"
                          />
                          <span className="absolute left-3 top-2.5 text-sm font-bold text-slate-400">%</span>
                        </div>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-slate-700 mb-1">Data e Hora Limite do Ciclo</label>
                        <input
                          type="datetime-local"
                          required
                          value={novaOferta.data_limite}
                          onChange={(e) => setNovaOferta({ ...novaOferta, data_limite: e.target.value })}
                          className="w-full border border-slate-300 rounded-lg p-2.5 text-sm focus:outline-comabel-blue"
                        />
                      </div>

                      {produtoSelecionado && (
                        <div className="mt-4 p-4 bg-blue-50 border border-blue-100 rounded-xl">
                          <div className="flex items-center gap-1.5 text-xs font-bold text-comabel-blue mb-2">
                            <Check size={14} className="text-emerald-600" /> Produto Selecionado: #{produtoSelecionado.CODPROD}
                          </div>
                          <div className="flex justify-between items-center text-sm mb-1">
                            <span className="text-slate-600">Preço Fardo (Tabela):</span>
                            <span className="line-through text-slate-400">R$ {precoTabelaFardo.toFixed(2)}</span>
                          </div>
                          <div className="flex justify-between items-center text-sm font-bold text-comabel-red border-b border-blue-200 pb-2 mb-2">
                            <span>Fardo c/ Desconto:</span>
                            <span>R$ {precoFardoComDesconto.toFixed(2)}</span>
                          </div>
                          <div className="flex justify-between items-center text-xs font-bold text-comabel-blue">
                            <span>Valor Unitário Final:</span>
                            <span>R$ {precoUnidadeComDesconto.toFixed(2)} / un</span>
                          </div>
                        </div>
                      )}

                      <button
                        type="submit"
                        className="w-full bg-comabel-blue hover:bg-comabel-blueDark text-white font-bold py-3 rounded-xl transition shadow-md text-sm mt-4"
                      >
                        Publicar Oferta para Agrupamento
                      </button>
                    </form>
                  </div>

                  {/* ÚLTIMOS 3 PEDIDOS ENCERRADOS */}
                  <div className="lg:col-span-2 space-y-4">
                    <h2 className="text-lg font-bold text-comabel-blue flex items-center gap-2">
                      <FileCheck size={20} className="text-comabel-red" /> Últimos 3 Pedidos Encerrados
                    </h2>

                    {pedidosConsolidados.length === 0 ? (
                      <div className="bg-white p-8 rounded-2xl border text-center text-slate-500 text-sm">
                        Nenhum ciclo de pedido encerrado ainda.
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {pedidosConsolidados.slice(0, 3).map((ped) => {
                          const isPedidoAberto = pedidoAberto === ped.codigo_pedido;

                          return (
                            <div key={ped.codigo_pedido} className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                              
                              <div
                                onClick={() => setPedidoAberto(isPedidoAberto ? null : ped.codigo_pedido)}
                                className="p-5 bg-comabel-blue text-white flex flex-col md:flex-row justify-between items-start md:items-center cursor-pointer hover:bg-comabel-blueDark transition gap-4"
                              >
                                <div>
                                  <div className="flex items-center gap-2">
                                    <span className="bg-comabel-red text-white font-extrabold text-xs px-2.5 py-0.5 rounded-md">
                                      {ped.codigo_pedido}
                                    </span>
                                    <span className={`text-xs font-extrabold px-2 py-0.5 rounded-md ${
                                      ped.status_pedido === 'FATURADO' ? 'bg-emerald-500 text-white' : 
                                      ped.status_pedido === 'CANCELADO' ? 'bg-slate-400 text-white' : 'bg-amber-400 text-slate-900'
                                    }`}>
                                      {ped.status_pedido}
                                    </span>
                                    <span className="text-xs text-blue-100">
                                      Encerramento: {formatarDataHora(ped.data_encerramento)}
                                    </span>
                                  </div>
                                  <h3 className="font-bold text-lg mt-1">{ped.total_fardos} fardo(s) fechado(s) neste pedido</h3>
                                </div>

                                <div className="flex items-center gap-4">
                                  <span className="text-lg font-bold text-white">
                                    R$ {ped.valor_total.toFixed(2)}
                                  </span>
                                  {isPedidoAberto ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
                                </div>
                              </div>

                              {isPedidoAberto && (
                                <div className="p-5 bg-slate-50 border-t border-slate-200 space-y-4">
                                  
                                  <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-slate-200">
                                    <div className="flex flex-wrap gap-2">
                                      <a
                                        href={`${API_URL}/pdf/pedido-geral/${ped.ref_oferta_id}`}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="flex items-center gap-1.5 bg-comabel-blue hover:bg-comabel-blueDark text-white text-xs font-bold py-2 px-3 rounded-lg transition shadow-sm"
                                      >
                                        <Download size={14} /> PDF Pedido Geral
                                      </a>

                                      <a
                                        href={`${API_URL}/pdf/pedido-detalhado/${ped.ref_oferta_id}`}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="flex items-center gap-1.5 bg-comabel-red hover:bg-comabel-redDark text-white text-xs font-bold py-2 px-3 rounded-lg transition shadow-sm"
                                      >
                                        <FileText size={14} /> PDF Por Participante
                                      </a>
                                    </div>

                                    {ped.status_pedido === 'ENCERRADO' && (
                                      <div className="flex gap-2">
                                        <button
                                          onClick={() => handleFaturarPedido(ped.codigo_pedido)}
                                          className="flex items-center gap-1 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold py-2 px-3 rounded-lg transition shadow-sm"
                                        >
                                          <CheckSquare size={14} /> Faturar Pedido
                                        </button>
                                        <button
                                          onClick={() => handleCancelarPedido(ped.codigo_pedido)}
                                          className="flex items-center gap-1 bg-slate-500 hover:bg-slate-600 text-white text-xs font-bold py-2 px-3 rounded-lg transition shadow-sm"
                                        >
                                          <XCircle size={14} /> Cancelar Pedido
                                        </button>
                                      </div>
                                    )}
                                  </div>

                                  <div className="space-y-3">
                                    {ped.itens.map((item) => {
                                      const isItemAberto = itemAberto === `${ped.codigo_pedido}-${item.oferta_id}`;

                                      return (
                                        <div key={item.oferta_id} className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                                          
                                          <div
                                            onClick={() => setItemAberto(isItemAberto ? null : `${ped.codigo_pedido}-${item.oferta_id}`)}
                                            className="p-3.5 flex justify-between items-center cursor-pointer hover:bg-slate-100 transition"
                                          >
                                            <div>
                                              {/* EXIBIÇÃO DO NOME E CÓDIGO DO PRODUTO */}
                                              <h4 className="font-bold text-slate-800 text-sm">{item.produto_nome}</h4>
                                              <p className="text-xs text-slate-500">
                                                {item.fardos_concluidos_count} fardo(s) | {item.qtd_por_fardo} un por fardo
                                              </p>
                                            </div>
                                            <div className="flex items-center gap-3">
                                              <span className="font-bold text-xs text-slate-700">
                                                Total: R$ {(item.fardos_concluidos_count * item.preco_fardo).toFixed(2)}
                                              </span>
                                              {isItemAberto ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                                            </div>
                                          </div>

                                          {isItemAberto && (
                                            <div className="p-3 bg-slate-50 border-t border-slate-100 space-y-2">
                                              {item.fardos_detalhes.map((fardo, index) => (
                                                <div key={fardo.id} className="bg-white p-2.5 rounded-lg border text-xs">
                                                  <span className="font-bold text-slate-700 block mb-1">
                                                    📦 Fardo #{index + 1}
                                                  </span>
                                                  <div className="space-y-1">
                                                    {fardo.compradores.map((comp, i) => (
                                                      <div key={i} className="flex justify-between text-slate-600">
                                                        <span>👤 <strong>{comp.nome}</strong></span>
                                                        <span>{comp.qtd} un</span>
                                                        <span className="font-bold text-comabel-blue">R$ {comp.total.toFixed(2)}</span>
                                                      </div>
                                                    ))}
                                                  </div>
                                                </div>
                                              ))}
                                            </div>
                                          )}

                                        </div>
                                      );
                                    })}
                                  </div>

                                </div>
                              )}

                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                </div>

                {/* CONSULTA HISTÓRICA DE PEDIDOS POR PERÍODO E STATUS */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 mt-8">
                  <h2 className="text-lg font-bold text-comabel-blue mb-1 flex items-center gap-2">
                    <Filter className="text-comabel-red" size={20} /> Consulta Geral de Pedidos por Período e Status
                  </h2>
                  <p className="text-xs text-slate-500 mb-6">Filtre o histórico de vendas coletivas por datas e status.</p>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6 bg-slate-50 p-4 rounded-xl border">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">Data Inicial</label>
                      <input
                        type="date"
                        value={filtroDataInicio}
                        onChange={(e) => setFiltroDataInicio(e.target.value)}
                        className="w-full border rounded-lg p-2 text-xs font-bold text-slate-800 bg-white"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">Data Final</label>
                      <input
                        type="date"
                        value={filtroDataFim}
                        onChange={(e) => setFiltroDataFim(e.target.value)}
                        className="w-full border rounded-lg p-2 text-xs font-bold text-slate-800 bg-white"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">Status do Pedido</label>
                      <select
                        value={filtroStatus}
                        onChange={(e) => setFiltroStatus(e.target.value)}
                        className="w-full border rounded-lg p-2 text-xs font-bold text-slate-800 bg-white"
                      >
                        <option value="TODOS">Todos os Status</option>
                        <option value="ENCERRADO">Encerrado (Pendente)</option>
                        <option value="FATURADO">Faturado (Concluído)</option>
                        <option value="CANCELADO">Cancelado (Não Efetivado)</option>
                      </select>
                    </div>
                  </div>

                  {pedidosConsultaFiltrados.length === 0 ? (
                    <p className="text-xs text-slate-500 text-center p-6 bg-slate-50 rounded-xl">
                      Nenhum pedido encontrado para o período e filtros selecionados.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {pedidosConsultaFiltrados.map((p) => (
                        <div key={p.codigo_pedido} className="border p-4 rounded-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-3 bg-white">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-extrabold text-xs text-comabel-blue">{p.codigo_pedido}</span>
                              <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded ${
                                p.status_pedido === 'FATURADO' ? 'bg-emerald-100 text-emerald-700' : 
                                p.status_pedido === 'CANCELADO' ? 'bg-slate-100 text-slate-600' : 'bg-amber-100 text-amber-700'
                              }`}>
                                {p.status_pedido}
                              </span>
                            </div>
                            <p className="text-xs text-slate-500 mt-0.5">Encerramento: {formatarDataHora(p.data_encerramento)} | {p.total_fardos} fardo(s)</p>
                          </div>

                          <div className="flex items-center gap-3">
                            <span className="font-bold text-sm text-slate-800">R$ {p.valor_total.toFixed(2)}</span>
                            <a
                              href={`${API_URL}/pdf/pedido-geral/${p.ref_oferta_id}`}
                              target="_blank"
                              rel="noreferrer"
                              className="bg-comabel-blue text-white text-xs px-2.5 py-1.5 rounded-lg font-bold"
                            >
                              PDF Geral
                            </a>
                            <a
                              href={`${API_URL}/pdf/pedido-detalhado/${p.ref_oferta_id}`}
                              target="_blank"
                              rel="noreferrer"
                              className="bg-comabel-red text-white text-xs px-2.5 py-1.5 rounded-lg font-bold"
                            >
                              PDF Participantes
                            </a>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                </div>

              </div>
            )}
          </div>
        )}

      </main>
    </div>
  );
}
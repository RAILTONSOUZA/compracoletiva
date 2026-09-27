package com.comabel.compracoletiva.service;

import com.comabel.compracoletiva.model.CompraItem;
import com.comabel.compracoletiva.model.FardoGrupo;
import com.comabel.compracoletiva.model.Oferta;
import com.comabel.compracoletiva.model.PedidoConsolidado;
import com.comabel.compracoletiva.repository.OfertaRepository;
import com.comabel.compracoletiva.repository.PedidoConsolidadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PedidoService {

    private final OfertaRepository ofertaRepository;
    private final PedidoConsolidadoRepository pedidoConsolidadoRepository;
    private final OfertaService ofertaService;

    public PedidoService(OfertaRepository ofertaRepository,
                         PedidoConsolidadoRepository pedidoConsolidadoRepository,
                         OfertaService ofertaService) {
        this.ofertaRepository = ofertaRepository;
        this.pedidoConsolidadoRepository = pedidoConsolidadoRepository;
        this.ofertaService = ofertaService;
    }

    @Transactional
    public List<Map<String, Object>> listarPedidosConsolidados() {
        ofertaService.verificarEFecharOfertasExpiradas();

        List<Oferta> ofertasEncerradas = ofertaRepository.findByStatus("ENCERRADA");

        Map<LocalDateTime, List<Oferta>> ciclos = new TreeMap<>(Collections.reverseOrder());
        for (Oferta oferta : ofertasEncerradas) {
            LocalDateTime dt = oferta.getDataLimite();
            ciclos.computeIfAbsent(dt, k -> new ArrayList<>()).add(oferta);
        }

        List<Map<String, Object>> pedidos = new ArrayList<>();
        int idx = 1;

        for (Map.Entry<LocalDateTime, List<Oferta>> entry : ciclos.entrySet()) {
            LocalDateTime dtLimite = entry.getKey();
            List<Oferta> listaOfertas = entry.getValue();

            String codigoPedido = String.format("PED-%s-%03d",
                    dtLimite.format(DateTimeFormatter.ofPattern("yyyyMMdd")), idx);

            int totalFardosFechados = 0;
            double valorTotalPedido = 0.0;
            List<Map<String, Object>> itensOfertas = new ArrayList<>();

            for (Oferta oferta : listaOfertas) {
                List<FardoGrupo> fardosConcluidos = oferta.getFardos().stream()
                        .filter(f -> "CONCLUIDO".equals(f.getStatus()))
                        .toList();

                if (!fardosConcluidos.isEmpty()) {
                    int qFardos = fardosConcluidos.size();
                    double valOferta = qFardos * oferta.getPrecoFardo();
                    totalFardosFechados += qFardos;
                    valorTotalPedido += valOferta;

                    List<Map<String, Object>> fardosDetalhe = new ArrayList<>();
                    for (FardoGrupo f : fardosConcluidos) {
                        List<Map<String, Object>> compradores = new ArrayList<>();
                        for (CompraItem item : f.getItens()) {
                            Map<String, Object> comp = new LinkedHashMap<>();
                            comp.put("nome", item.getUsuarioNome());
                            comp.put("qtd", item.getQtdUnidades());
                            comp.put("total", item.getValorTotal());
                            compradores.add(comp);
                        }
                        Map<String, Object> fDet = new LinkedHashMap<>();
                        fDet.put("id", f.getId());
                        fDet.put("compradores", compradores);
                        fardosDetalhe.add(fDet);
                    }

                    Map<String, Object> itemOfertaMap = new LinkedHashMap<>();
                    itemOfertaMap.put("oferta_id", oferta.getId());
                    itemOfertaMap.put("produto_nome", oferta.getProdutoNome());
                    itemOfertaMap.put("qtd_por_fardo", oferta.getQtdPorFardo());
                    itemOfertaMap.put("preco_fardo", oferta.getPrecoFardo());
                    itemOfertaMap.put("preco_unidade", oferta.getPrecoUnidade());
                    itemOfertaMap.put("fardos_concluidos_count", qFardos);
                    itemOfertaMap.put("fardos_detalhes", fardosDetalhe);
                    itensOfertas.add(itemOfertaMap);
                }
            }

            if (totalFardosFechados > 0) {
                // Recupera ou cria persistência real no banco de dados
                PedidoConsolidado pc = pedidoConsolidadoRepository.findByCodigoPedido(codigoPedido)
                        .orElseGet(() -> {
                            PedidoConsolidado novo = new PedidoConsolidado();
                            novo.setCodigoPedido(codigoPedido);
                            novo.setDataEncerramento(dtLimite);
                            novo.setStatusPedido("ENCERRADO");
                            novo.setTotalFardos(0);
                            novo.setValorTotal(0.0);
                            return pedidoConsolidadoRepository.save(novo);
                        });

                pc.setTotalFardos(totalFardosFechados);
                pc.setValorTotal(valorTotalPedido);
                pedidoConsolidadoRepository.save(pc);

                Map<String, Object> pedMap = new LinkedHashMap<>();
                pedMap.put("codigo_pedido", codigoPedido);
                pedMap.put("data_encerramento", dtLimite);
                pedMap.put("status_pedido", pc.getStatusPedido());
                pedMap.put("ref_oferta_id", listaOfertas.get(0).getId());
                pedMap.put("total_fardos", totalFardosFechados);
                pedMap.put("valor_total", valorTotalPedido);
                pedMap.put("itens", itensOfertas);

                pedidos.add(pedMap);
                idx++;
            }
        }

        return pedidos;
    }

    @Transactional
    public void faturarPedido(String codigoPedido) {
        PedidoConsolidado pc = pedidoConsolidadoRepository.findByCodigoPedido(codigoPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        pc.setStatusPedido("FATURADO");
        pc.setDataFaturamento(LocalDateTime.now());
        pedidoConsolidadoRepository.save(pc);
    }

    @Transactional
    public void cancelarPedido(String codigoPedido) {
        PedidoConsolidado pc = pedidoConsolidadoRepository.findByCodigoPedido(codigoPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        pc.setStatusPedido("CANCELADO");
        pedidoConsolidadoRepository.save(pc);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> meusPedidosConcluidos(String usuarioNome) {
        List<Map<String, Object>> todos = listarPedidosConsolidados();
        List<Map<String, Object>> faturados = todos.stream()
                .filter(p -> "FATURADO".equals(p.get("status_pedido")))
                .toList();

        List<Map<String, Object>> pedidosFuncionario = new ArrayList<>();

        for (Map<String, Object> ped : faturados) {
            List<Map<String, Object>> itensOfertas = (List<Map<String, Object>>) ped.get("itens");
            List<Map<String, Object>> itensUsuario = new ArrayList<>();
            double totalPedidoUsuario = 0.0;

            for (Map<String, Object> itemOferta : itensOfertas) {
                int qtdTotalItem = 0;
                double valTotalItem = 0.0;

                List<Map<String, Object>> fardosDetalhes = (List<Map<String, Object>>) itemOferta.get("fardos_detalhes");
                for (Map<String, Object> fardo : fardosDetalhes) {
                    List<Map<String, Object>> compradores = (List<Map<String, Object>>) fardo.get("compradores");
                    for (Map<String, Object> comp : compradores) {
                        String nome = (String) comp.get("nome");
                        if (nome != null && nome.trim().equalsIgnoreCase(usuarioNome.trim())) {
                            qtdTotalItem += (Integer) comp.get("qtd");
                            valTotalItem += (Double) comp.get("total");
                        }
                    }
                }

                if (qtdTotalItem > 0) {
                    Map<String, Object> iu = new LinkedHashMap<>();
                    iu.put("produto_nome", itemOferta.get("produto_nome"));
                    iu.put("qtd_unidades", qtdTotalItem);
                    iu.put("preco_unidade", itemOferta.get("preco_unidade"));
                    iu.put("valor_total", valTotalItem);
                    itensUsuario.add(iu);
                    totalPedidoUsuario += valTotalItem;
                }
            }

            if (!itensUsuario.isEmpty()) {
                Map<String, Object> pf = new LinkedHashMap<>();
                pf.put("codigo_pedido", ped.get("codigo_pedido"));
                pf.put("data_encerramento", ped.get("data_encerramento"));
                pf.put("status_pedido", ped.get("status_pedido"));
                pf.put("valor_total_usuario", totalPedidoUsuario);
                pf.put("itens", itensUsuario);
                pedidosFuncionario.add(pf);
            }
        }

        return pedidosFuncionario;
    }
}

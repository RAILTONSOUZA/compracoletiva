package com.comabel.compracoletiva.service;

import com.comabel.compracoletiva.dto.CompraCreateDto;
import com.comabel.compracoletiva.model.CompraItem;
import com.comabel.compracoletiva.model.FardoGrupo;
import com.comabel.compracoletiva.model.Oferta;
import com.comabel.compracoletiva.repository.CompraItemRepository;
import com.comabel.compracoletiva.repository.FardoGrupoRepository;
import com.comabel.compracoletiva.repository.OfertaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CompraService {

    private final OfertaRepository ofertaRepository;
    private final FardoGrupoRepository fardoGrupoRepository;
    private final CompraItemRepository compraItemRepository;
    private final OfertaService ofertaService;

    public CompraService(OfertaRepository ofertaRepository,
                         FardoGrupoRepository fardoGrupoRepository,
                         CompraItemRepository compraItemRepository,
                         OfertaService ofertaService) {
        this.ofertaRepository = ofertaRepository;
        this.fardoGrupoRepository = fardoGrupoRepository;
        this.compraItemRepository = compraItemRepository;
        this.ofertaService = ofertaService;
    }

    @Transactional
    public void realizarCompra(CompraCreateDto dto) {
        ofertaService.verificarEFecharOfertasExpiradas();

        Oferta oferta = ofertaRepository.findById(dto.getOfertaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta não encontrada"));

        if ("ENCERRADA".equals(oferta.getStatus()) || oferta.getDataLimite().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta oferta já expirou!");
        }

        int limiteMaximoPorUsuario = oferta.getQtdPorFardo() - 1;

        // Calcula total já comprado pelo usuário nesta oferta
        int totalJaComprado = 0;
        for (FardoGrupo fardo : oferta.getFardos()) {
            for (CompraItem item : fardo.getItens()) {
                if (item.getUsuarioNome().trim().equalsIgnoreCase(dto.getUsuarioNome().trim())) {
                    totalJaComprado += item.getQtdUnidades();
                }
            }
        }

        if ((totalJaComprado + dto.getQtdDesejada()) > limiteMaximoPorUsuario) {
            int restantePermitido = limiteMaximoPorUsuario - totalJaComprado;
            if (restantePermitido > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Você já possui %d un. neste ciclo. Só é permitido comprar mais %d un.", totalJaComprado, restantePermitido));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Você já atingiu o limite máximo de %d unidades para este ciclo de oferta!", limiteMaximoPorUsuario));
            }
        }

        int qtdPendente = dto.getQtdDesejada();

        while (qtdPendente > 0) {
            List<FardoGrupo> fardosEmAndamento = fardoGrupoRepository.findEmAndamentoComLock(oferta.getId());
            FardoGrupo fardoAtual = fardosEmAndamento.isEmpty() ? null : fardosEmAndamento.get(0);

            if (fardoAtual == null) {
                fardoAtual = new FardoGrupo(oferta);
                fardoAtual = fardoGrupoRepository.save(fardoAtual);
            }

            int vagasFardo = oferta.getQtdPorFardo() - fardoAtual.getUnidadesReservadas();
            int alocar = Math.min(qtdPendente, vagasFardo);

            CompraItem item = new CompraItem(
                    fardoAtual,
                    dto.getUsuarioNome(),
                    alocar,
                    alocar * oferta.getPrecoUnidade()
            );
            compraItemRepository.save(item);
            fardoAtual.getItens().add(item);

            fardoAtual.setUnidadesReservadas(fardoAtual.getUnidadesReservadas() + alocar);
            qtdPendente -= alocar;

            if (fardoAtual.getUnidadesReservadas().equals(oferta.getQtdPorFardo())) {
                fardoAtual.setStatus("CONCLUIDO");
                fardoGrupoRepository.save(fardoAtual);

                if (qtdPendente > 0) {
                    FardoGrupo novoFardo = new FardoGrupo(oferta);
                    fardoGrupoRepository.save(novoFardo);
                }
            } else {
                fardoGrupoRepository.save(fardoAtual);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obterMinhasComprasAtivas(String usuarioNome) {
        ofertaService.verificarEFecharOfertasExpiradas();

        List<Oferta> ofertas = ofertaRepository.findAll();
        List<Map<String, Object>> comprasAtivas = new ArrayList<>();

        for (Oferta oferta : ofertas) {
            for (FardoGrupo fardo : oferta.getFardos()) {
                if ("EM_ANDAMENTO".equals(fardo.getStatus()) || "CONCLUIDO".equals(fardo.getStatus())) {
                    int minhasUnidades = 0;
                    for (CompraItem item : fardo.getItens()) {
                        if (item.getUsuarioNome().trim().equalsIgnoreCase(usuarioNome.trim())) {
                            minhasUnidades += item.getQtdUnidades();
                        }
                    }

                    if (minhasUnidades > 0) {
                        int faltam = oferta.getQtdPorFardo() - fardo.getUnidadesReservadas();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("oferta_id", oferta.getId());
                        map.put("produto_nome", oferta.getProdutoNome());
                        map.put("qtd_por_fardo", oferta.getQtdPorFardo());
                        map.put("minhas_unidades", minhasUnidades);
                        map.put("unidades_reservadas", fardo.getUnidadesReservadas());
                        map.put("faltam_unidades", Math.max(0, faltam));
                        map.put("preco_unidade", oferta.getPrecoUnidade());
                        map.put("valor_investido", minhasUnidades * oferta.getPrecoUnidade());
                        map.put("data_limite", oferta.getDataLimite());
                        map.put("status_fardo", fardo.getStatus());

                        comprasAtivas.add(map);
                    }
                }
            }
        }

        return comprasAtivas;
    }
}

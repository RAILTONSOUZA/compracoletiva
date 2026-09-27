package com.comabel.compracoletiva.service;

import com.comabel.compracoletiva.dto.OfertaCreateDto;
import com.comabel.compracoletiva.model.CompraItem;
import com.comabel.compracoletiva.model.FardoGrupo;
import com.comabel.compracoletiva.model.Oferta;
import com.comabel.compracoletiva.model.Produto;
import com.comabel.compracoletiva.repository.CompraItemRepository;
import com.comabel.compracoletiva.repository.FardoGrupoRepository;
import com.comabel.compracoletiva.repository.OfertaRepository;
import com.comabel.compracoletiva.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Service
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final FardoGrupoRepository fardoGrupoRepository;
    private final ProdutoRepository produtoRepository;
    private final CompraItemRepository compraItemRepository;

    public OfertaService(OfertaRepository ofertaRepository,
                         FardoGrupoRepository fardoGrupoRepository,
                         ProdutoRepository produtoRepository,
                         CompraItemRepository compraItemRepository) {
        this.ofertaRepository = ofertaRepository;
        this.fardoGrupoRepository = fardoGrupoRepository;
        this.produtoRepository = produtoRepository;
        this.compraItemRepository = compraItemRepository;
    }

    @Transactional
    public Oferta criarOferta(OfertaCreateDto dto) {
        Produto produto = produtoRepository.findById(dto.getCodprod())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado na base de dados."));

        double desconto = dto.getDescontoPercentual() != null ? dto.getDescontoPercentual() : 0.0;
        int qtdPorFardo = Math.max(1, produto.getQtunitcx());
        double precoTabela = produto.getCxptabela() != null ? produto.getCxptabela() : 0.0;

        double precoFardoComDesconto = precoTabela * (1.0 - (desconto / 100.0));
        double precoUnidade = precoFardoComDesconto / qtdPorFardo;

        String nomeComCodigo = String.format("[%d] %s", produto.getCodprod(), produto.getDescricao());

        Oferta oferta = new Oferta();
        oferta.setCodprod(produto.getCodprod());
        oferta.setProdutoNome(nomeComCodigo);
        oferta.setQtdPorFardo(qtdPorFardo);
        oferta.setPrecoFardo(precoFardoComDesconto);
        oferta.setPrecoUnidade(precoUnidade);
        oferta.setDataLimite(dto.getDataLimite());
        oferta.setStatus("ATIVA");

        oferta = ofertaRepository.save(oferta);

        FardoGrupo primeiroFardo = new FardoGrupo(oferta);
        fardoGrupoRepository.save(primeiroFardo);

        oferta.getFardos().add(primeiroFardo);
        return oferta;
    }

    @Transactional
    public List<Oferta> listarOfertas() {
        verificarEFecharOfertasExpiradas();
        return ofertaRepository.findAll();
    }

    @Scheduled(fixedRate = 60000) // Roda a cada 1 minuto em background
    @Transactional
    public void verificarEFecharOfertasExpiradas() {
        LocalDateTime agora = LocalDateTime.now();
        List<Oferta> expiradas = ofertaRepository.findByStatusAndDataLimiteLessThanEqual("ATIVA", agora);

        for (Oferta oferta : expiradas) {
            oferta.setStatus("ENCERRADA");
            for (FardoGrupo fardo : oferta.getFardos()) {
                if ("EM_ANDAMENTO".equals(fardo.getStatus())) {
                    fardo.setStatus("CANCELADO");
                }
            }
        }

        if (!expiradas.isEmpty()) {
            ofertaRepository.saveAll(expiradas);
        }
    }

    @Transactional
    public int cancelarParticipacao(Long ofertaId, String usuarioNome) {
        verificarEFecharOfertasExpiradas();

        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta não encontrada."));

        if (!"ATIVA".equals(oferta.getStatus()) || oferta.getDataLimite().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ciclo desta oferta já foi encerrado. Não é mais possível cancelar.");
        }

        int itensRemovidos = 0;
        for (FardoGrupo fardo : oferta.getFardos()) {
            if ("EM_ANDAMENTO".equals(fardo.getStatus())) {
                Iterator<CompraItem> it = fardo.getItens().iterator();
                while (it.hasNext()) {
                    CompraItem item = it.next();
                    if (item.getUsuarioNome().trim().equalsIgnoreCase(usuarioNome.trim())) {
                        fardo.setUnidadesReservadas(fardo.getUnidadesReservadas() - item.getQtdUnidades());
                        it.remove();
                        compraItemRepository.delete(item);
                        itensRemovidos++;
                    }
                }
            }
        }

        if (itensRemovidos == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este fardo já foi fechado/concluído ou você não possui itens em andamento nesta oferta.");
        }

        ofertaRepository.save(oferta);
        return itensRemovidos;
    }
}

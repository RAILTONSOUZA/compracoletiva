package com.comabel.compracoletiva.controller;

import com.comabel.compracoletiva.dto.OfertaCreateDto;
import com.comabel.compracoletiva.model.Oferta;
import com.comabel.compracoletiva.service.OfertaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class OfertaController {

    private final OfertaService ofertaService;

    public OfertaController(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @PostMapping({"/ofertas", "/ofertas/"})
    public ResponseEntity<Oferta> criarOferta(@Valid @RequestBody OfertaCreateDto dto) {
        Oferta criada = ofertaService.criarOferta(dto);
        return ResponseEntity.ok(criada);
    }

    @GetMapping({"/ofertas", "/ofertas/"})
    public ResponseEntity<List<Oferta>> listarOfertas() {
        return ResponseEntity.ok(ofertaService.listarOfertas());
    }

    @DeleteMapping("/comprar/{ofertaId}/{usuarioNome}")
    public ResponseEntity<Map<String, Object>> cancelarParticipacao(
            @PathVariable Long ofertaId,
            @PathVariable String usuarioNome) {
        int removidos = ofertaService.cancelarParticipacao(ofertaId, usuarioNome);
        return ResponseEntity.ok(Map.of(
                "message", "Participação cancelada com sucesso!",
                "itens_removidos", removidos
        ));
    }
}

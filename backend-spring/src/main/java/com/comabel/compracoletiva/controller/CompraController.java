package com.comabel.compracoletiva.controller;

import com.comabel.compracoletiva.dto.CompraCreateDto;
import com.comabel.compracoletiva.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping({"/comprar", "/comprar/"})
    public ResponseEntity<Map<String, String>> comprar(@Valid @RequestBody CompraCreateDto dto) {
        compraService.realizarCompra(dto);
        return ResponseEntity.ok(Map.of("message", "Compra realizada com sucesso!"));
    }

    @GetMapping("/minhas-compras-ativas/{usuarioNome}")
    public ResponseEntity<List<Map<String, Object>>> minhasComprasAtivas(@PathVariable String usuarioNome) {
        return ResponseEntity.ok(compraService.obterMinhasComprasAtivas(usuarioNome));
    }
}

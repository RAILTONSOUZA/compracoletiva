package com.comabel.compracoletiva.controller;

import com.comabel.compracoletiva.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping({"/pedidos-consolidados", "/pedidos-consolidados/"})
    public ResponseEntity<List<Map<String, Object>>> listarPedidosConsolidados() {
        return ResponseEntity.ok(pedidoService.listarPedidosConsolidados());
    }

    @PutMapping("/pedidos/faturar/{codigoPedido}")
    public ResponseEntity<Map<String, String>> faturarPedido(@PathVariable String codigoPedido) {
        pedidoService.faturarPedido(codigoPedido);
        return ResponseEntity.ok(Map.of(
                "message", "Pedido " + codigoPedido + " faturado com sucesso!",
                "status", "FATURADO"
        ));
    }

    @PutMapping("/pedidos/cancelar/{codigoPedido}")
    public ResponseEntity<Map<String, String>> cancelarPedido(@PathVariable String codigoPedido) {
        pedidoService.cancelarPedido(codigoPedido);
        return ResponseEntity.ok(Map.of(
                "message", "Pedido " + codigoPedido + " cancelado com sucesso!",
                "status", "CANCELADO"
        ));
    }

    @GetMapping("/meus-pedidos/{usuarioNome}")
    public ResponseEntity<List<Map<String, Object>>> meusPedidos(@PathVariable String usuarioNome) {
        return ResponseEntity.ok(pedidoService.meusPedidosConcluidos(usuarioNome));
    }
}

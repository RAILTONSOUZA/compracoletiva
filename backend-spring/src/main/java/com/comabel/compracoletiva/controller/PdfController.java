package com.comabel.compracoletiva.controller;

import com.comabel.compracoletiva.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/pdf")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/pedido-geral/{ofertaId}")
    public ResponseEntity<byte[]> gerarPdfPedidoGeral(@PathVariable Long ofertaId) {
        byte[] pdfBytes = pdfService.gerarPdfPedidoGeral(ofertaId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Pedido_Geral_Estoque.pdf")
                .body(pdfBytes);
    }

    @GetMapping("/comprovante-funcionario/{codigoPedido}/{usuarioNome}")
    public ResponseEntity<byte[]> gerarComprovanteFuncionario(
            @PathVariable String codigoPedido,
            @PathVariable String usuarioNome) {
        byte[] pdfBytes = pdfService.gerarPdfComprovanteFuncionario(codigoPedido, usuarioNome);

        String nomeArquivo = String.format("Comprovante_%s_%s.pdf", codigoPedido, usuarioNome.replace(" ", "_"));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomeArquivo)
                .body(pdfBytes);
    }
}

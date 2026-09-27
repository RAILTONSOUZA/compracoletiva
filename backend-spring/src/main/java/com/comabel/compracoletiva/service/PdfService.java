package com.comabel.compracoletiva.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class PdfService {

    private final PedidoService pedidoService;

    private static final Color BLUE_COMABEL = new Color(0, 82, 165);
    private static final Color RED_COMABEL = new Color(227, 6, 19);
    private static final Color BG_LIGHT = new Color(241, 245, 249);
    private static final Color BORDER_COLOR = new Color(203, 213, 225);

    public PdfService(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    public byte[] gerarPdfPedidoGeral(Long ofertaId) {
        List<Map<String, Object>> pedidos = pedidoService.listarPedidosConsolidados();
        Map<String, Object> pedidoRef = null;

        for (Map<String, Object> p : pedidos) {
            List<Map<String, Object>> itens = (List<Map<String, Object>>) p.get("itens");
            for (Map<String, Object> it : itens) {
                if (ofertaId.equals(it.get("oferta_id"))) {
                    pedidoRef = p;
                    break;
                }
            }
            if (pedidoRef != null) break;
        }

        if (pedidoRef == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado.");
        }

        String codigoPedido = (String) pedidoRef.get("codigo_pedido");
        String status = (String) pedidoRef.get("status_pedido");
        LocalDateTime dtEnc = (LocalDateTime) pedidoRef.get("data_encerramento");
        String dtStr = dtEnc.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Título
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BLUE_COMABEL);
            Paragraph title = new Paragraph("COMABEL - PEDIDO GERAL DE COMPRAS (ESTOQUE)", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            doc.add(new Paragraph("Número do Pedido: " + codigoPedido + " (" + status + ")", fontSubtitle));
            doc.add(new Paragraph("Data de Encerramento: " + dtStr, FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY)));
            doc.add(new Paragraph(" "));

            // Tabela
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4.5f, 2f, 1.8f, 2f, 2f});

            adicionarHeader(table, new String[]{"Produto", "Fardos Fechados", "Unidades", "Preço Fardo", "Valor Total"});

            double valorTotalPedidoGeral = 0.0;
            int fardosTotais = 0;

            List<Map<String, Object>> itens = (List<Map<String, Object>>) pedidoRef.get("itens");
            for (Map<String, Object> it : itens) {
                int qFardos = (Integer) it.get("fardos_concluidos_count");
                int qtdPorFardo = (Integer) it.get("qtd_por_fardo");
                int unidadesTotais = qFardos * qtdPorFardo;
                double precoFardo = (Double) it.get("preco_fardo");
                double valorItemTotal = qFardos * precoFardo;

                valorTotalPedidoGeral += valorItemTotal;
                fardosTotais += qFardos;

                adicionarCelula(table, (String) it.get("produto_nome"), Element.ALIGN_LEFT, false);
                adicionarCelula(table, qFardos + " fardo(s)", Element.ALIGN_CENTER, false);
                adicionarCelula(table, unidadesTotais + " un", Element.ALIGN_CENTER, false);
                adicionarCelula(table, String.format("R$ %.2f", precoFardo), Element.ALIGN_RIGHT, false);
                adicionarCelula(table, String.format("R$ %.2f", valorItemTotal), Element.ALIGN_RIGHT, false);
            }

            // Linha Total
            PdfPCell cellTotal = new PdfPCell(new Phrase("TOTAL GERAL DO PEDIDO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            cellTotal.setBackgroundColor(BG_LIGHT);
            cellTotal.setBorderColor(BORDER_COLOR);
            cellTotal.setPadding(6);
            table.addCell(cellTotal);

            adicionarCelula(table, fardosTotais + " fardo(s)", Element.ALIGN_CENTER, true);
            adicionarCelula(table, "-", Element.ALIGN_CENTER, true);
            adicionarCelula(table, "-", Element.ALIGN_CENTER, true);
            adicionarCelula(table, String.format("R$ %.2f", valorTotalPedidoGeral), Element.ALIGN_RIGHT, true);

            doc.add(table);
            doc.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    public byte[] gerarPdfComprovanteFuncionario(String codigoPedido, String usuarioNome) {
        List<Map<String, Object>> pedidos = pedidoService.listarPedidosConsolidados();
        Map<String, Object> pedidoRef = pedidos.stream()
                .filter(p -> codigoPedido.equalsIgnoreCase((String) p.get("codigo_pedido")))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        LocalDateTime dtEnc = (LocalDateTime) pedidoRef.get("data_encerramento");
        String dtStr = dtEnc.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        List<Map<String, Object>> itens = (List<Map<String, Object>>) pedidoRef.get("itens");
        List<Map<String, Object>> itensUsuario = new ArrayList<>();
        double totalComprado = 0.0;

        for (Map<String, Object> it : itens) {
            int qtdTotal = 0;
            double valTotal = 0.0;

            List<Map<String, Object>> fardos = (List<Map<String, Object>>) it.get("fardos_detalhes");
            for (Map<String, Object> f : fardos) {
                List<Map<String, Object>> compradores = (List<Map<String, Object>>) f.get("compradores");
                for (Map<String, Object> c : compradores) {
                    String nome = (String) c.get("nome");
                    if (nome != null && nome.trim().equalsIgnoreCase(usuarioNome.trim())) {
                        qtdTotal += (Integer) c.get("qtd");
                        valTotal += (Double) c.get("total");
                    }
                }
            }

            if (qtdTotal > 0) {
                Map<String, Object> iu = new LinkedHashMap<>();
                iu.put("produto", it.get("produto_nome"));
                iu.put("qtd", qtdTotal);
                iu.put("preco_un", it.get("preco_unidade"));
                iu.put("total", valTotal);
                itensUsuario.add(iu);
                totalComprado += valTotal;
            }
        }

        if (itensUsuario.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum item encontrado para este participante neste pedido.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("COMABEL - COMPROVANTE DE COMPRA COLETIVA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BLUE_COMABEL));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Número do Pedido: " + codigoPedido, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, RED_COMABEL)));
            doc.add(new Paragraph("Participante: " + usuarioNome.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK)));
            doc.add(new Paragraph("Data de Encerramento: " + dtStr, FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY)));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5f, 1.8f, 2f, 2f});

            adicionarHeader(table, new String[]{"Produto", "Quantidade", "Preço Unid.", "Subtotal"});

            for (Map<String, Object> item : itensUsuario) {
                adicionarCelula(table, (String) item.get("produto"), Element.ALIGN_LEFT, false);
                adicionarCelula(table, item.get("qtd") + " un", Element.ALIGN_CENTER, false);
                adicionarCelula(table, String.format("R$ %.2f", (Double) item.get("preco_un")), Element.ALIGN_RIGHT, false);
                adicionarCelula(table, String.format("R$ %.2f", (Double) item.get("total")), Element.ALIGN_RIGHT, false);
            }

            PdfPCell cellTotal = new PdfPCell(new Phrase("TOTAL A PAGAR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            cellTotal.setBackgroundColor(BG_LIGHT);
            cellTotal.setBorderColor(BORDER_COLOR);
            cellTotal.setPadding(6);
            table.addCell(cellTotal);

            adicionarCelula(table, "-", Element.ALIGN_CENTER, true);
            adicionarCelula(table, "-", Element.ALIGN_CENTER, true);
            adicionarCelula(table, String.format("R$ %.2f", totalComprado), Element.ALIGN_RIGHT, true);

            doc.add(table);
            doc.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar comprovante", e);
        }
    }

    private void adicionarHeader(PdfPTable table, String[] colunas) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String col : colunas) {
            PdfPCell cell = new PdfPCell(new Phrase(col, font));
            cell.setBackgroundColor(BLUE_COMABEL);
            cell.setBorderColor(BORDER_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void adicionarCelula(PdfPTable table, String texto, int align, boolean bold) {
        Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK)
                         : FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_COLOR);
        if (bold) cell.setBackgroundColor(BG_LIGHT);
        cell.setPadding(6);
        table.addCell(cell);
    }
}

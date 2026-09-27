package com.comabel.compracoletiva.service;

import com.comabel.compracoletiva.model.Cliente;
import com.comabel.compracoletiva.model.Produto;
import com.comabel.compracoletiva.repository.ClienteRepository;
import com.comabel.compracoletiva.repository.ProdutoRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Service
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public CatalogoService(ProdutoRepository produtoRepository, ClienteRepository clienteRepository) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarProdutoPorId(Long codprod) {
        return produtoRepository.findById(codprod);
    }

    @Transactional
    public void importarProdutosSeNecessario() {
        if (produtoRepository.count() > 0) {
            log.info("Produtos já carregados no banco (total: {}). Pulando importação.", produtoRepository.count());
            return;
        }

        File file = new File("PRODUTO.xlsx");
        if (!file.exists()) {
            file = new File("../backend/PRODUTO.xlsx");
        }

        if (!file.exists()) {
            log.warn("Arquivo PRODUTO.xlsx não encontrado.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) return;

            Row headerRow = rowIterator.next();
            Map<String, Integer> colMap = new HashMap<>();
            for (Cell cell : headerRow) {
                colMap.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
            }

            Integer colCod = colMap.get("CODPROD");
            Integer colDesc = colMap.get("DESCRICAO");
            Integer colQtd = colMap.get("QTUNITCX");
            Integer colPreco = colMap.get("CXPTABELA");

            if (colCod == null || colDesc == null) {
                log.warn("Colunas obrigatórias não encontradas no PRODUTO.xlsx: {}", colMap.keySet());
                return;
            }

            List<Produto> produtos = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    Cell cellCod = row.getCell(colCod);
                    if (cellCod == null) continue;

                    long codprod;
                    if (cellCod.getCellType() == CellType.NUMERIC) {
                        codprod = (long) cellCod.getNumericCellValue();
                    } else {
                        String s = cellCod.getStringCellValue().trim();
                        if (s.isEmpty()) continue;
                        codprod = Long.parseLong(s);
                    }

                    Cell cellDesc = row.getCell(colDesc);
                    String desc = (cellDesc != null) ? cellDesc.getStringCellValue().trim() : "";

                    int qtunitcx = 1;
                    if (colQtd != null && row.getCell(colQtd) != null) {
                        Cell c = row.getCell(colQtd);
                        if (c.getCellType() == CellType.NUMERIC) qtunitcx = (int) c.getNumericCellValue();
                        else qtunitcx = Integer.parseInt(c.getStringCellValue().trim());
                    }

                    double cxptabela = 0.0;
                    if (colPreco != null && row.getCell(colPreco) != null) {
                        Cell c = row.getCell(colPreco);
                        if (c.getCellType() == CellType.NUMERIC) cxptabela = c.getNumericCellValue();
                        else cxptabela = Double.parseDouble(c.getStringCellValue().trim().replace(",", "."));
                    }

                    produtos.add(new Produto(codprod, desc, Math.max(1, qtunitcx), cxptabela));
                } catch (Exception e) {
                    // linha inválida ou vazia, pula
                }
            }

            produtoRepository.saveAll(produtos);
            log.info("✅ {} produtos importados do PRODUTO.xlsx com sucesso!", produtos.size());

        } catch (Exception e) {
            log.error("Erro ao importar PRODUTO.xlsx: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void importarClientesSeNecessario() {
        if (clienteRepository.count() > 0) {
            log.info("Clientes já carregados no banco (total: {}). Pulando importação.", clienteRepository.count());
            return;
        }

        File file = new File("CLIENTE.xlsx");
        if (!file.exists()) {
            file = new File("../backend/CLIENTE.xlsx");
        }

        if (!file.exists()) {
            log.warn("Arquivo CLIENTE.xlsx não encontrado.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) return;

            Row headerRow = rowIterator.next();
            Map<String, Integer> colMap = new HashMap<>();
            for (Cell cell : headerRow) {
                colMap.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
            }

            Integer colCod = colMap.get("CODIGO");
            Integer colCpf = colMap.get("CPFCNPJ");
            Integer colNome = colMap.get("NOME");
            if (colNome == null) colNome = colMap.get("RAZAOSOCIAL");
            if (colNome == null) colNome = colMap.get("CLIENTE");

            if (colCod == null || colCpf == null) {
                log.warn("Colunas obrigatórias não encontradas no CLIENTE.xlsx: {}", colMap.keySet());
                return;
            }

            List<Cliente> clientes = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    Cell cellCod = row.getCell(colCod);
                    if (cellCod == null) continue;

                    long codigo;
                    if (cellCod.getCellType() == CellType.NUMERIC) {
                        codigo = (long) cellCod.getNumericCellValue();
                    } else {
                        String s = cellCod.getStringCellValue().trim();
                        if (s.isEmpty()) continue;
                        codigo = Long.parseLong(s);
                    }

                    Cell cellCpf = row.getCell(colCpf);
                    String cpfcnpjRaw = "";
                    if (cellCpf != null) {
                        if (cellCpf.getCellType() == CellType.NUMERIC) {
                            cpfcnpjRaw = String.format("%.0f", cellCpf.getNumericCellValue());
                        } else {
                            cpfcnpjRaw = cellCpf.getStringCellValue().trim();
                        }
                    }

                    String digitsOnly = cpfcnpjRaw.replaceAll("\\D", "");
                    String cpfClean = digitsOnly.length() <= 11 ? String.format("%011d", Long.parseLong(digitsOnly.isEmpty() ? "0" : digitsOnly)) : digitsOnly;

                    String nome = "";
                    if (colNome != null && row.getCell(colNome) != null) {
                        nome = row.getCell(colNome).getStringCellValue().trim();
                    }

                    clientes.add(new Cliente(codigo, cpfcnpjRaw, cpfClean, nome));
                } catch (Exception e) {
                    // pula linha inválida
                }
            }

            clienteRepository.saveAll(clientes);
            log.info("✅ {} clientes importados do CLIENTE.xlsx com sucesso!", clientes.size());

        } catch (Exception e) {
            log.error("Erro ao importar CLIENTE.xlsx: {}", e.getMessage(), e);
        }
    }
}

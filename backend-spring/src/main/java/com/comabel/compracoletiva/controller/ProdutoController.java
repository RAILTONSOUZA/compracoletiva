package com.comabel.compracoletiva.controller;

import com.comabel.compracoletiva.model.Produto;
import com.comabel.compracoletiva.service.CatalogoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/produtos", "/produtos/"})
@CrossOrigin(origins = "*")
public class ProdutoController {

    private final CatalogoService catalogoService;

    public ProdutoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public List<Produto> listarProdutos() {
        return catalogoService.listarProdutos();
    }
}

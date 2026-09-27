package com.comabel.compracoletiva.config;

import com.comabel.compracoletiva.service.CatalogoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CatalogoService catalogoService;

    public DataInitializer(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Override
    public void run(String... args) throws Exception {
        catalogoService.importarProdutosSeNecessario();
        catalogoService.importarClientesSeNecessario();
    }
}

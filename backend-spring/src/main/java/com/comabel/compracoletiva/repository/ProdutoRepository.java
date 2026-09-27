package com.comabel.compracoletiva.repository;

import com.comabel.compracoletiva.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByDescricaoContainingIgnoreCase(String termo);
}

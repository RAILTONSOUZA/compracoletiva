package com.comabel.compracoletiva.repository;

import com.comabel.compracoletiva.model.CompraItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraItemRepository extends JpaRepository<CompraItem, Long> {
    List<CompraItem> findByUsuarioNomeIgnoreCase(String usuarioNome);
    List<CompraItem> findByFardoGrupoIdAndUsuarioNomeIgnoreCase(Long fardoGrupoId, String usuarioNome);
}

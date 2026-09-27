package com.comabel.compracoletiva.repository;

import com.comabel.compracoletiva.model.PedidoConsolidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoConsolidadoRepository extends JpaRepository<PedidoConsolidado, Long> {
    Optional<PedidoConsolidado> findByCodigoPedido(String codigoPedido);
}

package com.comabel.compracoletiva.repository;

import com.comabel.compracoletiva.model.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByStatus(String status);
    List<Oferta> findByStatusAndDataLimiteLessThanEqual(String status, LocalDateTime agora);
}

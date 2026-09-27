package com.comabel.compracoletiva.repository;

import com.comabel.compracoletiva.model.FardoGrupo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FardoGrupoRepository extends JpaRepository<FardoGrupo, Long> {
    List<FardoGrupo> findByOfertaIdAndStatus(Long ofertaId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FardoGrupo f WHERE f.oferta.id = :ofertaId AND f.status = 'EM_ANDAMENTO'")
    List<FardoGrupo> findEmAndamentoComLock(@Param("ofertaId") Long ofertaId);
}

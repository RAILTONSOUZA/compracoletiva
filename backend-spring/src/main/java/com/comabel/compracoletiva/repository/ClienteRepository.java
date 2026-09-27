package com.comabel.compracoletiva.repository;

import com.comabel.compracoletiva.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCpfcnpjClean(String cpfcnpjClean);
    Optional<Cliente> findByCpfcnpj(String cpfcnpj);
}

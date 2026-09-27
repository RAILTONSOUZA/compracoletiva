package com.comabel.compracoletiva.controller;

import com.comabel.compracoletiva.dto.LoginCpfDto;
import com.comabel.compracoletiva.model.Cliente;
import com.comabel.compracoletiva.repository.ClienteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class AuthController {

    private final ClienteRepository clienteRepository;

    public AuthController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @PostMapping({"/login-cpf", "/login-cpf/"})
    public ResponseEntity<Map<String, Object>> loginCpf(@Valid @RequestBody LoginCpfDto dto) {
        String input = dto.getCpfcnpj() != null ? dto.getCpfcnpj().trim() : "";
        String digitsOnly = input.replaceAll("\\D", "");

        if (digitsOnly.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CPF ou CNPJ válido.");
        }

        String cpfClean = digitsOnly.length() <= 11 ? String.format("%011d", Long.parseLong(digitsOnly)) : digitsOnly;

        Optional<Cliente> clienteOpt = clienteRepository.findByCpfcnpjClean(cpfClean);
        if (clienteOpt.isEmpty()) {
            clienteOpt = clienteRepository.findByCpfcnpj(digitsOnly);
        }

        if (clienteOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CPF/CNPJ não encontrado na base de dados da Comabel.");
        }

        Cliente cliente = clienteOpt.get();

        return ResponseEntity.ok(Map.of(
                "valido", true,
                "codigo", cliente.getCodigo(),
                "cpfcnpj", cliente.getCpfcnpjClean(),
                "nome", cliente.getNome() != null ? cliente.getNome() : ""
        ));
    }
}

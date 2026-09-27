package com.comabel.compracoletiva.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @Column(name = "codigo")
    private Long codigo;

    @Column(name = "cpfcnpj")
    private String cpfcnpj;

    @Column(name = "cpfcnpj_clean")
    private String cpfcnpjClean;

    @Column(name = "nome")
    private String nome;

    public Cliente() {
    }

    public Cliente(Long codigo, String cpfcnpj, String cpfcnpjClean, String nome) {
        this.codigo = codigo;
        this.cpfcnpj = cpfcnpj;
        this.cpfcnpjClean = cpfcnpjClean;
        this.nome = nome;
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getCpfcnpj() {
        return cpfcnpj;
    }

    public void setCpfcnpj(String cpfcnpj) {
        this.cpfcnpj = cpfcnpj;
    }

    public String getCpfcnpjClean() {
        return cpfcnpjClean;
    }

    public void setCpfcnpjClean(String cpfcnpjClean) {
        this.cpfcnpjClean = cpfcnpjClean;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}

package com.comabel.compracoletiva.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ofertas")
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "codprod")
    @JsonProperty("codprod")
    private Long codprod;

    @Column(name = "produto_nome", length = 500)
    @JsonProperty("produto_nome")
    private String produtoNome;

    @Column(name = "qtd_por_fardo")
    @JsonProperty("qtd_por_fardo")
    private Integer qtdPorFardo;

    @Column(name = "preco_fardo")
    @JsonProperty("preco_fardo")
    private Double precoFardo;

    @Column(name = "preco_unidade")
    @JsonProperty("preco_unidade")
    private Double precoUnidade;

    @Column(name = "data_limite")
    @JsonProperty("data_limite")
    private LocalDateTime dataLimite;

    @Column(name = "status", length = 50)
    @JsonProperty("status")
    private String status = "ATIVA"; // ATIVA ou ENCERRADA

    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @JsonProperty("fardos")
    private List<FardoGrupo> fardos = new ArrayList<>();

    public Oferta() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCodprod() {
        return codprod;
    }

    public void setCodprod(Long codprod) {
        this.codprod = codprod;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public Integer getQtdPorFardo() {
        return qtdPorFardo;
    }

    public void setQtdPorFardo(Integer qtdPorFardo) {
        this.qtdPorFardo = qtdPorFardo;
    }

    public Double getPrecoFardo() {
        return precoFardo;
    }

    public void setPrecoFardo(Double precoFardo) {
        this.precoFardo = precoFardo;
    }

    public Double getPrecoUnidade() {
        return precoUnidade;
    }

    public void setPrecoUnidade(Double precoUnidade) {
        this.precoUnidade = precoUnidade;
    }

    public LocalDateTime getDataLimite() {
        return dataLimite;
    }

    public void setDataLimite(LocalDateTime dataLimite) {
        this.dataLimite = dataLimite;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<FardoGrupo> getFardos() {
        return fardos;
    }

    public void setFardos(List<FardoGrupo> fardos) {
        this.fardos = fardos;
    }
}

package com.comabel.compracoletiva.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @Column(name = "codprod")
    @JsonProperty("CODPROD")
    private Long codprod;

    @Column(name = "descricao", length = 500)
    @JsonProperty("DESCRICAO")
    private String descricao;

    @Column(name = "qtunitcx")
    @JsonProperty("QTUNITCX")
    private Integer qtunitcx;

    @Column(name = "cxptabela")
    @JsonProperty("CXPTABELA")
    private Double cxptabela;

    public Produto() {
    }

    public Produto(Long codprod, String descricao, Integer qtunitcx, Double cxptabela) {
        this.codprod = codprod;
        this.descricao = descricao;
        this.qtunitcx = qtunitcx;
        this.cxptabela = cxptabela;
    }

    public Long getCodprod() {
        return codprod;
    }

    public void setCodprod(Long codprod) {
        this.codprod = codprod;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getQtunitcx() {
        return qtunitcx;
    }

    public void setQtunitcx(Integer qtunitcx) {
        this.qtunitcx = qtunitcx;
    }

    public Double getCxptabela() {
        return cxptabela;
    }

    public void setCxptabela(Double cxptabela) {
        this.cxptabela = cxptabela;
    }
}

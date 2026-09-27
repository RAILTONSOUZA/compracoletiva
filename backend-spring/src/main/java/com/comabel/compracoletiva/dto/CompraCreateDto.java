package com.comabel.compracoletiva.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompraCreateDto {

    @NotNull
    @JsonProperty("oferta_id")
    private Long ofertaId;

    @NotBlank
    @JsonProperty("usuario_nome")
    private String usuarioNome;

    @NotNull
    @Min(1)
    @JsonProperty("qtd_desejada")
    private Integer qtdDesejada;

    public CompraCreateDto() {
    }

    public CompraCreateDto(Long ofertaId, String usuarioNome, Integer qtdDesejada) {
        this.ofertaId = ofertaId;
        this.usuarioNome = usuarioNome;
        this.qtdDesejada = qtdDesejada;
    }

    public Long getOfertaId() {
        return ofertaId;
    }

    public void setOfertaId(Long ofertaId) {
        this.ofertaId = ofertaId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public Integer getQtdDesejada() {
        return qtdDesejada;
    }

    public void setQtdDesejada(Integer qtdDesejada) {
        this.qtdDesejada = qtdDesejada;
    }
}

package com.comabel.compracoletiva.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class OfertaCreateDto {

    @NotNull
    private Long codprod;

    @JsonProperty("desconto_percentual")
    private Double descontoPercentual;

    @NotNull
    @JsonProperty("data_limite")
    private LocalDateTime dataLimite;

    public OfertaCreateDto() {
    }

    public OfertaCreateDto(Long codprod, Double descontoPercentual, LocalDateTime dataLimite) {
        this.codprod = codprod;
        this.descontoPercentual = descontoPercentual;
        this.dataLimite = dataLimite;
    }

    public Long getCodprod() {
        return codprod;
    }

    public void setCodprod(Long codprod) {
        this.codprod = codprod;
    }

    public Double getDescontoPercentual() {
        return descontoPercentual;
    }

    public void setDescontoPercentual(Double descontoPercentual) {
        this.descontoPercentual = descontoPercentual;
    }

    public LocalDateTime getDataLimite() {
        return dataLimite;
    }

    public void setDataLimite(LocalDateTime dataLimite) {
        this.dataLimite = dataLimite;
    }
}

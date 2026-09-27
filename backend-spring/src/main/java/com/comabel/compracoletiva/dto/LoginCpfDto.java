package com.comabel.compracoletiva.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class LoginCpfDto {

    @NotBlank
    @JsonProperty("cpfcnpj")
    private String cpfcnpj;

    public LoginCpfDto() {
    }

    public LoginCpfDto(String cpfcnpj) {
        this.cpfcnpj = cpfcnpj;
    }

    public String getCpfcnpj() {
        return cpfcnpj;
    }

    public void setCpfcnpj(String cpfcnpj) {
        this.cpfcnpj = cpfcnpj;
    }
}

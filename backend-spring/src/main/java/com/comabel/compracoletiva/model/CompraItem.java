package com.comabel.compracoletiva.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras_itens")
public class CompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fardo_grupo_id")
    @JsonBackReference
    private FardoGrupo fardoGrupo;

    @Column(name = "usuario_nome")
    @JsonProperty("usuario_nome")
    private String usuarioNome;

    @Column(name = "qtd_unidades")
    @JsonProperty("qtd_unidades")
    private Integer qtdUnidades;

    @Column(name = "valor_total")
    @JsonProperty("valor_total")
    private Double valorTotal;

    @Column(name = "data_reserva")
    @JsonProperty("data_reserva")
    private LocalDateTime dataReserva = LocalDateTime.now();

    public CompraItem() {
    }

    public CompraItem(FardoGrupo fardoGrupo, String usuarioNome, Integer qtdUnidades, Double valorTotal) {
        this.fardoGrupo = fardoGrupo;
        this.usuarioNome = usuarioNome;
        this.qtdUnidades = qtdUnidades;
        this.valorTotal = valorTotal;
        this.dataReserva = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FardoGrupo getFardoGrupo() {
        return fardoGrupo;
    }

    public void setFardoGrupo(FardoGrupo fardoGrupo) {
        this.fardoGrupo = fardoGrupo;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public Integer getQtdUnidades() {
        return qtdUnidades;
    }

    public void setQtdUnidades(Integer qtdUnidades) {
        this.qtdUnidades = qtdUnidades;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataReserva() {
        return dataReserva;
    }

    public void setDataReserva(LocalDateTime dataReserva) {
        this.dataReserva = dataReserva;
    }
}

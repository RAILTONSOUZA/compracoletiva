package com.comabel.compracoletiva.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fardos_grupos")
public class FardoGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id")
    @JsonBackReference
    private Oferta oferta;

    @Column(name = "unidades_reservadas")
    @JsonProperty("unidades_reservadas")
    private Integer unidadesReservadas = 0;

    @Column(name = "status", length = 50)
    @JsonProperty("status")
    private String status = "EM_ANDAMENTO"; // EM_ANDAMENTO, CONCLUIDO ou CANCELADO

    @OneToMany(mappedBy = "fardoGrupo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @JsonProperty("itens")
    private List<CompraItem> itens = new ArrayList<>();

    public FardoGrupo() {
    }

    public FardoGrupo(Oferta oferta) {
        this.oferta = oferta;
        this.unidadesReservadas = 0;
        this.status = "EM_ANDAMENTO";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Oferta getOferta() {
        return oferta;
    }

    public void setOferta(Oferta oferta) {
        this.oferta = oferta;
    }

    public Integer getUnidadesReservadas() {
        return unidadesReservadas;
    }

    public void setUnidadesReservadas(Integer unidadesReservadas) {
        this.unidadesReservadas = unidadesReservadas;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CompraItem> getItens() {
        return itens;
    }

    public void setItens(List<CompraItem> itens) {
        this.itens = itens;
    }
}

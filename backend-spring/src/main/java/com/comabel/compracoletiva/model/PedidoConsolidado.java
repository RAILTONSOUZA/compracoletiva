package com.comabel.compracoletiva.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos_consolidados")
public class PedidoConsolidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_pedido", unique = true, nullable = false)
    private String codigoPedido;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    @Column(name = "status_pedido", length = 50)
    private String statusPedido = "ENCERRADO"; // ENCERRADO, FATURADO, CANCELADO

    @Column(name = "data_faturamento")
    private LocalDateTime dataFaturamento;

    @Column(name = "total_fardos")
    private Integer totalFardos = 0;

    @Column(name = "valor_total")
    private Double valorTotal = 0.0;

    public PedidoConsolidado() {
    }

    public PedidoConsolidado(String codigoPedido, LocalDateTime dataEncerramento, String statusPedido, Integer totalFardos, Double valorTotal) {
        this.codigoPedido = codigoPedido;
        this.dataEncerramento = dataEncerramento;
        this.statusPedido = statusPedido;
        this.totalFardos = totalFardos;
        this.valorTotal = valorTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoPedido() {
        return codigoPedido;
    }

    public void setCodigoPedido(String codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public LocalDateTime getDataEncerramento() {
        return dataEncerramento;
    }

    public void setDataEncerramento(LocalDateTime dataEncerramento) {
        this.dataEncerramento = dataEncerramento;
    }

    public String getStatusPedido() {
        return statusPedido;
    }

    public void setStatusPedido(String statusPedido) {
        this.statusPedido = statusPedido;
    }

    public LocalDateTime getDataFaturamento() {
        return dataFaturamento;
    }

    public void setDataFaturamento(LocalDateTime dataFaturamento) {
        this.dataFaturamento = dataFaturamento;
    }

    public Integer getTotalFardos() {
        return totalFardos;
    }

    public void setTotalFardos(Integer totalFardos) {
        this.totalFardos = totalFardos;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }
}

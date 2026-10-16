package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;


@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;

    private String clienteId;
    private String clienteNome; // cópia para exibição e histórico

    private TipoAtendimento tipoAtendimento;
    private String observacao;

    private List<ItemPedido> itens = new ArrayList<>();

    // Valores calculados no servidor
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentualDesconto = BigDecimal.ZERO;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private StatusPedido status;

    // Entrada (RECEBIDO)
    private LocalDateTime dataHoraEntrada;
    private String responsavelEntrada;

    // Saída (SAIU_PARA_ENTREGA ou RETIRADO)
    private LocalDateTime dataHoraSaida;
    private String responsavelSaida;

    // Conclusão
    private LocalDateTime dataHoraFinalizacao;
    private String responsavelFinalizacao;

    // Desconto (somente GERENTE)
    private LocalDateTime dataHoraDesconto;
    private String responsavelDesconto;

    // Cancelamento (somente GERENTE, com justificativa)
    private LocalDateTime dataHoraCancelamento;
    private String responsavelCancelamento;
    private String justificativaCancelamento;

    /** Quantidade total de itens (soma das quantidades). */
    @Transient // calculado: aparece no JSON, mas não é gravado no MongoDB
    public int getQuantidadeItens() {
        return itens == null ? 0 : itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
    }

    @Transient // calculado: aparece no JSON, mas não é gravado no MongoDB
    public boolean isCancelado() {
        return status == StatusPedido.CANCELADO;
    }

    /** Texto amigável do status (vai no JSON para a tela exibir). */
    @Transient // calculado: aparece no JSON, mas não é gravado no MongoDB
    public String getStatusDescricao() {
        return status == null ? null : status.getDescricao();
    }

    @Transient // calculado: aparece no JSON, mas não é gravado no MongoDB
    public String getTipoAtendimentoDescricao() {
        return tipoAtendimento == null ? null : tipoAtendimento.getDescricao();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(LocalDateTime dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getResponsavelEntrada() {
        return responsavelEntrada;
    }

    public void setResponsavelEntrada(String responsavelEntrada) {
        this.responsavelEntrada = responsavelEntrada;
    }

    public LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public void setDataHoraSaida(LocalDateTime dataHoraSaida) {
        this.dataHoraSaida = dataHoraSaida;
    }

    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public void setResponsavelSaida(String responsavelSaida) {
        this.responsavelSaida = responsavelSaida;
    }

    public LocalDateTime getDataHoraFinalizacao() {
        return dataHoraFinalizacao;
    }

    public void setDataHoraFinalizacao(LocalDateTime dataHoraFinalizacao) {
        this.dataHoraFinalizacao = dataHoraFinalizacao;
    }

    public String getResponsavelFinalizacao() {
        return responsavelFinalizacao;
    }

    public void setResponsavelFinalizacao(String responsavelFinalizacao) {
        this.responsavelFinalizacao = responsavelFinalizacao;
    }

    public LocalDateTime getDataHoraDesconto() {
        return dataHoraDesconto;
    }

    public void setDataHoraDesconto(LocalDateTime dataHoraDesconto) {
        this.dataHoraDesconto = dataHoraDesconto;
    }

    public String getResponsavelDesconto() {
        return responsavelDesconto;
    }

    public void setResponsavelDesconto(String responsavelDesconto) {
        this.responsavelDesconto = responsavelDesconto;
    }

    public LocalDateTime getDataHoraCancelamento() {
        return dataHoraCancelamento;
    }

    public void setDataHoraCancelamento(LocalDateTime dataHoraCancelamento) {
        this.dataHoraCancelamento = dataHoraCancelamento;
    }

    public String getResponsavelCancelamento() {
        return responsavelCancelamento;
    }

    public void setResponsavelCancelamento(String responsavelCancelamento) {
        this.responsavelCancelamento = responsavelCancelamento;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }
}

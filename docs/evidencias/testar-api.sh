#!/usr/bin/env bash
# =============================================================================
# Testes de integração da API do Dashboard (seção 7.3 da DOCUMENTACAO.md)
#
# Uso (com a aplicação rodando e o banco com pedidos):
#   GERENTE=gerente01:SENHA ATENDENTE=atendente01:SENHA ./docs/evidencias/testar-api.sh
#
# Opcional: BASE=http://localhost:8080
# Gera docs/evidencias/resultado-api.txt com cada requisição, código HTTP e resposta.
# No Windows, rode pelo Git Bash.
# =============================================================================

BASE="${BASE:-http://localhost:8080}"
API="$BASE/api/dashboard"
GERENTE="${GERENTE:?Informe GERENTE=usuario:senha}"
ATENDENTE="${ATENDENTE:?Informe ATENDENTE=usuario:senha}"
SAIDA="$(dirname "$0")/resultado-api.txt"

HOJE=$(date +%F)
INICIO_MES=$(date +%Y-%m-01)

ok=0
total=0

{
  echo "Testes de integração da API do Dashboard"
  echo "Data/hora: $(date '+%d/%m/%Y %H:%M:%S')   Base: $BASE"
  echo "============================================================"
} > "$SAIDA"

# testar "ID" "descrição" código_esperado [opções do curl...] URL
testar() {
  local id="$1" descricao="$2" esperado="$3"
  shift 3
  total=$((total + 1))
  local corpo codigo
  corpo=$(curl -s -w $'\n%{http_code}' "$@")
  codigo="${corpo##*$'\n'}"
  corpo="${corpo%$'\n'*}"

  local resultado="FALHOU"
  if [ "$codigo" = "$esperado" ]; then
    resultado="OK"
    ok=$((ok + 1))
  fi
  printf '%-4s %-6s esperado %s, obtido %s  %s\n' "$id" "$resultado" "$esperado" "$codigo" "$descricao"
  {
    echo
    echo "[$id] $descricao"
    echo "Requisição: curl $*" | sed -E 's/(-u [^:]+:)[^ ]+/\1*****/'
    echo "HTTP $codigo (esperado $esperado): $resultado"
    echo "Resposta:"
    echo "$corpo" | head -c 1500
    echo
  } >> "$SAIDA"
}

echo "Testando $API ..."
echo

testar T01 "Resumo com período padrão (últimos 30 dias)" 200 -u "$GERENTE" "$API/resumo"
testar T02 "Resumo do mês atual, só entregas" 200 -u "$GERENTE" "$API/resumo?inicio=$INICIO_MES&fim=$HOJE&tipoAtendimento=ENTREGA"
testar T03a "Série por dia" 200 -u "$GERENTE" "$API/vendas/serie?agrupamento=DIA"
testar T03b "Série por semana" 200 -u "$GERENTE" "$API/vendas/serie?agrupamento=SEMANA"
testar T03c "Série por mês" 200 -u "$GERENTE" "$API/vendas/serie?agrupamento=MES"
testar T04a "Vendas por hora" 200 -u "$GERENTE" "$API/vendas/por-hora"
testar T04b "Vendas por dia da semana" 200 -u "$GERENTE" "$API/vendas/por-dia-semana"
testar T04c "Vendas por tipo de atendimento" 200 -u "$GERENTE" "$API/vendas/por-tipo-atendimento"
testar T05 "Situação dos pedidos" 200 -u "$GERENTE" "$API/pedidos/situacao"
testar T06 "Últimos 5 pedidos" 200 -u "$GERENTE" "$API/pedidos/recentes?limite=5"
testar T07 "Ranking por faturamento, categoria Pizza" 200 -u "$GERENTE" "$API/produtos/ranking?criterio=FATURAMENTO&categoria=Pizza&limite=5"
testar T08 "Opções de filtro" 200 -u "$GERENTE" "$API/filtros"
testar T09 "Sem autenticação" 401 "$API/resumo"
testar T10 "ATENDENTE sem permissão" 403 -u "$ATENDENTE" "$API/resumo"
testar T11 "POST não permitido (API somente leitura)" 403 -u "$GERENTE" -X POST "$API/resumo"
testar T12 "Início depois do fim" 400 -u "$GERENTE" "$API/resumo?inicio=2026-11-10&fim=2026-11-01"
testar T13 "Período maior que 366 dias" 400 -u "$GERENTE" "$API/resumo?inicio=2025-01-01&fim=2026-11-10"
testar T14 "Status inválido" 400 -u "$GERENTE" "$API/resumo?status=ENTREGUE"
testar T15 "Data em formato errado" 400 -u "$GERENTE" "$API/resumo?inicio=10/11/2026"
testar T16 "Limite fora da faixa" 400 -u "$GERENTE" "$API/pedidos/recentes?limite=100"

echo
echo "$ok de $total cenários OK"
{
  echo
  echo "============================================================"
  echo "RESULTADO: $ok de $total cenários OK"
} >> "$SAIDA"
echo "Detalhes gravados em $SAIDA"

[ "$ok" -eq "$total" ]

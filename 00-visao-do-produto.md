# Visão do Produto — Finance Personal

## Por que (Motivação)
- Preciso controlar finanças pessoais usando conta PJ também (apps populares não suportam bem).
- Quero automatizar: categorias, tetos de gastos, metas/alertas e projeção até 07/2027.
- Quero reduzir trabalho manual de planilha, mantendo rastreabilidade e controle.

## Propósito
Criar um web app pessoal (React) + API (Spring) para controle financeiro baseado em transações, orçamento e metas.

## Público-alvo
- MVP multiusuário com isolamento por user_id via JWT.
- Sem multi-tenant ou compartilhamento entre usuários.

## Metas de sucesso
- Em 5 minutos por semana eu consigo:
  - Ver gasto do mês por categoria e total
  - Saber se passei/estou perto do teto de alguma categoria
  - Registrar quanto guardei no mês
  - Ver projeção até julho/2027 (casamento/reforma)
  - Revisar e corrigir categorias rapidamente

## Princípios
- Guardrails > perfeição: regras simples de categoria primeiro (sem IA no MVP).
- Dados consistentes: valores em centavos + IN/OUT (sem negativo).
- Tudo auditável: sempre saber como uma transação foi categorizada (manual/regra).
- Segurança por padrão: autenticação JWT obrigatória fora dos endpoints públicos.
- Alertas apenas in-app no MVP (sem e-mail/push).

## Escopo do MVP
- Contas (PF/PJ, cartão, carteira)
- Transações (manual + import CSV)
- Categorias/subcategorias
- Regras automáticas de categorização
- Orçamentos (tetos) por mês/categoria
- Alertas 80% e 100% do teto
- Metas (emergência, casamento/reforma) + aportes
- Dashboard mensal

## Fora do escopo (por enquanto)
- Open Finance direto com bancos (burocrático)
- IA/ML avançado para categoria
- Integrações de pagamento (Pix/boleto)
- App nativo mobile (primeiro PWA)

## Changelog
- Ajustei o público-alvo para MVP single-user com user_id mantido nas tabelas para evolução futura.
- Registrei que alertas no MVP são apenas in-app, reforçando o escopo de entrega.
- Atualizei o escopo do MVP para importar apenas CSV.

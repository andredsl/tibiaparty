# Tibia Party Finder - Especificação Funcional

## 1. Objetivo do Produto

Criar uma plataforma web que permita jogadores de Tibia encontrar e formar **parties temporárias** (PTs) de forma **rápida, prática e confiável** para realizar hunts e bosses, organizadas por servidor.

**Proposta de valor:** "Encontre sua PT em menos de 30 segundos."

---

## 2. Público-Alvo

### 2.1 Perfil Primário
- Jogadores de Tibia nível 100+
- Jogam em horários variados e não possuem guild ativa
- Buscam PTs ocasionais sem compromisso fixo
- Valorizam praticidade sobre funcionalidades complexas

### 2.2 Perfil Secundário
- Jogadores de guilds que precisam completar vagas em PTs
- Líderes de hunt que buscam vocações específicas
- Jogadores retornando ao jogo que ainda não têm contatos

### 2.3 Comportamento Esperado
- Acesso rápido (mobile ou desktop)
- Sessões curtas (1-3 minutos para encontrar PT)
- Uso frequente mas irregular (conforme necessidade de hunt)

---

## 3. Principais Dores Resolvidas

| Dor | Solução |
|-----|---------|
| **Ficar spammando no World Chat** procurando PT | Feed organizado de PTs disponíveis filtrado por server |
| **Não saber quem está online** querendo hunt | Lista em tempo real de jogadores buscando PT |
| **Entrar em PT com fake ou troll** | Sistema de verificação de personagem + reputação |
| **Perder tempo com PT que não sai** | Status de PT (formando/pronta/em hunt) + timeout automático |
| **Dificuldade em achar vocação específica** | Filtro por vocação necessária |
| **Não encontrar PT para boss específico** | Categoria separada para bosses com horário |
| **Comunicação fragmentada** | Chat integrado por PT |

---

## 4. Fluxo Principal do Usuário

### 4.1 Primeiro Acesso (Onboarding)
```
[Acessa o site]
       ↓
[Clica em "Entrar com Tibia"]
       ↓
[Informa nome do personagem principal]
       ↓
[Sistema gera código único]
       ↓
[Usuário coloca código no Comment do personagem no Tibia.com]
       ↓
[Sistema valida via API/scraping do Tibia.com]
       ↓
[Conta verificada - dados do char importados automaticamente]
       ↓
[Seleciona server padrão]
       ↓
[Acessa dashboard]
```

### 4.2 Fluxo: Buscar PT Existente
```
[Login]
   ↓
[Dashboard mostra PTs do seu server]
   ↓
[Filtra: Hunt ou Boss]
   ↓
[Vê lista de PTs abertas com: local/boss, level range, vagas, vocações necessárias]
   ↓
[Clica em "Entrar"]
   ↓
[Líder recebe notificação]
   ↓
[Líder aprova/recusa]
   ↓
[Se aprovado: entra no chat da PT]
   ↓
[PT fica em status "Pronta" quando completa]
```

### 4.3 Fluxo: Criar Nova PT
```
[Login]
   ↓
[Clica em "Criar PT"]
   ↓
[Seleciona tipo: Hunt ou Boss]
   ↓
[Preenche formulário rápido:]
   - Hunt: Local, Level mín/máx, Vocações necessárias, Tipo de loot (split/random)
   - Boss: Nome do boss, Horário previsto, Level mínimo
   ↓
[PT criada e visível no feed]
   ↓
[Recebe solicitações de entrada]
   ↓
[Aprova jogadores]
   ↓
[Quando completa: marca como "Em Hunt"]
   ↓
[Ao finalizar: encerra PT e avalia membros]
```

### 4.4 Fluxo: "Estou Disponível" (Modo Passivo)
```
[Login]
   ↓
[Ativa "Estou LFP" (Looking for Party)]
   ↓
[Define: vocação, level, preferência (hunt/boss/ambos)]
   ↓
[Aparece na lista de jogadores disponíveis]
   ↓
[Líderes podem convidar diretamente]
   ↓
[Recebe convite → aceita/recusa]
```

---

## 5. Regras de Negócio Principais

### 5.1 Verificação e Anti-Fake

| Regra | Descrição |
|-------|-----------|
| **RN01** | Todo usuário deve verificar pelo menos 1 personagem via código no comment do Tibia.com |
| **RN02** | Dados do personagem (level, vocação, world) são atualizados automaticamente a cada 6 horas |
| **RN03** | Personagens com level < 50 não podem criar PTs (apenas entrar) |
| **RN04** | Conta Tibia.com deve ter pelo menos 30 dias de criação |
| **RN05** | Limite de 1 conta no site por conta Tibia.com |

### 5.2 Criação e Gestão de PT

| Regra | Descrição |
|-------|-----------|
| **RN06** | PT de Hunt: máximo 5 membros (4 vocações + 1 extra) |
| **RN07** | PT de Boss: máximo definido pelo líder (até 20) |
| **RN08** | PT expira automaticamente após 2 horas sem atividade |
| **RN09** | PT marcada como "Em Hunt" expira após 4 horas |
| **RN10** | Usuário pode participar de apenas 1 PT ativa por vez |
| **RN11** | Líder pode definir level range (ex: 300-500) |
| **RN12** | Líder pode exigir vocações específicas (ex: "preciso de ED") |
| **RN13** | PT de Boss deve ter horário estimado (obrigatório) |

### 5.3 Reputação e Anti-Spam

| Regra | Descrição |
|-------|-----------|
| **RN14** | Sistema de reputação: membros avaliam uns aos outros (1-5 estrelas) após PT |
| **RN15** | Avaliação é opcional mas incentivada |
| **RN16** | Jogadores com reputação < 2.0 ficam marcados com aviso |
| **RN17** | Jogadores com 3+ denúncias de "não apareceu" em 7 dias: bloqueio temporário de 24h |
| **RN18** | Rate limit: máximo 3 PTs criadas por dia por usuário |
| **RN19** | Rate limit: máximo 10 solicitações de entrada por hora |
| **RN20** | Líder que cancelar PT com membros confirmados perde 0.2 de reputação |

### 5.4 Separação por Server

| Regra | Descrição |
|-------|-----------|
| **RN21** | Cada PT pertence a um único server |
| **RN22** | Usuário vê por padrão apenas PTs do seu server principal |
| **RN23** | Pode alternar entre servers onde tem chars verificados |
| **RN24** | Não é possível entrar em PT de server onde não tem char verificado |

### 5.5 Chat e Comunicação

| Regra | Descrição |
|-------|-----------|
| **RN25** | Chat disponível apenas para membros confirmados da PT |
| **RN26** | Histórico do chat é deletado quando PT encerra |
| **RN27** | Palavras bloqueadas: links externos, palavrões, spam repetido |
| **RN28** | Denúncia de mensagem disponível para todos os membros |

---

## 6. Entidades Principais

```
USUÁRIO
├── id
├── email
├── senha (hash)
├── reputação média
├── total de hunts
├── data de criação
└── status (ativo/bloqueado)

PERSONAGEM (verificado)
├── id
├── usuário_id
├── nome
├── world (server)
├── level
├── vocação
├── verificado_em
├── última_atualização
└── é_principal (boolean)

PT (Party)
├── id
├── tipo (hunt/boss)
├── líder_id
├── world
├── status (formando/pronta/em_hunt/encerrada)
├── level_min
├── level_max
├── local_ou_boss
├── vocações_necessárias[]
├── max_membros
├── horário_previsto (boss)
├── tipo_loot (hunt)
├── criada_em
├── expira_em
└── encerrada_em

MEMBRO_PT
├── pt_id
├── personagem_id
├── papel (líder/membro)
├── status (pendente/confirmado/recusado)
├── entrou_em
└── avaliação_recebida

AVALIAÇÃO
├── pt_id
├── avaliador_id
├── avaliado_id
├── nota (1-5)
├── comentário (opcional)
└── criada_em
```

---

## 7. Métricas de Sucesso (KPIs)

| Métrica | Meta |
|---------|------|
| Tempo médio para encontrar PT | < 2 minutos |
| Taxa de PT que realmente acontece | > 70% |
| Taxa de verificação completa | > 90% |
| Retenção semanal | > 40% |
| NPS (Net Promoter Score) | > 50 |
| Denúncias de fake/troll | < 5% das PTs |

---

## 8. Diferenciais Competitivos

1. **Verificação real** - Diferente de Discord/forums onde qualquer um pode mentir stats
2. **Específico para PT temporária** - Não tenta ser rede social ou guild finder
3. **Mobile-first** - Funciona bem no celular enquanto joga
4. **Sem fricção** - Mínimo de cliques para entrar em PT
5. **Por server** - Foco total na relevância local

---

## 9. Fora do Escopo (V1)

- Sistema de guild
- Marketplace de itens
- Tracking de bosses (já existem sites para isso)
- Sistema de pagamento/premium
- App nativo (será PWA)
- Integração com Discord (futuro)

---

## 10. Riscos Identificados

| Risco | Mitigação |
|-------|-----------|
| Tibia.com bloquear scraping | Implementar cache agressivo, rate limiting, fallback manual |
| Baixa adoção inicial (poucos users = poucas PTs) | Lançar focado em 2-3 servers BR populosos primeiro |
| Abuso do sistema de reputação | Moderação manual + algoritmo anti-manipulação |
| Usuários não encerrarem PTs | Timeout automático + gamificação para encerrar |

---

*Documento criado para o projeto Tibia Party Finder*
*Versão 1.0*

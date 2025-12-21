# Tibia Party Finder - Arquitetura Técnica

## 1. Visão Geral da Arquitetura

### 1.1 Diagrama Lógico (Camadas)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE (Browser)                               │
│                        HTML/CSS/JS + Thymeleaf Render                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼ HTTP/HTTPS
┌─────────────────────────────────────────────────────────────────────────────┐
│                            SPRING BOOT APPLICATION                           │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         PRESENTATION LAYER                             │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │   Web       │  │   API       │  │  Thymeleaf  │  │  Exception  │  │  │
│  │  │ Controllers │  │ Controllers │  │  Views      │  │  Handlers   │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                          BUSINESS LAYER                                │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │   Party     │  │  Character  │  │    User     │  │ Verification│  │  │
│  │  │  Service    │  │  Service    │  │   Service   │  │   Service   │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                   │  │
│  │  │  Rating     │  │Notification │  │   Tibia     │                   │  │
│  │  │  Service    │  │  Service    │  │ API Client  │                   │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                   │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        PERSISTENCE LAYER                               │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │   Party     │  │  Character  │  │    User     │  │   Rating    │  │  │
│  │  │ Repository  │  │ Repository  │  │ Repository  │  │ Repository  │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼ JPA/Hibernate                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                          DOMAIN LAYER                                  │  │
│  │  ┌──────┐ ┌───────────┐ ┌───────┐ ┌─────────────┐ ┌────────┐         │  │
│  │  │ User │ │ Character │ │ Party │ │ PartyMember │ │ Rating │   ...   │  │
│  │  └──────┘ └───────────┘ └───────┘ └─────────────┘ └────────┘         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              POSTGRESQL                                      │
│                      (Particionado por World/Server)                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Diagrama de Componentes

```
                    ┌──────────────────────────────────────┐
                    │           Tibia.com                   │
                    │     (Verificação de Character)        │
                    └──────────────────────────────────────┘
                                      ▲
                                      │ HTTP (Scraping/API)
                                      │
┌─────────────────────────────────────┴────────────────────────────────────────┐
│                                                                              │
│    ┌────────────┐      ┌────────────┐      ┌────────────┐                   │
│    │   Web      │      │  Security  │      │  Scheduled │                   │
│    │  Filter    │─────▶│   Filter   │─────▶│   Tasks    │                   │
│    └────────────┘      └────────────┘      └────────────┘                   │
│          │                   │                   │                           │
│          ▼                   ▼                   ▼                           │
│    ┌─────────────────────────────────────────────────────┐                  │
│    │                    CONTROLLERS                       │                  │
│    │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │                  │
│    │  │  Home    │ │  Party   │ │Character │ │  Auth  │ │                  │
│    │  │Controller│ │Controller│ │Controller│ │  Ctrl  │ │                  │
│    │  └──────────┘ └──────────┘ └──────────┘ └────────┘ │                  │
│    └─────────────────────────────────────────────────────┘                  │
│                              │                                               │
│                              ▼                                               │
│    ┌─────────────────────────────────────────────────────┐                  │
│    │                     SERVICES                         │                  │
│    │  ┌────────────┐ ┌────────────┐ ┌──────────────────┐ │                  │
│    │  │PartyService│ │UserService │ │VerificationSvc   │ │                  │
│    │  │            │ │            │ │                  │ │                  │
│    │  │- create    │ │- register  │ │- generateCode    │ │                  │
│    │  │- join      │ │- login     │ │- verifyCharacter │ │                  │
│    │  │- leave     │ │- getProfile│ │- syncCharacter   │ │                  │
│    │  │- findByWorld│ │           │ │                  │ │                  │
│    │  └────────────┘ └────────────┘ └──────────────────┘ │                  │
│    │  ┌────────────┐ ┌────────────┐ ┌──────────────────┐ │                  │
│    │  │RatingServ  │ │TibiaClient │ │ NotificationSvc  │ │                  │
│    │  │            │ │            │ │ (Interface)      │ │                  │
│    │  │- rate      │ │- fetchChar │ │                  │ │                  │
│    │  │- getAvg    │ │- parseData │ │- notify(event)   │ │                  │
│    │  └────────────┘ └────────────┘ └──────────────────┘ │                  │
│    └─────────────────────────────────────────────────────┘                  │
│                              │                                               │
│                              ▼                                               │
│    ┌─────────────────────────────────────────────────────┐                  │
│    │                  REPOSITORIES                        │                  │
│    │  ┌────────────┐ ┌────────────┐ ┌──────────────────┐ │                  │
│    │  │PartyRepo   │ │UserRepo    │ │CharacterRepo     │ │                  │
│    │  │            │ │            │ │                  │ │                  │
│    │  │+findByWorld│ │+findByEmail│ │+findByNameAndWorld│ │                  │
│    │  │+findActive │ │            │ │+findByUserId     │ │                  │
│    │  └────────────┘ └────────────┘ └──────────────────┘ │                  │
│    └─────────────────────────────────────────────────────┘                  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                    ┌──────────────────────────────────────┐
                    │            PostgreSQL                 │
                    │  ┌────────┐ ┌────────┐ ┌──────────┐ │
                    │  │ users  │ │ parties│ │characters│ │
                    │  └────────┘ └────────┘ └──────────┘ │
                    │  ┌────────┐ ┌────────┐ ┌──────────┐ │
                    │  │members │ │ratings │ │  worlds  │ │
                    │  └────────┘ └────────┘ └──────────┘ │
                    └──────────────────────────────────────┘
```

---

## 2. Estrutura de Pacotes

```
src/main/java/com/tibia/partyfinder/
│
├── TibiaPartyFinderApplication.java          # Main class
│
├── config/                                    # Configurações
│   ├── SecurityConfig.java                   # Spring Security
│   ├── WebConfig.java                        # MVC configs
│   ├── AsyncConfig.java                      # Async/Threading
│   ├── CacheConfig.java                      # Cache (Caffeine)
│   └── SchedulerConfig.java                  # Jobs agendados
│
├── domain/                                    # Entidades JPA
│   ├── entity/
│   │   ├── User.java
│   │   ├── Character.java
│   │   ├── Party.java
│   │   ├── PartyMember.java
│   │   ├── Rating.java
│   │   └── World.java
│   ├── enums/
│   │   ├── PartyType.java                    # HUNT, BOSS
│   │   ├── PartyStatus.java                  # FORMING, READY, IN_PROGRESS, FINISHED
│   │   ├── Vocation.java                     # EK, RP, ED, MS
│   │   ├── MemberStatus.java                 # PENDING, CONFIRMED, REJECTED
│   │   └── MemberRole.java                   # LEADER, MEMBER
│   └── vo/                                    # Value Objects
│       ├── LevelRange.java
│       └── VerificationCode.java
│
├── repository/                                # Spring Data JPA
│   ├── UserRepository.java
│   ├── CharacterRepository.java
│   ├── PartyRepository.java
│   ├── PartyMemberRepository.java
│   ├── RatingRepository.java
│   └── WorldRepository.java
│
├── service/                                   # Lógica de negócio
│   ├── UserService.java
│   ├── CharacterService.java
│   ├── PartyService.java
│   ├── RatingService.java
│   ├── VerificationService.java
│   ├── notification/                          # Notificações (extensível)
│   │   ├── NotificationService.java          # Interface
│   │   ├── InAppNotificationService.java     # Implementação padrão
│   │   ├── DiscordNotificationService.java   # Futura implementação
│   │   └── WebPushNotificationService.java   # Futura implementação
│   └── external/
│       └── TibiaApiClient.java               # Integração Tibia.com
│
├── controller/                                # Controllers MVC
│   ├── web/                                   # Páginas HTML (Thymeleaf)
│   │   ├── HomeController.java
│   │   ├── AuthController.java
│   │   ├── PartyController.java
│   │   ├── CharacterController.java
│   │   └── ProfileController.java
│   └── api/                                   # REST APIs (futuro/AJAX)
│       ├── PartyApiController.java
│       └── NotificationApiController.java
│
├── dto/                                       # Data Transfer Objects
│   ├── request/
│   │   ├── CreatePartyRequest.java
│   │   ├── JoinPartyRequest.java
│   │   ├── RegisterRequest.java
│   │   └── VerifyCharacterRequest.java
│   ├── response/
│   │   ├── PartyResponse.java
│   │   ├── CharacterResponse.java
│   │   └── UserProfileResponse.java
│   └── mapper/
│       ├── PartyMapper.java
│       └── CharacterMapper.java
│
├── security/                                  # Segurança
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   └── AuthenticationSuccessHandler.java
│
├── scheduler/                                 # Jobs agendados
│   ├── PartyExpirationJob.java               # Expira PTs inativas
│   ├── CharacterSyncJob.java                 # Atualiza dados dos chars
│   └── CleanupJob.java                       # Limpeza de dados antigos
│
├── exception/                                 # Exceções customizadas
│   ├── GlobalExceptionHandler.java           # @ControllerAdvice
│   ├── PartyNotFoundException.java
│   ├── CharacterNotVerifiedException.java
│   ├── PartyFullException.java
│   └── RateLimitExceededException.java
│
├── validation/                                # Validadores customizados
│   ├── LevelRangeValidator.java
│   └── WorldValidator.java
│
└── util/
    ├── SlugGenerator.java                    # URLs amigáveis
    └── VerificationCodeGenerator.java
```

```
src/main/resources/
│
├── application.yml                            # Configurações principais
├── application-dev.yml                        # Perfil desenvolvimento
├── application-prod.yml                       # Perfil produção
│
├── templates/                                 # Thymeleaf templates
│   ├── layout/
│   │   ├── base.html                         # Template base
│   │   ├── header.html                       # Fragment cabeçalho
│   │   └── footer.html                       # Fragment rodapé
│   ├── home/
│   │   └── index.html
│   ├── auth/
│   │   ├── login.html
│   │   └── register.html
│   ├── party/
│   │   ├── list.html                         # Lista de PTs
│   │   ├── create.html                       # Criar PT
│   │   ├── detail.html                       # Detalhes da PT
│   │   └── fragments/
│   │       ├── party-card.html               # Card de PT reutilizável
│   │       └── member-list.html
│   ├── character/
│   │   ├── verify.html
│   │   └── list.html
│   ├── profile/
│   │   └── view.html
│   └── error/
│       ├── 404.html
│       └── 500.html
│
├── static/
│   ├── css/
│   │   └── style.css
│   ├── js/
│   │   ├── app.js
│   │   └── party.js
│   └── images/
│       └── vocations/
│
├── db/migration/                              # Flyway migrations
│   ├── V1__create_users_table.sql
│   ├── V2__create_characters_table.sql
│   ├── V3__create_parties_table.sql
│   ├── V4__create_party_members_table.sql
│   ├── V5__create_ratings_table.sql
│   └── V6__create_worlds_table.sql
│
└── messages/                                  # i18n
    ├── messages.properties
    └── messages_pt_BR.properties
```

---

## 3. Fluxo de Requisições HTTP

### 3.1 Fluxo Geral (Request → Response)

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Browser  │───▶│  Filter  │───▶│Controller│───▶│ Service  │───▶│Repository│
│          │    │  Chain   │    │          │    │          │    │          │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                     │               │               │               │
                     │               │               │               ▼
                     │               │               │          ┌──────────┐
                     │               │               │          │PostgreSQL│
                     │               │               │          └──────────┘
                     │               │               │               │
                     ▼               ▼               ▼               │
              ┌────────────────────────────────────────────────────────┐
              │                    RESPONSE FLOW                        │
              │  Repository → Service → Controller → View → Browser    │
              └────────────────────────────────────────────────────────┘
```

### 3.2 Exemplo: Listar PTs por Server

```
GET /parties?world=Antica&type=HUNT

┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. REQUEST                                                                   │
│    Browser → GET /parties?world=Antica&type=HUNT                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. SECURITY FILTER                                                           │
│    - Verifica se usuário está autenticado                                   │
│    - Carrega UserDetails na sessão                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. CONTROLLER (PartyController.java)                                        │
│                                                                              │
│    @GetMapping("/parties")                                                  │
│    public String listParties(                                               │
│        @RequestParam String world,                                          │
│        @RequestParam PartyType type,                                        │
│        Model model,                                                          │
│        @AuthenticationPrincipal UserDetails user) {                         │
│                                                                              │
│        List<PartyResponse> parties = partyService                           │
│            .findActiveByWorldAndType(world, type);                          │
│                                                                              │
│        model.addAttribute("parties", parties);                              │
│        model.addAttribute("currentWorld", world);                           │
│        return "party/list";                                                 │
│    }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. SERVICE (PartyService.java)                                              │
│                                                                              │
│    public List<PartyResponse> findActiveByWorldAndType(                     │
│            String world, PartyType type) {                                  │
│                                                                              │
│        List<Party> parties = partyRepository                                │
│            .findByWorldNameAndTypeAndStatusIn(                              │
│                world,                                                        │
│                type,                                                         │
│                List.of(FORMING, READY)                                      │
│            );                                                                │
│                                                                              │
│        return parties.stream()                                              │
│            .map(partyMapper::toResponse)                                    │
│            .toList();                                                        │
│    }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 5. REPOSITORY (PartyRepository.java)                                        │
│                                                                              │
│    @Query("""                                                               │
│        SELECT p FROM Party p                                                │
│        JOIN FETCH p.world w                                                 │
│        JOIN FETCH p.leader l                                                │
│        WHERE w.name = :world                                                │
│        AND p.type = :type                                                   │
│        AND p.status IN :statuses                                            │
│        ORDER BY p.createdAt DESC                                            │
│    """)                                                                      │
│    List<Party> findByWorldNameAndTypeAndStatusIn(...);                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 6. DATABASE QUERY                                                            │
│                                                                              │
│    SELECT p.*, w.*, l.*                                                     │
│    FROM parties p                                                            │
│    INNER JOIN worlds w ON p.world_id = w.id                                 │
│    INNER JOIN characters l ON p.leader_id = l.id                            │
│    WHERE w.name = 'Antica'                                                  │
│    AND p.type = 'HUNT'                                                      │
│    AND p.status IN ('FORMING', 'READY')                                     │
│    ORDER BY p.created_at DESC;                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 7. THYMELEAF RENDER (party/list.html)                                       │
│                                                                              │
│    <div th:each="party : ${parties}">                                       │
│        <div th:replace="~{party/fragments/party-card :: card(${party})}">  │
│    </div>                                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 8. RESPONSE                                                                  │
│    HTML renderizado → Browser                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Exemplo: Criar PT (POST)

```
POST /parties/create

┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. FORM SUBMIT                                                               │
│    { type: "HUNT", world: "Antica", location: "Asuras",                     │
│      levelMin: 300, levelMax: 500, vocationsNeeded: ["ED", "EK"] }          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. CONTROLLER                                                                │
│                                                                              │
│    @PostMapping("/parties/create")                                          │
│    public String createParty(                                               │
│        @Valid @ModelAttribute CreatePartyRequest request,                   │
│        BindingResult result,                                                │
│        @AuthenticationPrincipal CustomUserDetails user,                     │
│        RedirectAttributes redirectAttrs) {                                  │
│                                                                              │
│        if (result.hasErrors()) {                                            │
│            return "party/create";                                           │
│        }                                                                     │
│                                                                              │
│        Party party = partyService.create(request, user.getCharacterId());   │
│        redirectAttrs.addFlashAttribute("success", "PT criada!");            │
│        return "redirect:/parties/" + party.getId();                         │
│    }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. SERVICE                                                                   │
│                                                                              │
│    @Transactional                                                            │
│    public Party create(CreatePartyRequest req, Long leaderId) {             │
│        // Validações de negócio                                             │
│        validateLeaderCanCreateParty(leaderId);                              │
│        validateDailyLimit(leaderId);                                        │
│                                                                              │
│        Character leader = characterRepository.findById(leaderId)            │
│            .orElseThrow();                                                   │
│                                                                              │
│        Party party = Party.builder()                                        │
│            .type(req.getType())                                             │
│            .world(leader.getWorld())                                        │
│            .leader(leader)                                                   │
│            .location(req.getLocation())                                     │
│            .levelRange(new LevelRange(req.getLevelMin(), req.getLevelMax()))│
│            .vocationsNeeded(req.getVocationsNeeded())                       │
│            .status(PartyStatus.FORMING)                                     │
│            .expiresAt(LocalDateTime.now().plusHours(2))                     │
│            .build();                                                         │
│                                                                              │
│        return partyRepository.save(party);                                  │
│    }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. REDIRECT                                                                  │
│    302 Redirect → /parties/{id}                                             │
│    PRG Pattern (Post-Redirect-Get)                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Estratégia para Separação por Server (World)

### 4.1 Modelo de Dados

```sql
-- Tabela de Worlds (servers)
CREATE TABLE worlds (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,        -- Ex: "Antica", "Secura"
    location VARCHAR(20) NOT NULL,           -- "SA", "EU", "NA"
    pvp_type VARCHAR(20) NOT NULL,           -- "Open PvP", "Optional PvP", etc.
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Index para busca rápida
CREATE INDEX idx_worlds_name ON worlds(name);
CREATE INDEX idx_worlds_location ON worlds(location);

-- Parties sempre associadas a um world
CREATE TABLE parties (
    id SERIAL PRIMARY KEY,
    world_id INTEGER NOT NULL REFERENCES worlds(id),
    -- ... outros campos

    -- Index composto para queries por world
    CONSTRAINT fk_party_world FOREIGN KEY (world_id) REFERENCES worlds(id)
);

-- Index para busca de parties por world (query mais comum)
CREATE INDEX idx_parties_world_status ON parties(world_id, status);
CREATE INDEX idx_parties_world_type_status ON parties(world_id, type, status);
```

### 4.2 Estratégia de Query

```java
// PartyRepository.java
public interface PartyRepository extends JpaRepository<Party, Long> {

    // Query principal - sempre filtrar por world primeiro
    @Query("""
        SELECT p FROM Party p
        WHERE p.world.id = :worldId
        AND p.status IN :statuses
        AND p.type = :type
        ORDER BY p.createdAt DESC
    """)
    List<Party> findActiveByWorld(
        @Param("worldId") Long worldId,
        @Param("type") PartyType type,
        @Param("statuses") List<PartyStatus> statuses
    );

    // Com paginação para escalar
    @Query("""
        SELECT p FROM Party p
        WHERE p.world.name = :worldName
        AND p.status IN :statuses
        ORDER BY p.createdAt DESC
    """)
    Page<Party> findActiveByWorldName(
        @Param("worldName") String worldName,
        @Param("statuses") List<PartyStatus> statuses,
        Pageable pageable
    );
}
```

### 4.3 Seleção de World na Sessão

```java
// WorldContext - Mantém o world selecionado na sessão
@Component
@SessionScope
public class WorldContext {
    private World currentWorld;

    public World getCurrentWorld() {
        return currentWorld;
    }

    public void setCurrentWorld(World world) {
        this.currentWorld = world;
    }
}

// Interceptor para garantir world selecionado
@Component
public class WorldInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {

        // Rotas que não precisam de world
        if (isPublicRoute(request.getRequestURI())) {
            return true;
        }

        WorldContext ctx = getWorldContext();
        if (ctx.getCurrentWorld() == null) {
            response.sendRedirect("/select-world");
            return false;
        }

        return true;
    }
}
```

### 4.4 Cache por World

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)  // Cache curto para dados em tempo real
            .maximumSize(1000));
        return cacheManager;
    }
}

// Service com cache por world
@Service
public class PartyService {

    @Cacheable(value = "parties", key = "#worldName + '-' + #type")
    public List<PartyResponse> findActive(String worldName, PartyType type) {
        // Query ao banco
    }

    @CacheEvict(value = "parties", key = "#party.world.name + '-' + #party.type")
    public Party create(CreatePartyRequest request, Long leaderId) {
        // Cria e invalida cache do world
    }
}
```

### 4.5 Preparação para Escala (Futuro)

```
FASE 1 (MVP):
- Single database
- Index por world_id
- Cache em memória

FASE 2 (Crescimento):
- Read replicas por região (SA, EU, NA)
- Cache distribuído (Redis)

FASE 3 (Escala):
- Database sharding por world
- Cada região com seu cluster
```

```yaml
# application-prod.yml (Fase 2)
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary-db:5432/tibia_pf
    replica:
      url: jdbc:postgresql://replica-db:5432/tibia_pf

  cache:
    type: redis
    redis:
      host: redis-cluster
      time-to-live: 60000
```

---

## 5. Estratégia de Notificações (Extensível)

### 5.1 Arquitetura de Notificações

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NOTIFICATION SYSTEM                                  │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                     NotificationService (Interface)                  │    │
│  │  + notify(userId, event)                                            │    │
│  │  + notifyPartyMembers(partyId, event)                              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                         │
│              ┌─────────────────────┼─────────────────────┐                  │
│              │                     │                     │                  │
│              ▼                     ▼                     ▼                  │
│  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐         │
│  │  InAppNotification │ │DiscordNotification│ │WebPushNotification│         │
│  │      Service       │ │     Service       │ │     Service       │         │
│  │                    │ │                    │ │                    │         │
│  │ - Salva no banco  │ │ - Webhook Discord │ │ - Service Worker  │         │
│  │ - Polling/SSE     │ │ - Bot commands    │ │ - VAPID keys      │         │
│  │                    │ │                    │ │                    │         │
│  │ [MVP - ATIVO]     │ │ [FUTURO]          │ │ [FUTURO]          │         │
│  └───────────────────┘ └───────────────────┘ └───────────────────┘         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Interface Base

```java
// NotificationService.java - Interface base
public interface NotificationService {

    void notify(Long userId, NotificationEvent event);

    void notifyPartyMembers(Long partyId, NotificationEvent event);

    boolean supports(NotificationType type);
}

// NotificationEvent.java - Evento base
@Data
@Builder
public class NotificationEvent {
    private NotificationEventType type;  // JOIN_REQUEST, MEMBER_JOINED, PARTY_READY, etc.
    private Long partyId;
    private Long triggeredByUserId;
    private String message;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
}

// NotificationEventType.java
public enum NotificationEventType {
    JOIN_REQUEST,           // Alguém pediu para entrar na PT
    JOIN_APPROVED,          // Líder aprovou entrada
    JOIN_REJECTED,          // Líder recusou entrada
    MEMBER_JOINED,          // Novo membro entrou
    MEMBER_LEFT,            // Membro saiu
    PARTY_READY,            // PT está completa
    PARTY_STARTED,          // PT iniciou hunt
    PARTY_FINISHED,         // PT encerrada
    PARTY_EXPIRING,         // PT vai expirar em 15min
    RATE_RECEIVED           // Recebeu avaliação
}
```

### 5.3 Implementação MVP (In-App)

```java
// InAppNotificationService.java
@Service
@Primary  // Implementação padrão no MVP
public class InAppNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;  // Server-Sent Events

    @Override
    @Async
    public void notify(Long userId, NotificationEvent event) {
        // 1. Persiste notificação
        Notification notification = Notification.builder()
            .userId(userId)
            .type(event.getType())
            .message(event.getMessage())
            .partyId(event.getPartyId())
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

        notificationRepository.save(notification);

        // 2. Envia em tempo real via SSE (se usuário conectado)
        sseEmitterService.sendToUser(userId, notification);
    }

    @Override
    public void notifyPartyMembers(Long partyId, NotificationEvent event) {
        List<Long> memberIds = partyMemberRepository.findUserIdsByPartyId(partyId);
        memberIds.forEach(userId -> notify(userId, event));
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.IN_APP;
    }
}
```

### 5.4 Preparação para Discord (Futuro)

```java
// DiscordNotificationService.java
@Service
@ConditionalOnProperty(name = "notifications.discord.enabled", havingValue = "true")
public class DiscordNotificationService implements NotificationService {

    @Value("${notifications.discord.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    @Override
    @Async
    public void notify(Long userId, NotificationEvent event) {
        // Busca Discord ID vinculado ao usuário
        Optional<String> discordId = userRepository.findDiscordIdByUserId(userId);

        if (discordId.isEmpty()) {
            return;  // Usuário não vinculou Discord
        }

        DiscordMessage message = DiscordMessage.builder()
            .content(formatMessage(event))
            .username("Tibia Party Finder")
            .build();

        restTemplate.postForEntity(webhookUrl, message, Void.class);
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.DISCORD;
    }
}
```

### 5.5 Preparação para Web Push (Futuro)

```java
// WebPushNotificationService.java
@Service
@ConditionalOnProperty(name = "notifications.webpush.enabled", havingValue = "true")
public class WebPushNotificationService implements NotificationService {

    private final PushService pushService;  // web-push library
    private final PushSubscriptionRepository subscriptionRepository;

    @Override
    @Async
    public void notify(Long userId, NotificationEvent event) {
        List<PushSubscription> subscriptions =
            subscriptionRepository.findByUserId(userId);

        subscriptions.forEach(sub -> {
            try {
                pushService.send(new Notification(
                    sub.getEndpoint(),
                    sub.getKeys(),
                    buildPayload(event)
                ));
            } catch (Exception e) {
                // Remove subscription inválida
                subscriptionRepository.delete(sub);
            }
        });
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.WEB_PUSH;
    }
}
```

### 5.6 Dispatcher de Notificações

```java
// NotificationDispatcher.java - Orquestra todos os canais
@Service
public class NotificationDispatcher {

    private final List<NotificationService> notificationServices;
    private final UserPreferenceService preferenceService;

    public void dispatch(Long userId, NotificationEvent event) {
        // Busca preferências do usuário
        Set<NotificationType> enabledTypes =
            preferenceService.getEnabledNotificationTypes(userId);

        // Dispara para todos os canais habilitados
        notificationServices.stream()
            .filter(service -> enabledTypes.stream()
                .anyMatch(service::supports))
            .forEach(service -> service.notify(userId, event));
    }

    public void dispatchToParty(Long partyId, NotificationEvent event) {
        List<Long> memberIds = getMemberIds(partyId);
        memberIds.forEach(userId -> dispatch(userId, event));
    }
}
```

### 5.7 Configuração

```yaml
# application.yml
notifications:
  in-app:
    enabled: true
    sse-timeout: 30000  # 30 segundos

  discord:
    enabled: false  # Ativar quando implementar
    webhook-url: ${DISCORD_WEBHOOK_URL:}

  webpush:
    enabled: false  # Ativar quando implementar
    public-key: ${VAPID_PUBLIC_KEY:}
    private-key: ${VAPID_PRIVATE_KEY:}
```

---

## 6. Dependências do Projeto

```xml
<!-- pom.xml - Dependências principais -->
<dependencies>
    <!-- Spring Boot Core -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- HTML Parsing (para Tibia.com) -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.2</version>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Thymeleaf Extras -->
    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>

    <dependency>
        <groupId>nz.net.ultraq.thymeleaf</groupId>
        <artifactId>thymeleaf-layout-dialect</artifactId>
    </dependency>
</dependencies>
```

---

## 7. Resumo da Arquitetura

| Aspecto | Decisão |
|---------|---------|
| **Padrão** | MVC Monolítico (pronto para extrair serviços) |
| **Camadas** | Controller → Service → Repository → Entity |
| **Database** | PostgreSQL com indexes por world |
| **Cache** | Caffeine (local) → Redis (futuro) |
| **Auth** | Spring Security + Session |
| **Templates** | Thymeleaf com fragments reutilizáveis |
| **Migrations** | Flyway |
| **Notificações** | Interface extensível (In-App → Discord → Web Push) |
| **Escala** | Index otimizados → Read Replicas → Sharding |

---

*Documento de Arquitetura Técnica v1.0*
*Tibia Party Finder*

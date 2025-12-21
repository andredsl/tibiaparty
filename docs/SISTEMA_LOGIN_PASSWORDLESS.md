# Sistema de Login Passwordless - Tibia Party Finder

## Visão Geral

Sistema de autenticação **sem senha** que utiliza o campo "Comment" do personagem no Tibia.com como prova de propriedade. O jogador prova que é dono do character inserindo um código temporário no comment.

---

## 1. Fluxo Passo a Passo

### 1.1 Fluxo de Primeiro Acesso (Registro + Verificação)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FLUXO DE PRIMEIRO ACESSO                              │
└─────────────────────────────────────────────────────────────────────────────┘

USUÁRIO                          SISTEMA                         TIBIA.COM
   │                                │                                │
   │  1. Acessa /register           │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │  2. Retorna form               │                                │
   │◀───────────────────────────────│                                │
   │                                │                                │
   │  3. Informa:                   │                                │
   │     - Character name           │                                │
   │     - Server (World)           │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │                                │  4. Busca character            │
   │                                │────────────────────────────────▶
   │                                │                                │
   │                                │  5. Retorna dados              │
   │                                │◀────────────────────────────────
   │                                │                                │
   │                                │  6. Valida:                    │
   │                                │     - Char existe?             │
   │                                │     - World correto?           │
   │                                │     - Level >= 50?             │
   │                                │     - Conta tem 30+ dias?      │
   │                                │                                │
   │                                │  7. Gera código:               │
   │                                │     TPF-A7X9K2                  │
   │                                │                                │
   │                                │  8. Salva VerificationToken    │
   │                                │     (expira em 15 min)         │
   │                                │                                │
   │  9. Exibe código + instruções  │                                │
   │◀───────────────────────────────│                                │
   │                                │                                │
   │  10. Usuário vai ao Tibia.com  │                                │
   │      e coloca código no        │                                │
   │      Comment do char           │                                │
   │──────────────────────────────────────────────────────────────────▶
   │                                │                                │
   │  11. Clica "Verificar"         │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │                                │  12. Busca char novamente      │
   │                                │────────────────────────────────▶
   │                                │                                │
   │                                │  13. Retorna dados + comment   │
   │                                │◀────────────────────────────────
   │                                │                                │
   │                                │  14. Valida código no comment  │
   │                                │                                │
   │                                │  15. Se válido:                │
   │                                │      - Cria/atualiza User      │
   │                                │      - Cria Character          │
   │                                │      - Marca token usado       │
   │                                │      - Cria sessão             │
   │                                │                                │
   │  16. Redirect /dashboard       │                                │
   │◀───────────────────────────────│                                │
   │      (com session cookie)      │                                │
   │                                │                                │
```

### 1.2 Fluxo de Login (Usuário já verificado)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FLUXO DE LOGIN                                     │
└─────────────────────────────────────────────────────────────────────────────┘

USUÁRIO                          SISTEMA                         TIBIA.COM
   │                                │                                │
   │  1. Acessa /login              │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │  2. Informa character + world  │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │                                │  3. Busca Character no banco   │
   │                                │                                │
   │                                │  4. Character verificado?      │
   │                                │                                │
   │                           ┌────┴────┐                           │
   │                           │         │                           │
   │                         SIM        NÃO                          │
   │                           │         │                           │
   │                           │         │  Redirect /register       │
   │                           │         │◀─────────────────────────│
   │                           │                                     │
   │                           │  5. Última verificação < 30 dias?   │
   │                           │                                     │
   │                      ┌────┴────┐                                │
   │                      │         │                                │
   │                    SIM        NÃO                               │
   │                      │         │                                │
   │                      │    Re-verificação necessária             │
   │                      │    (mesmo fluxo do registro)             │
   │                      │         │                                │
   │                      │                                          │
   │                      │  6. Gera código de login                 │
   │                      │     TPF-L3M8N1 (L = login)               │
   │                      │                                          │
   │  7. Exibe código     │                                          │
   │◀─────────────────────│                                          │
   │                                │                                │
   │  8. Coloca no comment          │                                │
   │──────────────────────────────────────────────────────────────────▶
   │                                │                                │
   │  9. Clica "Verificar"          │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │                                │  10. Valida código             │
   │                                │────────────────────────────────▶
   │                                │                                │
   │                                │  11. Cria sessão               │
   │                                │                                │
   │  12. Redirect /dashboard       │                                │
   │◀───────────────────────────────│                                │
   │                                │                                │
```

### 1.3 Fluxo de Adicionar Character Secundário

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ADICIONAR CHARACTER SECUNDÁRIO                            │
└─────────────────────────────────────────────────────────────────────────────┘

   │  (Usuário já logado)           │                                │
   │                                │                                │
   │  1. Acessa /characters/add     │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │  2. Informa novo char + world  │                                │
   │───────────────────────────────▶│                                │
   │                                │                                │
   │                                │  3. Valida char não está       │
   │                                │     vinculado a outro user     │
   │                                │                                │
   │                                │  4. Gera código                │
   │                                │     TPF-C5P2Q8 (C = char)      │
   │                                │                                │
   │  (mesmo fluxo de verificação)  │                                │
   │                                │                                │
   │                                │  5. Se válido:                 │
   │                                │     - Cria Character           │
   │                                │     - Vincula ao User atual    │
   │                                │                                │
```

---

## 2. Entidades Envolvidas

### 2.1 Diagrama ER

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MODELO DE DADOS                                    │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│      USER        │       │    CHARACTER     │       │      WORLD       │
├──────────────────┤       ├──────────────────┤       ├──────────────────┤
│ id (PK)          │       │ id (PK)          │       │ id (PK)          │
│ email (opcional) │       │ user_id (FK)     │───────│ name             │
│ reputation       │       │ world_id (FK)    │───────│ location         │
│ status           │       │ name             │       │ pvp_type         │
│ created_at       │       │ level            │       │ is_active        │
│ last_login_at    │       │ vocation         │       └──────────────────┘
└──────────────────┘       │ is_main          │
         │                 │ verified_at      │
         │ 1:N             │ tibia_account_age│
         │                 │ created_at       │
         └─────────────────┤ updated_at       │
                           └──────────────────┘
                                    │
                                    │ 1:N
                                    │
                    ┌───────────────┴───────────────┐
                    │     VERIFICATION_TOKEN        │
                    ├───────────────────────────────┤
                    │ id (PK)                       │
                    │ character_name                │
                    │ world_name                    │
                    │ code                          │
                    │ type (REGISTER/LOGIN/ADD_CHAR)│
                    │ user_id (FK, nullable)        │
                    │ status (PENDING/USED/EXPIRED) │
                    │ attempts                      │
                    │ created_at                    │
                    │ expires_at                    │
                    │ used_at                       │
                    │ ip_address                    │
                    └───────────────────────────────┘

                    ┌───────────────────────────────┐
                    │       USER_SESSION            │
                    ├───────────────────────────────┤
                    │ id (PK)                       │
                    │ user_id (FK)                  │
                    │ character_id (FK)             │
                    │ session_token                 │
                    │ ip_address                    │
                    │ user_agent                    │
                    │ created_at                    │
                    │ expires_at                    │
                    │ last_activity_at              │
                    └───────────────────────────────┘
```

### 2.2 Entidades JPA

```java
// User.java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;  // Opcional, para notificações futuras

    @Column(precision = 3, scale = 2)
    private BigDecimal reputation;  // 0.00 a 5.00

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;  // ACTIVE, BLOCKED, PENDING

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Character> characters = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        reputation = new BigDecimal("5.00");
        status = UserStatus.ACTIVE;
    }

    public Character getMainCharacter() {
        return characters.stream()
            .filter(Character::isMain)
            .findFirst()
            .orElse(characters.isEmpty() ? null : characters.get(0));
    }
}

// Character.java
@Entity
@Table(name = "characters",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "world_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vocation vocation;

    @Column(name = "is_main", nullable = false)
    private boolean main;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "tibia_account_age_days")
    private Integer tibiaAccountAgeDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        verifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// VerificationToken.java
@Entity
@Table(name = "verification_tokens",
       indexes = {
           @Index(name = "idx_token_code", columnList = "code"),
           @Index(name = "idx_token_char_world", columnList = "character_name, world_name"),
           @Index(name = "idx_token_expires", columnList = "expires_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_name", nullable = false)
    private String characterName;

    @Column(name = "world_name", nullable = false)
    private String worldName;

    @Column(nullable = false, unique = true, length = 12)
    private String code;  // TPF-XXXXXX

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationTokenType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Null para registro, preenchido para login/add_char

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationTokenStatus status;

    @Column(nullable = false)
    private Integer attempts;  // Tentativas de verificação

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(15);
        status = VerificationTokenStatus.PENDING;
        attempts = 0;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return status == VerificationTokenStatus.USED;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed() && attempts < 5;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsUsed() {
        this.status = VerificationTokenStatus.USED;
        this.usedAt = LocalDateTime.now();
    }
}

// Enums
public enum VerificationTokenType {
    REGISTER,    // Primeiro acesso
    LOGIN,       // Login recorrente
    ADD_CHAR     // Adicionar character secundário
}

public enum VerificationTokenStatus {
    PENDING,     // Aguardando verificação
    USED,        // Já utilizado
    EXPIRED      // Expirado automaticamente
}

public enum UserStatus {
    ACTIVE,
    BLOCKED,
    PENDING
}

public enum Vocation {
    ELITE_KNIGHT("EK"),
    ROYAL_PALADIN("RP"),
    ELDER_DRUID("ED"),
    MASTER_SORCERER("MS"),
    KNIGHT("K"),
    PALADIN("P"),
    DRUID("D"),
    SORCERER("S"),
    NONE("None");

    private final String abbreviation;

    Vocation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static Vocation fromTibiaName(String name) {
        return switch (name.toLowerCase()) {
            case "elite knight" -> ELITE_KNIGHT;
            case "royal paladin" -> ROYAL_PALADIN;
            case "elder druid" -> ELDER_DRUID;
            case "master sorcerer" -> MASTER_SORCERER;
            case "knight" -> KNIGHT;
            case "paladin" -> PALADIN;
            case "druid" -> DRUID;
            case "sorcerer" -> SORCERER;
            default -> NONE;
        };
    }
}
```

---

## 3. Serviços Necessários

### 3.1 Diagrama de Serviços

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CAMADA DE SERVIÇOS                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          AuthController                                      │
│  /register, /login, /verify, /logout                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
┌───────────────────────────────┐   ┌───────────────────────────────┐
│    VerificationService        │   │       SessionService          │
├───────────────────────────────┤   ├───────────────────────────────┤
│ + generateToken()             │   │ + createSession()             │
│ + verify()                    │   │ + invalidateSession()         │
│ + validateCode()              │   │ + getCurrentUser()            │
│ + cleanupExpiredTokens()      │   │ + refreshSession()            │
└───────────────────────────────┘   └───────────────────────────────┘
         │          │                        │
         │          │                        │
         ▼          ▼                        ▼
┌──────────────┐ ┌──────────────┐   ┌───────────────────────────────┐
│ TibiaClient  │ │ TokenRepo    │   │     UserService               │
├──────────────┤ ├──────────────┤   ├───────────────────────────────┤
│ + fetchChar()│ │ + save()     │   │ + createUser()                │
│ + parseData()│ │ + findByCode │   │ + findById()                  │
│ + getComment │ │ + expire()   │   │ + updateLastLogin()           │
└──────────────┘ └──────────────┘   └───────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│                        TIBIA.COM                                  │
│              https://www.tibia.com/characters/                   │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 VerificationService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final TibiaApiClient tibiaClient;
    private final VerificationCodeGenerator codeGenerator;

    private static final int TOKEN_EXPIRATION_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MIN_LEVEL = 50;
    private static final int MIN_ACCOUNT_AGE_DAYS = 30;

    /**
     * Gera um novo token de verificação
     */
    @Transactional
    public VerificationTokenResponse generateToken(
            String characterName,
            String worldName,
            VerificationTokenType type,
            Long userId,
            String ipAddress) {

        // 1. Busca dados do character no Tibia.com
        TibiaCharacterData charData = tibiaClient.fetchCharacter(characterName)
            .orElseThrow(() -> new CharacterNotFoundException(characterName));

        // 2. Validações
        validateCharacterData(charData, worldName, type);

        // 3. Verifica se já existe character vinculado a outro user
        if (type == VerificationTokenType.REGISTER || type == VerificationTokenType.ADD_CHAR) {
            validateCharacterNotOwned(characterName, worldName, userId);
        }

        // 4. Invalida tokens anteriores pendentes
        tokenRepository.expireAllPendingForCharacter(characterName, worldName);

        // 5. Gera novo código único
        String code = codeGenerator.generate(type);

        // 6. Cria e salva o token
        VerificationToken token = VerificationToken.builder()
            .characterName(characterName)
            .worldName(worldName)
            .code(code)
            .type(type)
            .user(userId != null ? userRepository.getReferenceById(userId) : null)
            .ipAddress(ipAddress)
            .build();

        tokenRepository.save(token);

        log.info("Token gerado para {} em {}: {}", characterName, worldName, code);

        // 7. Retorna resposta com instruções
        return VerificationTokenResponse.builder()
            .code(code)
            .characterName(charData.getName())
            .world(charData.getWorld())
            .level(charData.getLevel())
            .vocation(charData.getVocation())
            .expiresAt(token.getExpiresAt())
            .instructions(buildInstructions(code))
            .build();
    }

    /**
     * Verifica o código no comment do character
     */
    @Transactional
    public VerificationResult verify(String code, String ipAddress) {
        // 1. Busca token
        VerificationToken token = tokenRepository.findByCode(code)
            .orElseThrow(() -> new InvalidVerificationCodeException("Código inválido"));

        // 2. Valida estado do token
        if (token.isExpired()) {
            throw new VerificationTokenExpiredException();
        }
        if (token.isUsed()) {
            throw new VerificationTokenAlreadyUsedException();
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            token.setStatus(VerificationTokenStatus.EXPIRED);
            tokenRepository.save(token);
            throw new TooManyVerificationAttemptsException();
        }

        // 3. Incrementa tentativas
        token.incrementAttempts();

        // 4. Busca dados atualizados do character (com retry para cache)
        TibiaCharacterData charData = fetchWithCacheRetry(
            token.getCharacterName(),
            token.getWorldName()
        );

        // 5. Verifica se código está no comment
        boolean codeFound = validateCodeInComment(charData.getComment(), code);

        if (!codeFound) {
            tokenRepository.save(token);
            return VerificationResult.builder()
                .success(false)
                .attemptsRemaining(MAX_ATTEMPTS - token.getAttempts())
                .message("Código não encontrado no comment. Verifique e tente novamente.")
                .build();
        }

        // 6. Marca token como usado
        token.markAsUsed();
        tokenRepository.save(token);

        // 7. Processa resultado baseado no tipo
        return processVerificationSuccess(token, charData, ipAddress);
    }

    /**
     * Processa verificação bem-sucedida
     */
    private VerificationResult processVerificationSuccess(
            VerificationToken token,
            TibiaCharacterData charData,
            String ipAddress) {

        User user;
        Character character;

        switch (token.getType()) {
            case REGISTER -> {
                // Cria novo usuário e character
                user = createNewUser();
                character = createCharacter(user, charData, true);
            }
            case LOGIN -> {
                // Retorna usuário existente
                character = characterRepository
                    .findByNameAndWorldName(token.getCharacterName(), token.getWorldName())
                    .orElseThrow();
                user = character.getUser();

                // Atualiza dados do character
                updateCharacterData(character, charData);
            }
            case ADD_CHAR -> {
                // Adiciona character ao usuário existente
                user = token.getUser();
                character = createCharacter(user, charData, false);
            }
            default -> throw new IllegalStateException("Tipo de token desconhecido");
        }

        log.info("Verificação bem-sucedida: {} ({}) - tipo: {}",
            charData.getName(), charData.getWorld(), token.getType());

        return VerificationResult.builder()
            .success(true)
            .user(user)
            .character(character)
            .message("Verificação concluída com sucesso!")
            .build();
    }

    /**
     * Busca character com retry para contornar cache do Tibia.com
     */
    private TibiaCharacterData fetchWithCacheRetry(String name, String world) {
        int maxRetries = 3;
        int retryDelayMs = 2000;

        for (int i = 0; i < maxRetries; i++) {
            TibiaCharacterData data = tibiaClient.fetchCharacter(name)
                .orElseThrow(() -> new CharacterNotFoundException(name));

            // Verifica se world ainda bate (sanity check)
            if (!data.getWorld().equalsIgnoreCase(world)) {
                throw new CharacterWorldMismatchException(name, world, data.getWorld());
            }

            // Se comment foi atualizado recentemente, retorna
            // (heurística: comment não vazio ou diferente de cache anterior)
            if (i == maxRetries - 1 || data.getComment() != null) {
                return data;
            }

            // Aguarda antes de retry
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Verificação interrompida", e);
            }
        }

        throw new TibiaApiException("Não foi possível obter dados atualizados");
    }

    /**
     * Valida se código está no comment (case insensitive, com tolerância)
     */
    private boolean validateCodeInComment(String comment, String expectedCode) {
        if (comment == null || comment.isBlank()) {
            return false;
        }

        // Normaliza: remove espaços extras, case insensitive
        String normalizedComment = comment.toUpperCase().replaceAll("\\s+", " ").trim();
        String normalizedCode = expectedCode.toUpperCase().trim();

        // Verifica presença do código
        return normalizedComment.contains(normalizedCode);
    }

    /**
     * Validações do character
     */
    private void validateCharacterData(
            TibiaCharacterData data,
            String expectedWorld,
            VerificationTokenType type) {

        // Valida world
        if (!data.getWorld().equalsIgnoreCase(expectedWorld)) {
            throw new CharacterWorldMismatchException(
                data.getName(), expectedWorld, data.getWorld());
        }

        // Valida level mínimo para criar PT (não para login)
        if (type == VerificationTokenType.REGISTER && data.getLevel() < MIN_LEVEL) {
            throw new CharacterLevelTooLowException(data.getLevel(), MIN_LEVEL);
        }

        // Valida idade da conta
        if (data.getAccountAgeDays() != null && data.getAccountAgeDays() < MIN_ACCOUNT_AGE_DAYS) {
            throw new AccountTooNewException(data.getAccountAgeDays(), MIN_ACCOUNT_AGE_DAYS);
        }
    }

    /**
     * Verifica se character já pertence a outro usuário
     */
    private void validateCharacterNotOwned(String name, String world, Long currentUserId) {
        characterRepository.findByNameAndWorldName(name, world)
            .ifPresent(existing -> {
                if (currentUserId == null || !existing.getUser().getId().equals(currentUserId)) {
                    throw new CharacterAlreadyOwnedException(name);
                }
            });
    }

    /**
     * Job agendado para limpar tokens expirados
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void cleanupExpiredTokens() {
        int expired = tokenRepository.expireOldPendingTokens(
            LocalDateTime.now().minusMinutes(TOKEN_EXPIRATION_MINUTES)
        );
        if (expired > 0) {
            log.info("Tokens expirados: {}", expired);
        }
    }

    private User createNewUser() {
        return userRepository.save(User.builder().build());
    }

    private Character createCharacter(User user, TibiaCharacterData data, boolean isMain) {
        World world = worldRepository.findByName(data.getWorld())
            .orElseThrow(() -> new WorldNotFoundException(data.getWorld()));

        Character character = Character.builder()
            .user(user)
            .world(world)
            .name(data.getName())
            .level(data.getLevel())
            .vocation(Vocation.fromTibiaName(data.getVocation()))
            .main(isMain)
            .tibiaAccountAgeDays(data.getAccountAgeDays())
            .build();

        return characterRepository.save(character);
    }

    private void updateCharacterData(Character character, TibiaCharacterData data) {
        character.setLevel(data.getLevel());
        character.setVocation(Vocation.fromTibiaName(data.getVocation()));
        character.setVerifiedAt(LocalDateTime.now());
        characterRepository.save(character);
    }

    private String buildInstructions(String code) {
        return String.format("""
            1. Acesse tibia.com e faça login
            2. Vá em "My Account" > "Characters"
            3. Clique em "Edit" no seu character
            4. No campo "Comment", adicione: %s
            5. Salve e volte aqui para verificar

            O código expira em 15 minutos.
            """, code);
    }
}
```

### 3.3 TibiaApiClient

```java
@Service
@Slf4j
public class TibiaApiClient {

    private static final String TIBIA_CHAR_URL = "https://www.tibia.com/community/?subtopic=characters&name=";

    private final RestTemplate restTemplate;
    private final Cache<String, TibiaCharacterData> cache;

    public TibiaApiClient() {
        this.restTemplate = new RestTemplate();

        // Cache de 1 minuto para evitar hammering
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();
    }

    /**
     * Busca dados do character no Tibia.com
     */
    public Optional<TibiaCharacterData> fetchCharacter(String characterName) {
        return fetchCharacter(characterName, false);
    }

    /**
     * Busca dados do character, ignorando cache se necessário
     */
    public Optional<TibiaCharacterData> fetchCharacter(String characterName, boolean bypassCache) {
        String cacheKey = characterName.toLowerCase();

        if (!bypassCache) {
            TibiaCharacterData cached = cache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("Cache hit para character: {}", characterName);
                return Optional.of(cached);
            }
        }

        try {
            String url = TIBIA_CHAR_URL + URLEncoder.encode(characterName, StandardCharsets.UTF_8);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Resposta inválida do Tibia.com para: {}", characterName);
                return Optional.empty();
            }

            TibiaCharacterData data = parseCharacterPage(response.getBody());

            if (data != null) {
                cache.put(cacheKey, data);
                return Optional.of(data);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Erro ao buscar character {}: {}", characterName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse da página HTML do character
     */
    private TibiaCharacterData parseCharacterPage(String html) {
        Document doc = Jsoup.parse(html);

        // Verifica se character existe
        if (html.contains("Could not find character")) {
            return null;
        }

        try {
            // Extrai tabela de informações
            Elements tables = doc.select("table.TableContent");

            String name = extractField(tables, "Name:");
            String world = extractField(tables, "World:");
            String vocation = extractField(tables, "Vocation:");
            String levelStr = extractField(tables, "Level:");
            String comment = extractField(tables, "Comment:");
            String accountStatus = extractField(tables, "Account Status:");

            // Parse level
            int level = 0;
            if (levelStr != null) {
                level = Integer.parseInt(levelStr.replaceAll("[^0-9]", ""));
            }

            // Estima idade da conta baseado em achievements/loyalty (simplificado)
            Integer accountAgeDays = estimateAccountAge(doc);

            return TibiaCharacterData.builder()
                .name(name)
                .world(world)
                .vocation(vocation)
                .level(level)
                .comment(comment)
                .accountStatus(accountStatus)
                .accountAgeDays(accountAgeDays)
                .fetchedAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Erro ao fazer parse do character: {}", e.getMessage());
            return null;
        }
    }

    private String extractField(Elements tables, String fieldName) {
        for (Element table : tables) {
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    String label = cells.get(0).text().trim();
                    if (label.equalsIgnoreCase(fieldName)) {
                        return cells.get(1).text().trim();
                    }
                }
            }
        }
        return null;
    }

    private Integer estimateAccountAge(Document doc) {
        // Simplificado: busca por "Loyalty Title" ou conta badges
        // Em produção, seria mais sofisticado
        try {
            String loyaltyTitle = extractField(doc.select("table.TableContent"), "Loyalty Title:");
            if (loyaltyTitle != null && !loyaltyTitle.equalsIgnoreCase("none")) {
                return 365; // Conta tem pelo menos 1 ano
            }
            return 30; // Assume conta nova mas válida
        } catch (Exception e) {
            return null;
        }
    }
}

// TibiaCharacterData.java
@Data
@Builder
public class TibiaCharacterData {
    private String name;
    private String world;
    private String vocation;
    private int level;
    private String comment;
    private String accountStatus;
    private Integer accountAgeDays;
    private LocalDateTime fetchedAt;
}
```

### 3.4 SessionService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final HttpSession httpSession;
    private final UserRepository userRepository;

    private static final String USER_SESSION_KEY = "TIBIA_PF_USER";
    private static final String CHARACTER_SESSION_KEY = "TIBIA_PF_CHARACTER";

    /**
     * Cria sessão após verificação bem-sucedida
     */
    public void createSession(User user, Character character) {
        httpSession.setAttribute(USER_SESSION_KEY, user.getId());
        httpSession.setAttribute(CHARACTER_SESSION_KEY, character.getId());
        httpSession.setMaxInactiveInterval(60 * 60 * 24); // 24 horas

        // Atualiza último login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Sessão criada para user {} com character {}",
            user.getId(), character.getName());
    }

    /**
     * Obtém usuário da sessão atual
     */
    public Optional<User> getCurrentUser() {
        Long userId = (Long) httpSession.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    /**
     * Obtém character ativo da sessão
     */
    public Optional<Character> getCurrentCharacter() {
        Long characterId = (Long) httpSession.getAttribute(CHARACTER_SESSION_KEY);
        if (characterId == null) {
            return Optional.empty();
        }
        return characterRepository.findById(characterId);
    }

    /**
     * Alterna character ativo (para users com múltiplos chars)
     */
    public void switchCharacter(Long characterId) {
        Long userId = (Long) httpSession.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            throw new NotAuthenticatedException();
        }

        // Valida que character pertence ao user
        Character character = characterRepository.findById(characterId)
            .filter(c -> c.getUser().getId().equals(userId))
            .orElseThrow(() -> new CharacterNotOwnedException(characterId));

        httpSession.setAttribute(CHARACTER_SESSION_KEY, characterId);
        log.info("Character alterado para {} (user {})", character.getName(), userId);
    }

    /**
     * Encerra sessão
     */
    public void invalidateSession() {
        httpSession.invalidate();
    }

    /**
     * Verifica se usuário está autenticado
     */
    public boolean isAuthenticated() {
        return httpSession.getAttribute(USER_SESSION_KEY) != null;
    }
}
```

### 3.5 VerificationCodeGenerator

```java
@Component
public class VerificationCodeGenerator {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Sem 0OIl1
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<VerificationTokenType, String> PREFIXES = Map.of(
        VerificationTokenType.REGISTER, "TPF-R",
        VerificationTokenType.LOGIN, "TPF-L",
        VerificationTokenType.ADD_CHAR, "TPF-C"
    );

    /**
     * Gera código único no formato TPF-XXXXXX
     */
    public String generate(VerificationTokenType type) {
        StringBuilder code = new StringBuilder(PREFIXES.get(type));

        for (int i = 0; i < CODE_LENGTH - 1; i++) {
            int index = RANDOM.nextInt(CHARSET.length());
            code.append(CHARSET.charAt(index));
        }

        return code.toString();
    }

    /**
     * Valida formato do código
     */
    public boolean isValidFormat(String code) {
        if (code == null || code.length() != 10) {
            return false;
        }
        return code.matches("TPF-[RLCA][A-Z2-9]{5}");
    }
}
```

---

## 4. Quando Exigir Verificação

### 4.1 Matriz de Verificação

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      QUANDO EXIGIR VERIFICAÇÃO                               │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────┬────────────────┬───────────────────────────────────┐
│ CENÁRIO                │ VERIFICAÇÃO    │ MOTIVO                            │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Primeiro acesso        │ OBRIGATÓRIA    │ Provar propriedade do character   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Login recorrente       │ OBRIGATÓRIA    │ Confirmar que ainda é o dono      │
│ (última verif < 30d)   │                │                                   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Login recorrente       │ OBRIGATÓRIA    │ Re-validar propriedade            │
│ (última verif > 30d)   │ (re-verif)     │                                   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Adicionar char         │ OBRIGATÓRIA    │ Provar propriedade do novo char   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Criar PT               │ NÃO            │ Já autenticado                    │
│                        │ (validar sessão│                                   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Entrar em PT           │ NÃO            │ Já autenticado                    │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Trocar char ativo      │ NÃO            │ Chars já verificados              │
│ (mesmo user)           │                │                                   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Ação sensível          │ OPCIONAL       │ Deletar conta, remover char       │
│ (configurável)         │ (re-verif)     │                                   │
├────────────────────────┼────────────────┼───────────────────────────────────┤
│ Denúncia grave         │ NÃO            │ Moderação interna                 │
│ recebida               │ (bloqueio)     │                                   │
└────────────────────────┴────────────────┴───────────────────────────────────┘
```

### 4.2 Fluxo de Decisão

```java
@Service
@RequiredArgsConstructor
public class AuthenticationDecisionService {

    private static final int REVERIFICATION_DAYS = 30;

    /**
     * Determina se precisa de verificação para login
     */
    public VerificationRequirement checkLoginRequirement(String characterName, String world) {
        Optional<Character> existing = characterRepository
            .findByNameAndWorldName(characterName, world);

        if (existing.isEmpty()) {
            // Character nunca registrado
            return VerificationRequirement.builder()
                .required(true)
                .type(VerificationTokenType.REGISTER)
                .reason("Character não registrado. Faça a verificação inicial.")
                .build();
        }

        Character character = existing.get();
        LocalDateTime lastVerified = character.getVerifiedAt();
        long daysSinceVerification = ChronoUnit.DAYS.between(lastVerified, LocalDateTime.now());

        if (daysSinceVerification > REVERIFICATION_DAYS) {
            // Verificação expirada
            return VerificationRequirement.builder()
                .required(true)
                .type(VerificationTokenType.LOGIN)
                .reason("Sua última verificação foi há " + daysSinceVerification +
                       " dias. Por segurança, verifique novamente.")
                .build();
        }

        // Verificação válida
        return VerificationRequirement.builder()
            .required(true)  // Sempre exige código por segurança (passwordless)
            .type(VerificationTokenType.LOGIN)
            .reason("Insira o código de verificação para continuar.")
            .build();
    }

    /**
     * Verifica se pode adicionar character
     */
    public VerificationRequirement checkAddCharacterRequirement(
            Long userId, String characterName, String world) {

        // Verifica se já pertence a alguém
        Optional<Character> existing = characterRepository
            .findByNameAndWorldName(characterName, world);

        if (existing.isPresent()) {
            if (existing.get().getUser().getId().equals(userId)) {
                return VerificationRequirement.builder()
                    .required(false)
                    .reason("Character já está vinculado à sua conta.")
                    .build();
            } else {
                return VerificationRequirement.builder()
                    .required(false)
                    .blocked(true)
                    .reason("Character já está vinculado a outro usuário.")
                    .build();
            }
        }

        return VerificationRequirement.builder()
            .required(true)
            .type(VerificationTokenType.ADD_CHAR)
            .reason("Verifique a propriedade do character.")
            .build();
    }
}

@Data
@Builder
public class VerificationRequirement {
    private boolean required;
    private boolean blocked;
    private VerificationTokenType type;
    private String reason;
}
```

---

## 5. Tratamento de Falhas

### 5.1 Matriz de Erros

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TRATAMENTO DE FALHAS                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┬──────────────┬─────────────────────────────────────┐
│ ERRO                    │ HTTP STATUS  │ AÇÃO                                │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Character não existe    │ 404          │ Exibir: "Character não encontrado.  │
│                         │              │ Verifique o nome e servidor."       │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ World incorreto         │ 400          │ Exibir: "Character pertence ao      │
│                         │              │ servidor X, não Y."                 │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Level muito baixo       │ 403          │ Exibir: "Level mínimo é 50.         │
│                         │              │ Seu level: X."                      │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Conta muito nova        │ 403          │ Exibir: "Conta Tibia deve ter pelo  │
│                         │              │ menos 30 dias."                     │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Character já vinculado  │ 409          │ Exibir: "Character já registrado    │
│                         │              │ por outro usuário."                 │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Código não encontrado   │ 400          │ Exibir: "Código não encontrado no   │
│ no comment              │              │ comment. Tentativas: X/5"           │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Código expirado         │ 410          │ Exibir: "Código expirou. Gere um    │
│                         │              │ novo código."                       │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Código já usado         │ 409          │ Exibir: "Código já foi utilizado."  │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Muitas tentativas       │ 429          │ Exibir: "Muitas tentativas. Aguarde │
│                         │              │ 15 min ou gere novo código."        │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Tibia.com fora do ar    │ 503          │ Exibir: "Tibia.com indisponível.    │
│                         │              │ Tente novamente em alguns minutos." │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Cache do Tibia.com      │ 200          │ Exibir: "Aguarde ~2 min para o      │
│ (código não aparece)    │ (retry)      │ Tibia.com atualizar e tente de novo"│
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Rate limit interno      │ 429          │ Exibir: "Muitas solicitações.       │
│                         │              │ Aguarde X segundos."                │
├─────────────────────────┼──────────────┼─────────────────────────────────────┤
│ Usuário bloqueado       │ 403          │ Exibir: "Conta bloqueada.           │
│                         │              │ Contate o suporte."                 │
└─────────────────────────┴──────────────┴─────────────────────────────────────┘
```

### 5.2 Exception Handler Global

```java
@ControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    // ===== Erros de Character =====

    @ExceptionHandler(CharacterNotFoundException.class)
    public String handleCharacterNotFound(
            CharacterNotFoundException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error", "Character não encontrado: " + ex.getCharacterName());
        attrs.addFlashAttribute("errorType", "CHARACTER_NOT_FOUND");
        return "redirect:/login";
    }

    @ExceptionHandler(CharacterWorldMismatchException.class)
    public String handleWorldMismatch(
            CharacterWorldMismatchException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error",
            String.format("Character '%s' pertence ao servidor %s, não %s.",
                ex.getCharacterName(), ex.getActualWorld(), ex.getExpectedWorld()));
        attrs.addFlashAttribute("errorType", "WORLD_MISMATCH");
        return "redirect:/login";
    }

    @ExceptionHandler(CharacterLevelTooLowException.class)
    public String handleLevelTooLow(
            CharacterLevelTooLowException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error",
            String.format("Level mínimo é %d. Seu character tem level %d.",
                ex.getMinLevel(), ex.getActualLevel()));
        attrs.addFlashAttribute("errorType", "LEVEL_TOO_LOW");
        return "redirect:/login";
    }

    @ExceptionHandler(AccountTooNewException.class)
    public String handleAccountTooNew(
            AccountTooNewException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error",
            String.format("Conta Tibia deve ter pelo menos %d dias. Sua conta tem ~%d dias.",
                ex.getMinDays(), ex.getActualDays()));
        attrs.addFlashAttribute("errorType", "ACCOUNT_TOO_NEW");
        return "redirect:/login";
    }

    @ExceptionHandler(CharacterAlreadyOwnedException.class)
    public String handleAlreadyOwned(
            CharacterAlreadyOwnedException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error",
            "Character já está vinculado a outro usuário.");
        attrs.addFlashAttribute("errorType", "ALREADY_OWNED");
        return "redirect:/login";
    }

    // ===== Erros de Verificação =====

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public String handleInvalidCode(
            InvalidVerificationCodeException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error", "Código de verificação inválido.");
        attrs.addFlashAttribute("errorType", "INVALID_CODE");
        return "redirect:/verify";
    }

    @ExceptionHandler(VerificationTokenExpiredException.class)
    public String handleTokenExpired(RedirectAttributes attrs) {
        attrs.addFlashAttribute("error",
            "Código expirado. Por favor, gere um novo código.");
        attrs.addFlashAttribute("errorType", "TOKEN_EXPIRED");
        attrs.addFlashAttribute("showRetry", true);
        return "redirect:/login";
    }

    @ExceptionHandler(VerificationTokenAlreadyUsedException.class)
    public String handleTokenUsed(RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", "Este código já foi utilizado.");
        attrs.addFlashAttribute("errorType", "TOKEN_USED");
        return "redirect:/login";
    }

    @ExceptionHandler(TooManyVerificationAttemptsException.class)
    public String handleTooManyAttempts(RedirectAttributes attrs) {
        attrs.addFlashAttribute("error",
            "Muitas tentativas de verificação. Gere um novo código.");
        attrs.addFlashAttribute("errorType", "TOO_MANY_ATTEMPTS");
        attrs.addFlashAttribute("showRetry", true);
        return "redirect:/login";
    }

    // ===== Erros de Integração =====

    @ExceptionHandler(TibiaApiException.class)
    public String handleTibiaApiError(
            TibiaApiException ex,
            RedirectAttributes attrs) {

        log.error("Erro na API do Tibia: {}", ex.getMessage());

        attrs.addFlashAttribute("error",
            "Tibia.com está indisponível no momento. Tente novamente em alguns minutos.");
        attrs.addFlashAttribute("errorType", "TIBIA_API_ERROR");
        attrs.addFlashAttribute("retryAfter", 120); // segundos
        return "redirect:/login";
    }

    @ExceptionHandler(TibiaCacheDelayException.class)
    public String handleCacheDelay(
            TibiaCacheDelayException ex,
            RedirectAttributes attrs,
            HttpServletRequest request) {

        attrs.addFlashAttribute("warning",
            "O Tibia.com pode levar até 2 minutos para atualizar o comment. " +
            "Aguarde um pouco e tente novamente.");
        attrs.addFlashAttribute("errorType", "CACHE_DELAY");
        attrs.addFlashAttribute("retryAfter", 120);

        // Preserva dados do form
        attrs.addFlashAttribute("characterName", request.getParameter("characterName"));
        attrs.addFlashAttribute("code", request.getParameter("code"));

        return "redirect:/verify";
    }

    // ===== Rate Limiting =====

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public String handleRateLimit(
            RateLimitExceededException ex,
            RedirectAttributes attrs) {

        attrs.addFlashAttribute("error",
            String.format("Muitas solicitações. Aguarde %d segundos.", ex.getRetryAfterSeconds()));
        attrs.addFlashAttribute("errorType", "RATE_LIMIT");
        attrs.addFlashAttribute("retryAfter", ex.getRetryAfterSeconds());
        return "redirect:/login";
    }

    // ===== Erro Genérico =====

    @ExceptionHandler(Exception.class)
    public String handleGenericError(
            Exception ex,
            RedirectAttributes attrs) {

        log.error("Erro não tratado: ", ex);

        attrs.addFlashAttribute("error",
            "Ocorreu um erro inesperado. Tente novamente.");
        attrs.addFlashAttribute("errorType", "GENERIC_ERROR");
        return "redirect:/login";
    }
}
```

### 5.3 Estratégia de Retry para Cache do Tibia.com

```java
@Service
@Slf4j
public class TibiaVerificationRetryStrategy {

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_DELAY_MS = 2000;
    private static final double BACKOFF_MULTIPLIER = 1.5;

    /**
     * Tenta verificar com retry exponencial
     */
    public VerificationAttemptResult attemptVerificationWithRetry(
            TibiaApiClient client,
            String characterName,
            String expectedCode) {

        int attempt = 0;
        int delayMs = INITIAL_DELAY_MS;
        String lastComment = null;

        while (attempt < MAX_RETRIES) {
            attempt++;

            log.info("Tentativa {} de verificação para {}", attempt, characterName);

            // Busca com bypass de cache local
            Optional<TibiaCharacterData> data = client.fetchCharacter(characterName, true);

            if (data.isEmpty()) {
                return VerificationAttemptResult.builder()
                    .success(false)
                    .error(VerificationError.CHARACTER_NOT_FOUND)
                    .build();
            }

            String comment = data.get().getComment();
            lastComment = comment;

            // Verifica se código está presente
            if (comment != null && comment.toUpperCase().contains(expectedCode.toUpperCase())) {
                return VerificationAttemptResult.builder()
                    .success(true)
                    .characterData(data.get())
                    .attemptsUsed(attempt)
                    .build();
            }

            // Se não é última tentativa, aguarda
            if (attempt < MAX_RETRIES) {
                log.info("Código não encontrado. Aguardando {}ms antes de retry...", delayMs);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                delayMs = (int) (delayMs * BACKOFF_MULTIPLIER);
            }
        }

        // Todas tentativas falharam
        return VerificationAttemptResult.builder()
            .success(false)
            .error(VerificationError.CODE_NOT_IN_COMMENT)
            .lastComment(lastComment)
            .attemptsUsed(attempt)
            .suggestWait(true)
            .suggestedWaitSeconds(120) // Esperar cache do Tibia
            .build();
    }
}

@Data
@Builder
public class VerificationAttemptResult {
    private boolean success;
    private VerificationError error;
    private TibiaCharacterData characterData;
    private String lastComment;
    private int attemptsUsed;
    private boolean suggestWait;
    private int suggestedWaitSeconds;
}

public enum VerificationError {
    CHARACTER_NOT_FOUND,
    CODE_NOT_IN_COMMENT,
    TIBIA_API_UNAVAILABLE
}
```

### 5.4 Rate Limiting

```java
@Component
@Slf4j
public class VerificationRateLimiter {

    // Limites por IP
    private final Cache<String, AtomicInteger> ipAttempts = Caffeine.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();

    // Limites por character
    private final Cache<String, AtomicInteger> charAttempts = Caffeine.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();

    private static final int MAX_ATTEMPTS_PER_IP = 10;
    private static final int MAX_ATTEMPTS_PER_CHAR = 5;

    public void checkRateLimit(String ipAddress, String characterName) {
        // Check IP limit
        AtomicInteger ipCount = ipAttempts.get(ipAddress,
            k -> new AtomicInteger(0));

        if (ipCount.incrementAndGet() > MAX_ATTEMPTS_PER_IP) {
            log.warn("Rate limit excedido para IP: {}", ipAddress);
            throw new RateLimitExceededException(900); // 15 min
        }

        // Check character limit
        String charKey = characterName.toLowerCase();
        AtomicInteger charCount = charAttempts.get(charKey,
            k -> new AtomicInteger(0));

        if (charCount.incrementAndGet() > MAX_ATTEMPTS_PER_CHAR) {
            log.warn("Rate limit excedido para character: {}", characterName);
            throw new RateLimitExceededException(900);
        }
    }

    public void resetForCharacter(String characterName) {
        charAttempts.invalidate(characterName.toLowerCase());
    }
}
```

---

## 6. Resumo do Sistema

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SISTEMA DE LOGIN PASSWORDLESS                             │
│                         Tibia Party Finder                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  PRINCÍPIO: "Prove que é dono do character inserindo um código temporário   │
│              no comment do Tibia.com"                                        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ FLUXO SIMPLIFICADO                                                   │    │
│  │                                                                      │    │
│  │  1. Usuário informa Character + World                               │    │
│  │  2. Sistema valida no Tibia.com                                     │    │
│  │  3. Sistema gera código: TPF-XXXXXX                                 │    │
│  │  4. Usuário coloca código no Comment                                │    │
│  │  5. Sistema verifica → Sessão criada                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ CARACTERÍSTICAS                                                      │    │
│  │                                                                      │    │
│  │  ✓ Sem senha (passwordless)                                         │    │
│  │  ✓ Código expira em 15 minutos                                      │    │
│  │  ✓ Uso único (não pode reutilizar código)                          │    │
│  │  ✓ Máximo 5 tentativas por código                                   │    │
│  │  ✓ Retry automático para cache do Tibia.com                        │    │
│  │  ✓ Rate limiting por IP e character                                 │    │
│  │  ✓ Re-verificação a cada 30 dias                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ SEGURANÇA                                                            │    │
│  │                                                                      │    │
│  │  • Level mínimo: 50                                                 │    │
│  │  • Conta Tibia mínima: 30 dias                                      │    │
│  │  • 1 character por conta no site                                    │    │
│  │  • Bloqueio após denúncias                                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

*Documento de Sistema de Login v1.0*
*Tibia Party Finder*

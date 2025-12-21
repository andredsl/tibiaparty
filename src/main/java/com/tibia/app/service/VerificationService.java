package com.tibia.app.service;

import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.domain.entity.VerificationToken;
import com.tibia.app.domain.entity.World;
import com.tibia.app.domain.enums.UserStatus;
import com.tibia.app.domain.enums.VerificationTokenStatus;
import com.tibia.app.domain.enums.VerificationTokenType;
import com.tibia.app.domain.enums.Vocation;
import com.tibia.app.dto.response.TibiaCharacterData;
import com.tibia.app.dto.response.VerificationResult;
import com.tibia.app.dto.response.VerificationTokenResponse;
import com.tibia.app.exception.*;
import com.tibia.app.repository.GameCharacterRepository;
import com.tibia.app.repository.UserRepository;
import com.tibia.app.repository.VerificationTokenRepository;
import com.tibia.app.repository.WorldRepository;
import com.tibia.app.service.external.TibiaApiClient;
import com.tibia.app.util.VerificationCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private static final int MIN_LEVEL = 8;
    private static final int MIN_ACCOUNT_AGE_DAYS = 30;
    private static final int REVERIFICATION_DAYS = 30;
    private static final int MAX_TOKENS_PER_HOUR = 5;

    private final VerificationTokenRepository tokenRepository;
    private final GameCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final WorldRepository worldRepository;
    private final TibiaApiClient tibiaClient;
    private final VerificationCodeGenerator codeGenerator;

    public VerificationService(
            VerificationTokenRepository tokenRepository,
            GameCharacterRepository characterRepository,
            UserRepository userRepository,
            WorldRepository worldRepository,
            TibiaApiClient tibiaClient,
            VerificationCodeGenerator codeGenerator) {
        this.tokenRepository = tokenRepository;
        this.characterRepository = characterRepository;
        this.userRepository = userRepository;
        this.worldRepository = worldRepository;
        this.tibiaClient = tibiaClient;
        this.codeGenerator = codeGenerator;
    }

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

        // 1. Rate limiting
        checkRateLimit(characterName, ipAddress);

        // 2. Busca dados do character no Tibia.com
        TibiaCharacterData charData = tibiaClient.fetchCharacter(characterName)
                .orElseThrow(() -> new CharacterNotFoundException(characterName));

        // 3. Validações
        validateCharacterData(charData, worldName, type);

        // 4. Verifica se character já está vinculado a outro user
        if (type == VerificationTokenType.REGISTER || type == VerificationTokenType.ADD_CHAR) {
            validateCharacterNotOwned(characterName, worldName, userId);
        }

        // 5. Invalida tokens anteriores pendentes
        tokenRepository.expireAllPendingForCharacter(characterName, worldName);

        // 6. Gera novo código único
        String code = codeGenerator.generate(type);

        // 7. Cria e salva o token
        VerificationToken token = VerificationToken.builder()
                .characterName(charData.getName()) // Usa nome correto do Tibia
                .worldName(charData.getWorld())
                .code(code)
                .type(type)
                .user(userId != null ? userRepository.getReferenceById(userId) : null)
                .ipAddress(ipAddress)
                .build();

        tokenRepository.save(token);

        log.info("Token gerado para {} em {}: {} (tipo: {})",
                charData.getName(), charData.getWorld(), code, type);

        // 8. Retorna resposta
        return VerificationTokenResponse.builderWithInstructions(code)
                .characterName(charData.getName())
                .world(charData.getWorld())
                .level(charData.getLevel())
                .vocation(charData.getVocation())
                .expiresAt(token.getExpiresAt())
                .minutesUntilExpiration(token.getMinutesUntilExpiration())
                .build();
    }

    /**
     * Verifica o código no comment do character
     */
    @Transactional
    public VerificationResult verify(String code, String ipAddress) {
        // 1. Busca token
        VerificationToken token = tokenRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(InvalidVerificationCodeException::new);

        // 2. Valida estado do token
        if (token.isExpired()) {
            token.markAsExpired();
            tokenRepository.save(token);
            throw new VerificationTokenExpiredException();
        }
        if (token.isUsed()) {
            throw new VerificationTokenAlreadyUsedException();
        }
        if (token.getRemainingAttempts() <= 0) {
            token.markAsExpired();
            tokenRepository.save(token);
            throw new TooManyVerificationAttemptsException();
        }

        // 3. Incrementa tentativas
        token.incrementAttempts();

        // 4. Busca dados atualizados do character (bypass cache)
        TibiaCharacterData charData = tibiaClient.fetchCharacter(token.getCharacterName(), true)
                .orElseThrow(() -> new CharacterNotFoundException(token.getCharacterName()));

        // 5. Valida world ainda é o mesmo
        if (!charData.getWorld().equalsIgnoreCase(token.getWorldName())) {
            tokenRepository.save(token);
            throw new CharacterWorldMismatchException(
                    token.getCharacterName(), token.getWorldName(), charData.getWorld());
        }

        // 6. Verifica se código está no comment
        boolean codeFound = validateCodeInComment(charData.getComment(), code);

        if (!codeFound) {
            tokenRepository.save(token);
            log.warn("Código não encontrado no comment de {}. Tentativa {}/5",
                    token.getCharacterName(), token.getAttempts());

            return VerificationResult.cacheDelay(token.getRemainingAttempts());
        }

        // 7. Marca token como usado
        token.markAsUsed();
        tokenRepository.save(token);

        // 8. Processa resultado baseado no tipo
        return processVerificationSuccess(token, charData);
    }

    /**
     * Processa verificação bem-sucedida
     */
    private VerificationResult processVerificationSuccess(
            VerificationToken token,
            TibiaCharacterData charData) {

        User user;
        GameCharacter character;

        switch (token.getType()) {
            case REGISTER -> {
                // Cria novo usuário e character
                user = createNewUser();
                character = createCharacter(user, charData, true);
                log.info("Novo usuário registrado: {} (char: {})", user.getId(), charData.getName());
            }
            case LOGIN -> {
                // Retorna usuário existente
                character = characterRepository
                        .findByNameAndWorldName(token.getCharacterName(), token.getWorldName())
                        .orElseThrow(() -> new CharacterNotFoundException(token.getCharacterName()));
                user = character.getUser();

                // Atualiza dados do character
                updateCharacterData(character, charData);
                log.info("Login realizado: {} (char: {})", user.getId(), charData.getName());
            }
            case ADD_CHAR -> {
                // Adiciona character ao usuário existente
                user = token.getUser();
                character = createCharacter(user, charData, false);
                log.info("Character adicionado: {} para user {}", charData.getName(), user.getId());
            }
            default -> throw new IllegalStateException("Tipo de token desconhecido: " + token.getType());
        }

        return VerificationResult.success(user, character);
    }

    /**
     * Valida se código está no comment
     */
    private boolean validateCodeInComment(String comment, String expectedCode) {
        if (comment == null || comment.isBlank()) {
            return false;
        }

        // Normaliza: remove espaços extras, case insensitive
        String normalizedComment = comment.toUpperCase().replaceAll("\\s+", " ").trim();
        String normalizedCode = expectedCode.toUpperCase().trim();

        return normalizedComment.contains(normalizedCode);
    }

    /**
     * Validações do character
     */
    private void validateCharacterData(TibiaCharacterData data, String expectedWorld, VerificationTokenType type) {
        // Valida world
        if (!data.getWorld().equalsIgnoreCase(expectedWorld)) {
            throw new CharacterWorldMismatchException(data.getName(), expectedWorld, data.getWorld());
        }

        // Valida level mínimo para criar PT (não para login de user existente)
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
     * Rate limiting
     */
    private void checkRateLimit(String characterName, String ipAddress) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        int byIp = tokenRepository.countRecentByIp(ipAddress, oneHourAgo);
        if (byIp >= MAX_TOKENS_PER_HOUR * 2) {
            throw new RateLimitExceededException(3600);
        }

        int byChar = tokenRepository.countRecentByCharacter(characterName, oneHourAgo);
        if (byChar >= MAX_TOKENS_PER_HOUR) {
            throw new RateLimitExceededException(3600);
        }
    }

    /**
     * Verifica se character precisa de re-verificação
     */
    public boolean needsReverification(GameCharacter character) {
        if (character.getVerifiedAt() == null) return true;
        long daysSince = ChronoUnit.DAYS.between(character.getVerifiedAt(), LocalDateTime.now());
        return daysSince > REVERIFICATION_DAYS;
    }

    /**
     * Determina tipo de token necessário para login
     */
    public VerificationTokenType determineLoginTokenType(String characterName, String worldName) {
        return characterRepository.findByNameAndWorldName(characterName, worldName)
                .map(c -> VerificationTokenType.LOGIN)
                .orElse(VerificationTokenType.REGISTER);
    }

    private User createNewUser() {
        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private GameCharacter createCharacter(User user, TibiaCharacterData data, boolean isMain) {
        World world = worldRepository.findByNameIgnoreCase(data.getWorld())
                .orElseGet(() -> createWorld(data.getWorld()));

        GameCharacter character = GameCharacter.builder()
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

    private World createWorld(String worldName) {
        // Cria world básico (em produção, deveria ter uma lista completa)
        World world = World.builder()
                .name(worldName)
                .location("Unknown")
                .pvpType("Unknown")
                .active(true)
                .build();
        return worldRepository.save(world);
    }

    private void updateCharacterData(GameCharacter character, TibiaCharacterData data) {
        character.setLevel(data.getLevel());
        character.setVocation(Vocation.fromTibiaName(data.getVocation()));
        character.setVerifiedAt(LocalDateTime.now());
        characterRepository.save(character);
    }

    /**
     * Job agendado para limpar tokens expirados
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void cleanupExpiredTokens() {
        int expired = tokenRepository.expireOldPendingTokens(LocalDateTime.now());
        if (expired > 0) {
            log.info("Tokens expirados: {}", expired);
        }

        // Limpa tokens antigos usados/expirados (mais de 7 dias)
        int deleted = tokenRepository.deleteOldTokens(LocalDateTime.now().minusDays(7));
        if (deleted > 0) {
            log.info("Tokens antigos deletados: {}", deleted);
        }
    }
}

package com.tibia.app.service;

import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.exception.NotAuthenticatedException;
import com.tibia.app.repository.GameCharacterRepository;
import com.tibia.app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private static final String USER_ID_KEY = "TIBIA_PF_USER_ID";
    private static final String CHARACTER_ID_KEY = "TIBIA_PF_CHARACTER_ID";
    private static final String WORLD_KEY = "TIBIA_PF_CURRENT_WORLD";

    private final UserRepository userRepository;
    private final GameCharacterRepository characterRepository;

    public SessionService(UserRepository userRepository, GameCharacterRepository characterRepository) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
    }

    /**
     * Cria sessão após verificação bem-sucedida
     */
    public void createSession(User user, GameCharacter character) {
        HttpSession session = getSession();
        session.setAttribute(USER_ID_KEY, user.getId());
        session.setAttribute(CHARACTER_ID_KEY, character.getId());
        session.setAttribute(WORLD_KEY, character.getWorld().getName());
        session.setMaxInactiveInterval(60 * 60 * 24); // 24 horas

        // Atualiza último login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Sessão criada para user {} com character {} em {}",
                user.getId(), character.getName(), character.getWorld().getName());
    }

    /**
     * Obtém usuário da sessão atual
     */
    public Optional<User> getCurrentUser() {
        Long userId = getUserId();
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findByIdWithCharacters(userId);
    }

    /**
     * Obtém usuário ou lança exceção
     */
    public User requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(NotAuthenticatedException::new);
    }

    /**
     * Obtém character ativo da sessão
     */
    public Optional<GameCharacter> getCurrentCharacter() {
        Long characterId = getCharacterId();
        if (characterId == null) {
            return Optional.empty();
        }
        return characterRepository.findByIdWithWorld(characterId);
    }

    /**
     * Obtém character ou lança exceção
     */
    public GameCharacter requireCurrentCharacter() {
        return getCurrentCharacter()
                .orElseThrow(NotAuthenticatedException::new);
    }

    /**
     * Obtém world atual da sessão
     */
    public Optional<String> getCurrentWorld() {
        HttpSession session = getSession();
        return Optional.ofNullable((String) session.getAttribute(WORLD_KEY));
    }

    /**
     * Alterna character ativo
     */
    public void switchCharacter(Long characterId) {
        Long userId = getUserId();
        if (userId == null) {
            throw new NotAuthenticatedException();
        }

        GameCharacter character = characterRepository.findByIdWithWorld(characterId)
                .filter(c -> c.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Character não pertence ao usuário"));

        HttpSession session = getSession();
        session.setAttribute(CHARACTER_ID_KEY, characterId);
        session.setAttribute(WORLD_KEY, character.getWorld().getName());

        log.info("Character alterado para {} (user {})", character.getName(), userId);
    }

    /**
     * Altera world atual (para ver PTs de outro server)
     */
    public void setCurrentWorld(String worldName) {
        HttpSession session = getSession();
        session.setAttribute(WORLD_KEY, worldName);
    }

    /**
     * Encerra sessão
     */
    public void invalidateSession() {
        HttpSession session = getSession();
        Long userId = getUserId();
        session.invalidate();
        log.info("Sessão encerrada para user {}", userId);
    }

    /**
     * Verifica se usuário está autenticado
     */
    public boolean isAuthenticated() {
        return getUserId() != null;
    }

    /**
     * Obtém ID do usuário da sessão
     */
    public Long getUserId() {
        HttpSession session = getSession();
        return (Long) session.getAttribute(USER_ID_KEY);
    }

    /**
     * Obtém ID do character da sessão
     */
    public Long getCharacterId() {
        HttpSession session = getSession();
        return (Long) session.getAttribute(CHARACTER_ID_KEY);
    }

    private HttpSession getSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("Não há request ativo");
        }
        return attrs.getRequest().getSession(true);
    }
}

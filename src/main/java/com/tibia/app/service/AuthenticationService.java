package com.tibia.app.service;

import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.dto.request.PasswordLoginRequest;
import com.tibia.app.dto.request.SetPasswordRequest;
import com.tibia.app.exception.InvalidCredentialsException;
import com.tibia.app.exception.PasswordMismatchException;
import com.tibia.app.exception.UserBlockedException;
import com.tibia.app.repository.GameCharacterRepository;
import com.tibia.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final GameCharacterRepository characterRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthenticationService(
            UserRepository userRepository,
            GameCharacterRepository characterRepository,
            PasswordEncoder passwordEncoder,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    /**
     * Define senha para usuário após verificação do char
     */
    @Transactional
    public void setPassword(User user, SetPasswordRequest request) {
        if (!request.passwordsMatch()) {
            throw new PasswordMismatchException();
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        log.info("Senha definida para usuário {}", user.getId());
    }

    /**
     * Login por nome do personagem + senha
     */
    @Transactional
    public GameCharacter loginWithPassword(PasswordLoginRequest request) {
        // Busca personagem pelo nome (case insensitive)
        GameCharacter character = characterRepository.findByNameIgnoreCase(request.getCharacterName())
                .orElseThrow(InvalidCredentialsException::new);

        User user = character.getUser();

        // Verifica se usuário está bloqueado
        if (user.isBlocked()) {
            throw new UserBlockedException();
        }

        // Verifica se usuário tem senha definida
        if (!user.hasPassword()) {
            throw new InvalidCredentialsException();
        }

        // Verifica senha
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Tentativa de login com senha incorreta para char: {}", request.getCharacterName());
            throw new InvalidCredentialsException();
        }

        // Cria sessão
        sessionService.createSession(user, character);

        log.info("Login com senha bem-sucedido para char: {}", character.getName());
        return character;
    }

    /**
     * Verifica se um personagem tem conta com senha
     */
    public boolean characterHasPassword(String characterName) {
        return characterRepository.findByNameIgnoreCase(characterName)
                .map(c -> c.getUser().hasPassword())
                .orElse(false);
    }
}

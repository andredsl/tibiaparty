package com.tibia.app.controller.web;

import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.domain.enums.VerificationTokenType;
import com.tibia.app.dto.request.LoginRequest;
import com.tibia.app.dto.request.PasswordLoginRequest;
import com.tibia.app.dto.request.SetPasswordRequest;
import com.tibia.app.dto.request.VerifyCodeRequest;
import com.tibia.app.dto.response.VerificationResult;
import com.tibia.app.dto.response.VerificationTokenResponse;
import com.tibia.app.exception.InvalidCredentialsException;
import com.tibia.app.exception.UserBlockedException;
import com.tibia.app.repository.WorldRepository;
import com.tibia.app.service.AuthenticationService;
import com.tibia.app.service.SessionService;
import com.tibia.app.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final VerificationService verificationService;
    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final WorldRepository worldRepository;

    public AuthController(
            VerificationService verificationService,
            AuthenticationService authenticationService,
            SessionService sessionService,
            WorldRepository worldRepository) {
        this.verificationService = verificationService;
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
        this.worldRepository = worldRepository;
    }

    /**
     * Página de login
     */
    @GetMapping("/login")
    public String loginPage(Model model, @RequestParam(required = false) String redirect) {
        if (sessionService.isAuthenticated()) {
            return "redirect:/parties";
        }

        model.addAttribute("passwordLoginRequest", new PasswordLoginRequest());
        model.addAttribute("redirect", redirect);

        return "auth/login";
    }

    /**
     * Processa login com senha
     */
    @PostMapping("/login")
    public String processPasswordLogin(
            @Valid @ModelAttribute PasswordLoginRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            GameCharacter character = authenticationService.loginWithPassword(request);
            redirectAttrs.addFlashAttribute("success",
                    "Bem-vindo, " + character.getName() + "!");
            return "redirect:/parties";

        } catch (InvalidCredentialsException e) {
            model.addAttribute("error", "Personagem ou senha inválidos");
            return "auth/login";
        } catch (UserBlockedException e) {
            model.addAttribute("error", "Esta conta está bloqueada");
            return "auth/login";
        } catch (Exception e) {
            log.error("Erro no login: {}", e.getMessage());
            model.addAttribute("error", "Erro ao fazer login. Tente novamente.");
            return "auth/login";
        }
    }

    /**
     * Página de cadastro (verificação por código)
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        if (sessionService.isAuthenticated()) {
            return "redirect:/parties";
        }

        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("worlds", worldRepository.findAllActiveOrderedByLocationAndName());

        return "auth/register";
    }

    /**
     * Processa solicitação de cadastro
     */
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute LoginRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("worlds", worldRepository.findAllActiveOrderedByLocationAndName());
            return "auth/register";
        }

        try {
            // Verifica se já existe uma conta para este char
            if (authenticationService.characterHasPassword(request.getCharacterName())) {
                redirectAttrs.addFlashAttribute("error",
                        "Este personagem já possui uma conta. Use a página de login.");
                return "redirect:/auth/login";
            }

            // Gera token de verificação para registro
            VerificationTokenResponse tokenResponse = verificationService.generateToken(
                    request.getCharacterName(),
                    request.getWorld(),
                    VerificationTokenType.REGISTER,
                    null,
                    getClientIp(httpRequest)
            );

            // Redireciona para página de verificação
            redirectAttrs.addFlashAttribute("token", tokenResponse);
            redirectAttrs.addFlashAttribute("isNewUser", true);

            return "redirect:/auth/verify";

        } catch (Exception e) {
            log.error("Erro no cadastro: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    /**
     * Página de verificação de código
     */
    @GetMapping("/verify")
    public String verifyPage(Model model) {
        if (!model.containsAttribute("token")) {
            return "redirect:/auth/register";
        }

        model.addAttribute("verifyRequest", new VerifyCodeRequest());
        return "auth/verify";
    }

    /**
     * Processa verificação do código
     */
    @PostMapping("/verify")
    public String processVerify(
            @Valid @ModelAttribute VerifyCodeRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Código inválido. Formato esperado: TPF-XXXXXX");
            return "auth/verify";
        }

        try {
            // Verifica o código
            VerificationResult result = verificationService.verify(
                    request.getCode(),
                    getClientIp(httpRequest)
            );

            if (result.isSuccess()) {
                User user = result.getUser();
                GameCharacter character = result.getCharacter();

                // Cria sessão
                sessionService.createSession(user, character);

                // Se usuário não tem senha, redireciona para definir
                if (!user.hasPassword()) {
                    redirectAttrs.addFlashAttribute("characterName", character.getName());
                    return "redirect:/auth/set-password";
                }

                redirectAttrs.addFlashAttribute("success",
                        "Bem-vindo, " + character.getName() + "!");

                return "redirect:/parties";
            } else {
                // Verificação falhou, mas pode tentar novamente
                model.addAttribute("error", result.getMessage());
                model.addAttribute("attemptsRemaining", result.getAttemptsRemaining());

                if (result.isSuggestWait()) {
                    model.addAttribute("suggestWait", true);
                    model.addAttribute("waitSeconds", result.getSuggestedWaitSeconds());
                }

                return "auth/verify";
            }

        } catch (Exception e) {
            log.error("Erro na verificação: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    /**
     * Página para definir senha após verificação
     */
    @GetMapping("/set-password")
    public String setPasswordPage(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        // Verifica se usuário já tem senha
        User user = sessionService.requireCurrentUser();
        if (user.hasPassword()) {
            return "redirect:/parties";
        }

        model.addAttribute("setPasswordRequest", new SetPasswordRequest());
        model.addAttribute("characterName", sessionService.requireCurrentCharacter().getName());

        return "auth/set-password";
    }

    /**
     * Processa definição de senha
     */
    @PostMapping("/set-password")
    public String processSetPassword(
            @Valid @ModelAttribute SetPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        User user = sessionService.requireCurrentUser();

        if (bindingResult.hasErrors()) {
            model.addAttribute("characterName", sessionService.requireCurrentCharacter().getName());
            return "auth/set-password";
        }

        if (!request.passwordsMatch()) {
            model.addAttribute("error", "As senhas não coincidem");
            model.addAttribute("characterName", sessionService.requireCurrentCharacter().getName());
            return "auth/set-password";
        }

        try {
            authenticationService.setPassword(user, request);

            redirectAttrs.addFlashAttribute("success",
                    "Conta criada com sucesso! Agora você pode fazer login com seu personagem e senha.");

            return "redirect:/parties";

        } catch (Exception e) {
            log.error("Erro ao definir senha: {}", e.getMessage());
            model.addAttribute("error", "Erro ao definir senha. Tente novamente.");
            model.addAttribute("characterName", sessionService.requireCurrentCharacter().getName());
            return "auth/set-password";
        }
    }

    /**
     * Logout
     */
    @GetMapping("/logout")
    public String logout(RedirectAttributes redirectAttrs) {
        sessionService.invalidateSession();
        redirectAttrs.addFlashAttribute("success", "Você saiu com sucesso.");
        return "redirect:/auth/login";
    }

    /**
     * Obtém IP do cliente
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

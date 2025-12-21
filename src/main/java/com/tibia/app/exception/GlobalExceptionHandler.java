package com.tibia.app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ===== Erros de Character =====

    @ExceptionHandler(CharacterNotFoundException.class)
    public String handleCharacterNotFound(CharacterNotFoundException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "CHARACTER_NOT_FOUND");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(CharacterWorldMismatchException.class)
    public String handleWorldMismatch(CharacterWorldMismatchException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "WORLD_MISMATCH");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(CharacterLevelTooLowException.class)
    public String handleLevelTooLow(CharacterLevelTooLowException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "LEVEL_TOO_LOW");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(AccountTooNewException.class)
    public String handleAccountTooNew(AccountTooNewException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "ACCOUNT_TOO_NEW");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(CharacterAlreadyOwnedException.class)
    public String handleAlreadyOwned(CharacterAlreadyOwnedException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "ALREADY_OWNED");
        return "redirect:/auth/login";
    }

    // ===== Erros de Verificação =====

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public String handleInvalidCode(InvalidVerificationCodeException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "INVALID_CODE");
        return "redirect:/auth/verify";
    }

    @ExceptionHandler(VerificationTokenExpiredException.class)
    public String handleTokenExpired(VerificationTokenExpiredException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "TOKEN_EXPIRED");
        attrs.addFlashAttribute("showRetry", true);
        return "redirect:/auth/login";
    }

    @ExceptionHandler(VerificationTokenAlreadyUsedException.class)
    public String handleTokenUsed(VerificationTokenAlreadyUsedException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "TOKEN_USED");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(TooManyVerificationAttemptsException.class)
    public String handleTooManyAttempts(TooManyVerificationAttemptsException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "TOO_MANY_ATTEMPTS");
        attrs.addFlashAttribute("showRetry", true);
        return "redirect:/auth/login";
    }

    // ===== Erros de Integração =====

    @ExceptionHandler(TibiaApiException.class)
    public String handleTibiaApiError(TibiaApiException ex, RedirectAttributes attrs) {
        log.error("Erro na API do Tibia: {}", ex.getMessage());
        attrs.addFlashAttribute("error",
                "Tibia.com está indisponível no momento. Tente novamente em alguns minutos.");
        attrs.addFlashAttribute("errorType", "TIBIA_API_ERROR");
        attrs.addFlashAttribute("retryAfter", 120);
        return "redirect:/auth/login";
    }

    @ExceptionHandler(WorldNotFoundException.class)
    public String handleWorldNotFound(WorldNotFoundException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "WORLD_NOT_FOUND");
        return "redirect:/auth/login";
    }

    // ===== Rate Limiting =====

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public String handleRateLimit(RateLimitExceededException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "RATE_LIMIT");
        attrs.addFlashAttribute("retryAfter", ex.getRetryAfterSeconds());
        return "redirect:/auth/login";
    }

    // ===== Autenticação =====

    @ExceptionHandler(NotAuthenticatedException.class)
    public String handleNotAuthenticated(NotAuthenticatedException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", "Você precisa estar logado para acessar esta página.");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public String handleInvalidCredentials(InvalidCredentialsException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "INVALID_CREDENTIALS");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(UserBlockedException.class)
    public String handleUserBlocked(UserBlockedException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "USER_BLOCKED");
        return "redirect:/auth/login";
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public String handlePasswordMismatch(PasswordMismatchException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        attrs.addFlashAttribute("errorType", "PASSWORD_MISMATCH");
        return "redirect:/auth/set-password";
    }

    // ===== Erro Genérico =====

    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception ex, HttpServletRequest request, RedirectAttributes attrs) {
        log.error("Erro não tratado em {}: ", request.getRequestURI(), ex);
        attrs.addFlashAttribute("error", "Ocorreu um erro inesperado. Tente novamente.");
        attrs.addFlashAttribute("errorType", "GENERIC_ERROR");
        return "redirect:/auth/login";
    }
}

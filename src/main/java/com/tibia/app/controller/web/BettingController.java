package com.tibia.app.controller.web;

import com.tibia.app.domain.entity.Bet;
import com.tibia.app.domain.entity.BettingRound;
import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.dto.request.CreateBetRequest;
import com.tibia.app.dto.response.BoostedCreatureInfo;
import com.tibia.app.dto.response.CreatureInfo;
import com.tibia.app.service.BettingService;
import com.tibia.app.service.SessionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/betting")
public class BettingController {

    private static final Logger log = LoggerFactory.getLogger(BettingController.class);

    private final BettingService bettingService;
    private final SessionService sessionService;

    public BettingController(BettingService bettingService, SessionService sessionService) {
        this.bettingService = bettingService;
        this.sessionService = sessionService;
    }

    /**
     * Pagina principal de apostas
     */
    @GetMapping
    public String bettingHome(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();
        User user = character.getUser();

        // Criatura boostada de hoje
        Optional<BoostedCreatureInfo> boostedToday = bettingService.getTodaysBoostedCreature();

        // Rodada atual (para amanha)
        BettingRound currentRound = bettingService.getOrCreateCurrentRound();

        // Valores do pool
        Long currentPool = bettingService.getCurrentPoolTotal();
        Long roundBetsTotal = bettingService.getCurrentRoundBetsTotal();
        Long accumulatedPool = bettingService.getCurrentAccumulatedPool();

        // Apostas do usuario
        List<Bet> userBets = bettingService.getUserBets(user);

        // Apostas do usuario na rodada atual
        List<Bet> userCurrentBets = bettingService.getUserBetsForCurrentRound(user);

        model.addAttribute("character", character);
        model.addAttribute("boostedToday", boostedToday.orElse(null));
        model.addAttribute("currentRound", currentRound);
        model.addAttribute("currentPool", currentPool);
        model.addAttribute("roundBetsTotal", roundBetsTotal);
        model.addAttribute("accumulatedPool", accumulatedPool);
        model.addAttribute("userBets", userBets);
        model.addAttribute("userCurrentBets", userCurrentBets);
        model.addAttribute("canBet", currentRound.canAcceptBets());

        return "betting/home";
    }

    /**
     * Pagina para criar uma nova aposta
     */
    @GetMapping("/create")
    public String createBetForm(
            @RequestParam(required = false) String search,
            Model model,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();
        User user = character.getUser();

        BettingRound currentRound = bettingService.getOrCreateCurrentRound();

        if (!currentRound.canAcceptBets()) {
            redirectAttrs.addFlashAttribute("error", "As apostas para amanha ja foram encerradas");
            return "redirect:/betting";
        }

        // Lista de criaturas
        List<CreatureInfo> creatures;
        if (search != null && !search.trim().isEmpty()) {
            creatures = bettingService.searchCreatures(search);
        } else {
            creatures = bettingService.getAllCreatures();
        }

        model.addAttribute("character", character);
        model.addAttribute("currentRound", currentRound);
        model.addAttribute("creatures", creatures);
        model.addAttribute("search", search);
        model.addAttribute("betRequest", new CreateBetRequest());
        model.addAttribute("currentPool", bettingService.getCurrentPoolTotal());

        return "betting/create";
    }

    /**
     * Processa a criacao de uma aposta
     */
    @PostMapping("/create")
    public String createBet(
            @Valid @ModelAttribute("betRequest") CreateBetRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();
        User user = character.getUser();

        // Validacao customizada
        if (request.getAmount() != null && request.getAmount() % 25 != 0) {
            bindingResult.rejectValue("amount", "invalid",
                    "O valor deve ser multiplo de 25 Tibia Coins");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("character", character);
            model.addAttribute("creatures", bettingService.getAllCreatures());
            model.addAttribute("currentRound", bettingService.getOrCreateCurrentRound());
            model.addAttribute("currentPool", bettingService.getCurrentPoolTotal());
            return "betting/create";
        }

        try {
            Bet bet = bettingService.createBet(user, character, request);
            redirectAttrs.addFlashAttribute("success",
                    String.format("Aposta de %d TC em %s registrada! Aguardando confirmacao de pagamento.",
                            bet.getAmount(), bet.getCreatureName()));
            return "redirect:/betting";

        } catch (Exception e) {
            log.error("Erro ao criar aposta: {}", e.getMessage());
            model.addAttribute("character", character);
            model.addAttribute("creatures", bettingService.getAllCreatures());
            model.addAttribute("currentRound", bettingService.getOrCreateCurrentRound());
            model.addAttribute("currentPool", bettingService.getCurrentPoolTotal());
            model.addAttribute("error", e.getMessage());
            return "betting/create";
        }
    }

    /**
     * Cancela uma aposta pendente
     */
    @PostMapping("/{id}/cancel")
    public String cancelBet(
            @PathVariable Long id,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        User user = sessionService.requireCurrentCharacter().getUser();

        try {
            bettingService.cancelBet(id, user);
            redirectAttrs.addFlashAttribute("success", "Aposta cancelada");
        } catch (Exception e) {
            log.error("Erro ao cancelar aposta: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/betting";
    }

    /**
     * Historico de rodadas
     */
    @GetMapping("/history")
    public String history(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();

        List<BettingRound> history = bettingService.getHistory();

        model.addAttribute("character", character);
        model.addAttribute("rounds", history);

        return "betting/history";
    }

    /**
     * Pagina de administracao de apostas
     */
    @GetMapping("/admin")
    public String adminPage(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        if (!sessionService.isAdmin()) {
            return "redirect:/betting?error=acesso_negado";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();

        // Apostas pendentes
        List<Bet> pendingBets = bettingService.getPendingBets();

        // Rodada atual (para amanha)
        BettingRound currentRound = bettingService.getOrCreateCurrentRound();

        // Rodadas em aberto prontas para finalizar (targetDate <= hoje)
        List<BettingRound> openRounds;
        try {
            openRounds = bettingService.getOpenRoundsReadyToFinalize();
        } catch (Exception e) {
            log.error("Erro ao buscar rodadas em aberto: {}", e.getMessage());
            openRounds = java.util.Collections.emptyList();
        }

        model.addAttribute("character", character);
        model.addAttribute("pendingBets", pendingBets);
        model.addAttribute("currentRound", currentRound);
        model.addAttribute("currentPool", bettingService.getCurrentPoolTotal());
        model.addAttribute("openRounds", openRounds);

        return "betting/admin";
    }

    /**
     * Confirma uma aposta (admin)
     */
    @PostMapping("/admin/{id}/confirm")
    public String confirmBet(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        if (!sessionService.isAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Acesso negado");
            return "redirect:/betting";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();

        try {
            bettingService.confirmBet(id, character.getName(), notes);
            redirectAttrs.addFlashAttribute("success", "Aposta confirmada!");
        } catch (Exception e) {
            log.error("Erro ao confirmar aposta: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/betting/admin";
    }

    /**
     * Finaliza uma rodada especifica por data
     */
    @PostMapping("/admin/finalize")
    public String finalizeRound(
            @RequestParam(required = false) String targetDate,
            RedirectAttributes redirectAttrs) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        if (!sessionService.isAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Acesso negado");
            return "redirect:/betting";
        }

        try {
            java.time.LocalDate date;
            if (targetDate != null && !targetDate.isEmpty()) {
                date = java.time.LocalDate.parse(targetDate);
            } else {
                date = java.time.LocalDate.now();
            }

            bettingService.finalizeRound(date);
            redirectAttrs.addFlashAttribute("success",
                    "Rodada de " + date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " finalizada com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao finalizar rodada: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/betting/admin";
    }
}

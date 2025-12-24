package com.tibia.app.controller.web;

import com.tibia.app.domain.entity.Bet;
import com.tibia.app.domain.entity.BettingRound;
import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.domain.enums.PartyType;
import com.tibia.app.domain.enums.Vocation;
import com.tibia.app.dto.request.CreateBetRequest;
import com.tibia.app.dto.request.CreatePartyRequest;
import com.tibia.app.dto.response.BoostedCreatureInfo;
import com.tibia.app.dto.response.CreatureInfo;
import com.tibia.app.dto.response.JoinRequestResponse;
import com.tibia.app.dto.response.PartyResponse;
import com.tibia.app.service.BettingService;
import com.tibia.app.service.PartyService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/parties")
public class PartyController {

    private static final Logger log = LoggerFactory.getLogger(PartyController.class);

    private final PartyService partyService;
    private final SessionService sessionService;
    private final BettingService bettingService;

    public PartyController(PartyService partyService, SessionService sessionService, BettingService bettingService) {
        this.partyService = partyService;
        this.sessionService = sessionService;
        this.bettingService = bettingService;
    }

    /**
     * Lista parties ativas
     */
    @GetMapping
    public String listParties(
            @RequestParam(required = false) PartyType type,
            Model model) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();
        List<PartyResponse> parties = partyService.listActivePartiesForCurrentWorld();

        // Filtra por tipo se especificado
        if (type != null) {
            parties = parties.stream()
                    .filter(p -> p.getType() == type)
                    .toList();
        }

        // Busca party atual do jogador
        Optional<PartyResponse> currentParty = partyService.getCurrentParty(character.getId());

        // Busca solicitacoes pendentes do jogador
        List<JoinRequestResponse> myPendingRequests = partyService.getMyPendingRequests();

        // Cria Set de IDs de parties com solicitacao pendente para facilitar verificacao no template
        Set<Long> pendingPartyIds = myPendingRequests.stream()
                .map(JoinRequestResponse::getPartyId)
                .collect(Collectors.toSet());

        // Busca recusas recentes
        List<JoinRequestResponse> recentRejections = partyService.getMyRecentRejections();

        model.addAttribute("character", character);
        model.addAttribute("parties", parties);
        model.addAttribute("selectedType", type);
        model.addAttribute("partyTypes", PartyType.values());
        model.addAttribute("currentParty", currentParty.orElse(null));
        model.addAttribute("isInParty", currentParty.isPresent());
        model.addAttribute("myPendingRequests", myPendingRequests);
        model.addAttribute("pendingPartyIds", pendingPartyIds);
        model.addAttribute("recentRejections", recentRejections);

        // Dados de apostas
        model.addAttribute("bettingError", false);
        try {
            User user = character.getUser();
            BettingRound currentRound = bettingService.getOrCreateCurrentRound();

            // Busca apostas do usuario na rodada atual
            List<Bet> userCurrentBets = bettingService.getUserBetsForCurrentRound(user);

            model.addAttribute("bettingRound", currentRound);
            model.addAttribute("bettingPool", bettingService.getCurrentPoolTotal());
            model.addAttribute("roundBetsTotal", bettingService.getCurrentRoundBetsTotal());
            model.addAttribute("accumulatedPool", bettingService.getCurrentAccumulatedPool());
            model.addAttribute("userCurrentBets", userCurrentBets);
            model.addAttribute("canBet", currentRound.canAcceptBets());
            model.addAttribute("topCreatures", bettingService.getAllCreatures().stream().limit(12).toList());
        } catch (Exception e) {
            log.warn("Erro ao carregar dados de apostas: {}", e.getMessage(), e);
            model.addAttribute("bettingError", true);
            model.addAttribute("bettingPool", 0L);
            model.addAttribute("roundBetsTotal", 0L);
            model.addAttribute("accumulatedPool", 0L);
            model.addAttribute("userCurrentBets", java.util.Collections.emptyList());
            model.addAttribute("canBet", false);
            model.addAttribute("topCreatures", java.util.Collections.emptyList());
        }

        return "party/list";
    }

    /**
     * Formulario de criacao de party
     */
    @GetMapping("/create")
    public String createPartyForm(Model model, RedirectAttributes redirectAttrs) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();

        // Verifica restricoes - agora permitimos criar se for tipo diferente
        boolean isInHuntOrBoss = partyService.isInActiveHuntOrBossParty(character.getId());

        model.addAttribute("character", character);
        model.addAttribute("partyRequest", new CreatePartyRequest());
        model.addAttribute("partyTypes", PartyType.values());
        model.addAttribute("vocations", Vocation.values());
        model.addAttribute("isInHuntOrBoss", isInHuntOrBoss);

        return "party/create";
    }

    /**
     * Processa criacao de party
     */
    @PostMapping("/create")
    public String createParty(
            @Valid @ModelAttribute("partyRequest") CreatePartyRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();

        // Validacoes customizadas
        if (request.getType() == PartyType.HUNT &&
            (request.getLocation() == null || request.getLocation().isBlank())) {
            bindingResult.rejectValue("location", "required", "Local da hunt e obrigatorio");
        }
        if (request.getType() == PartyType.BOSS &&
            (request.getBossName() == null || request.getBossName().isBlank())) {
            bindingResult.rejectValue("bossName", "required", "Nome do boss e obrigatorio");
        }
        if (request.getType() == PartyType.SOUL_CORE &&
            (request.getSoulCoreName() == null || request.getSoulCoreName().isBlank())) {
            bindingResult.rejectValue("soulCoreName", "required", "Nome do Soul Core e obrigatorio");
        }
        if (request.getLevelMin() != null && request.getLevelMax() != null &&
            request.getLevelMin() > request.getLevelMax()) {
            bindingResult.rejectValue("levelMax", "invalid", "Level maximo deve ser maior que o minimo");
        }

        // Valida que se definiu slots por vocacao, o total deve ser >= 2
        if (request.hasVocationRequirements() && request.getTotalVocationSlots() < 2) {
            bindingResult.reject("vocationSlots", "Se definir vagas por vocacao, o total deve ser pelo menos 2");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("character", character);
            model.addAttribute("partyTypes", PartyType.values());
            model.addAttribute("vocations", Vocation.values());
            return "party/create";
        }

        try {
            PartyResponse party = partyService.createParty(request);
            redirectAttrs.addFlashAttribute("success",
                    "Party criada com sucesso! " + party.getDisplayName());
            return "redirect:/parties";

        } catch (Exception e) {
            log.error("Erro ao criar party: {}", e.getMessage());
            model.addAttribute("character", character);
            model.addAttribute("partyTypes", PartyType.values());
            model.addAttribute("vocations", Vocation.values());
            model.addAttribute("error", e.getMessage());
            return "party/create";
        }
    }

    /**
     * Solicita entrada em uma party
     */
    @PostMapping("/{id}/join")
    public String requestJoinParty(
            @PathVariable Long id,
            @RequestParam(required = false) String message,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            PartyService.JoinRequestResult result = partyService.requestToJoin(id, message);

            if (result.success()) {
                redirectAttrs.addFlashAttribute("success", result.message());
            } else {
                redirectAttrs.addFlashAttribute("error", result.message());
            }
        } catch (Exception e) {
            log.error("Erro ao solicitar entrada na party: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties";
    }

    /**
     * Sai de uma party
     */
    @PostMapping("/{id}/leave")
    public String leaveParty(
            @PathVariable Long id,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            partyService.leaveParty(id);
            redirectAttrs.addFlashAttribute("success", "Voce saiu da PT");
        } catch (Exception e) {
            log.error("Erro ao sair da party: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties";
    }

    /**
     * Cancela uma party
     */
    @PostMapping("/{id}/cancel")
    public String cancelParty(
            @PathVariable Long id,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            partyService.cancelParty(id);
            redirectAttrs.addFlashAttribute("success", "Party cancelada");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties";
    }

    /**
     * Pagina de gerenciamento da party (para lideres)
     */
    @GetMapping("/{id}/manage")
    public String manageParty(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();

        try {
            PartyResponse party = partyService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Party nao encontrada"));

            // Verifica se e o lider
            if (!party.getLeaderId().equals(character.getId())) {
                redirectAttrs.addFlashAttribute("error", "Apenas o lider pode gerenciar a party");
                return "redirect:/parties";
            }

            List<JoinRequestResponse> pendingRequests = partyService.getPendingRequests(id);

            model.addAttribute("character", character);
            model.addAttribute("party", party);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("pendingCount", pendingRequests.size());

            return "party/manage";

        } catch (Exception e) {
            log.error("Erro ao acessar gerenciamento: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/parties";
        }
    }

    /**
     * Aceita uma solicitacao de entrada
     */
    @PostMapping("/{partyId}/requests/{requestId}/accept")
    public String acceptJoinRequest(
            @PathVariable Long partyId,
            @PathVariable Long requestId,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            partyService.acceptJoinRequest(requestId);
            redirectAttrs.addFlashAttribute("success", "Jogador aceito na party!");
        } catch (Exception e) {
            log.error("Erro ao aceitar solicitacao: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties/" + partyId + "/manage";
    }

    /**
     * Recusa uma solicitacao de entrada
     */
    @PostMapping("/{partyId}/requests/{requestId}/reject")
    public String rejectJoinRequest(
            @PathVariable Long partyId,
            @PathVariable Long requestId,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            partyService.rejectJoinRequest(requestId, reason);
            redirectAttrs.addFlashAttribute("success", "Solicitacao recusada");
        } catch (Exception e) {
            log.error("Erro ao recusar solicitacao: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties/" + partyId + "/manage";
    }

    /**
     * Cancela a propria solicitacao de entrada
     */
    @PostMapping("/requests/{requestId}/cancel")
    public String cancelJoinRequest(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            partyService.cancelJoinRequest(requestId);
            redirectAttrs.addFlashAttribute("success", "Solicitacao cancelada");
        } catch (Exception e) {
            log.error("Erro ao cancelar solicitacao: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties";
    }

    /**
     * Marca uma notificacao de recusa como vista
     */
    @PostMapping("/rejections/{requestId}/dismiss")
    public String dismissRejection(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            partyService.markRejectionAsSeen(requestId);
        } catch (Exception e) {
            log.error("Erro ao dispensar notificacao: {}", e.getMessage());
        }

        return "redirect:/parties";
    }

    /**
     * Aposta rapida direto da pagina de parties
     */
    @PostMapping("/quick-bet")
    public String quickBet(
            @RequestParam String creatureName,
            @RequestParam Long amount,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        GameCharacter character = sessionService.requireCurrentCharacter();
        User user = character.getUser();

        try {
            CreateBetRequest request = new CreateBetRequest();
            request.setCreatureName(creatureName);
            request.setAmount(amount);

            Bet bet = bettingService.createBet(user, character, request);
            redirectAttrs.addFlashAttribute("success",
                    String.format("Aposta de %d TC em %s registrada! Aguardando confirmacao de pagamento.",
                            bet.getAmount(), bet.getCreatureName()));
        } catch (Exception e) {
            log.error("Erro ao criar aposta rapida: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties";
    }

    /**
     * Cancela aposta da pagina de parties
     */
    @PostMapping("/cancel-bet/{betId}")
    public String cancelBetFromParties(
            @PathVariable Long betId,
            RedirectAttributes redirectAttrs) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        User user = sessionService.requireCurrentCharacter().getUser();

        try {
            bettingService.cancelBet(betId, user);
            redirectAttrs.addFlashAttribute("success", "Aposta cancelada");
        } catch (Exception e) {
            log.error("Erro ao cancelar aposta: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/parties";
    }
}

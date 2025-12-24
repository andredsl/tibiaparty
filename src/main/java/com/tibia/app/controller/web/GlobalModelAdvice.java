package com.tibia.app.controller.web;

import com.tibia.app.dto.response.BoostedCreatureInfo;
import com.tibia.app.service.BettingService;
import com.tibia.app.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAdvice.class);

    private final BettingService bettingService;
    private final SessionService sessionService;

    public GlobalModelAdvice(BettingService bettingService, SessionService sessionService) {
        this.bettingService = bettingService;
        this.sessionService = sessionService;
    }

    /**
     * Indica se o usuario atual e admin
     */
    @ModelAttribute("isAdmin")
    public Boolean isAdmin() {
        try {
            return sessionService.isAdmin();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Adiciona a criatura boostada do dia a todos os templates
     */
    @ModelAttribute("globalBoostedCreature")
    public BoostedCreatureInfo getBoostedCreature() {
        try {
            return bettingService.getTodaysBoostedCreature().orElse(null);
        } catch (Exception e) {
            log.warn("Erro ao buscar criatura boostada: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Adiciona o total do pool atual a todos os templates
     */
    @ModelAttribute("globalCurrentPool")
    public Long getCurrentPool() {
        try {
            return bettingService.getCurrentPoolTotal();
        } catch (Exception e) {
            log.warn("Erro ao buscar pool atual: {}", e.getMessage());
            return 0L;
        }
    }
}

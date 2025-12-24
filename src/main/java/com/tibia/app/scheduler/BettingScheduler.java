package com.tibia.app.scheduler;

import com.tibia.app.service.BettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class BettingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BettingScheduler.class);

    private final BettingService bettingService;

    public BettingScheduler(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    /**
     * Finaliza a rodada de apostas automaticamente todos os dias as 10:05 UTC.
     * O Server Save do Tibia ocorre as 10:00 UTC, entao aguardamos 5 minutos
     * para garantir que a API do TibiaData ja atualizou a criatura boostada.
     */
    @Scheduled(cron = "0 5 10 * * *", zone = "UTC")
    public void finalizeDailyRound() {
        LocalDate today = LocalDate.now();
        log.info("Iniciando finalizacao automatica da rodada de apostas para {}", today);

        try {
            bettingService.finalizeRound(today);
            log.info("Rodada de apostas para {} finalizada automaticamente com sucesso", today);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.contains("ja foi finalizada")) {
                log.info("Rodada para {} ja foi finalizada anteriormente (provavelmente manualmente)", today);
            } else if (message != null && message.contains("nao encontrada")) {
                log.info("Nenhuma rodada de apostas encontrada para {}", today);
            } else {
                log.error("Erro ao finalizar rodada automaticamente para {}: {}", today, message, e);
            }
        }
    }

    /**
     * Loga informacoes sobre a proxima execucao (util para debug)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "UTC")
    public void logUpcomingFinalization() {
        log.info("Lembrete: A rodada de apostas sera finalizada automaticamente em 1 hora (10:05 UTC)");
    }
}

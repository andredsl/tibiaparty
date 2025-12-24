package com.tibia.app.service;

import com.tibia.app.domain.entity.Bet;
import com.tibia.app.domain.entity.BettingRound;
import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import com.tibia.app.domain.enums.BetStatus;
import com.tibia.app.dto.request.CreateBetRequest;
import com.tibia.app.dto.response.BoostedCreatureInfo;
import com.tibia.app.dto.response.CreatureInfo;
import com.tibia.app.exception.BusinessException;
import com.tibia.app.repository.BetRepository;
import com.tibia.app.repository.BettingRoundRepository;
import com.tibia.app.service.external.TibiaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BettingService {

    private static final Logger log = LoggerFactory.getLogger(BettingService.class);

    private static final long MIN_BET_AMOUNT = 25L;
    private static final long BET_INCREMENT = 25L;

    private final BetRepository betRepository;
    private final BettingRoundRepository bettingRoundRepository;
    private final TibiaDataService tibiaDataService;

    public BettingService(BetRepository betRepository,
                          BettingRoundRepository bettingRoundRepository,
                          TibiaDataService tibiaDataService) {
        this.betRepository = betRepository;
        this.bettingRoundRepository = bettingRoundRepository;
        this.tibiaDataService = tibiaDataService;
    }

    /**
     * Retorna a rodada atual (para o dia seguinte) ou cria uma nova
     */
    @Transactional
    public BettingRound getOrCreateCurrentRound() {
        LocalDate targetDate = LocalDate.now().plusDays(1);

        return bettingRoundRepository.findByTargetDate(targetDate)
                .orElseGet(() -> {
                    log.info("Criando nova rodada de apostas para {}", targetDate);

                    // Busca valor acumulado de rodadas anteriores sem vencedor
                    Long accumulated = calculateAccumulatedPool();

                    BettingRound round = BettingRound.builder()
                            .targetDate(targetDate)
                            .accumulatedPool(accumulated)
                            .build();

                    return bettingRoundRepository.save(round);
                });
    }

    /**
     * Calcula o valor acumulado de rodadas sem vencedor.
     * Soma o total_pool de todas as rodadas finalizadas sem vencedor
     * que ocorreram APOS a ultima rodada COM vencedor.
     */
    private Long calculateAccumulatedPool() {
        List<BettingRound> allFinalized = bettingRoundRepository.findAllFinalizedOrderByDateDesc();

        if (allFinalized.isEmpty()) {
            return 0L;
        }

        long accumulated = 0L;
        for (BettingRound round : allFinalized) {
            if (round.getHasWinners()) {
                // Encontrou uma rodada com vencedor, para de acumular
                break;
            }
            // Rodada finalizada sem vencedor, soma o total_pool
            accumulated += (round.getTotalPool() != null ? round.getTotalPool() : 0L);
        }

        return accumulated;
    }

    /**
     * Cria uma nova aposta
     */
    @Transactional
    public Bet createBet(User user, GameCharacter character, CreateBetRequest request) {
        // Validacoes
        validateBetAmount(request.getAmount());

        BettingRound round = getOrCreateCurrentRound();

        if (!round.canAcceptBets()) {
            throw new BusinessException("A rodada de apostas para amanha ja foi encerrada");
        }

        // Valida criatura
        Optional<CreatureInfo> creature = tibiaDataService.getCreatureByName(request.getCreatureName());
        if (creature.isEmpty()) {
            throw new BusinessException("Criatura invalida: " + request.getCreatureName());
        }

        // Cria a aposta
        Bet bet = Bet.builder()
                .bettingRound(round)
                .user(user)
                .character(character)
                .creatureName(creature.get().getName())
                .creatureImageUrl(creature.get().getImageUrl())
                .amount(request.getAmount())
                .status(BetStatus.PENDING)
                .build();

        bet = betRepository.save(bet);

        log.info("Aposta criada: Usuario {} apostou {} TC em {} para {}",
                user.getId(), request.getAmount(), creature.get().getName(), round.getTargetDate());

        return bet;
    }

    /**
     * Confirma uma aposta (admin)
     */
    @Transactional
    public Bet confirmBet(Long betId, String adminName, String notes) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new BusinessException("Aposta nao encontrada"));

        if (!bet.isPending()) {
            throw new BusinessException("Apenas apostas pendentes podem ser confirmadas");
        }

        bet.confirm(adminName);
        bet.setAdminNotes(notes);

        // Atualiza o pool da rodada
        BettingRound round = bet.getBettingRound();
        round.setTotalPool(round.getTotalPool() + bet.getAmount());
        bettingRoundRepository.save(round);

        log.info("Aposta {} confirmada por {}. Valor: {} TC", betId, adminName, bet.getAmount());

        return betRepository.save(bet);
    }

    /**
     * Cancela uma aposta pendente
     */
    @Transactional
    public Bet cancelBet(Long betId, User user) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new BusinessException("Aposta nao encontrada"));

        if (!bet.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Voce nao pode cancelar esta aposta");
        }

        if (!bet.canBeCancelled()) {
            throw new BusinessException("Esta aposta nao pode mais ser cancelada");
        }

        bet.cancel();

        log.info("Aposta {} cancelada pelo usuario {}", betId, user.getId());

        return betRepository.save(bet);
    }

    /**
     * Finaliza uma rodada de apostas
     */
    @Transactional
    public void finalizeRound(LocalDate targetDate) {
        BettingRound round = bettingRoundRepository.findByTargetDate(targetDate)
                .orElseThrow(() -> new BusinessException("Rodada nao encontrada para " + targetDate));

        if (round.getIsFinalized()) {
            throw new BusinessException("Esta rodada ja foi finalizada");
        }

        // Busca a criatura boostada do dia
        Optional<BoostedCreatureInfo> boosted = tibiaDataService.getBoostedCreature();
        if (boosted.isEmpty()) {
            throw new BusinessException("Nao foi possivel obter a criatura boostada de hoje");
        }

        String winningCreature = boosted.get().getName();
        round.setWinningCreature(winningCreature);
        round.setWinningCreatureImageUrl(boosted.get().getImageUrl());

        // Busca apostas confirmadas na criatura vencedora
        List<Bet> winningBets = betRepository.findByBettingRoundAndCreatureNameIgnoreCaseAndStatus(
                round, winningCreature, BetStatus.CONFIRMED);

        // Calcula premio
        Long totalPrize = round.getTotalPrizePool();

        if (winningBets.isEmpty()) {
            // Ninguem acertou - valor acumula
            round.setHasWinners(false);
            log.info("Rodada {} finalizada sem vencedores. Criatura: {}. Pool {} TC acumulado.",
                    targetDate, winningCreature, totalPrize);
        } else {
            // Distribui premio entre vencedores proporcionalmente
            round.setHasWinners(true);

            long totalWinningBets = winningBets.stream().mapToLong(Bet::getAmount).sum();

            for (Bet bet : winningBets) {
                // Premio proporcional ao valor apostado
                long prize = (bet.getAmount() * totalPrize) / totalWinningBets;
                bet.markAsWinner(prize);
                betRepository.save(bet);
            }

            log.info("Rodada {} finalizada com {} vencedores. Criatura: {}. Premio total: {} TC",
                    targetDate, winningBets.size(), winningCreature, totalPrize);
        }

        // Marca apostas perdedoras
        List<Bet> losingBets = betRepository.findByBettingRoundAndStatus(round, BetStatus.CONFIRMED);
        for (Bet bet : losingBets) {
            if (!bet.getCreatureName().equalsIgnoreCase(winningCreature)) {
                bet.markAsLoser();
                betRepository.save(bet);
            }
        }

        round.setIsFinalized(true);
        round.setFinalizedAt(LocalDateTime.now());
        bettingRoundRepository.save(round);
    }

    /**
     * Retorna todas as apostas de um usuario
     */
    public List<Bet> getUserBets(User user) {
        return betRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Retorna a aposta do usuario para a rodada atual (se existir)
     */
    public Bet getUserBetForCurrentRound(User user) {
        BettingRound currentRound = getOrCreateCurrentRound();
        return betRepository.findByUserAndBettingRound(user, currentRound).orElse(null);
    }

    /**
     * Retorna todas as apostas do usuario para a rodada atual
     */
    public List<Bet> getUserBetsForCurrentRound(User user) {
        BettingRound currentRound = getOrCreateCurrentRound();
        return betRepository.findByUserAndBettingRoundOrderByCreatedAtDesc(user, currentRound);
    }

    /**
     * Verifica se usuario ja apostou na rodada atual
     */
    public boolean hasUserBetOnCurrentRound(User user) {
        BettingRound currentRound = getOrCreateCurrentRound();
        return betRepository.existsByUserAndBettingRound(user, currentRound);
    }

    /**
     * Retorna apostas pendentes (para admin)
     */
    public List<Bet> getPendingBets() {
        return betRepository.findByStatusOrderByCreatedAtDesc(BetStatus.PENDING);
    }

    /**
     * Retorna o total do pool atual (apostas da rodada atual + acumulado de rodadas anteriores)
     */
    public Long getCurrentPoolTotal() {
        return getCurrentRoundBetsTotal() + getCurrentAccumulatedPool();
    }

    /**
     * Retorna o total_pool da rodada atual (nao finalizada) da tabela betting_rounds
     */
    public Long getCurrentRoundBetsTotal() {
        BettingRound round = getOrCreateCurrentRound();
        return round.getTotalPool() != null ? round.getTotalPool() : 0L;
    }

    /**
     * Retorna o valor acumulado de rodadas finalizadas sem vencedor.
     * Soma o total_pool de todas as rodadas finalizadas sem vencedor
     * que ocorreram APOS a ultima rodada COM vencedor.
     */
    public Long getCurrentAccumulatedPool() {
        List<BettingRound> allFinalized = bettingRoundRepository.findAllFinalizedOrderByDateDesc();

        if (allFinalized.isEmpty()) {
            return 0L;
        }

        long accumulated = 0L;
        for (BettingRound round : allFinalized) {
            if (round.getHasWinners()) {
                // Encontrou uma rodada com vencedor, para de acumular
                break;
            }
            // Rodada finalizada sem vencedor, soma o total_pool
            accumulated += (round.getTotalPool() != null ? round.getTotalPool() : 0L);
        }

        return accumulated;
    }

    /**
     * Retorna todas as rodadas finalizadas
     */
    public List<BettingRound> getHistory() {
        return bettingRoundRepository.findAllFinalizedOrderByDateDesc();
    }

    /**
     * Retorna estatisticas de apostas da rodada atual
     */
    public List<Object[]> getCurrentRoundStats() {
        BettingRound round = getOrCreateCurrentRound();
        return betRepository.findBetStatsByRound(round, BetStatus.CONFIRMED);
    }

    /**
     * Valida o valor da aposta
     */
    private void validateBetAmount(Long amount) {
        if (amount == null || amount < MIN_BET_AMOUNT) {
            throw new BusinessException("Aposta minima e de " + MIN_BET_AMOUNT + " Tibia Coins");
        }

        if (amount % BET_INCREMENT != 0) {
            throw new BusinessException("O valor da aposta deve ser multiplo de " + BET_INCREMENT);
        }
    }

    /**
     * Retorna a criatura boostada de hoje
     */
    public Optional<BoostedCreatureInfo> getTodaysBoostedCreature() {
        return tibiaDataService.getBoostedCreature();
    }

    /**
     * Retorna todas as criaturas disponiveis para aposta
     */
    public List<CreatureInfo> getAllCreatures() {
        return tibiaDataService.getAllCreatures();
    }

    /**
     * Busca criaturas por nome
     */
    public List<CreatureInfo> searchCreatures(String query) {
        return tibiaDataService.searchCreatures(query);
    }
}

package com.tibia.app.service;

import com.tibia.app.domain.entity.*;
import com.tibia.app.domain.enums.*;
import com.tibia.app.dto.request.CreatePartyRequest;
import com.tibia.app.dto.response.JoinRequestResponse;
import com.tibia.app.dto.response.PartyResponse;
import com.tibia.app.exception.PartyLimitExceededException;
import com.tibia.app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PartyService {

    private static final Logger log = LoggerFactory.getLogger(PartyService.class);
    private static final int MAX_ACTIVE_PARTIES_PER_USER = 30;
    private static final int PARTY_DURATION_HOURS = 2;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyVocationSlotRepository vocationSlotRepository;
    private final PartyJoinRequestRepository joinRequestRepository;
    private final SessionService sessionService;

    public PartyService(
            PartyRepository partyRepository,
            PartyMemberRepository partyMemberRepository,
            PartyVocationSlotRepository vocationSlotRepository,
            PartyJoinRequestRepository joinRequestRepository,
            SessionService sessionService) {
        this.partyRepository = partyRepository;
        this.partyMemberRepository = partyMemberRepository;
        this.vocationSlotRepository = vocationSlotRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.sessionService = sessionService;
    }

    /**
     * Cria uma nova party
     */
    @Transactional
    public PartyResponse createParty(CreatePartyRequest request) {
        GameCharacter leader = sessionService.requireCurrentCharacter();

        // Validacao de limites por tipo de PT
        if (request.getType() == PartyType.SOUL_CORE) {
            // Soul Core: maximo 2 PTs ativas como lider
            int activeSoulCoreParties = partyRepository.countActiveByLeaderAndType(
                    leader.getId(), PartyType.SOUL_CORE);
            if (activeSoulCoreParties >= 2) {
                throw new IllegalStateException("Voce ja possui 2 PTs de Soul Core ativas. Cancele uma antes de criar outra.");
            }
        } else {
            // Hunt/Boss: maximo 1 PT ativa (como lider ou membro)
            if (isInActiveHuntOrBossParty(leader.getId())) {
                throw new IllegalStateException("Voce ja esta em uma PT de Hunt ou Boss. Saia antes de criar outra.");
            }
        }

        // Verifica limite geral de parties criadas (rate limit)
        int activeParties = partyRepository.countCreatedByUserSince(
                leader.getUser().getId(),
                LocalDateTime.now().minusHours(24)
        );
        if (activeParties >= MAX_ACTIVE_PARTIES_PER_USER) {
            throw new PartyLimitExceededException(MAX_ACTIVE_PARTIES_PER_USER);
        }

        // Calcula max members baseado no tipo e configuracoes
        int maxMembers;
        if (request.getType() == PartyType.SOUL_CORE) {
            // Soul Core: maximo 5 players
            maxMembers = 5;
        } else if (request.hasVocationRequirements()) {
            maxMembers = request.getTotalVocationSlots();
        } else {
            maxMembers = request.getMaxMembers() != null ? request.getMaxMembers() : 5;
        }

        // Verifica se o lider participa
        boolean leaderParticipates = request.getLeaderParticipates() == null || request.getLeaderParticipates();

        // Define expiracao - Soul Core nao expira
        LocalDateTime expiresAt = request.getType() == PartyType.SOUL_CORE
                ? LocalDateTime.now().plusYears(10) // Nao expira (10 anos)
                : LocalDateTime.now().plusHours(PARTY_DURATION_HOURS);

        // Cria a party
        Party party = Party.builder()
                .type(request.getType())
                .world(leader.getWorld())
                .leader(leader)
                .status(PartyStatus.FORMING)
                .location(request.getType() == PartyType.HUNT ? request.getLocation() : null)
                .bossName(request.getType() == PartyType.BOSS ? request.getBossName() : null)
                .soulCoreName(request.getType() == PartyType.SOUL_CORE ? request.getSoulCoreName() : null)
                .pricePerPlayer(request.getType() == PartyType.SOUL_CORE ? request.getPricePerPlayer() : null)
                .leaderParticipates(leaderParticipates)
                .levelMin(request.getLevelMin())
                .levelMax(request.getLevelMax())
                .maxMembers(maxMembers)
                .scheduledTime(request.getStartWhenFull() != null && request.getStartWhenFull() ? null : request.getScheduledTime())
                .startWhenFull(request.getStartWhenFull())
                .description(request.getDescription())
                .expiresAt(expiresAt)
                .build();

        party = partyRepository.save(party);

        // Adiciona slots de vocacao
        addVocationSlots(party, request);

        // Adiciona o lider como membro confirmado apenas se ele participa
        if (leaderParticipates) {
            PartyMember leaderMember = PartyMember.builder()
                    .party(party)
                    .character(leader)
                    .role(MemberRole.LEADER)
                    .status(MemberStatus.CONFIRMED)
                    .build();

            partyMemberRepository.save(leaderMember);

            // Atualiza o slot da vocacao do lider se houver requisitos
            if (party.hasVocationRequirements()) {
                party.getSlotForVocation(leader.getVocation())
                        .ifPresent(slot -> {
                            slot.incrementFilled();
                            vocationSlotRepository.save(slot);
                        });
            }
        }

        log.info("Party criada: {} por {} em {}",
                party.getDisplayName(), leader.getName(), leader.getWorld().getName());

        return PartyResponse.fromEntity(party);
    }

    /**
     * Adiciona slots de vocacao a party
     */
    private void addVocationSlots(Party party, CreatePartyRequest request) {
        if (request.getSlotsEK() != null && request.getSlotsEK() > 0) {
            PartyVocationSlot slot = PartyVocationSlot.builder()
                    .party(party)
                    .vocation(Vocation.ELITE_KNIGHT)
                    .slotsNeeded(request.getSlotsEK())
                    .slotsFilled(0)
                    .build();
            vocationSlotRepository.save(slot);
            party.addVocationSlot(slot);
        }

        if (request.getSlotsRP() != null && request.getSlotsRP() > 0) {
            PartyVocationSlot slot = PartyVocationSlot.builder()
                    .party(party)
                    .vocation(Vocation.ROYAL_PALADIN)
                    .slotsNeeded(request.getSlotsRP())
                    .slotsFilled(0)
                    .build();
            vocationSlotRepository.save(slot);
            party.addVocationSlot(slot);
        }

        if (request.getSlotsED() != null && request.getSlotsED() > 0) {
            PartyVocationSlot slot = PartyVocationSlot.builder()
                    .party(party)
                    .vocation(Vocation.ELDER_DRUID)
                    .slotsNeeded(request.getSlotsED())
                    .slotsFilled(0)
                    .build();
            vocationSlotRepository.save(slot);
            party.addVocationSlot(slot);
        }

        if (request.getSlotsMS() != null && request.getSlotsMS() > 0) {
            PartyVocationSlot slot = PartyVocationSlot.builder()
                    .party(party)
                    .vocation(Vocation.MASTER_SORCERER)
                    .slotsNeeded(request.getSlotsMS())
                    .slotsFilled(0)
                    .build();
            vocationSlotRepository.save(slot);
            party.addVocationSlot(slot);
        }
    }

    /**
     * Solicita entrada em uma party (aguarda aprovacao do lider)
     */
    @Transactional
    public JoinRequestResult requestToJoin(Long partyId, String message) {
        GameCharacter character = sessionService.requireCurrentCharacter();

        Party party = partyRepository.findByIdWithDetails(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party nao encontrada"));

        // Validacoes
        if (!party.isActive()) {
            return JoinRequestResult.error("Esta PT nao esta mais ativa");
        }

        if (party.isExpired()) {
            return JoinRequestResult.error("Esta PT expirou");
        }

        if (party.isFull()) {
            return JoinRequestResult.error("Esta PT esta lotada");
        }

        // Verifica se ja e membro
        if (isMemberOf(partyId, character.getId())) {
            return JoinRequestResult.error("Voce ja esta nesta PT");
        }

        // Verifica se ja esta em outra party
        if (isInActiveParty(character.getId())) {
            return JoinRequestResult.error("Voce ja esta em uma PT. Saia primeiro antes de solicitar entrada em outra.");
        }

        // Verifica se ja tem solicitacao pendente
        if (joinRequestRepository.existsPendingRequest(partyId, character.getId())) {
            return JoinRequestResult.error("Voce ja tem uma solicitacao pendente para esta PT");
        }

        // Verifica world
        if (!party.getWorld().getId().equals(character.getWorld().getId())) {
            return JoinRequestResult.error("Voce precisa estar no mesmo world da PT");
        }

        // Verifica level
        if (party.getLevelMin() != null && character.getLevel() < party.getLevelMin()) {
            return JoinRequestResult.error("Seu level e menor que o minimo exigido (" + party.getLevelMin() + ")");
        }
        if (party.getLevelMax() != null && character.getLevel() > party.getLevelMax()) {
            return JoinRequestResult.error("Seu level e maior que o maximo permitido (" + party.getLevelMax() + ")");
        }

        // Verifica vaga para vocacao
        if (party.hasVocationRequirements()) {
            Optional<PartyVocationSlot> slotOpt = party.getSlotForVocation(character.getVocation());

            if (slotOpt.isEmpty()) {
                return JoinRequestResult.error("Esta PT nao procura " + character.getVocation().getAbbreviation());
            }

            PartyVocationSlot slot = slotOpt.get();
            if (slot.isFull()) {
                return JoinRequestResult.error("As vagas para " + character.getVocation().getAbbreviation() +
                        " ja estao preenchidas (" + slot.getSlotsFilled() + "/" + slot.getSlotsNeeded() + ")");
            }
        }

        // Cria solicitacao
        PartyJoinRequest request = PartyJoinRequest.builder()
                .party(party)
                .character(character)
                .status(JoinRequestStatus.PENDING)
                .message(message)
                .build();

        joinRequestRepository.save(request);

        log.info("{} solicitou entrada na party {} ({})",
                character.getName(), party.getId(), party.getDisplayName());

        return JoinRequestResult.success("Solicitacao enviada! Aguarde o lider aceitar.");
    }

    /**
     * Aceita uma solicitacao de entrada (apenas lider)
     */
    @Transactional
    public void acceptJoinRequest(Long requestId) {
        GameCharacter leader = sessionService.requireCurrentCharacter();

        PartyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitacao nao encontrada"));

        Party party = request.getParty();

        // Verifica se e o lider
        if (!party.getLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("Apenas o lider pode aceitar solicitacoes");
        }

        // Verifica se a solicitacao ainda esta pendente
        if (!request.isPending()) {
            throw new IllegalArgumentException("Esta solicitacao ja foi respondida");
        }

        // Verifica se a party ainda aceita membros
        if (party.isFull()) {
            throw new IllegalArgumentException("A PT esta lotada");
        }

        GameCharacter character = request.getCharacter();

        // Verifica vaga para vocacao
        if (party.hasVocationRequirements()) {
            Optional<PartyVocationSlot> slotOpt = party.getSlotForVocation(character.getVocation());

            if (slotOpt.isEmpty() || slotOpt.get().isFull()) {
                throw new IllegalArgumentException("Nao ha mais vagas para " + character.getVocation().getAbbreviation());
            }

            // Incrementa o slot preenchido
            slotOpt.get().incrementFilled();
            vocationSlotRepository.save(slotOpt.get());
        }

        // Aceita a solicitacao
        request.accept();
        joinRequestRepository.save(request);

        // Adiciona como membro
        PartyMember member = PartyMember.builder()
                .party(party)
                .character(character)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.CONFIRMED)
                .build();

        partyMemberRepository.save(member);

        // Verifica se a party ficou completa
        if (party.getConfirmedMembersCount() + 1 >= party.getMaxMembers()) {
            party.setStatus(PartyStatus.READY);
            partyRepository.save(party);
        }

        log.info("{} foi aceito na party {} por {}",
                character.getName(), party.getId(), leader.getName());
    }

    /**
     * Recusa uma solicitacao de entrada (apenas lider)
     */
    @Transactional
    public void rejectJoinRequest(Long requestId, String reason) {
        GameCharacter leader = sessionService.requireCurrentCharacter();

        PartyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitacao nao encontrada"));

        Party party = request.getParty();

        // Verifica se e o lider
        if (!party.getLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("Apenas o lider pode recusar solicitacoes");
        }

        // Verifica se a solicitacao ainda esta pendente
        if (!request.isPending()) {
            throw new IllegalArgumentException("Esta solicitacao ja foi respondida");
        }

        // Recusa a solicitacao
        request.reject(reason);
        joinRequestRepository.save(request);

        log.info("{} foi recusado na party {} por {}: {}",
                request.getCharacter().getName(), party.getId(), leader.getName(),
                reason != null ? reason : "Sem motivo");
    }

    /**
     * Cancela uma solicitacao pendente (pelo solicitante)
     */
    @Transactional
    public void cancelJoinRequest(Long requestId) {
        GameCharacter character = sessionService.requireCurrentCharacter();

        PartyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitacao nao encontrada"));

        if (!request.getCharacter().getId().equals(character.getId())) {
            throw new IllegalArgumentException("Voce nao pode cancelar esta solicitacao");
        }

        if (!request.isPending()) {
            throw new IllegalArgumentException("Esta solicitacao ja foi respondida");
        }

        request.setStatus(JoinRequestStatus.CANCELLED);
        joinRequestRepository.save(request);

        log.info("{} cancelou solicitacao para party {}", character.getName(), request.getParty().getId());
    }

    /**
     * Sai de uma party
     */
    @Transactional
    public void leaveParty(Long partyId) {
        GameCharacter character = sessionService.requireCurrentCharacter();

        Party party = partyRepository.findByIdWithDetails(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party nao encontrada"));

        // Verifica se e o lider
        if (party.getLeader().getId().equals(character.getId())) {
            throw new IllegalArgumentException("O lider nao pode sair da PT. Cancele a PT se necessario.");
        }

        // Busca o membro
        PartyMember member = party.getMembers().stream()
                .filter(m -> m.getCharacter().getId().equals(character.getId()) &&
                            m.getStatus() == MemberStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Voce nao esta nesta PT"));

        // Decrementa o slot de vocacao
        if (party.hasVocationRequirements()) {
            party.getSlotForVocation(character.getVocation())
                    .ifPresent(slot -> {
                        slot.decrementFilled();
                        vocationSlotRepository.save(slot);
                    });
        }

        // Remove o membro
        member.setStatus(MemberStatus.LEFT);
        partyMemberRepository.save(member);

        // Atualiza status da party se necessario
        if (party.getStatus() == PartyStatus.READY) {
            party.setStatus(PartyStatus.FORMING);
            partyRepository.save(party);
        }

        log.info("{} saiu da party {}", character.getName(), partyId);
    }

    /**
     * Lista parties ativas do world atual
     */
    @Transactional(readOnly = true)
    public List<PartyResponse> listActiveParties(String worldName, PartyType type) {
        List<PartyStatus> activeStatuses = List.of(PartyStatus.FORMING, PartyStatus.READY);

        List<Party> parties;
        if (type != null) {
            parties = partyRepository.findByWorldNameAndStatuses(worldName, activeStatuses, null)
                    .stream()
                    .filter(p -> p.getType() == type)
                    .collect(Collectors.toList());
        } else {
            parties = partyRepository.findByWorldNameAndStatuses(worldName, activeStatuses, null)
                    .getContent();
        }

        return parties.stream()
                .filter(p -> !p.isExpired())
                .map(PartyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lista parties ativas do world do character logado
     */
    @Transactional(readOnly = true)
    public List<PartyResponse> listActivePartiesForCurrentWorld() {
        String worldName = sessionService.getCurrentWorld()
                .orElseThrow(() -> new IllegalStateException("World nao definido na sessao"));

        return listActiveParties(worldName, null);
    }

    /**
     * Busca party por ID
     */
    @Transactional(readOnly = true)
    public Optional<PartyResponse> findById(Long id) {
        return partyRepository.findByIdWithDetails(id)
                .map(PartyResponse::fromEntity);
    }

    /**
     * Cancela uma party (apenas o lider pode)
     */
    @Transactional
    public void cancelParty(Long partyId) {
        GameCharacter character = sessionService.requireCurrentCharacter();

        Party party = partyRepository.findByIdWithDetails(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party nao encontrada"));

        if (!party.getLeader().getId().equals(character.getId())) {
            throw new IllegalArgumentException("Apenas o lider pode cancelar a party");
        }

        if (!party.isActive()) {
            throw new IllegalArgumentException("Party ja esta finalizada");
        }

        party.setStatus(PartyStatus.CANCELLED);
        partyRepository.save(party);

        log.info("Party {} cancelada por {}", partyId, character.getName());
    }

    /**
     * Verifica se o character e membro de uma party especifica
     */
    @Transactional(readOnly = true)
    public boolean isMemberOf(Long partyId, Long characterId) {
        return partyRepository.findByIdWithDetails(partyId)
                .map(party -> party.getMembers().stream()
                        .anyMatch(m -> m.getCharacter().getId().equals(characterId) &&
                                      m.getStatus() == MemberStatus.CONFIRMED))
                .orElse(false);
    }

    /**
     * Verifica se o character esta em alguma party ativa
     */
    @Transactional(readOnly = true)
    public boolean isInActiveParty(Long characterId) {
        return partyMemberRepository.existsActivePartyMembership(characterId);
    }

    /**
     * Verifica se o character esta em uma party ativa de Hunt ou Boss
     */
    @Transactional(readOnly = true)
    public boolean isInActiveHuntOrBossParty(Long characterId) {
        return partyMemberRepository.existsActivePartyMembershipByTypes(
                characterId, List.of(PartyType.HUNT, PartyType.BOSS));
    }

    /**
     * Retorna a party ativa do character (se houver)
     */
    @Transactional(readOnly = true)
    public Optional<PartyResponse> getCurrentParty(Long characterId) {
        return partyMemberRepository.findActivePartyByCharacter(characterId)
                .map(PartyResponse::fromEntity);
    }

    /**
     * Lista solicitacoes pendentes para uma party (para o lider)
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getPendingRequests(Long partyId) {
        GameCharacter leader = sessionService.requireCurrentCharacter();

        Party party = partyRepository.findByIdWithDetails(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party nao encontrada"));

        if (!party.getLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("Apenas o lider pode ver as solicitacoes");
        }

        return joinRequestRepository.findByPartyIdAndStatus(partyId, JoinRequestStatus.PENDING)
                .stream()
                .map(JoinRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lista solicitacoes pendentes do character atual
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getMyPendingRequests() {
        GameCharacter character = sessionService.requireCurrentCharacter();

        return joinRequestRepository.findByCharacterIdAndStatus(character.getId(), JoinRequestStatus.PENDING)
                .stream()
                .map(JoinRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lista recusas recentes do character atual (ultimas 24h)
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getMyRecentRejections() {
        GameCharacter character = sessionService.requireCurrentCharacter();

        return joinRequestRepository.findRecentRejections(character.getId(), LocalDateTime.now().minusHours(24))
                .stream()
                .map(JoinRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Conta solicitacoes pendentes de uma party
     */
    @Transactional(readOnly = true)
    public int countPendingRequests(Long partyId) {
        return joinRequestRepository.countPendingByParty(partyId);
    }

    /**
     * Marca uma notificacao de recusa como vista
     */
    @Transactional
    public void markRejectionAsSeen(Long requestId) {
        GameCharacter character = sessionService.requireCurrentCharacter();

        PartyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitacao nao encontrada"));

        // Verifica se e o dono da solicitacao
        if (!request.getCharacter().getId().equals(character.getId())) {
            throw new IllegalArgumentException("Voce nao pode marcar esta notificacao");
        }

        // Verifica se e uma recusa
        if (!request.isRejected()) {
            throw new IllegalArgumentException("Esta solicitacao nao foi recusada");
        }

        request.setSeenAt(LocalDateTime.now());
        joinRequestRepository.save(request);
    }

    /**
     * Verifica se tem solicitacao pendente para uma party
     */
    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long partyId, Long characterId) {
        return joinRequestRepository.existsPendingRequest(partyId, characterId);
    }

    /**
     * Job para expirar parties antigas
     */
    @Scheduled(fixedRate = 60000) // 1 minuto
    @Transactional
    public void expireOldParties() {
        int expired = partyRepository.expireOldParties(LocalDateTime.now());
        if (expired > 0) {
            log.info("{} parties expiradas", expired);
        }
    }

    /**
     * Resultado de solicitacao de entrada na party
     */
    public record JoinRequestResult(boolean success, String message) {
        public static JoinRequestResult success(String message) {
            return new JoinRequestResult(true, message);
        }

        public static JoinRequestResult error(String message) {
            return new JoinRequestResult(false, message);
        }
    }
}

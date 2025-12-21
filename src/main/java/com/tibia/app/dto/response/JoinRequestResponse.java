package com.tibia.app.dto.response;

import com.tibia.app.domain.entity.PartyJoinRequest;
import com.tibia.app.domain.enums.JoinRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {

    private Long id;
    private Long partyId;
    private String partyName;
    private String leaderName;
    private Long characterId;
    private String characterName;
    private String characterVocation;
    private int characterLevel;
    private JoinRequestStatus status;
    private String message;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static JoinRequestResponse fromEntity(PartyJoinRequest request) {
        return JoinRequestResponse.builder()
                .id(request.getId())
                .partyId(request.getParty().getId())
                .partyName(request.getParty().getDisplayName())
                .leaderName(request.getParty().getLeader().getName())
                .characterId(request.getCharacter().getId())
                .characterName(request.getCharacter().getName())
                .characterVocation(request.getCharacter().getVocationAbbreviation())
                .characterLevel(request.getCharacter().getLevel())
                .status(request.getStatus())
                .message(request.getMessage())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }
}

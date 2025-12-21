package com.tibia.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TibiaCharacterData {

    private String name;
    private String world;
    private String vocation;
    private int level;
    private String comment;
    private String accountStatus;
    private Integer accountAgeDays;
    private String guild;
    private LocalDateTime fetchedAt;

    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }

    public boolean isPremium() {
        return "Premium Account".equalsIgnoreCase(accountStatus);
    }
}

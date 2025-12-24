package com.tibia.app.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatureInfo {
    private String name;
    private String race;
    private String imageUrl;
    private Boolean featured;

    public String getDisplayName() {
        return name;
    }
}

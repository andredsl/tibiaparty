package com.tibia.app.dto.response;

import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.enums.Vocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterResponse {

    private Long id;
    private String name;
    private String world;
    private int level;
    private Vocation vocation;
    private String vocationAbbreviation;
    private boolean main;
    private LocalDateTime verifiedAt;

    public static CharacterResponse fromEntity(GameCharacter character) {
        return CharacterResponse.builder()
                .id(character.getId())
                .name(character.getName())
                .world(character.getWorld().getName())
                .level(character.getLevel())
                .vocation(character.getVocation())
                .vocationAbbreviation(character.getVocationAbbreviation())
                .main(character.isMain())
                .verifiedAt(character.getVerifiedAt())
                .build();
    }
}

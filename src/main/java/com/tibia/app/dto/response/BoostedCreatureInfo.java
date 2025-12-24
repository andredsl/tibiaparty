package com.tibia.app.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoostedCreatureInfo {
    private String name;
    private String imageUrl;
    private LocalDate date;

    public boolean isToday() {
        return LocalDate.now().equals(date);
    }
}

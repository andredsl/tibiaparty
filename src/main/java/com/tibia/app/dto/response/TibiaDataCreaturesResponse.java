package com.tibia.app.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TibiaDataCreaturesResponse {

    @JsonProperty("creatures")
    private CreaturesData creatures;

    @JsonProperty("information")
    private Information information;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreaturesData {
        @JsonProperty("boosted")
        private BoostedCreature boosted;

        @JsonProperty("creature_list")
        private List<CreatureListItem> creatureList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoostedCreature {
        @JsonProperty("name")
        private String name;

        @JsonProperty("image_url")
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreatureListItem {
        @JsonProperty("name")
        private String name;

        @JsonProperty("race")
        private String race;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("featured")
        private Boolean featured;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Information {
        @JsonProperty("api")
        private ApiInfo api;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("status")
        private StatusInfo status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiInfo {
        @JsonProperty("version")
        private Integer version;

        @JsonProperty("release")
        private String release;

        @JsonProperty("commit")
        private String commit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusInfo {
        @JsonProperty("http_code")
        private Integer httpCode;

        @JsonProperty("error")
        private Integer error;

        @JsonProperty("message")
        private String message;
    }
}

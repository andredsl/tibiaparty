package com.tibia.app.service.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tibia.app.dto.response.TibiaCharacterData;
import com.tibia.app.exception.TibiaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TibiaApiClient {

    private static final Logger log = LoggerFactory.getLogger(TibiaApiClient.class);

    private static final String TIBIADATA_API_URL = "https://api.tibiadata.com/v4/character/";
    private static final int TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, TibiaCharacterData> cache;

    public TibiaApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * Busca dados do character no TibiaData API
     */
    public Optional<TibiaCharacterData> fetchCharacter(String characterName) {
        return fetchCharacter(characterName, false);
    }

    /**
     * Busca dados do character, com opcao de ignorar cache
     */
    public Optional<TibiaCharacterData> fetchCharacter(String characterName, boolean bypassCache) {
        String cacheKey = characterName.toLowerCase().trim();

        if (!bypassCache) {
            TibiaCharacterData cached = cache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("Cache hit para character: {}", characterName);
                return Optional.of(cached);
            }
        }

        try {
            String encodedName = URLEncoder.encode(characterName, StandardCharsets.UTF_8);
            String url = TIBIADATA_API_URL + encodedName;
            log.debug("Buscando character em TibiaData: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("TibiaData API retornou status {}", response.statusCode());
                throw new TibiaApiException("Erro ao buscar dados do TibiaData: HTTP " + response.statusCode());
            }

            TibiaDataResponse apiResponse = objectMapper.readValue(response.body(), TibiaDataResponse.class);

            if (apiResponse.character == null || apiResponse.character.character == null) {
                log.warn("Character nao encontrado: {}", characterName);
                return Optional.empty();
            }

            TibiaCharacterData data = mapToCharacterData(apiResponse.character);

            if (data != null && data.getName() != null) {
                cache.put(cacheKey, data);
                log.info("Character encontrado: {} (Level {} {} em {})",
                        data.getName(), data.getLevel(), data.getVocation(), data.getWorld());
                return Optional.of(data);
            }

            return Optional.empty();

        } catch (TibiaApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao buscar character {}: {}", characterName, e.getMessage());
            throw new TibiaApiException("Erro ao buscar dados do TibiaData", e);
        }
    }

    /**
     * Mapeia resposta da API para nosso DTO
     */
    private TibiaCharacterData mapToCharacterData(CharacterWrapper wrapper) {
        CharacterInfo info = wrapper.character;
        if (info == null || info.name == null || info.name.isEmpty()) {
            return null;
        }

        // Calcula idade da conta baseado no loyalty title
        Integer accountAgeDays = estimateAccountAge(info.loyaltyTitle, info.accountStatus);

        // Extrai guild se existir
        String guild = null;
        if (wrapper.guild != null && wrapper.guild.name != null) {
            guild = wrapper.guild.name;
        }

        return TibiaCharacterData.builder()
                .name(info.name)
                .world(info.world)
                .vocation(info.vocation)
                .level(info.level)
                .comment(info.comment)
                .accountStatus(info.accountStatus)
                .guild(guild)
                .accountAgeDays(accountAgeDays)
                .fetchedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Estima a idade da conta baseado no loyalty title
     */
    private Integer estimateAccountAge(String loyaltyTitle, String accountStatus) {
        if (loyaltyTitle != null && !loyaltyTitle.isEmpty() && !loyaltyTitle.equalsIgnoreCase("none")) {
            return switch (loyaltyTitle.toLowerCase()) {
                case "scout of tibia" -> 365;
                case "veteran of tibia" -> 730;
                case "sage of tibia" -> 1095;
                case "tutorial of tibia" -> 1825;
                case "vanguard of tibia" -> 3650;
                default -> 365;
            };
        }

        if ("Premium Account".equalsIgnoreCase(accountStatus)) {
            return 30;
        }

        return 7;
    }

    /**
     * Invalida cache para um character especifico
     */
    public void invalidateCache(String characterName) {
        cache.invalidate(characterName.toLowerCase().trim());
    }

    /**
     * Limpa todo o cache
     */
    public void clearCache() {
        cache.invalidateAll();
    }

    // Classes para mapeamento JSON da TibiaData API

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TibiaDataResponse {
        @JsonProperty("character")
        public CharacterWrapper character;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CharacterWrapper {
        @JsonProperty("character")
        public CharacterInfo character;

        @JsonProperty("guild")
        public GuildInfo guild;

        @JsonProperty("account_badges")
        public List<Object> accountBadges;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CharacterInfo {
        @JsonProperty("name")
        public String name;

        @JsonProperty("world")
        public String world;

        @JsonProperty("vocation")
        public String vocation;

        @JsonProperty("level")
        public int level;

        @JsonProperty("comment")
        public String comment;

        @JsonProperty("account_status")
        public String accountStatus;

        @JsonProperty("loyalty_title")
        public String loyaltyTitle;

        @JsonProperty("traded")
        public boolean traded;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GuildInfo {
        @JsonProperty("name")
        public String name;

        @JsonProperty("rank")
        public String rank;
    }
}

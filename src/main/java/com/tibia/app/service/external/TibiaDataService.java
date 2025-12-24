package com.tibia.app.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tibia.app.dto.response.BoostedCreatureInfo;
import com.tibia.app.dto.response.CreatureInfo;
import com.tibia.app.dto.response.TibiaDataCreaturesResponse;
import com.tibia.app.exception.TibiaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TibiaDataService {

    private static final Logger log = LoggerFactory.getLogger(TibiaDataService.class);

    private static final String TIBIA_DATA_API_URL = "https://api.tibiadata.com/v4/creatures";
    private static final int TIMEOUT_SECONDS = 15;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, TibiaDataCreaturesResponse> cache;

    public TibiaDataService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10)
                .build();
    }

    /**
     * Busca todas as criaturas da API TibiaData
     */
    public TibiaDataCreaturesResponse fetchCreaturesData() {
        return fetchCreaturesData(false);
    }

    /**
     * Busca todas as criaturas, com opcao de ignorar cache
     */
    public TibiaDataCreaturesResponse fetchCreaturesData(boolean bypassCache) {
        String cacheKey = "creatures";

        if (!bypassCache) {
            TibiaDataCreaturesResponse cached = cache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("Cache hit para creatures");
                return cached;
            }
        }

        try {
            log.debug("Buscando criaturas da API TibiaData: {}", TIBIA_DATA_API_URL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TIBIA_DATA_API_URL))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("TibiaData API retornou status {}", response.statusCode());
                throw new TibiaApiException("O servico TibiaData esta instavel no momento. Tente novamente em alguns minutos.");
            }

            TibiaDataCreaturesResponse creaturesResponse = objectMapper.readValue(
                    response.body(),
                    TibiaDataCreaturesResponse.class
            );

            if (creaturesResponse != null) {
                cache.put(cacheKey, creaturesResponse);
                log.info("Criaturas carregadas com sucesso. Total: {}",
                        creaturesResponse.getCreatures() != null && creaturesResponse.getCreatures().getCreatureList() != null
                                ? creaturesResponse.getCreatures().getCreatureList().size()
                                : 0);
            }

            return creaturesResponse;

        } catch (TibiaApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao buscar criaturas da TibiaData API: {}", e.getMessage());
            throw new TibiaApiException("O servico TibiaData esta instavel no momento. Tente novamente em alguns minutos.", e);
        }
    }

    /**
     * Retorna a lista de todas as criaturas
     */
    public List<CreatureInfo> getAllCreatures() {
        try {
            TibiaDataCreaturesResponse response = fetchCreaturesData();

            if (response == null || response.getCreatures() == null ||
                    response.getCreatures().getCreatureList() == null) {
                return Collections.emptyList();
            }

            return response.getCreatures().getCreatureList().stream()
                    .map(item -> CreatureInfo.builder()
                            .name(item.getName())
                            .race(item.getRace())
                            .imageUrl(item.getImageUrl())
                            .featured(item.getFeatured())
                            .build())
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .collect(Collectors.toList());
        } catch (TibiaApiException e) {
            log.warn("Erro ao buscar criaturas: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retorna informacoes sobre a criatura boostada do dia
     */
    public Optional<BoostedCreatureInfo> getBoostedCreature() {
        try {
            TibiaDataCreaturesResponse response = fetchCreaturesData();

            if (response == null || response.getCreatures() == null ||
                    response.getCreatures().getBoosted() == null) {
                return Optional.empty();
            }

            TibiaDataCreaturesResponse.BoostedCreature boosted = response.getCreatures().getBoosted();

            return Optional.of(BoostedCreatureInfo.builder()
                    .name(boosted.getName())
                    .imageUrl(boosted.getImageUrl())
                    .date(LocalDate.now())
                    .build());
        } catch (TibiaApiException e) {
            log.warn("Erro ao buscar criatura boostada: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca informacoes de uma criatura especifica pelo nome
     */
    public Optional<CreatureInfo> getCreatureByName(String name) {
        return getAllCreatures().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Busca criaturas por nome parcial (para autocomplete)
     */
    public List<CreatureInfo> searchCreatures(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllCreatures();
        }

        String lowerQuery = query.toLowerCase().trim();

        return getAllCreatures().stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerQuery))
                .limit(20)
                .collect(Collectors.toList());
    }

    /**
     * Invalida o cache
     */
    public void invalidateCache() {
        cache.invalidateAll();
    }
}

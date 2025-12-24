package com.tibia.app.service.external;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tibia.app.dto.response.BoostedCreatureInfo;
import com.tibia.app.dto.response.CreatureInfo;
import com.tibia.app.dto.response.TibiaDataCreaturesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;
    private final Cache<String, TibiaDataCreaturesResponse> cache;

    public TibiaDataService() {
        this.restTemplate = new RestTemplate();
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

            TibiaDataCreaturesResponse response = restTemplate.getForObject(
                    TIBIA_DATA_API_URL,
                    TibiaDataCreaturesResponse.class
            );

            if (response != null) {
                cache.put(cacheKey, response);
                log.info("Criaturas carregadas com sucesso. Total: {}",
                        response.getCreatures() != null && response.getCreatures().getCreatureList() != null
                                ? response.getCreatures().getCreatureList().size()
                                : 0);
            }

            return response;

        } catch (Exception e) {
            log.error("Erro ao buscar criaturas da TibiaData API: {}", e.getMessage());
            throw new RuntimeException("Erro ao buscar dados da TibiaData API", e);
        }
    }

    /**
     * Retorna a lista de todas as criaturas
     */
    public List<CreatureInfo> getAllCreatures() {
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
    }

    /**
     * Retorna informacoes sobre a criatura boostada do dia
     */
    public Optional<BoostedCreatureInfo> getBoostedCreature() {
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

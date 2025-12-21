package com.tibia.app.service.external;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tibia.app.dto.response.TibiaCharacterData;
import com.tibia.app.exception.TibiaApiException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TibiaApiClient {

    private static final Logger log = LoggerFactory.getLogger(TibiaApiClient.class);

    private static final String TIBIA_CHAR_URL = "https://www.tibia.com/community/?subtopic=characters&name=";
    private static final int TIMEOUT_MS = 10000;

    // Cache local para evitar hammering no Tibia.com
    private final Cache<String, TibiaCharacterData> cache;

    public TibiaApiClient() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * Busca dados do character no Tibia.com
     */
    public Optional<TibiaCharacterData> fetchCharacter(String characterName) {
        return fetchCharacter(characterName, false);
    }

    /**
     * Busca dados do character, com opção de ignorar cache
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
            String url = TIBIA_CHAR_URL + URLEncoder.encode(characterName, StandardCharsets.UTF_8);
            log.debug("Buscando character em: {}", url);

            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            TibiaCharacterData data = parseCharacterPage(doc);

            if (data != null) {
                cache.put(cacheKey, data);
                log.info("Character encontrado: {} (Level {} {} em {})",
                        data.getName(), data.getLevel(), data.getVocation(), data.getWorld());
                return Optional.of(data);
            }

            log.warn("Character não encontrado: {}", characterName);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Erro ao buscar character {}: {}", characterName, e.getMessage());
            throw new TibiaApiException("Erro ao buscar dados do Tibia.com", e);
        }
    }

    /**
     * Parse da página HTML do character
     */
    private TibiaCharacterData parseCharacterPage(Document doc) {
        String html = doc.html();

        // Verifica se character existe
        if (html.contains("Could not find character")) {
            return null;
        }

        try {
            String name = extractTableValue(doc, "Name:");
            String world = extractTableValue(doc, "World:");
            String vocation = extractTableValue(doc, "Vocation:");
            String levelStr = extractTableValue(doc, "Level:");
            String comment = extractTableValue(doc, "Comment:");
            String accountStatus = extractTableValue(doc, "Account Status:");
            String guild = extractGuild(doc);

            // Limpa nome (remove títulos, traded)
            if (name != null) {
                name = cleanCharacterName(name);
            }

            // Parse level
            int level = 0;
            if (levelStr != null) {
                level = Integer.parseInt(levelStr.replaceAll("[^0-9]", ""));
            }

            // Estima idade da conta
            Integer accountAgeDays = estimateAccountAge(doc);

            return TibiaCharacterData.builder()
                    .name(name)
                    .world(world)
                    .vocation(vocation)
                    .level(level)
                    .comment(comment)
                    .accountStatus(accountStatus)
                    .guild(guild)
                    .accountAgeDays(accountAgeDays)
                    .fetchedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao fazer parse do character: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai valor de uma linha da tabela de informações
     */
    private String extractTableValue(Document doc, String fieldName) {
        Elements tables = doc.select("table.TableContent");

        for (Element table : tables) {
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    String label = cells.get(0).text().trim();
                    if (label.equalsIgnoreCase(fieldName)) {
                        return cells.get(1).text().trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extrai informação da guild
     */
    private String extractGuild(Document doc) {
        String guildInfo = extractTableValue(doc, "Guild Membership:");
        if (guildInfo != null && !guildInfo.isEmpty()) {
            // Formato: "Member of the Guild Name"
            return guildInfo.replaceFirst("(?i)^(member|leader|vice.leader)\\s+of\\s+(the\\s+)?", "").trim();
        }
        return null;
    }

    /**
     * Limpa o nome do character (remove títulos e marcações)
     */
    private String cleanCharacterName(String name) {
        // Remove "(traded)" do final
        name = name.replaceAll("\\s*\\(traded\\)\\s*$", "").trim();
        // Remove títulos que aparecem após o nome
        int commaIndex = name.indexOf(',');
        if (commaIndex > 0) {
            name = name.substring(0, commaIndex).trim();
        }
        return name;
    }

    /**
     * Estima a idade da conta baseado em indicadores
     */
    private Integer estimateAccountAge(Document doc) {
        try {
            // Verifica Loyalty Title
            String loyaltyTitle = extractTableValue(doc, "Loyalty Title:");
            if (loyaltyTitle != null && !loyaltyTitle.equalsIgnoreCase("none")) {
                // Títulos de loyalty indicam conta antiga
                return switch (loyaltyTitle.toLowerCase()) {
                    case "scout of tibia" -> 365;          // 1 ano
                    case "veteran of tibia" -> 730;       // 2 anos
                    case "sage of tibia" -> 1095;         // 3 anos
                    case "tutorial of tibia" -> 1825;     // 5 anos
                    case "vanguard of tibia" -> 3650;     // 10 anos
                    default -> 365;
                };
            }

            // Se Premium, assume pelo menos 30 dias
            String accountStatus = extractTableValue(doc, "Account Status:");
            if ("Premium Account".equalsIgnoreCase(accountStatus)) {
                return 30;
            }

            // Conta free sem loyalty
            return 7; // Assume conta nova

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invalida cache para um character específico
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
}

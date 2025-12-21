package com.tibia.app.config;

import com.tibia.app.domain.entity.World;
import com.tibia.app.repository.WorldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(WorldRepository worldRepository) {
        return args -> {
            if (worldRepository.count() == 0) {
                log.info("Inicializando banco de dados com worlds...");

                List<World> worlds = List.of(
                        // South America - Open PvP
                        World.builder().name("Belobra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Descubra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Gentebra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Inabra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Kalibra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Lobera").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Lutabra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Ombra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Pacembra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Quelibra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Serdebra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Venebra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Yonabra").location("SA").pvpType("Open PvP").build(),

                        // South America - Optional PvP
                        World.builder().name("Assobra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Axerabra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Bombra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Calmera").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Ferobra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Honbra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Impera").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Libertabra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Nossobra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Quintera").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Refugia").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Solidera").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Torpera").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Utobra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Visabra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Xandebra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Zenobra").location("SA").pvpType("Optional PvP").build(),

                        // South America - Retro
                        World.builder().name("Epoca").location("SA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Julera").location("SA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Mudabra").location("SA").pvpType("Retro Hardcore PvP").build(),
                        World.builder().name("Issobra").location("SA").pvpType("Optional PvP").build(),

                        // Europe - Open PvP
                        World.builder().name("Antica").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Harmonia").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Monza").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Peloria").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Premia").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Secura").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Vunira").location("EU").pvpType("Open PvP").build(),

                        // Europe - Optional PvP
                        World.builder().name("Celebra").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Emera").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Firmera").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Garnera").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Luminera").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Menera").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Pacera").location("EU").pvpType("Optional PvP").build(),

                        // North America - Open PvP
                        World.builder().name("Gladera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Wintera").location("NA").pvpType("Open PvP").build(),

                        // North America - Optional PvP
                        World.builder().name("Astera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Batabra").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Concorda").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Nefera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Seanera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Talera").location("NA").pvpType("Optional PvP").build()
                );

                worldRepository.saveAll(worlds);
                log.info("{} worlds criados", worlds.size());
            }
        };
    }
}

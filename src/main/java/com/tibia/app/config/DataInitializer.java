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
                        // Europe - Open PvP
                        World.builder().name("Antica").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Citra").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Peloria").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Thyria").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Vunira").location("EU").pvpType("Open PvP").build(),
                        World.builder().name("Xyla").location("EU").pvpType("Open PvP").build(),

                        // Europe - Optional PvP
                        World.builder().name("Bona").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Celesta").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Dia").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Harmonia").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Kalanta").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Karmeya").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Monza").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Nevia").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Refugia").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Secura").location("EU").pvpType("Optional PvP").build(),
                        World.builder().name("Sonira").location("EU").pvpType("Optional PvP").build(),

                        // Europe - Retro Open PvP
                        World.builder().name("Eclipta").location("EU").pvpType("Retro Open PvP").build(),
                        World.builder().name("Epoca").location("EU").pvpType("Retro Open PvP").build(),
                        World.builder().name("Escura").location("EU").pvpType("Retro Open PvP").build(),

                        // Europe - Retro Hardcore PvP
                        World.builder().name("Retalia").location("EU").pvpType("Retro Hardcore PvP").build(),

                        // Europe - Hardcore PvP
                        World.builder().name("Zuna").location("EU").pvpType("Hardcore PvP").build(),

                        // North America - Open PvP
                        World.builder().name("Aethera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Havera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Ignitera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Lobera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Quidera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Quintera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Solidera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Talera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Wintera").location("NA").pvpType("Open PvP").build(),
                        World.builder().name("Xymera").location("NA").pvpType("Open PvP").build(),

                        // North America - Optional PvP
                        World.builder().name("Astera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Blumera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Calmera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Gladera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Kalimera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Luminera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Menera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Nefera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Pacera").location("NA").pvpType("Optional PvP").build(),
                        World.builder().name("Yovera").location("NA").pvpType("Optional PvP").build(),

                        // North America - Retro Open PvP
                        World.builder().name("Firmera").location("NA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Mystera").location("NA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Tempestera").location("NA").pvpType("Retro Open PvP").build(),

                        // North America - Hardcore PvP
                        World.builder().name("Monstera").location("NA").pvpType("Retro Hardcore PvP").build(),
                        World.builder().name("Zunera").location("NA").pvpType("Hardcore PvP").build(),

                        // South America - Open PvP
                        World.builder().name("Cantabra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Dracobra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Ferobra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Honbra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Inabra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Jadebra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Ombra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Ourobra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Quelibra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Rasteibra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Serdebra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Tornabra").location("SA").pvpType("Open PvP").build(),
                        World.builder().name("Unebra").location("SA").pvpType("Open PvP").build(),

                        // South America - Optional PvP
                        World.builder().name("Belobra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Celebra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Collabra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Descubra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Etebra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Gentebra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Issobra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Kalibra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Luzibra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Ustebra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Venebra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Yonabra").location("SA").pvpType("Optional PvP").build(),
                        World.builder().name("Yubra").location("SA").pvpType("Optional PvP").build(),

                        // South America - Retro Open PvP
                        World.builder().name("Gladibra").location("SA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Lutabra").location("SA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Penumbra").location("SA").pvpType("Retro Open PvP").build(),
                        World.builder().name("Sombra").location("SA").pvpType("Retro Open PvP").build(),

                        // South America - Retro Hardcore PvP
                        World.builder().name("Terribra").location("SA").pvpType("Retro Hardcore PvP").build(),

                        // Oceania - Optional PvP
                        World.builder().name("Oceanis").location("OC").pvpType("Optional PvP").build(),
                        World.builder().name("Stralis").location("OC").pvpType("Optional PvP").build(),

                        // Oceania - Open PvP
                        World.builder().name("Victoris").location("OC").pvpType("Open PvP").build()
                        );

                worldRepository.saveAll(worlds);
                log.info("{} worlds criados", worlds.size());
            }
        };
    }
}

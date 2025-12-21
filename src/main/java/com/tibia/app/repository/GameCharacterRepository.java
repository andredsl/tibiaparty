package com.tibia.app.repository;

import com.tibia.app.domain.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    @Query("""
            SELECT c FROM GameCharacter c
            JOIN FETCH c.world w
            WHERE LOWER(c.name) = LOWER(:name)
            AND LOWER(w.name) = LOWER(:worldName)
            """)
    Optional<GameCharacter> findByNameAndWorldName(
            @Param("name") String name,
            @Param("worldName") String worldName);

    @Query("""
            SELECT c FROM GameCharacter c
            JOIN FETCH c.world
            WHERE c.user.id = :userId
            ORDER BY c.main DESC, c.level DESC
            """)
    List<GameCharacter> findByUserIdWithWorld(@Param("userId") Long userId);

    List<GameCharacter> findByUserId(Long userId);

    @Query("SELECT c FROM GameCharacter c WHERE c.user.id = :userId AND c.main = true")
    Optional<GameCharacter> findMainByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM GameCharacter c
            JOIN FETCH c.world w
            WHERE c.id = :id
            """)
    Optional<GameCharacter> findByIdWithWorld(@Param("id") Long id);

    boolean existsByNameIgnoreCaseAndWorldId(String name, Long worldId);

    @Query("""
            SELECT c FROM GameCharacter c
            JOIN FETCH c.world w
            JOIN FETCH c.user u
            WHERE LOWER(c.name) = LOWER(:name)
            """)
    Optional<GameCharacter> findByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(c) FROM GameCharacter c WHERE c.user.id = :userId")
    int countByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM GameCharacter c
            JOIN FETCH c.world w
            WHERE w.id = :worldId
            AND c.level BETWEEN :levelMin AND :levelMax
            ORDER BY c.level DESC
            """)
    List<GameCharacter> findByWorldAndLevelRange(
            @Param("worldId") Long worldId,
            @Param("levelMin") int levelMin,
            @Param("levelMax") int levelMax);
}

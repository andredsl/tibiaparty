package com.tibia.app.repository;

import com.tibia.app.domain.entity.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldRepository extends JpaRepository<World, Long> {

    Optional<World> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<World> findByActiveTrue();

    @Query("SELECT w FROM World w WHERE w.active = true ORDER BY w.location, w.name")
    List<World> findAllActiveOrderedByLocationAndName();

    @Query("SELECT w FROM World w WHERE w.location = :location AND w.active = true ORDER BY w.name")
    List<World> findByLocation(@Param("location") String location);

    @Query("SELECT DISTINCT w.location FROM World w WHERE w.active = true ORDER BY w.location")
    List<String> findDistinctLocations();
}

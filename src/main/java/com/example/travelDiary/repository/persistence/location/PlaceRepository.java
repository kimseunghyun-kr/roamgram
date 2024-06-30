package com.example.travelDiary.repository.persistence.location;

import com.example.travelDiary.domain.model.location.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {
    Optional<Place> findByGoogleMapsKeyId(String googleMapsKeyId);
}

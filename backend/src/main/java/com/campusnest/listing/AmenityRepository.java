package com.campusnest.listing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findAllByOrderBySortOrderAsc();
}

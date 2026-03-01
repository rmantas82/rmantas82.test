package com.rmantas82.test.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.rmantas82.test.entity.Vehicle;

/**
 * Repository interface for Vehicle entity.
 */
@Repository
public interface VehicleRepository {
    /**
     * Find all vehicles.
     *
     * @return a list of all vehicles
     */
    List<Vehicle> findAll();

    /**
     * Find a vehicle by its ID.
     *
     * @param id the ID of the vehicle to find
     * @return an Optional containing the vehicle if found, or empty if not found
     */
    Optional<Vehicle> findById(Long id);

    /**
     * Save a vehicle.
     *
     * @param vehicle the vehicle to save
     * @return the saved vehicle
     */
    Vehicle save(Vehicle vehicle);

    /**
     * Delete a vehicle by its ID.
     *
     * @param id the ID of the vehicle to delete
     */
    void deleteById(Long id);

}

package com.rmantas82.test.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rmantas82.test.entity.Vehicle;
import com.rmantas82.test.repository.VehicleRepository;

/**
 * Implementation of the VehicleService interface.
 * This class provides the implementation for all vehicle-related operations.
 */
@Service
@Transactional(readOnly = false)
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vehicle saveVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}

package com.rmantas82.test.service;

import java.util.List;
import java.util.Optional;

import com.rmantas82.test.entity.Brand;

/**
 * Service interface for Brand operations.
 */
public interface BrandService {

    /**
     * Get all brands.
     *
     * @return a list of all brands
     */
    List<Brand> getAllBrands();

    /**
     * Get a brand by its ID.
     *
     * @param id the ID of the brand to find
     * @return an Optional containing the brand if found, or empty if not found
     */
    Optional<Brand> getBrandById(Long id);

    /**
     * Save a brand.
     *
     * @param brand the brand to save
     * @return the saved brand
     */
    Brand saveBrand(Brand brand);

    /**
     * Delete a brand by its ID.
     *
     * @param id the ID of the brand to delete
     */
    void deleteBrand(Long id);

}

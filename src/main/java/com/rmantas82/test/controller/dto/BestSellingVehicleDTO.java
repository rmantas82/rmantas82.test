package com.rmantas82.test.controller.dto;

/**
 * DTO (Data Transfer Object) de respuesta para el endpoint que devuelve
 * los vehículos más vendidos.
 *
 * ¿Qué es un DTO?
 * - Un objeto diseñado para transportar datos entre capas o hacia el cliente.
 * - No representa una entidad persistente (no está mapeado a ninguna tabla).
 * - Define explícitamente el contrato de salida de la API.
 *
 * ¿Por qué usar un DTO en lugar de devolver directamente la entidad Vehicle?
 * - La respuesta incluye un dato calculado (salesCount) que no forma parte del modelo persistente.
 * - Evita exponer campos innecesarios o sensibles de la entidad.
 * - Desacopla el modelo de dominio de la representación externa.
 *
 * ¿Por qué usar un record?
 * - Es inmutable por diseño, lo que lo hace ideal para respuestas HTTP.
 * - Genera automáticamente constructor, getters, equals, hashCode y toString.
 * - Reduce boilerplate y expresa claramente que es un objeto de solo datos.
 *
 * @param vehicleId    identificador del vehículo
 * @param vehicleModel modelo del vehículo (por ejemplo, "Golf", "Clio")
 * @param salesCount   número total de ventas en el período consultado
 */
public record BestSellingVehicleDTO(
        Long vehicleId,
        String vehicleModel,
        long salesCount
) { }
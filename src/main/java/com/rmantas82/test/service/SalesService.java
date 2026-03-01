package com.rmantas82.test.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.rmantas82.test.controller.dto.BestSellingVehicleDTO;
import com.rmantas82.test.entity.Sales;

/**
 * Contrato de la capa de servicio para operaciones relacionadas con {@link Sales}.
 *
 * Responsabilidades principales:
 * - Exponer operaciones de consulta de ventas (por id, paginadas y filtradas por dominio).
 * - Proveer un cálculo de "mejores ventas" (top N) con filtros opcionales por fecha, devolviendo
 *   un DTO orientado a la salida del endpoint.
 *
 * Motivos para definir una interfaz:
 * - Aísla a los controladores del detalle de implementación (bajo acoplamiento).
 * - Facilita testing (p. ej. mock del servicio en tests de capa web).
 * - Permite evolucionar o sustituir la implementación sin afectar a los consumidores.
 */
public interface SalesService {

    /**
     * Devuelve todas las ventas sin paginación.
     *
     * Uso típico:
     * - Operaciones internas o cálculos donde se necesita el conjunto completo de datos.
     *
     * @return lista completa de ventas
     */
    List<Sales> getAllSales();

    /**
     * Devuelve una venta por su ID.
     *
     * @param id ID de la venta
     * @return {@link Optional} con la venta si existe; {@link Optional#empty()} si no
     */
    Optional<Sales> getSalesById(Long id);

    /**
     * Devuelve una página de ventas con un tamaño fijo de 10 elementos por página.
     *
     * Comportamiento esperado:
     * - {@code page} es 0-based.
     * - Si {@code page} es {@code null} o negativo, se asume la primera página (0).
     * - Si la página solicitada no existe, se devuelve una lista vacía.
     *
     * @param page número de página (0-based)
     * @return sublista con los elementos de esa página
     */
    List<Sales> getSalesPage(Integer page);

    /**
     * Devuelve todas las ventas asociadas a una marca.
     *
     * @param brandId ID de la marca
     * @return lista de ventas de esa marca (vacía si no hay coincidencias)
     */
    List<Sales> getSalesByBrandId(Long brandId);

    /**
     * Devuelve todas las ventas asociadas a un vehículo.
     *
     * @param vehicleId ID del vehículo
     * @return lista de ventas de ese vehículo (vacía si no hay coincidencias)
     */
    List<Sales> getSalesByVehicleId(Long vehicleId);

    /**
     * Devuelve los 5 vehículos con más ventas dentro del período indicado (si se informa).
     *
     * Consideraciones:
     * - {@code startDate} y {@code endDate} son opcionales y se interpretan como inclusivos.
     * - Si ambos son {@code null}, se considera todo el histórico.
     * - El resultado se devuelve ordenado de mayor a menor número de ventas, con un máximo de 5 elementos.
     *
     * @param startDate inicio del período (inclusive). {@code null} = sin límite inferior
     * @param endDate   fin del período (inclusive). {@code null} = sin límite superior
     * @return lista de hasta 5 {@link BestSellingVehicleDTO}
     */
    List<BestSellingVehicleDTO> getBestSellingVehicles(LocalDate startDate, LocalDate endDate);
}
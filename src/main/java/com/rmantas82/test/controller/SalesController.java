package com.rmantas82.test.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rmantas82.test.controller.dto.BestSellingVehicleDTO;
import com.rmantas82.test.entity.Sales;
import com.rmantas82.test.service.SalesService;

/**
 * Controlador REST para operaciones de consulta sobre ventas ({@link Sales}).
 *
 * Endpoints expuestos:
 * - GET /api/sales                         → listado con paginación mediante query param "page"
 * - GET /api/sales/{id}                    → detalle por id
 * - GET /api/sales/brands/{brandId}        → ventas por marca
 * - GET /api/sales/vehicles/{vehicleId}    → ventas por vehículo
 * - GET /api/sales/vehicles/bestSelling    → top 5 vehículos más vendidos (filtro de fechas opcional)
 *
 * Responsabilidad del controlador:
 * - Traducir la petición HTTP a llamadas a la capa de servicio:
 *   extraer parámetros, delegar y construir la respuesta HTTP.
 * - Evitar lógica de negocio: el cálculo/filtrado se resuelve en {@link SalesService}.
 *
 * Nota sobre el orden de rutas:
 * - La ruta estática "/vehicles/bestSelling" se declara antes que "/vehicles/{vehicleId}"
 *   para evitar que Spring intente tratar "bestSelling" como un path variable numérico.
 */
@RestController
@RequestMapping("/api/sales")
public class SalesController {

    /**
     * Servicio de ventas inyectado por constructor.
     *
     * Ventajas:
     * - Campo {@code final} (inmutabilidad).
     * - Dependencias explícitas.
     * - Tests más sencillos (se puede instanciar el controller con un mock del servicio).
     */
    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    /**
     * Devuelve una página de ventas (10 elementos por página).
     *
     * Ejemplos:
     * - GET /api/sales            → primera página
     * - GET /api/sales?page=0     → primera página
     * - GET /api/sales?page=2     → tercera página (índices 20..29)
     *
     * @param page número de página (0-based). Si no se informa, se asume la primera.
     * @return 200 OK con la lista de ventas de la página solicitada
     */
    @GetMapping
    public ResponseEntity<List<Sales>> getAllSales(
            @RequestParam(name = "page", required = false) Integer page) {
        return ResponseEntity.ok(salesService.getSalesPage(page));
    }

    /**
     * Devuelve una venta por su ID.
     *
     * @param id identificador de la venta
     * @return 200 OK con la venta si existe, o 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSalesById(@PathVariable Long id) {
        return salesService.getSalesById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Devuelve todas las ventas asociadas a una marca.
     *
     * @param brandId identificador de la marca
     * @return 200 OK con la lista de ventas (posiblemente vacía)
     */
    @GetMapping("/brands/{brandId}")
    public ResponseEntity<List<Sales>> getSalesByBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(salesService.getSalesByBrandId(brandId));
    }

    /**
     * Devuelve el top 5 de vehículos más vendidos, con filtro opcional por rango de fechas.
     *
     * Ejemplos:
     * - GET /api/sales/vehicles/bestSelling
     * - GET /api/sales/vehicles/bestSelling?startDate=2025-01-01
     * - GET /api/sales/vehicles/bestSelling?startDate=2025-01-01&endDate=2025-01-15
     *
     * @param startDate fecha de inicio (inclusive) en formato ISO yyyy-MM-dd. Opcional.
     * @param endDate   fecha de fin (inclusive) en formato ISO yyyy-MM-dd. Opcional.
     * @return 200 OK con hasta 5 elementos ordenados por número de ventas (descendente)
     */
    @GetMapping("/vehicles/bestSelling")
    public ResponseEntity<List<BestSellingVehicleDTO>> getBestSellingVehicles(
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(salesService.getBestSellingVehicles(startDate, endDate));
    }

    /**
     * Devuelve todas las ventas asociadas a un vehículo.
     *
     * @param vehicleId identificador del vehículo
     * @return 200 OK con la lista de ventas (posiblemente vacía)
     */
    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<List<Sales>> getSalesByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(salesService.getSalesByVehicleId(vehicleId));
    }
}
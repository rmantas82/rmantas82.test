package com.rmantas82.test.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rmantas82.test.controller.dto.BestSellingVehicleDTO;
import com.rmantas82.test.entity.Sales;
import com.rmantas82.test.entity.Vehicle;
import com.rmantas82.test.repository.SalesRepository;

/**
 * Implementación de {@link SalesService}.
 *
 * Responsabilidades:
 * - Consultas de ventas (todas, por id, paginadas).
 * - Consultas filtradas por dimensión de negocio (marca y vehículo).
 * - Cálculo de "mejores ventas" (top 5 vehículos) con filtro opcional por rango de fechas.
 *
 * Decisiones de diseño:
 * - La paginación y los filtros se aplican en memoria partiendo del conjunto completo de ventas
 *   recuperado desde el repositorio. Esto mantiene la lógica de agregación/ordenación dentro
 *   de la capa de servicio y evita acoplar el repositorio a casos de uso concretos.
 * - El cálculo del top 5 evita ordenar el conjunto completo: mantiene un top-K de tamaño fijo,
 *   eficiente cuando K es pequeño.
 *
 * Nota sobre transacciones:
 * - Este servicio realiza únicamente lecturas. En caso de introducir operaciones de escritura,
 *   se recomienda revisar y declarar transaccionalidad en la capa de servicio.
 */
@Service
public class SalesServiceImpl implements SalesService {

    /**
     * Tamaño fijo de página para paginación en memoria.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Repositorio de acceso a datos de ventas.
     *
     * Inyección por constructor para:
     * - Dependencias explícitas.
     * - Campos {@code final} (inmutabilidad).
     * - Tests más simples (inyección de mocks sin contexto Spring).
     */
    private final SalesRepository salesRepository;

    public SalesServiceImpl(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    /**
     * Devuelve todas las ventas sin paginar.
     *
     * Se utiliza tanto para exponer el listado completo como para algoritmos que requieren
     * el conjunto íntegro (por ejemplo, agregaciones/estadísticas).
     */
    @Override
    public List<Sales> getAllSales() {
        return salesRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Sales> getSalesById(Long id) {
        return salesRepository.findById(id);
    }

    /**
     * Devuelve una página de ventas (10 elementos) según el número de página solicitado.
     *
     * Funcionamiento:
     * - {@code page} es 0-based.
     * - {@code null} o negativo se interpreta como la primera página (0).
     * - Se calcula una ventana sobre la lista completa mediante {@link List#subList(int, int)}.
     * - Si la página solicitada queda fuera de rango, se devuelve una lista vacía.
     *
     * Nota:
     * - {@code subList} devuelve una vista sobre la lista original; no copia los datos.
     *
     * @param page número de página (0-based)
     * @return sublista con máximo {@link #PAGE_SIZE} elementos de la página solicitada
     */
    @Override
    public List<Sales> getSalesPage(Integer page) {

        int p = (page == null || page < 0) ? 0 : page;

        List<Sales> all = salesRepository.findAll();

        int from = p * PAGE_SIZE;
        if (from >= all.size()) {
            return List.of();
        }

        int to = Math.min(from + PAGE_SIZE, all.size());
        return all.subList(from, to);
    }

    /**
     * Devuelve todas las ventas asociadas a una marca específica.
     *
     * Implementación:
     * - Recorre todas las ventas y filtra por {@code brandId}.
     *
     * Detalle:
     * - Se usa {@link Objects#equals(Object, Object)} para comparar IDs {@link Long} por valor
     *   y manejar {@code null} de forma segura.
     *
     * @param brandId ID de la marca
     * @return lista de ventas de esa marca (puede ser vacía)
     */
    @Override
    public List<Sales> getSalesByBrandId(Long brandId) {

        List<Sales> all = salesRepository.findAll();
        List<Sales> result = new ArrayList<>();

        for (Sales sale : all) {
            if (sale.getBrand() != null && Objects.equals(sale.getBrand().getId(), brandId)) {
                result.add(sale);
            }
        }

        return result;
    }

    /**
     * Devuelve todas las ventas asociadas a un vehículo específico.
     *
     * Implementación:
     * - Recorre todas las ventas y filtra por {@code vehicleId}.
     *
     * @param vehicleId ID del vehículo
     * @return lista de ventas de ese vehículo (puede ser vacía)
     */
    @Override
    public List<Sales> getSalesByVehicleId(Long vehicleId) {

        List<Sales> all = salesRepository.findAll();
        List<Sales> result = new ArrayList<>();

        for (Sales sale : all) {
            if (sale.getVehicle() != null && Objects.equals(sale.getVehicle().getId(), vehicleId)) {
                result.add(sale);
            }
        }

        return result;
    }

    /**
     * Devuelve los 5 vehículos con más ventas en el período indicado.
     *
     * Enfoque:
     * 1) Recorrer las ventas una vez y contar ventas por vehículo (con filtro de fechas opcional).
     * 2) Mantener un top-K (K=5) en un array de tamaño fijo sin ordenar el conjunto completo.
     * 3) Construir los DTOs respetando el orden ya calculado.
     *
     * Complejidad:
     * - Conteo: O(n) siendo n el número de ventas.
     * - Top 5: O(m * K) siendo m el número de vehículos distintos y K=5 (constante).
     *
     * @param startDate inicio del período (inclusive). {@code null} = sin límite inferior
     * @param endDate   fin del período (inclusive). {@code null} = sin límite superior
     * @return lista de hasta 5 {@link BestSellingVehicleDTO} ordenados de mayor a menor ventas
     */
    @Override
    public List<BestSellingVehicleDTO> getBestSellingVehicles(LocalDate startDate, LocalDate endDate) {

        List<Sales> all = salesRepository.findAll();

        // ── FASE 1: Conteo por vehículo con filtro de fechas ─────────────────────────────
        Map<Long, Long> counter = new HashMap<>();

        for (Sales sale : all) {

            LocalDate date = sale.getSaleDate();

            if (startDate != null && date.isBefore(startDate)) {
                continue;
            }
            if (endDate != null && date.isAfter(endDate)) {
                continue;
            }

            // Se asume vehicle presente en la venta; si fuese opcional, habría que validar null.
            Long vehicleId = sale.getVehicle().getId();
            counter.put(vehicleId, counter.getOrDefault(vehicleId, 0L) + 1);
        }

        // ── FASE 2: Top 5 sin ordenar el conjunto completo ──────────────────────────────
        TopEntry[] top = new TopEntry[5];
        int size = 0;

        for (Map.Entry<Long, Long> entry : counter.entrySet()) {

            TopEntry candidate = new TopEntry(entry.getKey(), entry.getValue());

            if (size < 5) {
                size = insertOrdered(top, size, candidate);
            } else if (candidate.count > top[size - 1].count) {
                size = insertOrdered(top, size, candidate);
            }
        }

        // ── FASE 3: Mapa auxiliar vehicleId → Vehicle ───────────────────────────────────
        // Evita consultas adicionales: deduplicamos vehículos a partir del dataset ya cargado.
        Map<Long, Vehicle> vehicles = new HashMap<>();
        for (Sales sale : all) {
            Vehicle v = sale.getVehicle();
            if (v != null) {
                vehicles.putIfAbsent(v.getId(), v);
            }
        }

        List<BestSellingVehicleDTO> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TopEntry t = top[i];
            Vehicle v = vehicles.get(t.vehicleId);
            // En un caso extremo (datos inconsistentes), v podría ser null.
            result.add(new BestSellingVehicleDTO(t.vehicleId, v != null ? v.getModel() : null, t.count));
        }

        return result;
    }

    /**
     * Inserta un candidato en el array {@code top} manteniendo orden descendente por {@code count}.
     *
     * Detalle:
     * - El array tiene tamaño máximo 5, por lo que cada inserción tiene coste O(1) (máximo 5 desplazamientos).
     * - Si el array está lleno, el último elemento se descarta al desplazar.
     *
     * @param top       array de tamaño 5 con los mejores candidatos actuales
     * @param size      número de elementos válidos actualmente en {@code top} (0..5)
     * @param candidate candidato a insertar
     * @return nuevo tamaño (máximo 5)
     */
    private static int insertOrdered(TopEntry[] top, int size, TopEntry candidate) {

        int pos = 0;
        while (pos < size && top[pos].count >= candidate.count) {
            pos++;
        }

        // Si no entra en el rango del top 5, se ignora
        if (pos >= 5) {
            return size;
        }

        int limit = Math.min(size, 4);
        for (int i = limit; i > pos; i--) {
            top[i] = top[i - 1];
        }

        top[pos] = candidate;

        return Math.min(size + 1, 5);
    }

    /**
     * Estructura auxiliar (vehicleId, count) usada durante el cálculo del top 5.
     *
     * Es un detalle interno de implementación:
     * - Reduce ruido en el algoritmo.
     * - Evita el uso de estructuras genéricas menos expresivas.
     */
    private static class TopEntry {
        Long vehicleId;
        long count;

        TopEntry(Long vehicleId, long count) {
            this.vehicleId = vehicleId;
            this.count = count;
        }
    }
}
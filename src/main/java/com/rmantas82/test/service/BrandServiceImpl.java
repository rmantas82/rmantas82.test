package com.rmantas82.test.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rmantas82.test.config.BrandCache;
import com.rmantas82.test.entity.Brand;
import com.rmantas82.test.repository.BrandRepository;

/**
 * Implementación de {@link BrandService} con soporte de caché concurrente y expiración (TTL).
 *
 * Objetivo:
 * - Reducir accesos repetidos a base de datos en operaciones de lectura.
 * - Mantener coherencia entre lecturas y escrituras mediante invalidación de caché.
 *
 * Estrategia:
 * - Las lecturas (GET) se resuelven a través de {@link BrandCache}. Si existe un valor vigente,
 *   se devuelve sin consultar BBDD; si no, se ejecuta el proveedor de datos y se cachea el resultado.
 * - Las escrituras (POST/PUT/DELETE) invalidan la caché afectada para evitar servir datos obsoletos.
 *
 * Nota:
 * - La invalidación en escritura evita que un usuario vea información antigua hasta que expire el TTL.
 * - La caché se mantiene encapsulada en {@link BrandCache} para centralizar la política de expiración
 *   y la gestión de concurrencia.
 */
@Service
@Transactional(readOnly = false)
public class BrandServiceImpl implements BrandService {

    /**
     * Repositorio de acceso a datos para {@link Brand}.
     *
     * Se utiliza inyección por constructor para:
     * - Declarar dependencias de forma explícita.
     * - Facilitar testing (inyección de mocks sin necesidad del contenedor).
     * - Permitir inmutabilidad (campo {@code final}).
     */
    private final BrandRepository brandRepository;

    /**
     * Caché concurrente con TTL para operaciones de lectura de {@link Brand}.
     *
     * Centraliza:
     * - Recuperación cacheada (si el dato está vigente).
     * - Carga perezosa desde BBDD cuando la caché está vacía o expirada.
     * - Invalidación tras operaciones de escritura.
     */
    private final BrandCache brandCache;

    public BrandServiceImpl(BrandRepository brandRepository, BrandCache brandCache) {
        this.brandRepository = brandRepository;
        this.brandCache = brandCache;
    }

    /**
     * Devuelve todas las marcas usando caché si los datos están vigentes.
     *
     * Funcionamiento:
     * - Si la lista está cacheada y no ha expirado, se devuelve directamente.
     * - En caso contrario, se consulta BBDD y se almacena el resultado en caché.
     *
     * Se pasa {@code brandRepository::findAll} como proveedor (evaluación perezosa): solo se ejecuta
     * si {@link BrandCache} determina que hay que ir a base de datos.
     */
    @Override
    public List<Brand> getAllBrands() {
        return brandCache.getAllBrands(brandRepository::findAll);
    }

    /**
     * Devuelve una marca por ID usando caché si los datos están vigentes.
     *
     * Funcionamiento:
     * - Si existe una entrada vigente para el ID, se devuelve sin tocar BBDD.
     * - Si no existe o ha expirado, se consulta BBDD y se cachea el resultado.
     *
     * La lambda {@code () -> brandRepository.findById(id)} captura el parámetro {@code id} y se
     * evalúa únicamente cuando {@link BrandCache} necesita consultar la base de datos.
     */
    @Override
    public Optional<Brand> getBrandById(Long id) {
        return brandCache.getBrandById(id, () -> brandRepository.findById(id));
    }

    /**
     * Guarda (crea o actualiza) una marca y después invalida la caché relacionada.
     *
     * Política de invalidación:
     * - Se invalida el ID afectado y la lista global para que el siguiente GET refleje el estado
     *   actual de la base de datos.
     *
     * Motivo de invalidar después de persistir:
     * - Si la operación de guardado falla, no se vacía la caché innecesariamente.
     * - Tras un guardado exitoso, se garantiza que futuras lecturas no sirvan datos obsoletos.
     */
    @Override
    public Brand saveBrand(Brand brand) {
        Brand saved = brandRepository.save(brand);

        // Tras persistir, invalidamos para forzar recarga en la siguiente lectura
        if (saved.getId() != null) {
            brandCache.invalidateId(saved.getId()); // invalida el ID afectado y la lista
        } else {
            // Caso defensivo: ante ausencia de ID, invalidamos toda la caché
            brandCache.invalidateAll();
        }

        return saved;
    }

    /**
     * Elimina una marca por ID y después invalida su entrada de caché (y la lista).
     *
     * Sin invalidación, la marca podría seguir apareciendo en lecturas cacheadas hasta que
     * expire el TTL.
     */
    @Override
    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);

        // Tras el borrado, invalidamos para evitar servir datos antiguos
        brandCache.invalidateId(id);
    }
}
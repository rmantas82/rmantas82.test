package com.rmantas82.test.config;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rmantas82.test.entity.Brand;

/**
 * Caché concurrente con TTL (Time-To-Live) para {@link Brand}.
 *
 * Objetivo:
 * - Reducir accesos repetidos a base de datos en operaciones de lectura.
 * - Mantener un comportamiento predecible bajo concurrencia (múltiples peticiones simultáneas).
 *
 * Enfoque:
 * - {@link ConcurrentHashMap} como estructura thread-safe.
 * - {@code CacheEntry<T>} encapsula valor + instante de expiración.
 * - {@link ConcurrentHashMap#compute(Object, java.util.function.BiFunction)} se usa para refrescar
 *   entradas de forma atómica por clave, evitando condiciones de carrera.
 *
 * Estructura:
 * - Un mapa para el listado completo.
 * - Un mapa para accesos por ID (incluyendo cacheo de "no encontrado" con {@link Optional#empty()}).
 *
 * Nota:
 * - Esta clase encapsula una caché simple y autocontenida. En entornos más complejos se podría
 *   externalizar a una solución dedicada, pero aquí se prioriza claridad y control del comportamiento
 *   (TTL, invalidación y concurrencia).
 */
@Component
public class BrandCache {

    /**
     * Tiempo de vida de cada entrada en caché (milisegundos).
     *
     * Se puede configurar vía propiedad {@code brand.cache.ttl.ms}. Si no se informa, se usa
     * un valor por defecto razonable.
     */
    @Value("${brand.cache.ttl.ms:30000}")
    private long ttlMs;

    /**
     * Caché del listado completo de marcas.
     *
     * Se usa una única clave fija para representar "todas las marcas".
     */
    private final ConcurrentHashMap<String, CacheEntry<List<Brand>>> listCache = new ConcurrentHashMap<>();

    /**
     * Caché por ID.
     *
     * Se cachea {@link Optional} para evitar repetir consultas cuando un ID no existe.
     */
    private final ConcurrentHashMap<Long, CacheEntry<Optional<Brand>>> byIdCache = new ConcurrentHashMap<>();

    /**
     * Obtiene todas las marcas desde caché si existe una entrada vigente; en caso contrario,
     * ejecuta el {@code loader}, cachea el resultado y lo devuelve.
     *
     * @param loader proveedor de datos (normalmente una consulta a BBDD) que solo se ejecuta
     *               cuando la entrada está ausente o expirada
     * @return lista de marcas
     */
    public List<Brand> getAllBrands(Supplier<List<Brand>> loader) {
        final String key = "ALL";

        // Lectura rápida: si hay entrada válida, evitamos cualquier cómputo adicional.
        CacheEntry<List<Brand>> entry = listCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }

        // Refresco atómico por clave: evita que varios hilos disparen el loader a la vez.
        CacheEntry<List<Brand>> updated = listCache.compute(key, (k, current) -> {
            if (current != null && !current.isExpired()) {
                return current;
            }
            List<Brand> loaded = loader.get();
            return CacheEntry.of(loaded, ttlMs);
        });

        return updated.value;
    }

    /**
     * Obtiene una marca por ID desde caché si existe una entrada vigente; en caso contrario,
     * ejecuta el {@code loader}, cachea el resultado y lo devuelve.
     *
     * @param id     id de la marca
     * @param loader proveedor de datos (normalmente una consulta a BBDD)
     * @return {@link Optional} con la marca o vacío si no existe
     */
    public Optional<Brand> getBrandById(Long id, Supplier<Optional<Brand>> loader) {

        CacheEntry<Optional<Brand>> entry = byIdCache.get(id);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }

        CacheEntry<Optional<Brand>> updated = byIdCache.compute(id, (k, current) -> {
            if (current != null && !current.isExpired()) {
                return current;
            }
            Optional<Brand> loaded = loader.get();
            return CacheEntry.of(loaded, ttlMs);
        });

        return updated.value;
    }

    /**
     * Invalida toda la caché de marcas (listado e IDs).
     *
     * Uso típico:
     * - Tras operaciones de escritura (creación, actualización, borrado) para forzar que la siguiente
     *   lectura refleje el estado actual de base de datos.
     */
    public void invalidateAll() {
        listCache.clear();
        byIdCache.clear();
    }

    /**
     * Invalida la caché asociada a un ID concreto y el listado completo.
     *
     * @param id id de la marca a invalidar
     */
    public void invalidateId(Long id) {
        byIdCache.remove(id);
        // Cualquier cambio individual afecta a la coherencia del listado.
        listCache.clear();
    }

    /**
     * Entrada de caché con expiración.
     *
     * @param <T> tipo del valor cacheado
     */
    private static final class CacheEntry<T> {

        private final T value;
        private final long expiresAtMs;

        private CacheEntry(T value, long expiresAtMs) {
            this.value = value;
            this.expiresAtMs = expiresAtMs;
        }

        /**
         * Crea una entrada que expira {@code ttlMs} milisegundos desde "ahora".
         */
        static <T> CacheEntry<T> of(T value, long ttlMs) {
            return new CacheEntry<>(value, System.currentTimeMillis() + ttlMs);
        }

        /**
         * @return {@code true} si la entrada ha expirado y debe refrescarse
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }
}
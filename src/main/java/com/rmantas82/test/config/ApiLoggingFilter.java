package com.rmantas82.test.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro HTTP que registra en fichero información de cada petición y su respuesta.
 *
 * Datos registrados por línea:
 * - Fecha/hora
 * - Método HTTP (GET, POST, PUT, DELETE, ...)
 * - URL solicitada (incluyendo query params)
 * - Código de estado de la respuesta
 * - Tiempo de procesamiento (ms)
 *
 * Elección técnica:
 * - Se implementa como {@link OncePerRequestFilter} para asegurar una única ejecución por petición,
 *   incluso ante re-despachos internos (forward/include).
 * - Al formar parte de la cadena de filtros Servlet, registra también peticiones que puedan ser
 *   rechazadas por filtros previos (por ejemplo, seguridad) antes de alcanzar el controlador.
 *
 * Concurrencia:
 * - La escritura a fichero se protege con un {@link ReentrantLock} para evitar que múltiples hilos
 *   escriban simultáneamente y se corrompa el contenido.
 */
@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    /**
     * Ruta del fichero donde se escriben los logs (relativa al directorio de trabajo).
     */
    private static final Path LOG_FILE = Path.of("logs", "api-requests.log");

    /**
     * Lock para serializar escrituras concurrentes en el fichero.
     *
     * Se configura como "fair" (FIFO) para repartir el turno de escritura entre hilos.
     */
    private static final ReentrantLock FILE_LOCK = new ReentrantLock(true);

    /**
     * Método central del filtro: se ejecuta una vez por cada petición HTTP.
     *
     * Flujo:
     * 1) Captura timestamp de inicio.
     * 2) Delega el procesamiento al resto de la cadena de filtros.
     * 3) En {@code finally} (siempre), calcula duración y escribe la línea de log.
     *
     * El {@code finally} garantiza que se registran también respuestas con error.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String fullUrl = (query == null || query.isBlank()) ? uri : uri + "?" + query;

            String logLine = String.format(
                    "%s | %s %s | %d | %dms%n",
                    LocalDateTime.now(),
                    request.getMethod(),
                    fullUrl,
                    status,
                    durationMs
            );

            FILE_LOCK.lock();
            try {
                Files.createDirectories(LOG_FILE.getParent());
                Files.writeString(
                        LOG_FILE,
                        logLine,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            } finally {
                FILE_LOCK.unlock();
            }
        }
    }
}
package com.rmantas82.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para la API.
 *
 * Objetivo:
 * - Permitir acceso público a operaciones de lectura (GET).
 * - Proteger operaciones de escritura (POST/PUT/DELETE) mediante autenticación y rol.
 *
 * Decisiones de diseño:
 * - Autenticación HTTP Basic: simple, estándar y fácil de probar con clientes HTTP.
 * - CSRF deshabilitado: esta API expone endpoints REST (sin formularios HTML ni sesión de navegador).
 * - Usuarios en memoria: suficiente para un entorno de demostración/desarrollo sin depender
 *   de un almacén de usuarios externo.
 * - Password hashing con BCrypt: práctica recomendada para almacenar contraseñas de forma segura.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Define la cadena de filtros de seguridad y las reglas de autorización.
     *
     * Reglas:
     * - GET /**      → público
     * - POST /**     → requiere rol ADMIN
     * - PUT /**      → requiere rol ADMIN
     * - DELETE /**   → requiere rol ADMIN
     * - Resto        → requiere autenticación (fallback defensivo)
     *
     * Nota:
     * - {@link SecurityFilterChain} es el enfoque recomendado en Spring Security moderno
     *   frente a {@code WebSecurityConfigurerAdapter} (deprecado).
     *
     * @param http configuración de seguridad provista por Spring Security
     * @return cadena de filtros configurada
     * @throws Exception si la configuración falla
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                // En APIs REST sin sesión ni formularios, CSRF suele deshabilitarse.
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // Autenticación HTTP Basic con configuración por defecto.
                .httpBasic(Customizer.withDefaults())

                // Deshabilita el login por formulario para evitar redirecciones HTML en una API.
                .formLogin(form -> form.disable())

                .build();
    }

    /**
     * Servicio de usuarios en memoria para autenticar operaciones protegidas.
     *
     * En un escenario productivo, este componente suele alimentarse de BBDD/LDAP/OIDC,
     * pero para una API autocontenida es suficiente definir credenciales en memoria.
     *
     * @param encoder codificador de contraseñas
     * @return {@link UserDetailsService} con usuarios definidos en memoria
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {

        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin"))
                .roles("ADMIN") // Spring lo transforma internamente a ROLE_ADMIN
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Codificador de contraseñas utilizado por Spring Security.
     *
     * BCrypt es una opción robusta:
     * - Incluye salt por defecto.
     * - Es deliberadamente costoso computacionalmente, mitigando fuerza bruta.
     *
     * @return encoder BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
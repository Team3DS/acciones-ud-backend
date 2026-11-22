package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.FiltroJwt;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.FiltroTokenInterno;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ManejadorAccesoDenegado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ManejadorNoAutorizado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.TokenInternoProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.VerificadorJwt;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, TokenInternoProperties.class})
public class SecurityConfig {

    private static final String AUTORIDAD_SERVICIO_INTERNO =
            "ROLE_" + UsuarioAutenticado.ROL_SERVICIO_INTERNO;

    private static final String[] RUTAS_PUBLICAS = {
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    private final VerificadorJwt verificador;
    private final TokenInternoProperties tokenInterno;
    private final EscritorProblemas problemas;
    private final ManejadorNoAutorizado noAutorizado;
    private final ManejadorAccesoDenegado accesoDenegado;

    public SecurityConfig(VerificadorJwt verificador,
                          TokenInternoProperties tokenInterno,
                          EscritorProblemas problemas,
                          ManejadorNoAutorizado noAutorizado,
                          ManejadorAccesoDenegado accesoDenegado) {
        this.verificador = verificador;
        this.tokenInterno = tokenInterno;
        this.problemas = problemas;
        this.noAutorizado = noAutorizado;
        this.accesoDenegado = accesoDenegado;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/plans").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/stripe").permitAll()
                        .requestMatchers("/internal/**").hasAuthority(AUTORIDAD_SERVICIO_INTERNO)
                        .anyRequest().authenticated())
                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint(noAutorizado)
                        .accessDeniedHandler(accesoDenegado))
                .addFilterBefore(new FiltroTokenInterno(tokenInterno, problemas),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new FiltroJwt(verificador),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

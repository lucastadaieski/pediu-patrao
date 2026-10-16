package com.umc.pediupatrao.config;

import com.umc.pediupatrao.security.AcessoNegadoHandler;
import com.umc.pediupatrao.security.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String GERENTE = "GERENTE";
    private static final String ATENDENTE = "ATENDENTE";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiAuthenticationEntryPoint apiEntryPoint,
                                                   AcessoNegadoHandler acessoNegadoHandler) throws Exception {

        RequestMatcher rotasApi = request ->
                request.getRequestURI().startsWith(request.getContextPath() + "/api/");

        RequestMatcher autenticacaoBasic = request -> {
            String header = request.getHeader("Authorization");
            return header != null && header.regionMatches(true, 0, "Basic ", 0, 6);
        };

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(autenticacaoBasic))
                .authorizeHttpRequests(auth -> auth
                        // Público
                        .requestMatchers("/login", "/error", "/css/**", "/dist/**", "/plugins/**",
                                "/js/**", "/images/**").permitAll()

                        // Usuários e perfis: somente ADMIN (impede atribuição indevida de privilégios)
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole(ADMIN)

                        // Auditoria: somente consulta, ADMIN e GERENTE
                        .requestMatchers(HttpMethod.GET, "/auditoria/**", "/api/auditoria/**").hasAnyRole(ADMIN, GERENTE)
                        .requestMatchers("/auditoria/**", "/api/auditoria/**").denyAll()

                        // Pedidos: cancelar e dar desconto é exclusivo do GERENTE
                        .requestMatchers(HttpMethod.POST,
                                "/api/pedidos/*/cancelar", "/api/pedidos/*/desconto").hasRole(GERENTE)
                        // Pedidos: registrar e atualizar andamento
                        .requestMatchers(HttpMethod.GET, "/pedidos/novo").hasAnyRole(GERENTE, ATENDENTE)
                        // Pedidos: consulta (ADMIN também consulta)
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/pedidos/*", "/api/pedidos", "/api/pedidos/*",
                                "/api/pedidos/*/acoes")
                                .hasAnyRole(ADMIN, GERENTE, ATENDENTE)
                        .requestMatchers("/pedidos/**", "/api/pedidos/**").hasAnyRole(GERENTE, ATENDENTE)

                        // Clientes: GERENTE consulta; ATENDENTE cadastra, altera e exclui
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes", "/api/clientes/*")
                                .hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers("/clientes/**", "/api/clientes/**").hasRole(ATENDENTE)

                        // Produtos: todos consultam (cardápio); ADMIN e GERENTE mantêm
                        .requestMatchers(HttpMethod.GET, "/produtos", "/api/produtos", "/api/produtos/*").authenticated()
                        .requestMatchers("/produtos/**", "/api/produtos/**").hasAnyRole(ADMIN, GERENTE)

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(apiEntryPoint))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(apiEntryPoint, rotasApi)
                        .accessDeniedHandler(acessoNegadoHandler)
                );

        return http.build();
    }
}

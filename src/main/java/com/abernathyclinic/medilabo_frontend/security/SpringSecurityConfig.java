package com.abernathyclinic.medilabo_frontend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

import jakarta.servlet.Filter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    private final JwtService jwtService = new JwtService();

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // static resources
                        .requestMatchers("/css/**", "/js/**").permitAll()

                        // UI pages require login
                        .requestMatchers("/ui/**").authenticated()

                        // API endpoints
                        .requestMatchers("/api/patient/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/patient/**").authenticated()
                        .requestMatchers("/api/history/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/history/**").authenticated()
                        .requestMatchers("/api/diabetes/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/diabetes/**").authenticated()

                        // everything else
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtCookieAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public Filter jwtCookieAuthFilter() {
        return (req, res, chain) -> {
            HttpServletRequest request = (HttpServletRequest) req;
            String token = readJwtFromCookie(request);
            if (token != null) {
                try {
                    var jws = jwtService.parse(token);
                    String username = jws.getBody().getSubject();
                    List<String> roles = jws.getBody().get("roles", List.class);
                    var authorities = roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .toList();
                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (JwtException e) {
                    SecurityContextHolder.clearContext();
                }
            }
            chain.doFilter(req, res);
        };
    }

    private String readJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if ("AUTH_TOKEN".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
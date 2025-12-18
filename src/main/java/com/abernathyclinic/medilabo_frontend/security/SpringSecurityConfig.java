package com.abernathyclinic.medilabo_frontend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//@Configuration
//public class SpringSecurityConfig {
//    private final JwtCookieAuthFilter jwtCookieAuthFilter;
//    @Autowired
//    public SpringSecurityConfig(JwtCookieAuthFilter jwtCookieAuthFilter) {
//        this.jwtCookieAuthFilter = jwtCookieAuthFilter;
//    }
//    @Bean
//    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/css/**", "/js/**").permitAll()
//                        .requestMatchers("/ui/**").authenticated()
//                        .requestMatchers("/api/patient/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/api/patient/**").authenticated()
//                        .requestMatchers("/api/history/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/api/history/**").authenticated()
//                        .requestMatchers("/api/diabetes/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/api/diabetes/**").authenticated()
//                        .anyRequest().permitAll()
//                )
//                .addFilterBefore(jwtCookieAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//}
@Configuration
public class SpringSecurityConfig {

    private final JwtCookieAuthFilter jwtCookieAuthFilter;

    @Autowired
    public SpringSecurityConfig(JwtCookieAuthFilter jwtCookieAuthFilter) {
        this.jwtCookieAuthFilter = jwtCookieAuthFilter;
    }

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/ui/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtCookieAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
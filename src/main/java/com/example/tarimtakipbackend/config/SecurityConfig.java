package com.example.tarimtakipbackend.config;

import com.example.tarimtakipbackend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Metot seviyesi @PreAuthorize anotasyonlarını etkinleştirir
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                
                // --- YENİ EKLENEN/DÜZENLENEN KISIM BAŞLANGICI ---
                // Çalışanların da erişebileceği GÖREV ile ilgili spesifik admin yolları:
                .requestMatchers("/admin/gorevler", "/admin/gorevler/duzenle/**", "/admin/gorevler/kaydet").hasAnyRole("ADMIN", "CALISAN")
                
                // Sadece Admin'in erişebileceği GÖREV ile ilgili spesifik admin yolları:
                .requestMatchers("/admin/gorevler/ekle", "/admin/gorevler/sil/**").hasRole("ADMIN")
                // --- YENİ EKLENEN/DÜZENLENEN KISIM SONU ---
                
                // Diğer tüm /admin/** yolları sadece Admin'e:
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Eğer /calisan diye ayrı bir prefix'iniz varsa (şu an için yok gibi duruyor):
                // .requestMatchers("/calisan/**").hasAnyRole("CALISAN", "ADMIN") 
                
                .anyRequest().authenticated() // Diğer tüm istekler kimlik doğrulama gerektirir
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Test için CSRF kapalı

        return http.build();
    }
}
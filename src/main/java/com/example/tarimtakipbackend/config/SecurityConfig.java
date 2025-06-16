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
@EnableMethodSecurity
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

                // GÖREV YÖNETİMİ YETKİLERİ
                .requestMatchers("/admin/gorevler", "/admin/gorevler/duzenle/**", "/admin/gorevler/kaydet").hasAnyRole("ADMIN", "CALISAN")
                .requestMatchers("/admin/gorevler/ekle", "/admin/gorevler/sil/**").hasRole("ADMIN")

                // SENSÖR OKUMA YÖNETİMİ YETKİLERİ (YENİ EKLENENLER)
                .requestMatchers("/admin/sensorler").hasAnyRole("ADMIN", "CALISAN") // Sensörleri listeleme
                .requestMatchers("/admin/sensorler/okuma/ekle").hasAnyRole("ADMIN", "CALISAN") // Okuma ekleme formu
                .requestMatchers("/admin/sensorler/okuma/kaydet").hasAnyRole("ADMIN", "CALISAN") // Okuma kaydetme işlemi
                .requestMatchers("/admin/sensorler/{sensorId}/okumalar").hasAnyRole("ADMIN", "CALISAN") // Belirli sensör okumalarını listeleme

                // SENSÖR YÖNETİMİ (Tanımlama, Düzenleme, Silme - Sadece Admin)
                // Diğer /admin/sensorler/** altındaki yollar (ekle, duzenle, sil) Admin'e özel kalacak.
                // Bu, genel /admin/** kuralından önce gelmeli ki daha spesifik olan bu kurallar ezilmesin.
                .requestMatchers("/admin/sensorler/ekle", "/admin/sensorler/duzenle/**", "/admin/sensorler/sil/**").hasRole("ADMIN")

                // Diğer tüm /admin/** yolları sadece Admin'e:
                // Bu kural, yukarıdaki daha spesifik /admin/sensorler/... kurallarından SONRA gelmeli.
                .requestMatchers("/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated()
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
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
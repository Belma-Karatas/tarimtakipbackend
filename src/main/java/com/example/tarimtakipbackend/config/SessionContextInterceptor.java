package com.example.tarimtakipbackend.config; // veya com.example.tarimtakipbackend.interceptor;

import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.sql.DataSource; // javax.sql.DataSource importu
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class SessionContextInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SessionContextInterceptor.class);

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Autowired
    private DataSource dataSource; // Veritabanı bağlantısı için DataSource

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            String username = authentication.getName();
            Kullanici kullanici = kullaniciRepository.findByKullaniciAdi(username).orElse(null);

            if (kullanici != null && kullanici.getKullaniciID() != null) {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("EXEC sp_set_session_context @key = N'current_user_id', @value = ?")) {
                    
                    stmt.setInt(1, kullanici.getKullaniciID());
                    stmt.execute();
                    // logger.debug("SESSION_CONTEXT 'current_user_id' set to: {} for user: {}", kullanici.getKullaniciID(), username);
                } catch (SQLException e) {
                    logger.error("SESSION_CONTEXT 'current_user_id' set edilirken SQL hatası oluştu user: {}: ", username, e);
                    // Bu durumda isteği kesmek yerine devam etmesine izin verilebilir,
                    // ancak RLS düzgün çalışmayacaktır.
                    // Üretim ortamında bu ciddi bir hata olarak ele alınmalıdır.
                }
            } else {
                // logger.warn("Kullanıcı bulundu ama ID'si null veya kullanıcı bulunamadı: {}", username);
                 // Eğer kullanıcı null ise veya ID'si null ise, context'i temizleyebiliriz veya null set edebiliriz.
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("EXEC sp_set_session_context @key = N'current_user_id', @value = NULL")) {
                    stmt.execute();
                    // logger.debug("SESSION_CONTEXT 'current_user_id' cleared (set to NULL).");
                } catch (SQLException e) {
                    logger.error("SESSION_CONTEXT 'current_user_id' temizlenirken SQL hatası oluştu: ", e);
                }
            }
        } else {
             // Kimliği doğrulanmamış kullanıcılar için context'i temizle
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("EXEC sp_set_session_context @key = N'current_user_id', @value = NULL")) {
                stmt.execute();
                // logger.debug("SESSION_CONTEXT 'current_user_id' cleared for unauthenticated/anonymous user.");
            } catch (SQLException e) {
                logger.error("SESSION_CONTEXT 'current_user_id' temizlenirken SQL hatası oluştu: ", e);
            }
        }
        return true; // İsteğin devam etmesine izin ver
    }

    // postHandle ve afterCompletion metotlarını şimdilik boş bırakabiliriz.
}
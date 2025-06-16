package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
// Eğer birden fazla rolü yönetmek isterseniz:
// import java.util.stream.Collectors;
// import java.util.Set;

@Service // Bu sınıfın bir Spring servis bileşeni olduğunu belirtir
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired // KullaniciRepository'yi otomatik olarak enjekte et
    private KullaniciRepository kullaniciRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // KullaniciRepository üzerinden kullanıcı adıyla kullanıcıyı veritabanından bul
        Kullanici kullanici = kullaniciRepository.findByKullaniciAdi(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

        // Kullanıcının aktif olup olmadığını kontrol et (isteğe bağlı ama iyi bir pratik)
        if (kullanici.getAktifMi() == null || !kullanici.getAktifMi()) {
            throw new UsernameNotFoundException("Kullanıcı aktif değil: " + username + ". Lütfen yönetici ile iletişime geçin.");
        }

        // Kullanıcının rollerini GrantedAuthority listesine çevir
        // Spring Security, rollerin "ROLE_" ön ekiyle başlamasını bekler (örn: ROLE_ADMIN, ROLE_CALISAN)
        // Eğer veritabanındaki RolAdi alanınızda "Admin", "Çalışan" gibi değerler varsa,
        // bunları "ROLE_ADMIN", "ROLE_CALISAN" şeklinde dönüştürmeliyiz.
        String rolAdi = "ROLE_" + kullanici.getRol().getRolAdi().toUpperCase().replace("İ", "I").replace("Ü","U").replace("Ö","O").replace("Ş","S").replace("Ğ","G").replace("Ç","C");
        // Türkçe karakterleri İngilizce'ye çevirme basit bir yöntem, daha robust bir çözüm gerekebilir.
        // Veya Roller tablosundaki RolAdi'nı direkt "ADMIN", "CALISAN" gibi tutabilirsiniz.
        
        Collection<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(rolAdi)
        );

        // Spring Security'nin User nesnesini oluşturup döndür
        // Bu nesne kullanıcı adı, şifre (veritabanındaki hash'li şifre), aktiflik durumu ve yetkileri içerir.
        return new User(kullanici.getKullaniciAdi(),
                        kullanici.getSifreHash(),
                        kullanici.getAktifMi(), // enabled
                        true, // accountNonExpired (şimdilik true)
                        true, // credentialsNonExpired (şimdilik true)
                        true, // accountNonLocked (şimdilik true)
                        authorities);
    }
}
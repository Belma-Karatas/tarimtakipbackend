package com.example.tarimtakipbackend.service;

import com.example.tarimtakipbackend.dto.KullaniciDto;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.entity.Rol;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import com.example.tarimtakipbackend.repository.RolRepository;
import org.slf4j.Logger; // SLF4J Logger ekle
import org.slf4j.LoggerFactory; // SLF4J Logger ekle
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional; // Optional importu

@Service
public class KullaniciKayitServis {

    // Logger tanımla
    private static final Logger logger = LoggerFactory.getLogger(KullaniciKayitServis.class);

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Kullanici kaydetYeniKullanici(KullaniciDto kullaniciDto, String varsayilanRolAdi) {
        if (kullaniciRepository.findByKullaniciAdi(kullaniciDto.getKullaniciAdi()).isPresent()) {
            logger.error("Kullanıcı adı zaten mevcut: {}", kullaniciDto.getKullaniciAdi());
            throw new IllegalArgumentException("Bu kullanıcı adı zaten mevcut: " + kullaniciDto.getKullaniciAdi());
        }

        logger.info("Aranan varsayılan rol adı: '{}'", varsayilanRolAdi); // Aranan rol adını logla
        Optional<Rol> rolOptional = rolRepository.findByRolAdi(varsayilanRolAdi);

        if (!rolOptional.isPresent()) {
            logger.error("Rol bulunamadı! Veritabanında '{}' adında bir rol yok.", varsayilanRolAdi);
            // Veritabanındaki tüm rolleri loglayalım, ne olduğunu görelim:
            rolRepository.findAll().forEach(r -> logger.info("Mevcut Rol: ID={}, Adı='{}'", r.getRolID(), r.getRolAdi()));
            throw new RuntimeException("Hata: Rol bulunamadı - " + varsayilanRolAdi);
        }
        
        Rol kullaniciRolu = rolOptional.get();
        logger.info("Bulunan rol: ID={}, Adı='{}'", kullaniciRolu.getRolID(), kullaniciRolu.getRolAdi());


        Kullanici yeniKullanici = new Kullanici();
        yeniKullanici.setAd(kullaniciDto.getAd());
        yeniKullanici.setSoyad(kullaniciDto.getSoyad());
        yeniKullanici.setKullaniciAdi(kullaniciDto.getKullaniciAdi());
        yeniKullanici.setSifreHash(passwordEncoder.encode(kullaniciDto.getSifre()));
        yeniKullanici.setRol(kullaniciRolu);
        yeniKullanici.setAktifMi(true);

        logger.info("Yeni kullanıcı kaydediliyor: {}", yeniKullanici.getKullaniciAdi());
        return kullaniciRepository.save(yeniKullanici);
    }
}
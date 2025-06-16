package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Kullanici;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KullaniciRepository extends JpaRepository<Kullanici, Integer> {
    // JpaRepository<Kullanici, Integer>: Kullanici entity'si için CRUD operasyonları sağlar,
    // Integer: Kullanici entity'sinin birincil anahtarının (KullaniciID) tipidir.

    // Kullanıcı adına göre kullanıcı bulmak için (Login işlemi için çok önemli)
    // Spring Data JPA bu metot isminden sorguyu otomatik türetir:
    // "SELECT k FROM Kullanici k WHERE k.kullaniciAdi = ?1"
    Optional<Kullanici> findByKullaniciAdi(String kullaniciAdi);

    // E-postaya göre kullanıcı bulmak için (eğer e-posta alanı eklerseniz ve benzersizse)
    // Optional<Kullanici> findByEposta(String eposta);
}
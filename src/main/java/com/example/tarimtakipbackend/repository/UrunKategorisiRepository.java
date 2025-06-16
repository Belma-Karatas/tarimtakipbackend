package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.UrunKategorisi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Optional<UrunKategorisi> findByKategoriAdi için

@Repository
public interface UrunKategorisiRepository extends JpaRepository<UrunKategorisi, Integer> {

    // Kategori adına göre kategori bulmak için (gerekirse)
    Optional<UrunKategorisi> findByKategoriAdi(String kategoriAdi);

}
package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Urun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // findByUrunAdi için

@Repository
public interface UrunRepository extends JpaRepository<Urun, Integer> {

    // Ürün adına göre ürün bulmak için (gerekirse, örneğin ürün adı benzersizse ve eklemeden önce kontrol etmek isterseniz)
    Optional<Urun> findByUrunAdi(String urunAdi);

}
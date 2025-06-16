package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Gorev;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GorevRepository extends JpaRepository<Gorev, Integer> {
    // Çalışana atanmış görevleri getirmek için (RLS alternatifi veya ek kontrol)
    // List<Gorev> findByAtananKullanici_KullaniciIDAndDurumNotIn(Integer kullaniciId, List<String> tamamlanmisDurumlar);
}

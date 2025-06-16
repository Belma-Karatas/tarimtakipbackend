package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Ekim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EkimRepository extends JpaRepository<Ekim, Integer> {
    // İhtiyaç duyulursa özel sorgu metotları buraya eklenebilir.
    // Örneğin, belirli bir tarladaki ekimleri getirmek için:
    // List<Ekim> findByTarla_TarlaID(Integer tarlaId);
}
package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Girdiler; // Entity adınız Girdiler ise
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GirdilerRepository extends JpaRepository<Girdiler, Integer> {
    // Girdiler tablosu için
}
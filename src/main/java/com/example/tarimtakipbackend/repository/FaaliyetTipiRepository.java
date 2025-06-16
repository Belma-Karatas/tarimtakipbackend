package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.FaaliyetTipi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaaliyetTipiRepository extends JpaRepository<FaaliyetTipi, Integer> {
}
package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.SensorTipi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorTipiRepository extends JpaRepository<SensorTipi, Integer> {
}
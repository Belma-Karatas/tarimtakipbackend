package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Integer> {
    // Belirli bir tarladaki sensörleri bulmak için (Opsiyonel, SP ile de yapılabilir)
    // List<Sensor> findByTarla_TarlaID(Integer tarlaId);

    // Sensör koduna göre sensör bulmak için (Opsiyonel)
    Optional<Sensor> findBySensorKodu(String sensorKodu);
}
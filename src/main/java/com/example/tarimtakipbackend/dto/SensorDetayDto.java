package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate; // KurulumTarihi, SonBakimTarihi için
import java.time.LocalDateTime; // KayitTarihi için

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDetayDto {
    private Integer sensorID;
    private String sensorKodu;
    private Integer tarlaID; // View'dan gelen TarlaID
    private String tarlaAdi;
    private String sensorTipi; // SensorTipleri.TipAdi
    private String olcumBirimi; // SensorTipleri.OlcumBirimi
    private String markaModel;
    private LocalDate kurulumTarihi;
    private String konumAciklamasi;
    private Boolean aktifMi; // Veritabanındaki BIT -> Boolean
    private LocalDate sonBakimTarihi;
    private LocalDateTime sensorKayitTarihi; // Sensorler.KayitTarihi
}
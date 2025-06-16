package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorOkumaDetayDto {
    private Integer okumaID; // SQL'deki OkumaID (BIGINT olabilir, Integer şimdilik yeterli varsayalım)
    private Integer sensorID;
    private String sensorKodu;
    private String tarlaAdi;
    private String sensorTipi;
    private LocalDateTime okumaZamani; // SQL DATETIME2 -> LocalDateTime
    private String deger;
    private String okumaBirimi;
    private String girenKullanici;
    private LocalDateTime kayitTarihi; // SQL DATETIME2 -> LocalDateTime
}
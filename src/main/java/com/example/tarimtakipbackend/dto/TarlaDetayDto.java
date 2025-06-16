package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime; // Veya SP'den ne dönüyorsa (java.sql.Timestamp)

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarlaDetayDto {
    private Integer tarlaID;
    private String tarlaAdi;
    private BigDecimal alan;
    private String tarlaAciklamasi; // vTarlaDetaylari'daki sütun adı
    private String toprakTipi;      // vTarlaDetaylari'daki sütun adı
    private String sulamaSistemi;   // vTarlaDetaylari'daki sütun adı
    private LocalDateTime tarlaKayitTarihi; // vTarlaDetaylari'daki sütun adı
    private LocalDateTime tarlaGuncellemeTarihi; // vTarlaDetaylari'daki sütun adı
    // View'ınızdaki diğer sütunlar buraya eklenebilir.
}
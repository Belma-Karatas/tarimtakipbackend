package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EkimDetayDto {
    private Integer ekimID;
    private String tarlaAdi;
    private String urunAdi;
    private LocalDate ekimTarihi; // SQL DATE -> LocalDate
    private LocalDate planlananHasatTarihi; // SQL DATE -> LocalDate
    private String ekilenMiktarAciklama;
    private String ekimDurumu;
    private String ekimNotlari;
    private String kaydedenKullanici; // Ad Soyad
    private LocalDateTime ekimKayitTarihi; // SQL DATETIME2 -> LocalDateTime
    private LocalDateTime ekimGuncellemeTarihi; // SQL DATETIME2 -> LocalDateTime
}
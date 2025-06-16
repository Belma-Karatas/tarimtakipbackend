package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HasatDetayDto {
    private Integer hasatID;
    private Integer ekimID; // Bu genellikle sadece ID olarak yeterli, detaylar diğer alanlarda
    private String tarlaAdi;
    private String urunAdi;
    private LocalDate hasatTarihi;
    private BigDecimal toplananMiktar;
    private String hasatBirimi;
    private String hasatKalitesi;
    private String depoBilgisi;
    private BigDecimal hasatMaliyeti;
    private BigDecimal hasatSatisFiyati;
    private String kaydedenKullanici;
    private String hasatNotlari;
    private LocalDateTime hasatKayitTarihi; // vHasatDetaylari'nda bu sütun varsa
}
package com.example.tarimtakipbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KullaniciDto {
    private String ad;
    private String soyad;
    private String kullaniciAdi;
    private String sifre;
    // private String rolAdi; // Rolü DTO ile de alabiliriz veya varsayılan atayabiliriz
}
package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EkimFormDto {
    private Integer ekimID;
    private Integer tarlaId;
    private Integer urunId;
    private String ekimTarihi;
    private String planlananHasatTarihi;
    private String ekilenMiktarAciklama;
    private String durum;
    private String notlar;
}
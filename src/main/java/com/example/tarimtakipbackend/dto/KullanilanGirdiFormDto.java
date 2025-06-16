package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal; // Miktar ve Maliyet için

// Validasyon anotasyonları eklenebilir (Controller'da @Valid ile kullanılırsa)
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;
// import jakarta.validation.constraints.DecimalMin;
// import jakarta.validation.constraints.PastOrPresent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KullanilanGirdiFormDto {

    private Integer kullanimID; // Güncelleme için, yeni kayıtta null

    private Integer ekimId; // İlişkili Ekim (opsiyonel)
    private Integer gorevId; // İlişkili Görev (opsiyonel) - EkimId veya GorevId'den biri dolu olmalı

    // @NotNull(message = "Girdi seçimi zorunludur.")
    private Integer girdiId;

    // @NotBlank(message = "Kullanım tarihi boş olamaz.")
    // @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Kullanım tarihi yyyy-MM-dd formatında olmalıdır.")
    private String kullanimTarihi; // HTML date input'tan String olarak gelir (yyyy-MM-dd)

    // @NotNull(message = "Miktar boş olamaz.")
    // @DecimalMin(value = "0.01", message = "Miktar pozitif olmalıdır.")
    private BigDecimal miktar;

    private BigDecimal maliyet; // Opsiyonel

    private String notlar; // Opsiyonel
}
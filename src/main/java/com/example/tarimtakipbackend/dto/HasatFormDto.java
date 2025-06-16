package com.example.tarimtakipbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HasatFormDto {
    private Integer hasatID; // Güncelleme için

    // @NotNull(message = "Ekim seçimi zorunludur.")
    private Integer ekimId;

    // @NotBlank(message = "Hasat tarihi boş olamaz.")
    private String hasatTarihi; // yyyy-MM-dd

    // @NotNull(message = "Toplanan miktar boş olamaz.")
    // @DecimalMin(value = "0.01", message = "Miktar pozitif olmalıdır.")
    private BigDecimal toplananMiktar;

    // @NotBlank(message = "Birim boş olamaz.")
    private String birim;

    private String kalite;
    private String depoBilgisi;
    private BigDecimal maliyet;
    private BigDecimal satisFiyati;
    private String notlar;
}
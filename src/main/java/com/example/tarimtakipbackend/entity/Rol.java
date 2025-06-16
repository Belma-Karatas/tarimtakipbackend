package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Lombok: Getter, Setter, toString, equals, hashCode metotlarını otomatik oluşturur
@NoArgsConstructor // Lombok: Parametresiz constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
@Entity // Bu sınıfın bir JPA entity'si olduğunu belirtir
@Table(name = "Roller") // Veritabanındaki "Roller" tablosuyla eşleşir
public class Rol {

    @Id // Bu alanın birincil anahtar (Primary Key) olduğunu belirtir
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID'nin veritabanı tarafından otomatik artırılacağını belirtir (IDENTITY stratejisi)
    @Column(name = "RolID") // Veritabanındaki "RolID" sütunuyla eşleşir
    private Integer rolID;

    @Column(name = "RolAdi", nullable = false, unique = true, length = 50) // "RolAdi" sütunu, boş olamaz, benzersiz ve max 50 karakter
    private String rolAdi;

    // Eğer Rol tablosundan Kullanicilar'a bir ilişki (OneToMany) kurmak istersen buraya ekleyebilirsin,
    // ama şimdilik Kullanici tarafından ManyToOne ilişki yeterli olacaktır.
    // @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private Set<Kullanici> kullanicilar;
}
package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SulamaSistemleri")
public class SulamaSistemi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SulamaSistemiID")
    private Integer sulamaSistemiID;

    @Column(name = "SistemAdi", nullable = false, unique = true, length = 100)
    private String sistemAdi;

    // Eğer Tarlalar ile çift yönlü ilişki kurmak istersen
    // @OneToMany(mappedBy = "sulamaSistemi")
    // private java.util.Set<Tarla> tarlalar;
}
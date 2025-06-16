package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Bu arayüzün bir Spring Data Repository olduğunu belirtir
public interface RolRepository extends JpaRepository<Rol, Integer> {
    // JpaRepository<Rol, Integer>: Rol entity'si için CRUD operasyonları sağlar,
    // Integer: Rol entity'sinin birincil anahtarının (RolID) tipidir.

    // Rol adına göre rol bulmak için (eğer gerekirse)
    Optional<Rol> findByRolAdi(String rolAdi);
}
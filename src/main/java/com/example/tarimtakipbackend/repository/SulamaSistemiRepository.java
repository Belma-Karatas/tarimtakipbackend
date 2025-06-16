package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.SulamaSistemi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SulamaSistemiRepository extends JpaRepository<SulamaSistemi, Integer> {
}
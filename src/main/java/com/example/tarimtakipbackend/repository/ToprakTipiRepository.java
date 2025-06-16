package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.ToprakTipi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToprakTipiRepository extends JpaRepository<ToprakTipi, Integer> {
}
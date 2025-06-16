package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.GirdiTipi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GirdiTipiRepository extends JpaRepository<GirdiTipi, Integer> {
}
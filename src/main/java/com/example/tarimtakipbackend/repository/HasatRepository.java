package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Hasatlar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HasatRepository extends JpaRepository<Hasatlar, Integer> {
}
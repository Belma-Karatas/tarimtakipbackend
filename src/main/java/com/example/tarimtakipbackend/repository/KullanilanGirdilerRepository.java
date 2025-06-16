package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.KullanilanGirdiler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// import java.util.List;

@Repository
public interface KullanilanGirdilerRepository extends JpaRepository<KullanilanGirdiler, Integer> {
    // İhtiyaç halinde özel sorgu metotları eklenebilir. Örneğin:
    // List<KullanilanGirdiler> findByEkim_EkimID(Integer ekimId);
    // List<KullanilanGirdiler> findByGorev_GorevID(Integer gorevId);
}
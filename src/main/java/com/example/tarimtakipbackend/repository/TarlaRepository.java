package com.example.tarimtakipbackend.repository;

import com.example.tarimtakipbackend.entity.Tarla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TarlaRepository extends JpaRepository<Tarla, Integer> {

    // Saklı yordamları çağırmak için (Eğer SP'leriniz varsa ve kullanmak isterseniz)
    // Örneğin, tüm tarlaları listelemek için vTarlaDetaylari view'ını kullanan bir SP'niz varsa:
    // @Procedure(name = "spTarla_Listele") // Entity'de @NamedStoredProcedureQuery ile tanımlanmalı
    // List<Tarla> listeleTarlalar(); // Dönüş tipi SP'nin döndürdüğü yapıya uygun olmalı (belki bir DTO)

    // Şimdilik JpaRepository'nin findAll() metodunu kullanacağız.
    // Eğer vTarlaDetaylari gibi bir view'ı direkt maplemek isterseniz, o view için ayrı bir read-only entity ve repository oluşturmak daha iyi olur.
    // Hızlıca ilerlemek için direkt Tarla entity'sini listeleyeceğiz.
}
package com.example.tarimtakipbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Kullanicilar") // Veritabanındaki "Kullanicilar" tablosuyla eşleşir
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KullaniciID")
    private Integer kullaniciID;

    @Column(name = "Ad", nullable = false, length = 50)
    private String ad;

    @Column(name = "Soyad", nullable = false, length = 50)
    private String soyad;

    @Column(name = "KullaniciAdi", nullable = false, unique = true, length = 50)
    private String kullaniciAdi;

    @Column(name = "SifreHash", nullable = false, length = 256)
    private String sifreHash; // Bu alan uygulama katmanında hash'lenmiş şifreyi tutacak

    @ManyToOne(fetch = FetchType.EAGER) // Kullanıcı çekildiğinde Rol bilgisi de EAGER (hemen) yüklensin
    @JoinColumn(name = "RolID", nullable = false) // "RolID" yabancı anahtar sütunu ile Rol tablosuna bağlanır
    private Rol rol;

    @Column(name = "AktifMi", columnDefinition = "BIT DEFAULT 1")
    private Boolean aktifMi = true; // Varsayılan olarak true

    @Column(name = "KayitTarihi", columnDefinition = "DATETIME2 DEFAULT GETDATE()", insertable = false, updatable = false)
    // insertable=false ve updatable=false: Bu alanın değerini veritabanı kendi DEFAULT'u ile belirlesin, JPA müdahale etmesin.
    // Ancak LocalDateTime ile GETDATE() arasında uyumsuzluk olabilir.
    // Genellikle bu tür alanlar için @CreationTimestamp (Hibernate) veya @CreatedDate (Spring Data JPA Auditing) kullanılır.
    // Şimdilik böyle bırakalım, veritabanı DEFAULT'una güvenelim.
    // Eğer sorun olursa, insertable=true yapıp Java tarafında set edebiliriz veya auditing kullanabiliriz.
    private LocalDateTime kayitTarihi;

    // Sütun düzeyinde şifreleme için eklediğimiz SifreliSoyad alanını da buraya ekleyebiliriz.
    // Ancak bu, JPA'nın bu alanı otomatik yönetmesini zorlaştırır.
    // Şifreleme/çözme işlemini servis katmanında veya özel bir AttributeConverter ile yapmak daha uygun olabilir.
    // Şimdilik bu projede temel JPA maplemelerine odaklanalım.
    // @Column(name = "SifreliSoyad", length = 256) // VARBINARY(256)
    // private byte[] sifreliSoyad;
}
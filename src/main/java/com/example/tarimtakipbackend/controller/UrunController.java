package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.UrunDetayDto;
import com.example.tarimtakipbackend.dto.UrunFormDto;
import com.example.tarimtakipbackend.entity.Urun;
import com.example.tarimtakipbackend.repository.UrunKategorisiRepository;
import com.example.tarimtakipbackend.service.UrunServis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/urunler")
@PreAuthorize("hasRole('ADMIN')")
public class UrunController {

    private static final Logger logger = LoggerFactory.getLogger(UrunController.class);

    @Autowired
    private UrunServis urunServis;

    @Autowired
    private UrunKategorisiRepository urunKategorisiRepository;

    @GetMapping
    public String listUrunler(Model model) {
        logger.info("/admin/urunler GET isteği - Ürünler listeleniyor.");
        model.addAttribute("pageTitle", "Admin - Ürün Listesi");
        model.addAttribute("activePage", "urunler");
        try {
            List<UrunDetayDto> urunler = urunServis.getAllUrunlerSP();
            model.addAttribute("urunler", urunler);
        } catch (Exception e) {
            logger.error("Ürünler listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Ürünler listelenirken bir hata oluştu.");
            model.addAttribute("urunler", List.of());
        }
        return "admin/urunler-liste";
    }

    @GetMapping("/ekle")
    public String showUrunEkleForm(Model model) {
        logger.info("/admin/urunler/ekle GET isteği - Yeni ürün formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Ürün Ekle");
        model.addAttribute("activePage", "urunler");
        if (!model.containsAttribute("urunForm")) {
            model.addAttribute("urunForm", new UrunFormDto());
        }
        model.addAttribute("urunKategorileri", urunKategorisiRepository.findAll());
        return "admin/urun-form";
    }

    @GetMapping("/duzenle/{id}")
    public String showUrunDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/urunler/duzenle/{} GET isteği - Ürün düzenleme formu gösteriliyor.", id);
        model.addAttribute("pageTitle", "Admin - Ürünü Düzenle");
        model.addAttribute("activePage", "urunler");

        if (!model.containsAttribute("urunForm")) { // Hata sonrası redirect'ten gelmiyorsa
            Optional<UrunDetayDto> urunDetayOptional = urunServis.getUrunDetayByIdSP(id);
            if (urunDetayOptional.isPresent()) {
                UrunDetayDto detay = urunDetayOptional.get();
                UrunFormDto urunForm = new UrunFormDto();
                urunForm.setUrunID(detay.getUrunID());
                urunForm.setUrunAdi(detay.getUrunAdi());
                urunForm.setKategoriId(detay.getKategoriId()); // UrunDetayDto'da kategoriId alanı olduğu varsayılıyor
                urunForm.setAciklama(detay.getUrunAciklamasi());
                urunForm.setBirim(detay.getUrunBirimi());
                model.addAttribute("urunForm", urunForm);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Ürün bulunamadı ID: " + id);
                return "redirect:/admin/urunler";
            }
        }
        // Her durumda (yeni veya hata sonrası) kategorileri forma gönder
        model.addAttribute("urunKategorileri", urunKategorisiRepository.findAll());
        return "admin/urun-form";
    }

    @PostMapping("/kaydet")
    public String saveUrun(@ModelAttribute("urunForm") UrunFormDto urunForm,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        logger.info("/admin/urunler/kaydet POST isteği - Ürün Adı: {}", urunForm.getUrunAdi());
        // activePage'i burada set etmeye gerek yok, çünkü ya redirect olacak ya da formu tekrar göstereceğiz
        // Formu tekrar gösterirken show...Form metotları zaten set ediyor.

        // Basit validasyon
        if (urunForm.getUrunAdi() == null || urunForm.getUrunAdi().trim().isEmpty()) {
            result.rejectValue("urunAdi", "NotEmpty", "Ürün adı boş olamaz.");
        }
        if (urunForm.getKategoriId() == null) {
            result.rejectValue("kategoriId", "NotNull", "Kategori seçilmelidir.");
        }
        // Diğer validasyonlar eklenebilir (Örn: Birim boş olamaz vb.)

        if (result.hasErrors()) {
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            // Hata varsa, formu redirect yapmadan direkt tekrar gösteriyoruz
            model.addAttribute("pageTitle", (urunForm.getUrunID() == null ? "Admin - Yeni Ürün Ekle" : "Admin - Ürünü Düzenle"));
            model.addAttribute("activePage", "urunler"); // Sidebar için
            model.addAttribute("urunKategorileri", urunKategorisiRepository.findAll()); // Dropdown için
            // urunForm zaten @ModelAttribute ile modelde
            return "admin/urun-form"; // Direkt formu göster
        }

        try {
            if (urunForm.getUrunID() == null) { // Yeni kayıt
                Urun kaydedilenUrun = urunServis.saveUrunSP(urunForm);
                redirectAttributes.addFlashAttribute("successMessage", "Ürün başarıyla eklendi. ID: " + kaydedilenUrun.getUrunID());
            } else { // Güncelleme
                boolean guncellendi = urunServis.updateUrunSP(urunForm);
                if (guncellendi) {
                    redirectAttributes.addFlashAttribute("successMessage", "Ürün başarıyla güncellendi. ID: " + urunForm.getUrunID());
                } else {
                    // Servis false dönerse (bu durum pek beklenmez, genellikle exception fırlatılır)
                    redirectAttributes.addFlashAttribute("errorMessage", "Ürün güncellenirken bir sorun oluştu. (Servis false döndü)");
                    redirectAttributes.addFlashAttribute("urunForm", urunForm);
                    return "redirect:/admin/urunler/duzenle/" + urunForm.getUrunID() + "?error";
                }
            }
            return "redirect:/admin/urunler"; // Başarılı olursa listeye yönlendir

        } catch (RuntimeException e) { // IllegalArgumentException da RuntimeException'dan türediği için bu blok yakalar
            logger.error("Ürün kaydı/güncellemesi sırasında hata: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem sırasında bir hata oluştu: " + e.getMessage());
            redirectAttributes.addFlashAttribute("urunForm", urunForm); // Hatalı formu ve girilen değerleri flash attribute ile taşı

            String redirectUrl;
            if (urunForm.getUrunID() != null) { // Hata güncelleme sırasında olduysa
                redirectUrl = "/admin/urunler/duzenle/" + urunForm.getUrunID() + "?error";
            } else { // Hata yeni kayıt sırasında olduysa
                redirectUrl = "/admin/urunler/ekle?error";
            }
            return "redirect:" + redirectUrl;
        }
    }

    @GetMapping("/sil/{id}")
    public String deleteUrun(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/urunler/sil/{} GET isteği.", id);
        try {
            boolean silindi = urunServis.deleteUrunSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Ürün başarıyla silindi. ID: " + id);
            } else {
                // Bu durum da servis exception fırlatmadığında olur, normalde beklenmez
                redirectAttributes.addFlashAttribute("errorMessage", "Ürün silinemedi. (Servis false döndü)");
            }
        } catch (RuntimeException e) {
            logger.error("Ürün silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/urunler";
    }
}
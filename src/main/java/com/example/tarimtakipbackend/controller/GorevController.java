package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.GorevDetayDto;
import com.example.tarimtakipbackend.dto.GorevFormDto;
import com.example.tarimtakipbackend.entity.Gorev;
import com.example.tarimtakipbackend.entity.Kullanici;
import com.example.tarimtakipbackend.repository.EkimRepository;
import com.example.tarimtakipbackend.repository.FaaliyetTipiRepository;
import com.example.tarimtakipbackend.repository.KullaniciRepository;
import com.example.tarimtakipbackend.repository.TarlaRepository;
import com.example.tarimtakipbackend.service.GorevServis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/gorevler")
// SINIF SEVİYESİNDEKİ @PreAuthorize("hasRole('ADMIN')") KALDIRILDI.
// Yetkilendirme artık metot bazlı yapılacak.
public class GorevController {

    private static final Logger logger = LoggerFactory.getLogger(GorevController.class);
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private GorevServis gorevServis;

    @Autowired
    private EkimRepository ekimRepository;

    @Autowired
    private TarlaRepository tarlaRepository;

    @Autowired
    private FaaliyetTipiRepository faaliyetTipiRepository;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')") // Hem Admin hem Çalışan görevleri listeleyebilir (RLS filtreleyecek)
    public String listGorevler(Model model, Authentication authentication) {
        logger.info("/admin/gorevler GET isteği - Görevler listeleniyor. Kullanıcı: {}", authentication.getName());
        
        boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            model.addAttribute("pageTitle", "Görevlerim");
            model.addAttribute("activePage", "gorevlerim");
        } else {
            model.addAttribute("pageTitle", "Admin - Görev Yönetimi");
            model.addAttribute("activePage", "gorevler");
        }
        model.addAttribute("isAdmin", isAdmin);

        try {
            List<GorevDetayDto> gorevler = gorevServis.getAllGorevlerSP();
            logger.info("Görevler SP'den alındı, Adet: {}", gorevler != null ? gorevler.size() : 0);
            model.addAttribute("gorevler", gorevler);
        } catch (Exception e) {
            logger.error("Görevler listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Görevler listelenirken bir hata oluştu: " + e.getMessage());
            model.addAttribute("gorevler", Collections.emptyList());
        }
        return "admin/gorevler-liste";
    }

    @GetMapping("/ekle")
    @PreAuthorize("hasRole('ADMIN')") // Sadece Admin yeni görev ekleyebilir
    public String showGorevEkleForm(Model model) {
        logger.info("/admin/gorevler/ekle GET isteği - Yeni görev formu gösteriliyor.");
        model.addAttribute("pageTitle", "Admin - Yeni Görev Ata/Ekle");
        model.addAttribute("activePage", "gorevler");
        model.addAttribute("isAdmin", true); 

        if (!model.containsAttribute("gorevForm")) {
            model.addAttribute("gorevForm", new GorevFormDto());
        }
        addDropdownDataToModel(model);
        return "admin/gorev-form";
    }

    @GetMapping("/duzenle/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')") // Hem Admin hem Çalışan görev düzenleme formunu açabilir
    public String showGorevDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        logger.info("/admin/gorevler/duzenle/{} GET isteği. Kullanıcı: {}", id, authentication.getName());
        
        boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);

        String pageTitle = isAdmin ? "Admin - Görevi Düzenle" : "Görev Durumunu Güncelle";
        String activePage = isAdmin ? "gorevler" : "gorevlerim";
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("activePage", activePage);

        if (!model.containsAttribute("gorevForm")) {
            Optional<Gorev> gorevEntityOptional = gorevServis.findById(id);
            if (gorevEntityOptional.isPresent()) {
                Gorev gorev = gorevEntityOptional.get();

                if (!isAdmin) {
                    String currentUsername = authentication.getName();
                    Kullanici atananKullanici = gorev.getAtananKullanici();
                    if (atananKullanici == null || !atananKullanici.getKullaniciAdi().equals(currentUsername)) {
                        logger.warn("Yetkisiz erişim denemesi: Çalışan {} kendisine ait olmayan görev ID {} için düzenleme formuna erişmeye çalıştı.", currentUsername, id);
                        redirectAttributes.addFlashAttribute("errorMessage", "Bu görevi düzenleme yetkiniz bulunmamaktadır.");
                        return "redirect:/admin/gorevler"; 
                    }
                }

                GorevFormDto gorevForm = new GorevFormDto();
                gorevForm.setGorevID(gorev.getGorevID());
                if (gorev.getEkim() != null) gorevForm.setEkimId(gorev.getEkim().getEkimID());
                if (gorev.getTarla() != null) gorevForm.setTarlaId(gorev.getTarla().getTarlaID());
                if (gorev.getFaaliyetTipi() != null) gorevForm.setFaaliyetTipiId(gorev.getFaaliyetTipi().getFaaliyetTipiID());
                gorevForm.setAciklama(gorev.getAciklama());
                if (gorev.getAtananKullanici() != null) gorevForm.setAtananKullaniciId(gorev.getAtananKullanici().getKullaniciID());
                
                if (gorev.getPlanlananBaslangicTarihi() != null) gorevForm.setPlanlananBaslangicTarihi(gorev.getPlanlananBaslangicTarihi().format(ISO_DATETIME_FORMATTER));
                if (gorev.getPlanlananBitisTarihi() != null) gorevForm.setPlanlananBitisTarihi(gorev.getPlanlananBitisTarihi().format(ISO_DATETIME_FORMATTER));
                if (gorev.getTamamlanmaTarihi() != null) gorevForm.setTamamlanmaTarihi(gorev.getTamamlanmaTarihi().format(ISO_DATETIME_FORMATTER));
                
                gorevForm.setDurum(gorev.getDurum());
                gorevForm.setOncelik(gorev.getOncelik());
                model.addAttribute("gorevForm", gorevForm);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Görev kaydı bulunamadı ID: " + id);
                return "redirect:/admin/gorevler";
            }
        }
        addDropdownDataToModel(model);
        return "admin/gorev-form";
    }

    @PostMapping("/kaydet")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')") // Hem Admin hem Çalışan kaydedebilir (içeride kontrol edilecek)
    public String saveGorev(@ModelAttribute("gorevForm") GorevFormDto gorevForm,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model, Authentication authentication) {

        logger.info("/admin/gorevler/kaydet POST isteği. Görev ID: {}, Kullanıcı: {}", gorevForm.getGorevID(), authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            if (gorevForm.getEkimId() == null && gorevForm.getTarlaId() == null) {
                result.rejectValue("tarlaId", "NotNull.gorevForm.iliskiliAlan", "Görev bir Ekim veya Tarla ile ilişkili olmalıdır.");
            }
            if (gorevForm.getFaaliyetTipiId() == null) {
                result.rejectValue("faaliyetTipiId", "NotNull.gorevForm.faaliyetTipiId", "Faaliyet tipi seçimi zorunludur.");
            }
            if (gorevForm.getAciklama() == null || gorevForm.getAciklama().trim().isEmpty()) {
                result.rejectValue("aciklama", "NotEmpty.gorevForm.aciklama", "Açıklama zorunludur.");
            }
        } else { 
            if (gorevForm.getDurum() == null || gorevForm.getDurum().trim().isEmpty()) {
                result.rejectValue("durum", "NotEmpty.gorevForm.durum", "Durum boş olamaz.");
            }
        }

        if (result.hasErrors()) {
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            String pageTitle = isAdmin ? (gorevForm.getGorevID() == null ? "Admin - Yeni Görev Ata/Ekle" : "Admin - Görevi Düzenle") : "Görev Durumunu Güncelle";
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("activePage", isAdmin ? "gorevler" : "gorevlerim");
            addDropdownDataToModel(model);
            return "admin/gorev-form";
        }

        try {
            if (!isAdmin && gorevForm.getGorevID() != null) { 
                String tamamlanmaTarihiStr = gorevForm.getTamamlanmaTarihi();
                if ("Tamamlandı".equalsIgnoreCase(gorevForm.getDurum()) && (tamamlanmaTarihiStr == null || tamamlanmaTarihiStr.trim().isEmpty())) {
                    tamamlanmaTarihiStr = LocalDateTime.now().format(ISO_DATETIME_FORMATTER);
                }
                gorevServis.updateGorevDurumSP(gorevForm.getGorevID(), gorevForm.getDurum(), tamamlanmaTarihiStr);
                redirectAttributes.addFlashAttribute("successMessage", "Görev durumu başarıyla güncellendi. ID: " + gorevForm.getGorevID());
            } else if (isAdmin) { 
                if (gorevForm.getGorevID() == null) {
                    Gorev kaydedilenGorev = gorevServis.saveGorevSP(gorevForm);
                    redirectAttributes.addFlashAttribute("successMessage", "Görev başarıyla eklendi. ID: " + kaydedilenGorev.getGorevID());
                } else {
                    boolean guncellendi = gorevServis.updateGorevSP(gorevForm);
                    if (guncellendi) {
                        redirectAttributes.addFlashAttribute("successMessage", "Görev başarıyla güncellendi. ID: " + gorevForm.getGorevID());
                    } else {
                        redirectAttributes.addFlashAttribute("errorMessage", "Görev güncellenemedi (beklenmedik durum).");
                        redirectAttributes.addFlashAttribute("gorevForm", gorevForm);
                        return "redirect:/admin/gorevler/duzenle/" + gorevForm.getGorevID() + "?error";
                    }
                }
            } else {
                throw new IllegalAccessException("Çalışanlar yeni görev ekleyemez veya yetkisiz güncelleme denemesi.");
            }
            return "redirect:/admin/gorevler";

        } catch (IllegalArgumentException | IllegalAccessException e) {
            logger.error("Görev kaydı/güncellemesi sırasında hata: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("gorevForm", gorevForm);
            String redirectUrl = (gorevForm.getGorevID() != null) ? "/admin/gorevler/duzenle/" + gorevForm.getGorevID() + "?error" : "/admin/gorevler/ekle?error";
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            logger.error("Görev kaydı/güncellemesi sırasında beklenmedik bir hata: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "İşlem sırasında beklenmedik bir hata oluştu.");
            redirectAttributes.addFlashAttribute("gorevForm", gorevForm);
            String redirectUrl = (gorevForm.getGorevID() != null) ? "/admin/gorevler/duzenle/" + gorevForm.getGorevID() + "?error" : "/admin/gorevler/ekle?error";
            return "redirect:" + redirectUrl;
        }
    }

    @GetMapping("/sil/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Sadece Admin silebilsin
    public String deleteGorev(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/gorevler/sil/{} GET isteği.", id);
        try {
            boolean silindi = gorevServis.deleteGorevSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Görev başarıyla silindi. ID: " + id);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Görev silinemedi (Servis false döndü, beklenmedik durum).");
            }
        } catch (RuntimeException e) {
            logger.error("Görev silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/gorevler";
    }

    private void addDropdownDataToModel(Model model) {
        model.addAttribute("ekimler", ekimRepository.findAll());
        model.addAttribute("tarlalar", tarlaRepository.findAll());
        model.addAttribute("faaliyetTipleri", faaliyetTipiRepository.findAll());
        model.addAttribute("kullanicilar", kullaniciRepository.findAll());
        model.addAttribute("durumListesi", List.of("Atandı", "Devam Ediyor", "Tamamlandı", "İptal Edildi", "Beklemede"));
        model.addAttribute("oncelikListesi", List.of(1, 2, 3, 4, 5));
    }
}
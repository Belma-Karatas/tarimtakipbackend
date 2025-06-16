package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.KullanilanGirdiDetayDto;
import com.example.tarimtakipbackend.dto.KullanilanGirdiFormDto;
import com.example.tarimtakipbackend.entity.KullanilanGirdiler; 
import com.example.tarimtakipbackend.repository.EkimRepository;
import com.example.tarimtakipbackend.repository.GirdilerRepository;
import com.example.tarimtakipbackend.repository.GorevRepository;
import com.example.tarimtakipbackend.service.KullanilanGirdiServis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException; 
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/kullanilangirdiler")
public class KullanilanGirdiController {

    private static final Logger logger = LoggerFactory.getLogger(KullanilanGirdiController.class);

    @Autowired
    private KullanilanGirdiServis kullanilanGirdiServis;

    @Autowired
    private GirdilerRepository girdilerRepository;

    @Autowired
    private EkimRepository ekimRepository;

    @Autowired
    private GorevRepository gorevRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listKullanilanGirdiler(Model model) {
        logger.info("/admin/kullanilangirdiler GET isteği - Kullanılan girdiler listeleniyor.");
        model.addAttribute("pageTitle", "Admin - Kullanılan Girdi Listesi");
        model.addAttribute("activePage", "kullanilangirdiler");
        try {
            List<KullanilanGirdiDetayDto> girdiler = kullanilanGirdiServis.getAllKullanilanGirdilerSP();
            model.addAttribute("kullanilangirdiler", girdiler);
        } catch (Exception e) {
            logger.error("Kullanılan girdiler listelenirken hata oluştu: ", e);
            model.addAttribute("errorMessage", "Kullanılan girdiler listelenirken bir hata oluştu: " + e.getMessage());
            model.addAttribute("kullanilangirdiler", Collections.emptyList());
        }
        return "admin/kullanilangirdiler-liste";
    }

    private void prepareFormModel(Model model, KullanilanGirdiFormDto formDto, boolean isAdmin, Integer ekimId, Integer gorevId) {
        if (!model.containsAttribute("kullanilanGirdiForm")) {
            if (formDto == null) { // Eğer formDto null ise (doğrudan /ekle çağrısı gibi)
                formDto = new KullanilanGirdiFormDto();
            }
            if (!isAdmin && gorevId != null) {
                formDto.setGorevId(gorevId);
            } else if (isAdmin && ekimId != null) {
                formDto.setEkimId(ekimId);
            } else if (isAdmin && gorevId != null) {
                 formDto.setGorevId(gorevId);
            }
            model.addAttribute("kullanilanGirdiForm", formDto);
        }
        model.addAttribute("isAdmin", isAdmin);
        addDropdownDataToModel(model);
    }


    @GetMapping("/ekle")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')")
    public String showKullanilanGirdiEkleForm(
            @RequestParam(name = "gorevId", required = false) Integer gorevId,
            @RequestParam(name = "ekimId", required = false) Integer ekimId,
            Model model, Authentication authentication) {
        
        boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
        
        logger.info("/admin/kullanilangirdiler/ekle GET isteği. Görev ID: {}, Ekim ID: {}, Admin: {}", gorevId, ekimId, isAdmin);
        model.addAttribute("pageTitle", isAdmin ? "Admin - Yeni Kullanılan Girdi Ekle" : "Görev İçin Girdi Ekle");
        model.addAttribute("activePage", isAdmin ? "kullanilangirdiler" : "gorevlerim");
        
        prepareFormModel(model, null, isAdmin, ekimId, gorevId);
        return "admin/kullanilangirdi-form";
    }

    @GetMapping("/duzenle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showKullanilanGirdiDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        logger.info("/admin/kullanilangirdiler/duzenle/{} GET isteği.", id);
        model.addAttribute("pageTitle", "Admin - Kullanılan Girdiyi Düzenle");
        model.addAttribute("activePage", "kullanilangirdiler");
        boolean isAdmin = true; // Bu sayfa sadece admin için

        KullanilanGirdiFormDto formDto = null;
        if (!model.containsAttribute("kullanilanGirdiForm")) {
            Optional<KullanilanGirdiDetayDto> detayDtoOpt = kullanilanGirdiServis.getKullanilanGirdiByIdSP(id);
            if (detayDtoOpt.isPresent()) {
                KullanilanGirdiDetayDto detayDto = detayDtoOpt.get();
                formDto = new KullanilanGirdiFormDto();
                formDto.setKullanimID(detayDto.getKullanimID());

              
                formDto.setEkimId(detayDto.getIliskiliEkimID());
                formDto.setGorevId(detayDto.getIliskiliGorevID());
                if (detayDto.getKullanimTarihi() != null) {
                    formDto.setKullanimTarihi(detayDto.getKullanimTarihi().toString());
                }
                formDto.setMiktar(detayDto.getMiktar());
                formDto.setMaliyet(detayDto.getKullanilanGirdiMaliyeti());
                formDto.setNotlar(detayDto.getKullanimNotlari());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Düzenlenecek kayıt bulunamadı. ID: " + id);
                return "redirect:/admin/kullanilangirdiler";
            }
        }
        prepareFormModel(model, formDto, isAdmin, null, null); // ekimId ve gorevId null, çünkü formdan geliyor
        return "admin/kullanilangirdi-form";
    }

    @PostMapping("/kaydet")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALISAN')")
    public String saveKullanilanGirdi(
            @ModelAttribute("kullanilanGirdiForm") KullanilanGirdiFormDto kullanilanGirdiFormDto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model, Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));

        logger.info("/admin/kullanilangirdiler/kaydet POST isteği. Kullanım ID: {}, Girdi ID: {}", kullanilanGirdiFormDto.getKullanimID(), kullanilanGirdiFormDto.getGirdiId());

        if (kullanilanGirdiFormDto.getGirdiId() == null) {
            result.rejectValue("girdiId", "NotNull", "Girdi seçimi zorunludur.");
        }
        if (kullanilanGirdiFormDto.getKullanimTarihi() == null || kullanilanGirdiFormDto.getKullanimTarihi().trim().isEmpty()) {
            result.rejectValue("kullanimTarihi", "NotEmpty", "Kullanım tarihi zorunludur.");
        }
        if (kullanilanGirdiFormDto.getMiktar() == null) {
            result.rejectValue("miktar", "NotNull", "Miktar boş olamaz.");
        }
        if (kullanilanGirdiFormDto.getEkimId() == null && kullanilanGirdiFormDto.getGorevId() == null) {
             result.rejectValue("ekimId", "NotNull.eitherOr", "Ekim veya Görev seçimi zorunludur.");
        }
        if (!isAdmin && kullanilanGirdiFormDto.getGorevId() == null) {
            result.rejectValue("gorevId", "Invalid.gorevId", "Çalışanlar sadece belirli bir görev için girdi ekleyebilir.");
        }

        if (result.hasErrors()) {
            logger.warn("Formda validasyon hataları var: {}", result.getAllErrors());
            model.addAttribute("pageTitle", isAdmin ? ((kullanilanGirdiFormDto.getKullanimID() == null) ? "Admin - Yeni Kullanılan Girdi Ekle" : "Admin - Kullanılan Girdiyi Düzenle") : "Görev İçin Girdi Ekle");
            model.addAttribute("activePage", isAdmin ? "kullanilangirdiler" : "gorevlerim");
            prepareFormModel(model, kullanilanGirdiFormDto, isAdmin, kullanilanGirdiFormDto.getEkimId(), kullanilanGirdiFormDto.getGorevId());
            return "admin/kullanilangirdi-form";
        }

        try {
            String successMessage;
            if (kullanilanGirdiFormDto.getKullanimID() == null) {
                KullanilanGirdiler kaydedilen = kullanilanGirdiServis.saveKullanilanGirdiSP(kullanilanGirdiFormDto);
                successMessage = "Kullanılan girdi başarıyla eklendi. ID: " + kaydedilen.getKullanimID();
            } else {
                if (!isAdmin) {
                    throw new AccessDeniedException("Bu işlemi yapma yetkiniz yok.");
                }
                boolean guncellendi = kullanilanGirdiServis.updateKullanilanGirdiSP(kullanilanGirdiFormDto);
                if (guncellendi) {
                    successMessage = "Kullanılan girdi başarıyla güncellendi. ID: " + kullanilanGirdiFormDto.getKullanimID();
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Kullanılan girdi güncellenemedi.");
                    redirectAttributes.addFlashAttribute("kullanilanGirdiForm", kullanilanGirdiFormDto);
                    return "redirect:/admin/kullanilangirdiler/duzenle/" + kullanilanGirdiFormDto.getKullanimID() + "?error";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            
            if (!isAdmin && kullanilanGirdiFormDto.getKullanimID() == null) {
                return "redirect:/admin/gorevler"; 
            }
            return "redirect:/admin/kullanilangirdiler";

        } catch (IllegalArgumentException | AccessDeniedException e) {
            logger.error("Kullanılan girdi kaydı/güncellemesi sırasında hata: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("kullanilanGirdiForm", kullanilanGirdiFormDto);
            
            String redirectUrl = (kullanilanGirdiFormDto.getKullanimID() != null) ?
                    "/admin/kullanilangirdiler/duzenle/" + kullanilanGirdiFormDto.getKullanimID() :
                    "/admin/kullanilangirdiler/ekle";
            
            if (kullanilanGirdiFormDto.getKullanimID() == null) {
                if (kullanilanGirdiFormDto.getGorevId() != null) redirectUrl += "?gorevId=" + kullanilanGirdiFormDto.getGorevId();
                else if (kullanilanGirdiFormDto.getEkimId() != null) redirectUrl += "?ekimId=" + kullanilanGirdiFormDto.getEkimId();
            }
            return "redirect:" + redirectUrl + (redirectUrl.contains("?") ? "&error" : "?error");

        } catch (Exception e) {
            logger.error("Kullanılan girdi kaydı/güncellemesi sırasında beklenmedik hata: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Beklenmedik bir hata oluştu: " + e.getMessage());
            redirectAttributes.addFlashAttribute("kullanilanGirdiForm", kullanilanGirdiFormDto);

            String redirectUrl = (kullanilanGirdiFormDto.getKullanimID() != null) ?
                    "/admin/kullanilangirdiler/duzenle/" + kullanilanGirdiFormDto.getKullanimID() :
                    "/admin/kullanilangirdiler/ekle";
            if (kullanilanGirdiFormDto.getKullanimID() == null) {
                 if (kullanilanGirdiFormDto.getGorevId() != null) redirectUrl += "?gorevId=" + kullanilanGirdiFormDto.getGorevId();
                else if (kullanilanGirdiFormDto.getEkimId() != null) redirectUrl += "?ekimId=" + kullanilanGirdiFormDto.getEkimId();
            }
            return "redirect:" + redirectUrl + (redirectUrl.contains("?") ? "&error_unexpected" : "?error_unexpected");
        }
    }

    @GetMapping("/sil/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteKullanilanGirdi(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/kullanilangirdiler/sil/{} GET isteği.", id);
        try {
            boolean silindi = kullanilanGirdiServis.deleteKullanilanGirdiSP(id);
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Kullanılan girdi başarıyla silindi. ID: " + id);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Kullanılan girdi silinemedi.");
            }
        } catch (Exception e) {
            logger.error("Kullanılan girdi silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/kullanilangirdiler";
    }

    private void addDropdownDataToModel(Model model) {
        model.addAttribute("girdilerListesi", girdilerRepository.findAll());
        model.addAttribute("ekimlerListesi", ekimRepository.findAll());
        model.addAttribute("gorevlerListesi", gorevRepository.findAll());
    }
}
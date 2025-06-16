package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.HasatDetayDto;
import com.example.tarimtakipbackend.dto.HasatFormDto;
import com.example.tarimtakipbackend.entity.Hasatlar;
import com.example.tarimtakipbackend.repository.EkimRepository;
import com.example.tarimtakipbackend.service.HasatServis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/hasatlar")
@PreAuthorize("hasRole('ADMIN')")
public class HasatController {

    private static final Logger logger = LoggerFactory.getLogger(HasatController.class);

    @Autowired
    private HasatServis hasatServis;

    @Autowired
    private EkimRepository ekimRepository;

    @GetMapping
    public String listHasatlar(Model model) {
        logger.info("/admin/hasatlar GET isteği - Hasatlar listeleniyor.");
        model.addAttribute("pageTitle", "Admin - Hasat Kayıtları");
        model.addAttribute("activePage", "hasatlar");
        try {
            List<HasatDetayDto> hasatlar = hasatServis.getAllHasatlarSP();
            model.addAttribute("hasatlar", hasatlar);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Hasatlar listelenirken hata: " + e.getMessage());
            model.addAttribute("hasatlar", Collections.emptyList());
            logger.error("Hasatlar listelenirken hata oluştu: ", e);
        }
        return "admin/hasatlar-liste";
    }

    private void prepareHasatFormModel(Model model, HasatFormDto formDto, Integer ekimIdParam) {
        if (!model.containsAttribute("hasatForm")) {
            if (formDto == null) { // Yeni ekleme veya parametresiz düzenleme çağrısı
                formDto = new HasatFormDto();
            }
            if (formDto.getHasatID() == null && ekimIdParam != null) { // Yeni ekleme ve ekimId URL'den geldiyse
                formDto.setEkimId(ekimIdParam);
            }
            model.addAttribute("hasatForm", formDto);
        }
        model.addAttribute("ekimlerListesi", ekimRepository.findAll());
    }

    @GetMapping("/ekle")
    public String showHasatEkleForm(@RequestParam(name = "ekimId", required = false) Integer ekimId, Model model) {
        logger.info("/admin/hasatlar/ekle GET isteği. Ekim ID: {}", ekimId);
        model.addAttribute("pageTitle", "Admin - Yeni Hasat Kaydı Ekle");
        model.addAttribute("activePage", "hasatlar");
        prepareHasatFormModel(model, null, ekimId);
        return "admin/hasat-form";
    }
    
    @GetMapping("/duzenle/{id}")
    public String showHasatDuzenleForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("/admin/hasatlar/duzenle/{} GET isteği.", id);
        model.addAttribute("pageTitle", "Admin - Hasat Kaydını Düzenle");
        model.addAttribute("activePage", "hasatlar");

        HasatFormDto formDto = null;
        if (!model.containsAttribute("hasatForm")) {
            Optional<HasatDetayDto> detayDtoOpt = hasatServis.getHasatByIdSP(id);
            if (detayDtoOpt.isPresent()) {
                HasatDetayDto detay = detayDtoOpt.get();
                formDto = new HasatFormDto();
                formDto.setHasatID(detay.getHasatID());
                formDto.setEkimId(detay.getEkimID());
                if (detay.getHasatTarihi() != null) formDto.setHasatTarihi(detay.getHasatTarihi().toString());
                formDto.setToplananMiktar(detay.getToplananMiktar());
                formDto.setBirim(detay.getHasatBirimi());
                formDto.setKalite(detay.getHasatKalitesi());
                formDto.setDepoBilgisi(detay.getDepoBilgisi());
                formDto.setMaliyet(detay.getHasatMaliyeti());
                formDto.setSatisFiyati(detay.getHasatSatisFiyati());
                formDto.setNotlar(detay.getHasatNotlari());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Düzenlenecek hasat kaydı bulunamadı. ID: " + id);
                return "redirect:/admin/hasatlar";
            }
        }
        prepareHasatFormModel(model, formDto, null); // Düzenlemede URL'den ekimId gelmez
        return "admin/hasat-form";
    }

    @PostMapping("/kaydet")
    public String saveHasat(@ModelAttribute("hasatForm") HasatFormDto hasatFormDto,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        logger.info("/admin/hasatlar/kaydet POST isteği. Hasat ID: {}, Ekim ID: {}", hasatFormDto.getHasatID(), hasatFormDto.getEkimId());

        // Temel validasyon (daha detaylısı serviste)
        if (hasatFormDto.getEkimId() == null) {
            result.rejectValue("ekimId", "NotNull", "Ekim seçimi zorunludur.");
        }
        if (hasatFormDto.getHasatTarihi() == null || hasatFormDto.getHasatTarihi().trim().isEmpty()) {
            result.rejectValue("hasatTarihi", "NotEmpty", "Hasat tarihi zorunludur.");
        }
        if (hasatFormDto.getToplananMiktar() == null) {
            result.rejectValue("toplananMiktar", "NotNull", "Toplanan miktar boş olamaz.");
        }
         if (hasatFormDto.getBirim() == null || hasatFormDto.getBirim().trim().isEmpty()) {
            result.rejectValue("birim", "NotEmpty", "Birim boş olamaz.");
        }


        if (result.hasErrors()) {
            model.addAttribute("pageTitle", hasatFormDto.getHasatID() == null ? "Admin - Yeni Hasat Kaydı Ekle" : "Admin - Hasat Kaydını Düzenle");
            model.addAttribute("activePage", "hasatlar");
            prepareHasatFormModel(model, hasatFormDto, hasatFormDto.getEkimId()); // Hatalı formu ve dropdownları tekrar yükle
            return "admin/hasat-form";
        }

        try {
            String successMessage;
            if (hasatFormDto.getHasatID() == null) {
                Hasatlar kaydedilen = hasatServis.saveHasatSP(hasatFormDto);
                successMessage = "Hasat başarıyla kaydedildi. ID: " + kaydedilen.getHasatID();
            } else { 
                boolean guncellendi = hasatServis.updateHasatSP(hasatFormDto); // Bu metot serviste olmalı
                if (guncellendi) {
                    successMessage = "Hasat başarıyla güncellendi. ID: " + hasatFormDto.getHasatID();
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Hasat güncellenemedi (Servis false döndü).");
                    redirectAttributes.addFlashAttribute("hasatForm", hasatFormDto);
                    return "redirect:/admin/hasatlar/duzenle/" + hasatFormDto.getHasatID() + "?error";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/admin/hasatlar";
        } catch (IllegalArgumentException e) {
            logger.warn("Hasat kaydı/güncellemesi sırasında validasyon hatası: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("hasatForm", hasatFormDto); // Hatalı formu flash attribute ile taşı
            
            String redirectUrl = (hasatFormDto.getHasatID() != null) ? 
                "/admin/hasatlar/duzenle/" + hasatFormDto.getHasatID() : 
                "/admin/hasatlar/ekle";
            if (hasatFormDto.getHasatID() == null && hasatFormDto.getEkimId() != null) {
                redirectUrl += "?ekimId=" + hasatFormDto.getEkimId(); // Yeni ekleme ise ekimId'yi koru
            }
            return "redirect:" + redirectUrl + (redirectUrl.contains("?") ? "&error" : "?error");

        } catch (Exception e) {
            logger.error("Hasat kaydı/güncellemesi sırasında beklenmedik hata: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Beklenmedik bir hata oluştu: " + e.getMessage());
            redirectAttributes.addFlashAttribute("hasatForm", hasatFormDto);
            
            String redirectUrl = (hasatFormDto.getHasatID() != null) ? 
                "/admin/hasatlar/duzenle/" + hasatFormDto.getHasatID() : 
                "/admin/hasatlar/ekle";
            if (hasatFormDto.getHasatID() == null && hasatFormDto.getEkimId() != null) {
                redirectUrl += "?ekimId=" + hasatFormDto.getEkimId();
            }
            return "redirect:" + redirectUrl + (redirectUrl.contains("?") ? "&error_unexpected" : "?error_unexpected");
        }
    }
    
    @GetMapping("/sil/{id}")
    public String deleteHasat(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        logger.info("/admin/hasatlar/sil/{} GET isteği.", id);
        try {
            boolean silindi = hasatServis.deleteHasatSP(id); // Bu metot serviste olmalı
            if (silindi) {
                redirectAttributes.addFlashAttribute("successMessage", "Hasat başarıyla silindi. ID: " + id);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Hasat silinemedi (Servis false döndü veya kayıt bulunamadı).");
            }
        } catch (Exception e) {
            logger.error("Hasat silme hatası ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Silme işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
        return "redirect:/admin/hasatlar";
    }
}
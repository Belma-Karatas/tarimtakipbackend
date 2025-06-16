package com.example.tarimtakipbackend.controller;

import com.example.tarimtakipbackend.dto.KullaniciDto;
import com.example.tarimtakipbackend.service.KullaniciKayitServis;
import org.slf4j.Logger; // Logger ekle
import org.slf4j.LoggerFactory; // Logger ekle
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class); // Logger ekle

    @Autowired
    private KullaniciKayitServis kullaniciKayitServis;

    @GetMapping("/login")
    public String loginPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            logger.info("/login GET isteği - Login sayfası gösteriliyor.");
            return "login"; // templates/login.html (Bu layout kullanmaz)
        }
        logger.info("/login GET isteği - Kullanıcı zaten login olmuş, dashboard'a yönlendiriliyor.");
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
  public String dashboardPage(Model model) {
    logger.info("/dashboard GET isteği - Dashboard sayfası gösteriliyor.");
    model.addAttribute("activePage", "dashboard"); // Sidebar için hala gerekli
    return "dashboard"; // Sadece view adını döndür
}
 
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            logger.info("/register GET isteği - Kullanıcı zaten login olmuş, dashboard'a yönlendiriliyor.");
            return "redirect:/dashboard";
        }
        logger.info("/register GET isteği - Kayıt formu gösteriliyor.");
        model.addAttribute("kullaniciDto", new KullaniciDto());
        return "register"; // templates/register.html (Bu layout kullanmaz)
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("kullaniciDto") KullaniciDto kullaniciDto,
                                      RedirectAttributes redirectAttributes) {
        logger.info("/register POST isteği - Kullanıcı kaydediliyor: {}", kullaniciDto.getKullaniciAdi());
        try {
            kullaniciKayitServis.kaydetYeniKullanici(kullaniciDto, "Çalışan"); // Rol adını kontrol et
            redirectAttributes.addFlashAttribute("successMessage", "Kayıt başarılı! Lütfen giriş yapınız."); // successMessage olarak değiştirildi
            return "redirect:/login?success";
        } catch (IllegalArgumentException e) {
            logger.warn("Kayıt sırasında IllegalArgumentException: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // errorMessage olarak değiştirildi
            return "redirect:/register?error";
        } catch (Exception e) {
            logger.error("Kayıt sırasında beklenmedik bir hata: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Beklenmedik bir hata oluştu.");
            return "redirect:/register?error";
        }
    }
}
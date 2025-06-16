package com.example.tarimtakipbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SessionContextInterceptor sessionContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor'ı tüm yollara uygulayabiliriz veya sadece belirli yollara.
        // RLS'in her zaman doğru kullanıcı bağlamıyla çalışması için tüm yollara uygulamak iyi bir pratik olabilir.
        registry.addInterceptor(sessionContextInterceptor).addPathPatterns("/**");
        // Veya sadece /admin/** ve /calisan/** gibi RLS'in önemli olduğu yollara:
        // registry.addInterceptor(sessionContextInterceptor).addPathPatterns("/admin/**", "/calisan/**");
    }
}
package com.example.userprojectjdbcsqlserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import com.example.userprojectjdbcsqlserver.security.FunctionAccessInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

// Sans spring-boot-starter-data-jpa (volontairement absent : projet JDBC pur, sans entite),
// l'autoconfiguration Boot qui enregistre PageableHandlerMethodArgumentResolver ne se declenche
// plus toute seule - sans ca, un parametre de controleur de type Pageable echoue au binding
// (Spring tente de le construire comme un bean classique, alors que c'est une interface).
@Configuration
@EnableSpringDataWebSupport
public class WebConfig implements WebMvcConfigurer {

    private final FunctionAccessInterceptor functionAccessInterceptor;

    public WebConfig(FunctionAccessInterceptor functionAccessInterceptor) {
        this.functionAccessInterceptor = functionAccessInterceptor;
    }

    // Verification de la fonction du profil sur les ecrans (@RequiresFunction), avant les controleurs.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(functionAccessInterceptor);
    }

    // Declare explicitement plutot que de compter sur l'autoconfiguration Boot (meme precedent
    // que le cookie de session dans SecurityConfig) : API REST stateless, la langue vient du
    // header Accept-Language envoye par le frontend (pas de cookie/session de langue).
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.FRENCH);
        resolver.setSupportedLocales(List.of(Locale.FRENCH, Locale.ENGLISH, Locale.GERMAN, new Locale("es")));
        return resolver;
    }
}

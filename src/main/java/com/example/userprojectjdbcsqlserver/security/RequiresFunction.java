package com.example.userprojectjdbcsqlserver.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * L'ecran servi par cet endpoint exige que le profil de l'utilisateur possede l'une des fonctions listees
 * (semantique "l'une des") ; sinon 403 (FUNCTION_ACCESS_DENIED) et ligne WARN dans le journal.
 *
 * Pose sur la classe du controleur (valeur par defaut de tous ses endpoints) ; une annotation sur une methode
 * la remplace (ex. un endpoint "lookup" utilise par plusieurs ecrans). Verifiee par FunctionAccessInterceptor
 * AVANT le controle de permissions (@PreAuthorize) : la fonction donne le menu, la permission donne le droit d'agir.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFunction {
    String[] value();
}

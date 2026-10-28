package com.example.userprojectjdbcsqlserver.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Endpoint personnel (authentification, profil de l'utilisateur connecte) : aucune fonction de profil requise.
 * Existe pour que "aucune annotation" ne puisse jamais etre un oubli silencieux : tout endpoint d'un controleur
 * porte soit @RequiresFunction, soit @FunctionExempt (verifie par un test).
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionExempt {
}

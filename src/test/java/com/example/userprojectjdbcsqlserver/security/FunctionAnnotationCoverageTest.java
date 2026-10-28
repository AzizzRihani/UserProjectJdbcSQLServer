package com.example.userprojectjdbcsqlserver.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou "fail-closed" : un endpoint sans @RequiresFunction ni @FunctionExempt serait ouvert a tout utilisateur
 * connecte, quel que soit son profil (le menu ne servirait a rien). Ce test echoue des qu'un nouveau endpoint est
 * ajoute sans choisir explicitement l'un des deux. Il analyse les classes par reflexion : ni contexte Spring ni base.
 */
class FunctionAnnotationCoverageTest {

    private static final String CONTROLLER_PACKAGE = "com.example.userprojectjdbcsqlserver.controller";

    @Test
    void everyEndpointDeclaresItsFunctionOrIsExplicitlyExempt() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> unprotected = new ArrayList<>();
        int endpoints = 0;
        for (BeanDefinition definition : scanner.findCandidateComponents(CONTROLLER_PACKAGE)) {
            Class<?> controller = Class.forName(definition.getBeanClassName());
            boolean classLevel = AnnotatedElementUtils.hasAnnotation(controller, RequiresFunction.class)
                    || AnnotatedElementUtils.hasAnnotation(controller, FunctionExempt.class);
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                endpoints++;
                boolean methodLevel = AnnotatedElementUtils.hasAnnotation(method, RequiresFunction.class)
                        || AnnotatedElementUtils.hasAnnotation(method, FunctionExempt.class);
                if (!classLevel && !methodLevel) {
                    unprotected.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertTrue(endpoints > 0, "aucun endpoint trouve : le scan du package a echoue");
        assertTrue(unprotected.isEmpty(), "Endpoints sans @RequiresFunction ni @FunctionExempt : " + unprotected);
    }

    @Test
    void personalEndpointsAreExemptAndScreensRequireAFunction() throws Exception {
        assertTrue(AnnotatedElementUtils.hasAnnotation(Class.forName(CONTROLLER_PACKAGE + ".AuthController"), FunctionExempt.class));
        Class<?> users = Class.forName(CONTROLLER_PACKAGE + ".UserController");
        assertTrue(AnnotatedElementUtils.hasAnnotation(users, RequiresFunction.class));
        assertTrue(AnnotatedElementUtils.hasAnnotation(users.getMethod("me", SecurityUser.class), FunctionExempt.class));
        // Un exempt sur une methode ne doit pas se retrouver par erreur sur un ecran.
        assertFalse(AnnotatedElementUtils.hasAnnotation(users.getMethod("findAll", SecurityUser.class), FunctionExempt.class));
    }
}

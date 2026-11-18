package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

// 400 : la valeur proposee ne respecte pas la definition du parametre (type, plage, longueur). Un constructeur par
// cas pour que chaque refus ait son propre message traduit.
public class ParameterValueInvalidException extends ApiException {

    private ParameterValueInvalidException(String messageKey, Object... args) {
        super(HttpStatus.BAD_REQUEST, messageKey, args);
    }

    public static ParameterValueInvalidException notAnInteger(String code) {
        return new ParameterValueInvalidException("error.parameter.value.int", code);
    }

    public static ParameterValueInvalidException notABoolean(String code) {
        return new ParameterValueInvalidException("error.parameter.value.boolean", code);
    }

    public static ParameterValueInvalidException outOfRange(String code, int min, int max) {
        return new ParameterValueInvalidException("error.parameter.value.range", code, min, max);
    }

    public static ParameterValueInvalidException tooLong(String code, int max) {
        return new ParameterValueInvalidException("error.parameter.value.length", code, max);
    }
}

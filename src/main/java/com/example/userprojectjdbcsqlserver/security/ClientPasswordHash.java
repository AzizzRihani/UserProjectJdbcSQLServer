package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.exception.PasswordFormatInvalidException;

import java.util.regex.Pattern;

// Le navigateur n'envoie jamais le mot de passe en clair : il envoie un hash Argon2id de 32 octets ecrit
// en 64 caracteres hexa MINUSCULES. Le serveur traite cette chaine comme "le mot de passe" et la re-hache
// avec son propre Argon2id sale (PasswordEncoder). Format strict : refuser tout le reste evite qu'un
// ancien client envoie un mot de passe en clair qui serait stocke, et fixe une representation unique
// (une casse differente donnerait un autre hash serveur).
public final class ClientPasswordHash {

    private static final Pattern FORMAT = Pattern.compile("^[0-9a-f]{64}$");

    private ClientPasswordHash() {
    }

    public static boolean isValid(String value) {
        return value != null && FORMAT.matcher(value).matches();
    }

    public static String requireValid(String value) {
        if (!isValid(value)) {
            throw new PasswordFormatInvalidException();
        }
        return value;
    }
}

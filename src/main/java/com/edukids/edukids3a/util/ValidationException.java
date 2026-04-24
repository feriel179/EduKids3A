package com.edukids.edukids3a.util;

/**
 * Erreur de validation formulaire (métier), distincte des erreurs techniques.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}

package com.example.pidev.model.validation;

import java.util.regex.Pattern;

public class TaxIdValidator {

    // Exemple de regex pour un numéro fiscal tunisien simple : 7 chiffres suivis d'une lettre majuscule
    private static final Pattern TAX_ID_PATTERN = Pattern.compile("^[0-9]{7}[A-Z]$");

    // Variante plus large : 7 à 13 caractères alphanumériques (selon ce que vous acceptez)
    // private static final Pattern TAX_ID_PATTERN = Pattern.compile("^[A-Z0-9]{7,13}$");

    /**
     * Valide le format d'un numéro fiscal.
     * @param taxId le numéro à valider
     * @return true si le format est correct, false sinon
     */
    public static boolean isValid(String taxId) {
        if (taxId == null || taxId.isBlank()) return false;
        return TAX_ID_PATTERN.matcher(taxId.trim()).matches();
    }

    /**
     * Nettoie la chaîne (supprime les espaces, met en majuscules) avant validation.
     * @param taxId le numéro brut
     * @return true si valide après nettoyage
     */
    public static boolean isValidNormalized(String taxId) {
        if (taxId == null || taxId.isBlank()) return false;
        String cleaned = taxId.trim().toUpperCase().replaceAll("\\s+", "");
        return TAX_ID_PATTERN.matcher(cleaned).matches();
    }
}
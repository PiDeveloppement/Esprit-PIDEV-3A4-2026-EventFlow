package com.example.pidev.utils;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * 🔐 Gestionnaire sécurisé du token Hugging Face
 *
 * Charge le HF_TOKEN depuis (par ordre de priorité):
 * 1. Variables d'environnement système (Production)
 * 2. Fichier .env local (Développement)
 *
 * ⚠️ JAMAIS exposer le token en entier dans les logs
 */
public class HFTokenLoader {

    private static Dotenv dotenv() {
        try {
            return Dotenv.configure().directory(".").ignoreIfMissing().load();
        } catch (Exception e) {
            return null;
        }
    }

    private static String loadHFToken() {
        String hfToken = System.getenv("HF_TOKEN");
        if (hfToken == null || hfToken.isBlank()) {
            Dotenv env = dotenv();
            if (env != null) {
                hfToken = env.get("HF_TOKEN");
            }
        }
        return (hfToken == null || hfToken.isBlank()) ? null : hfToken;
    }

    /**
     * Récupère le token HF (JAMAIS l'afficher entièrement!)
     */
    public static String getHFToken() {
        String token = loadHFToken();
        if (token == null) {
            throw new IllegalStateException("HF_TOKEN non configuré");
        }
        return token;
    }

    /**
     * Affiche le token de manière sécurisée (pour déboguer)
     * Format: hf_xxxxxxxxxx***
     */
    public static String getMaskedToken() {
        String token = getHFToken();
        if (token.length() < 15) {
            return "***";
        }
        return token.substring(0, 10) + "...";
    }

    /**
     * Vérifie que le token est valide
     */
    public static boolean isTokenValid() {
        try {
            String token = getHFToken();
            return token.startsWith("hf_") && token.length() > 20;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Test du chargement
     */
    public static void main(String[] args) {
        try {
            String token = getHFToken();
            System.out.println("✅ Token chargé avec succès");
            System.out.println("   Aperçu: " + getMaskedToken());
            System.out.println("   Valide: " + isTokenValid());
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

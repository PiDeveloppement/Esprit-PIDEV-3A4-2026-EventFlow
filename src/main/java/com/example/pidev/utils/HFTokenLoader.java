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

    private static final Dotenv dotenv = Dotenv.configure()
        .directory(".") // Cherche .env dans la racine du projet
        .ignoreIfMissing() // Ne crash pas si .env n'existe pas
        .load();

    private static final String HF_TOKEN = loadHFToken();

    /**
     * Charger le HF_TOKEN de manière sécurisée
     */
    private static String loadHFToken() {
        // 1. Essayer d'abord depuis les variables d'environnement système (production)
        String hfToken = System.getenv("HF_TOKEN");

        // 2. Si pas trouvé, chercher dans .env (développement)
        if (hfToken == null || hfToken.isEmpty()) {
            hfToken = dotenv.get("HF_TOKEN");
        }

        // 3. Valider
        if (hfToken == null || hfToken.isEmpty()) {
            String errorMsg = "❌ HF_TOKEN non configuré!\n" +
                "Ajoute le dans l'une de ces options:\n" +
                "  1. Variables d'environnement système: setx HF_TOKEN hf_xxxxx\n" +
                "  2. Fichier .env local (jamais à committer): HF_TOKEN=hf_xxxxx";
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        System.out.println("✅ HF_TOKEN chargé depuis: " +
            (System.getenv("HF_TOKEN") != null ? "variables système" : "fichier .env"));

        return hfToken;
    }

    /**
     * Récupère le token HF (JAMAIS l'afficher entièrement!)
     */
    public static String getHFToken() {
        if (HF_TOKEN == null || HF_TOKEN.isEmpty()) {
            throw new RuntimeException("❌ HF_TOKEN vide ou non chargé");
        }
        return HF_TOKEN;
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
        String token = getHFToken();
        return token.startsWith("hf_") && token.length() > 20;
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


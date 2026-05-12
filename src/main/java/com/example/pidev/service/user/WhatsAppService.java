package com.example.pidev.service.user;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class WhatsAppService {

    // Vos identifiants Twilio chargés depuis les variables d'environnement
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");

    // Numéro Sandbox WhatsApp (fourni par Twilio)
    private static final String WHATSAPP_SANDBOX_NUMBER = "+14155238886";

    // Code d'invitation pour le sandbox (personnalisez-le depuis votre console)
    private static final String SANDBOX_INVITE_CODE = "orange-popsicle";

    static {
        // Vérification que les variables d'environnement sont bien définies
        if (ACCOUNT_SID == null || AUTH_TOKEN == null) {
            System.err.println("❌ ERREUR: Variables d'environnement TWILIO manquantes!");
            System.err.println("Veuillez définir TWILIO_ACCOUNT_SID et TWILIO_AUTH_TOKEN");
            System.err.println("Sous Windows: setx TWILIO_ACCOUNT_SID \"votre_sid\"");
            System.err.println("Sous Linux/Mac: export TWILIO_ACCOUNT_SID=\"votre_sid\"");
        } else {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            System.out.println("✅ WhatsApp Service initialisé avec les variables d'environnement");
            System.out.println("📱 Sandbox: envoyez 'join " + SANDBOX_INVITE_CODE + "' au " + WHATSAPP_SANDBOX_NUMBER);
        }
    }

    /**
     * Envoie un message de réinitialisation via WhatsApp
     */
    public static void sendResetPasswordWhatsApp(String toPhoneNumber, String token) {
        try {
            // Vérifier que Twilio est initialisé
            if (ACCOUNT_SID == null || AUTH_TOKEN == null) {
                System.err.println("❌ Impossible d'envoyer: variables d'environnement Twilio manquantes");
                return;
            }

            String cleanNumber = formatPhoneNumber(toPhoneNumber);

            // ✅ Plus de lien web ! Juste le token
            String messageBody =
                    "🔐 *RÉINITIALISATION MOT DE PASSE EVENTFLOW*\n\n" +
                            "Bonjour,\n\n" +
                            "Voici votre code de réinitialisation :\n\n" +
                            "📱 *TOKEN :* `" + token + "`\n\n" +
                            "📋 *INSTRUCTIONS :*\n" +
                            "1. Ouvrez l'application EventFlow\n" +
                            "2. Cliquez sur 'Mot de passe oublié'\n" +
                            "3. Entrez votre numéro de téléphone\n" +
                            "4. Sur l'écran suivant, collez ce token\n\n" +
                            "⏱️ Ce token expirera dans *1 heure*\n\n" +
                            "Merci,\n" +
                            "L'équipe EventFlow";

            // Envoyer via WhatsApp
            Message message = Message.creator(
                            new PhoneNumber("whatsapp:" + cleanNumber),
                            new PhoneNumber("whatsapp:" + WHATSAPP_SANDBOX_NUMBER),
                            messageBody)
                    .create();

            System.out.println("✅ Token envoyé sur WhatsApp !");
            System.out.println("🔑 Token: " + token);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi WhatsApp: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Formate le numéro de téléphone
     */
    private static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        // Nettoyer
        String clean = phoneNumber.replaceAll("[^0-9+]", "");

        // Ajouter indicatif si nécessaire
        if (clean.matches("\\d{8}")) {
            clean = "+216" + clean;
        } else if (!clean.startsWith("+")) {
            clean = "+" + clean;
        }

        return clean;
    }

    /**
     * Vérifie si un numéro a rejoint le sandbox
     */
    public static boolean isNumberJoinedSandbox(String phoneNumber) {
        // Implémentez la vérification si nécessaire
        // Par défaut, on suppose que oui pour les tests
        return true;
    }

    /**
     * Méthode utilitaire pour tester la configuration
     */
    public static boolean isConfigured() {
        return ACCOUNT_SID != null && AUTH_TOKEN != null;
    }
}
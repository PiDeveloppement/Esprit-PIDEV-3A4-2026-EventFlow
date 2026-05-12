package com.example.pidev.service.user;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    // ✅ REMPLACEZ CES VALEURS AVEC VOS IDENTIFIANTS GMAIL
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USERNAME = "sellamiarij7@gmail.com"; // Votre adresse Gmail
    private static final String SMTP_PASSWORD = "uern jvab upaa wolm"; // Le mot de passe à 16 caractères
    private static final String FROM_EMAIL = "sellamiarij7@gmail.com"; // Votre adresse Gmail (la même)

    static {
        System.out.println("📧 Service d'email initialisé");
    }

    /**
     * Envoie un email de bienvenue après inscription
     */
    public static void sendWelcomeEmail(String toEmail, String userName) {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true"); // Pour le debug (optionnel)

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
        session.setDebug(true); // Affiche les logs de communication SMTP

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🎉 Bienvenue sur EventFlow !");

            // Contenu HTML de l'email
            String htmlContent = getWelcomeEmailTemplate(userName);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            System.out.println("📧 Envoi d'email de bienvenue à: " + toEmail);
            Transport.send(message);

            System.out.println("✅ Email de bienvenue envoyé avec succès!");

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Envoie une notification à tous les admins pour chaque nouvelle inscription
     */
    public static void sendNewUserNotificationToAdmin(com.example.pidev.model.user.UserModel newUser) {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        try {
            // Récupérer tous les emails des admins
            UserService userService = new UserService();
            List<String> adminEmails = userService.getAllAdminEmails();

            if (adminEmails.isEmpty()) {
                System.out.println("⚠️ Aucun admin trouvé pour notifier");
                return;
            }

            // Convertir la liste en chaîne avec des virgules
            String adminEmailsString = String.join(",", adminEmails);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminEmailsString));
            message.setSubject("📢 Nouvel utilisateur inscrit sur EventFlow");

            // Contenu HTML de la notification
            String htmlContent = getAdminNotificationTemplate(newUser);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            System.out.println("📧 Envoi notification à " + adminEmails.size() + " admin(s)");
            Transport.send(message);

            System.out.println("✅ Notification envoyée aux admins pour: " + newUser.getEmail());

        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi notification admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ Template HTML pour la notification admin
     */
    private static String getAdminNotificationTemplate(com.example.pidev.model.user.UserModel user) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: auto; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 15px; padding: 40px; color: white; }" +
                "        .header { text-align: center; margin-bottom: 30px; }" +
                "        .header h1 { font-size: 32px; margin: 0; }" +
                "        .header p { font-size: 16px; opacity: 0.9; }" +
                "        .content { background: white; border-radius: 10px; padding: 30px; color: #333; }" +
                "        .user-info { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea; }" +
                "        .info-row { margin: 12px 0; padding: 5px 0; border-bottom: 1px solid #eee; display: flex; }" +
                "        .info-label { font-weight: bold; color: #667eea; width: 120px; }" +
                "        .info-value { flex: 1; }" +
                "        .stats { display: flex; justify-content: space-around; margin: 30px 0; }" +
                "        .stat-box { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; text-align: center; min-width: 120px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                "        .stat-number { font-size: 28px; font-weight: bold; }" +
                "        .stat-label { font-size: 12px; opacity: 0.9; margin-top: 5px; }" +
                "        .button { display: inline-block; background: #667eea; color: white; text-decoration: none; padding: 12px 30px; border-radius: 25px; font-weight: bold; margin: 20px 0; }" +
                "        .button:hover { background: #5a67d8; }" +
                "        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>📢 Nouvelle Inscription</h1>" +
                "            <p>Un nouvel utilisateur a rejoint EventFlow</p>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <h2 style='margin-top: 0; color: #333;'>Détails du nouvel inscrit</h2>" +
                "            <div class='user-info'>" +
                "                <div class='info-row'>" +
                "                    <span class='info-label'>👤 Nom complet:</span>" +
                "                    <span class='info-value'><strong>" + user.getFirst_Name() + " " + user.getLast_Name() + "</strong></span>" +
                "                </div>" +
                "                <div class='info-row'>" +
                "                    <span class='info-label'>📧 Email:</span>" +
                "                    <span class='info-value'>" + user.getEmail() + "</span>" +
                "                </div>" +
                "                <div class='info-row'>" +
                "                    <span class='info-label'>📱 Téléphone:</span>" +
                "                    <span class='info-value'>" + (user.getPhone() != null ? user.getPhone() : "Non renseigné") + "</span>" +
                "                </div>" +
                "                <div class='info-row'>" +
                "                    <span class='info-label'>🏫 Faculté:</span>" +
                "                    <span class='info-value'>" + (user.getFaculte() != null ? user.getFaculte() : "Non renseignée") + "</span>" +
                "                </div>" +
                "                <div class='info-row'>" +
                "                    <span class='info-label'>📅 Date:</span>" +
                "                    <span class='info-value'>" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "                </div>" +
                "            </div>" +
                "            <div class='stats'>" +
                "                <div class='stat-box'>" +
                "                    <div class='stat-number'>" + getTotalUsersCount() + "</div>" +
                "                    <div class='stat-label'>Total utilisateurs</div>" +
                "                </div>" +
                "                <div class='stat-box'>" +
                "                    <div class='stat-number'>" + getNewUsersThisMonth() + "</div>" +
                "                    <div class='stat-label'>Inscrits ce mois</div>" +
                "                </div>" +
                "            </div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>© 2024 EventFlow - Plateforme de gestion d'événements</p>" +
                "            <p style='font-size: 12px; margin-top: 10px;'>Cet email est envoyé automatiquement à chaque nouvelle inscription.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Template HTML pour l'email de bienvenue
     */
    private static String getWelcomeEmailTemplate(String userName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: auto; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 15px; padding: 40px; color: white; }" +
                "        .header { text-align: center; margin-bottom: 30px; }" +
                "        .header h1 { font-size: 32px; margin: 0; }" +
                "        .content { background: white; border-radius: 10px; padding: 30px; color: #333; }" +
                "        .welcome-message { font-size: 18px; line-height: 1.6; }" +
                "        .features { margin: 25px 0; }" +
                "        .feature-item { margin: 10px 0; padding-left: 25px; position: relative; }" +
                "        .feature-item:before { content: '✅'; position: absolute; left: 0; }" +
                "        .button { display: inline-block; background: #667eea; color: white; text-decoration: none; padding: 12px 30px; border-radius: 25px; font-weight: bold; margin-top: 20px; }" +
                "        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }" +
                "        .social-links { margin-top: 20px; }" +
                "        .social-links a { color: white; text-decoration: none; margin: 0 10px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🎉 Bienvenue sur EventFlow !</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <div class='welcome-message'>" +
                "                <h2>Bonjour " + userName + ",</h2>" +
                "                <p>Nous sommes ravis de vous accueillir sur EventFlow, votre plateforme de gestion d'événements préférée !</p>" +
                "            </div>" +
                "            <div class='features'>" +
                "                <h3>🎯 Ce qui vous attend :</h3>" +
                "                <div class='feature-item'>Créez et gérez vos événements facilement</div>" +
                "                <div class='feature-item'>Suivez vos participants en temps réel</div>" +
                "                <div class='feature-item'>Recevez des notifications importantes</div>" +
                "                <div class='feature-item'>Collaborez avec votre équipe</div>" +
                "            </div>"+
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>© 2024 EventFlow. Tous droits réservés.</p>" +
                "            <div class='social-links'>" +
                "                <a href='#'>Facebook</a> | " +
                "                <a href='#'>Twitter</a> | " +
                "                <a href='#'>LinkedIn</a>" +
                "            </div>" +
                "            <p style='margin-top: 15px; font-size: 12px;'>" +
                "                Cet email a été envoyé suite à votre inscription sur EventFlow.<br>" +
                "                Si vous n'êtes pas à l'origine de cette inscription, ignorez cet email." +
                "            </p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * ✅ Méthodes utilitaires pour les statistiques
     */
    private static int getTotalUsersCount() {
        try {
            UserService userService = new UserService();
            return userService.getTotalParticipantsCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getNewUsersThisMonth() {
        try {
            UserService userService = new UserService();
            return userService.getNewUsersThisMonthCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
package com.example.pidev.utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailUtils {

    public static void envoyerMailConfirmation(String destinataire, String sujet, String contenu) {
        Logger.getLogger("javax.mail").setLevel(Level.SEVERE);
        // 1. Configuration du serveur Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // 2. Connexion avec tes identifiants
        String monEmail = "jridighofrane48@gmail.com";
        String mdpApplication = "dtnq scaa rcnd iqxz"; // Le code généré à l'étape 1

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(monEmail, mdpApplication);
            }
        });

        try {
            // 3. Création du message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(monEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setText(contenu);

            // 4. Envoi
            Transport.send(message);
            System.out.println("✅ Email envoyé !");
        } catch (MessagingException e) {
            System.out.println("❌ Erreur d'envoi : " + e.getMessage());
        }
    }
}
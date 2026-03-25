package com.example.pidev.service.chat;

import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.user.UserService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatbotService {

    private final UserService userService;

    public ChatbotService() {
        this.userService = new UserService();
    }

    /**
     * Traite la question de l'utilisateur et retourne une réponse
     */
    public String processQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "Veuillez poser une question.";
        }

        String lowercaseQuestion = question.toLowerCase().trim();

        // Règle 1: Demande de nombre total d'utilisateurs
        if (containsAny(lowercaseQuestion, "combien", "nombre", "total") &&
                containsAny(lowercaseQuestion, "utilisateur", "inscrit", "compte")) {
            return getTotalUsersResponse();
        }

        // Règle 2: Demande de la liste des admins
        if (containsAny(lowercaseQuestion, "liste", "affiche", "montre") &&
                containsAny(lowercaseQuestion, "admin", "administrateur")) {
            return getAdminListResponse();
        }

        // Règle 3: Demande des nouveaux utilisateurs du mois
        if (containsAny(lowercaseQuestion, "nouveau", "nouveaux", "ce mois") &&
                containsAny(lowercaseQuestion, "inscrit", "utilisateur")) {
            return getNewUsersThisMonthResponse();
        }

        // Règle 4: Recherche du rôle d'un utilisateur spécifique
        if (containsAny(lowercaseQuestion, "rôle", "role", "est", "qui est") &&
                containsAny(lowercaseQuestion, "utilisateur", "email")) {
            return getUserRoleResponse(question);
        }

        // Règle 5: Suppression d'utilisateur (avec confirmation)
        if (containsAny(lowercaseQuestion, "supprimer", "effacer", "delete", "enlever") &&
                containsAny(lowercaseQuestion, "utilisateur", "compte", "email")) {
            return getDeleteUserConfirmation(question);
        }

        // Règle 6: Statistiques avancées
        if (containsAny(lowercaseQuestion, "statistique", "stats", "graphique", "aperçu")) {
            return getStatsResponse();
        }

        // Règle 7: Informations sur un utilisateur spécifique
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher emailMatcher = emailPattern.matcher(question);
        if (emailMatcher.find()) {
            return getUserInfoResponse(emailMatcher.group());
        }

        // Règle 8: Aide / Commandes disponibles
        if (containsAny(lowercaseQuestion, "aide", "help", "commandes", "peux-tu", "quoi")) {
            return getHelpResponse();
        }

        return "Je n'ai pas compris votre question. Tapez 'aide' pour voir les commandes disponibles.";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String getTotalUsersResponse() {
        int total = userService.getTotalParticipantsCount();
        return "📊 **Nombre total d'utilisateurs** :\n" +
                "Il y a actuellement **" + total + "** utilisateur(s) inscrit(s) sur la plateforme.";
    }

    private String getAdminListResponse() {
        List<String> admins = userService.getAllAdminEmails();
        if (admins.isEmpty()) {
            return "⚠️ Aucun administrateur trouvé dans la base de données.";
        }

        StringBuilder response = new StringBuilder("👥 **Liste des administrateurs** :\n");
        for (int i = 0; i < admins.size(); i++) {
            response.append(i + 1).append(". ").append(admins.get(i)).append("\n");
        }
        response.append("\nTotal : **").append(admins.size()).append("** admin(s)");
        return response.toString();
    }

    private String getNewUsersThisMonthResponse() {
        int newUsers = userService.getNewUsersThisMonthCount();
        return "📅 **Nouveaux utilisateurs ce mois-ci** :\n" +
                "**" + newUsers + "** nouvel(s) utilisateur(s) se sont inscrits ce mois-ci.";
    }

    private String getUserRoleResponse(String question) {
        // Essayer d'extraire un email de la question
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(question);

        if (matcher.find()) {
            String email = matcher.group();
            return getUserInfoResponse(email);
        }

        return "Pour connaître le rôle d'un utilisateur, veuillez préciser son email.\n" +
                "Exemple : 'Quel est le rôle de sellamiarij7@gmail.com ?'";
    }

    private String getUserInfoResponse(String email) {
        UserModel user = userService.getUserByEmail(email);

        if (user != null) {
            String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Non défini";
            String phone = user.getPhone() != null ? user.getPhone() : "Non renseigné";
            String faculte = user.getFaculte() != null ? user.getFaculte() : "Non renseignée";

            return "👤 **Informations pour " + email + "** :\n" +
                    "• Nom : " + user.getFirst_Name() + " " + user.getLast_Name() + "\n" +
                    "• Rôle : **" + roleName + "**\n" +
                    "• Faculté : " + faculte + "\n" +
                    "• Téléphone : " + phone + "\n" +
                    "• ID : " + user.getId_User();
        } else {
            return "❌ Aucun utilisateur trouvé avec l'email : " + email;
        }
    }

    private String getDeleteUserConfirmation(String question) {
        // Extraire l'email de la question
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(question);

        if (matcher.find()) {
            String email = matcher.group();
            UserModel user = userService.getUserByEmail(email);

            if (user != null) {
                // Retourner une demande de confirmation (sera traitée par le contrôleur)
                return "CONFIRM_DELETE:" + user.getId_User() + ":" + email;
            } else {
                return "❌ Aucun utilisateur trouvé avec l'email : " + email;
            }
        }

        return "Pour supprimer un utilisateur, veuillez préciser son email.\n" +
                "Exemple : 'Supprimer l'utilisateur avec l'email test@example.com'";
    }

    private String getStatsResponse() {
        int total = userService.getTotalParticipantsCount();
        int newThisMonth = userService.getNewUsersThisMonthCount();
        List<String> admins = userService.getAllAdminEmails();
        int faculteCount = userService.getAllFacultes().size();

        return "📈 **Statistiques de la plateforme** :\n\n" +
                "• **Total utilisateurs** : " + total + "\n" +
                "• **Nouveaux ce mois** : " + newThisMonth + "\n" +
                "• **Nombre d'admins** : " + admins.size() + "\n" +
                "• **Facultés représentées** : " + faculteCount + "\n" +
                "• **Taux de croissance** : " + calculateGrowthRate() + "%";
    }

    private String calculateGrowthRate() {
        // Calcul simple du taux de croissance
        int newThisMonth = userService.getNewUsersThisMonthCount();
        int total = userService.getTotalParticipantsCount();
        if (total == 0) return "0";
        double rate = (double) newThisMonth / total * 100;
        return String.format("%.1f", rate);
    }

    private String getHelpResponse() {
        return "🤖 **Commandes disponibles** :\n\n" +
                "📊 **Statistiques**\n" +
                "  • \"Combien d'utilisateurs ?\"\n" +
                "  • \"Statistiques\"\n\n" +
                "👥 **Administrateurs**\n" +
                "  • \"Liste des admins\"\n\n" +
                "🔍 **Recherche utilisateur**\n" +
                "  • \"Rôle de email@example.com\"\n" +
                "  • \"Infos sur email@example.com\"\n\n" +
                "📅 **Nouveautés**\n" +
                "  • \"Nouveaux utilisateurs ce mois\"\n\n" +
                "🗑️ **Suppression**\n" +
                "  • \"Supprimer email@example.com\"\n\n" +
                "❓ **Aide**\n" +
                "  • \"Aide\" ou \"Commandes\"";
    }
}
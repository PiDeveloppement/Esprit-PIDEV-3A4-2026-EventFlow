package com.example.pidev.controller.chat;

import com.example.pidev.model.event.Event;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.user.UserService;
import com.example.pidev.service.role.RoleService;
import com.example.pidev.service.resource.ReservationService;
import com.example.pidev.service.sponsor.SponsorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ChatController {

    @FXML private VBox chatBox;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private ScrollPane scrollPane;
    @FXML private Label statusIndicator;

    private UserService userService;
    private EventService eventService;
    private RoleService roleService;
    private ReservationService resourceService;
    private SponsorService sponsorService;

    // État pour la confirmation de suppression
    private boolean awaitingDeleteConfirmation = false;
    private int pendingDeleteUserId = -1;
    private String pendingDeleteEmail = "";

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
            eventService = new EventService();
            roleService = new RoleService();
            resourceService = new ReservationService();
            sponsorService = new SponsorService();

            System.out.println("✅ Services initialisés dans ChatController");
        } catch (SQLException e) {
            System.err.println("❌ Erreur initialisation services: " + e.getMessage());
            e.printStackTrace();
        }

        // Configuration de l'envoi avec Entrée
        inputField.setOnAction(event -> handleSendMessage());

        // Auto-scroll vers le bas
        chatBox.heightProperty().addListener((observable, oldValue, newValue) ->
                scrollPane.setVvalue(1.0));

        // Message de bienvenue
        addWelcomeMessage();

        System.out.println("✅ ChatController initialisé");
    }

    private void addWelcomeMessage() {
        String welcome = "👋 Bonjour ! Je suis votre assistant de gestion.\n\n" +
                "📌 **Questions possibles :**\n\n" +
                "**👥 Utilisateurs :**\n" +
                "• Combien d'utilisateurs sont inscrits ?\n" +
                "• Liste des administrateurs\n" +
                "• Nouveaux utilisateurs ce mois\n" +
                "• Rôle de [email]\n" +
                "• Utilisateurs par rôle\n" +
                "• Statistiques utilisateurs\n\n" +
                "**📅 Événements :**\n" +
                "• Événements à venir\n" +
                "• Événements en cours\n" +
                "• Événements terminés\n" +
                "• Événements gratuits\n" +
                "• Événements payants\n" +
                "• Prochain événement\n" +
                "• Événements par catégorie\n" +
                "• Capacité totale des événements\n\n" +
                "**💼 Sponsors :**\n" +
                "• Nombre de sponsors\n" +
                "• Sponsors actifs\n" +
                "• Budget total des sponsors\n\n" +
                "**📦 Ressources :**\n" +
                "• Ressources disponibles\n" +
                "• Équipements disponibles\n" +
                "• Salles disponibles\n" +
                "• Réservations en cours\n\n" +
                "**📊 Statistiques globales :**\n" +
                "• Statistiques complètes\n" +
                "• Rapport d'activité\n" +
                "• Taux de participation\n" +
                "• Événements par statut\n\n" +
                "**⚙️ Actions :**\n" +
                "• Supprimer utilisateur [email]\n" +
                "• Aide\n" +
                "• Commandes";

        addMessageToChat("Assistant", welcome, Pos.CENTER_LEFT);
    }

    @FXML
    private void handleSendMessage() {
        String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty()) return;

        // Afficher le message de l'utilisateur
        addMessageToChat("Vous", userMessage, Pos.CENTER_RIGHT);
        inputField.clear();

        // Vérifier si on attend une confirmation de suppression
        if (awaitingDeleteConfirmation) {
            handleDeleteConfirmation(userMessage);
            return;
        }

        // Traiter la question
        processUserQuestion(userMessage);
    }

    private void processUserQuestion(String question) {
        new Thread(() -> {
            try {
                String response = generateResponse(question.toLowerCase());
                Platform.runLater(() -> {
                    addMessageToChat("Assistant", response, Pos.CENTER_LEFT);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    addMessageToChat("Assistant", "❌ Désolé, une erreur s'est produite.", Pos.CENTER_LEFT);
                });
            }
        }).start();
    }

    private String generateResponse(String question) {

        // ==================== QUESTIONS SUR LES UTILISATEURS ====================
        if (question.contains("combien") && question.contains("utilisateur")) {
            int count = userService.getTotalParticipantsCount();
            return "📊 **Total utilisateurs** :\n\n" +
                    "Il y a actuellement **" + count + "** utilisateurs inscrits sur la plateforme.";
        }

        else if (question.contains("liste") && question.contains("admin")) {
            List<UserModel> admins = userService.getUsersByRole("admin");
            if (admins.isEmpty()) {
                return "📋 Aucun administrateur trouvé.";
            }
            StringBuilder response = new StringBuilder("👥 **Liste des administrateurs** :\n\n");
            for (UserModel admin : admins) {
                response.append("• **").append(admin.getFirst_Name()).append(" ").append(admin.getLast_Name())
                        .append("** (").append(admin.getEmail()).append(")\n");
            }
            return response.toString();
        }

        else if (question.contains("nouveau") && question.contains("mois")) {
            int count = userService.getNewUsersThisMonthCount();
            return "📅 **Nouveaux utilisateurs** :\n\n" +
                    "**" + count + "** nouveaux utilisateurs se sont inscrits ce mois-ci.";
        }

        else if (question.contains("rôle") && question.contains("@")) {
            // Extraire l'email de la question
            String email = extractEmail(question);
            if (email == null) {
                return "❌ Veuillez fournir un email valide. Exemple: 'Rôle de user@example.com'";
            }
            UserModel user = userService.getUserByEmail(email);
            if (user == null) {
                return "❌ Aucun utilisateur trouvé avec l'email: " + email;
            }
            String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "Non défini";
            return "👤 **Rôle de " + email + "** :\n\n" +
                    "Rôle actuel : **" + roleName + "**";
        }

        else if (question.contains("utilisateur") && question.contains("par rôle")) {
            List<Object[]> stats = userService.getUsersCountByRole();
            StringBuilder response = new StringBuilder("📊 **Utilisateurs par rôle** :\n\n");
            for (Object[] stat : stats) {
                response.append("• **").append(stat[0]).append("** : ").append(stat[1]).append(" utilisateur(s)\n");
            }
            return response.toString();
        }

        // ==================== QUESTIONS SUR LES ÉVÉNEMENTS ====================
        else if (question.contains("événement") && question.contains("venir")) {
            List<Event> events = eventService.getUpcomingEvents();
            if (events.isEmpty()) {
                return "📅 Aucun événement à venir.";
            }
            StringBuilder response = new StringBuilder("📅 **Événements à venir** :\n\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Event event : events) {
                response.append("• **").append(event.getTitle()).append("**\n")
                        .append("  📍 ").append(event.getLocation()).append("\n")
                        .append("  🕐 ").append(event.getStartDate().format(formatter)).append("\n")
                        .append("  🎟️ ").append(event.getCapacity()).append(" places\n\n");
            }
            return response.toString();
        }

        else if (question.contains("événement") && question.contains("cours")) {
            List<Event> events = eventService.getEventsByStatus("ONGOING");
            if (events.isEmpty()) {
                return "📅 Aucun événement en cours.";
            }
            StringBuilder response = new StringBuilder("📅 **Événements en cours** :\n\n");
            for (Event event : events) {
                response.append("• **").append(event.getTitle()).append("**\n");
            }
            return response.toString();
        }

        else if (question.contains("événement") && question.contains("terminé")) {
            List<Event> events = eventService.getEventsByStatus("COMPLETED");
            if (events.isEmpty()) {
                return "📅 Aucun événement terminé.";
            }
            StringBuilder response = new StringBuilder("📅 **Événements terminés** :\n\n");
            for (Event event : events) {
                response.append("• **").append(event.getTitle()).append("**\n");
            }
            return response.toString();
        }

        else if (question.contains("événement") && question.contains("gratuit")) {
            List<Event> events = eventService.getFreeEvents();
            if (events.isEmpty()) {
                return "🎫 Aucun événement gratuit trouvé.";
            }
            StringBuilder response = new StringBuilder("🎫 **Événements gratuits** :\n\n");
            for (Event event : events) {
                response.append("• **").append(event.getTitle()).append("**\n");
            }
            return response.toString();
        }

        else if (question.contains("événement") && question.contains("payant")) {
            List<Event> events = eventService.getPaidEvents();
            if (events.isEmpty()) {
                return "💰 Aucun événement payant trouvé.";
            }
            StringBuilder response = new StringBuilder("💰 **Événements payants** :\n\n");
            for (Event event : events) {
                response.append("• **").append(event.getTitle()).append("** - ")
                        .append(event.getTicketPrice()).append(" DT\n");
            }
            return response.toString();
        }

        else if (question.contains("prochain") && question.contains("événement")) {
            Event nextEvent = eventService.getNextEvent();
            if (nextEvent == null) {
                return "📅 Aucun événement à venir.";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return "📅 **Prochain événement** :\n\n" +
                    "**" + nextEvent.getTitle() + "**\n" +
                    "📍 " + nextEvent.getLocation() + "\n" +
                    "🕐 " + nextEvent.getStartDate().format(formatter) + "\n" +
                    "🎟️ " + nextEvent.getCapacity() + " places\n" +
                    (nextEvent.isFree() ? "🎫 Gratuit" : "💰 " + nextEvent.getTicketPrice() + " DT");
        }

        else if (question.contains("capacité") && question.contains("totale")) {
            int totalCapacity = eventService.getTotalCapacity();
            return "📊 **Capacité totale** :\n\n" +
                    "La capacité totale de tous les événements est de **" + totalCapacity + "** places.";
        }

        // ==================== QUESTIONS SUR LES SPONSORS ====================
        else if (question.contains("nombre") && question.contains("sponsor")) {
            int count = 0;
            try {
                count = sponsorService.getTotalSponsors();
            } catch (SQLException e) {
                System.err.println("❌ Erreur SQL getTotalSponsors: " + e.getMessage());
                e.printStackTrace();
            }
            return "💼 **Sponsors** :\n\n" +
                    "Il y a actuellement **" + count + "** sponsors.";
        }

        else if (question.contains("sponsor") && question.contains("actif")) {
            int count = 0;
            count = sponsorService.getActiveSponsors();
            return "✅ **Sponsors actifs** :\n\n" +
                    "**" + count + "** sponsors sont actuellement actifs.";
        }

        else if (question.contains("budget") && question.contains("sponsor")) {
            double totalBudget = 0.0;
            totalBudget = sponsorService.getTotalBudget();
            return "💰 **Budget total des sponsors** :\n\n" +
                    "**" + String.format("%.2f", totalBudget) + " DT**";
        }

        // ==================== QUESTIONS SUR LES RESSOURCES ====================
        else if (question.contains("ressource") && question.contains("disponible")) {
            int count = 0;
            count = resourceService.getAvailableResources();
            return "📦 **Ressources disponibles** :\n\n" +
                    "**" + count + "** ressources sont actuellement disponibles.";
        }

        else if (question.contains("équipement") && question.contains("disponible")) {
            int count = 0;
            count = resourceService.getAvailableEquipment();
            return "💻 **Équipements disponibles** :\n\n" +
                    "**" + count + "** équipements sont disponibles.";
        }

        else if (question.contains("salle") && question.contains("disponible")) {
            int count = 0;
            count = resourceService.getAvailableRooms();
            return "🏢 **Salles disponibles** :\n\n" +
                    "**" + count + "** salles sont disponibles.";
        }

        else if (question.contains("réservation") && question.contains("cours")) {
            int count = 0;
            count = resourceService.getCurrentReservations();
            return "📅 **Réservations en cours** :\n\n" +
                    "Il y a **" + count + "** réservations actuellement.";
        }

        // ==================== STATISTIQUES GLOBALES ====================
        else if (question.contains("statistique") || question.contains("globale")) {
            return generateGlobalStats();
        }

        else if (question.contains("rapport") || question.contains("activité")) {
            return generateActivityReport();
        }

        else if (question.contains("taux") && question.contains("participation")) {
            double rate = 0.0;
            try {
                rate = eventService.getAverageParticipationRate();
            } catch (Exception e) {
                System.err.println("❌ Erreur getAverageParticipationRate: " + e.getMessage());
                e.printStackTrace();
            }
            return "📊 **Taux de participation moyen** :\n\n" +
                    "**" + String.format("%.1f", rate) + "%** de taux de participation moyen.";
        }

        // ==================== COMMANDES SPÉCIALES ====================
        else if (question.contains("supprimer") && question.contains("utilisateur") && question.contains("@")) {
            String email = extractEmail(question);
            if (email == null) {
                return "❌ Veuillez fournir un email valide. Exemple: 'Supprimer utilisateur user@example.com'";
            }
            UserModel user = userService.getUserByEmail(email);
            if (user == null) {
                return "❌ Aucun utilisateur trouvé avec l'email: " + email;
            }

            // Demander confirmation
            awaitingDeleteConfirmation = true;
            pendingDeleteUserId = user.getId_User();
            pendingDeleteEmail = email;

            return "⚠️ Êtes-vous sûr de vouloir supprimer l'utilisateur **" + email + "** ?\n\n" +
                    "Répondez par **oui** pour confirmer ou **non** pour annuler.";
        }

        else if (question.equals("oui") && awaitingDeleteConfirmation) {
            return handleDeleteConfirmation(question);
        }

        else if (question.equals("non") && awaitingDeleteConfirmation) {
            awaitingDeleteConfirmation = false;
            pendingDeleteUserId = -1;
            pendingDeleteEmail = "";
            return "❌ Suppression annulée.";
        }

        else if (question.contains("aide") || question.contains("help") || question.contains("commandes")) {
            return getHelpMessage();
        }

        // ==================== RÉPONSE PAR DÉFAUT ====================
        return "🤔 Je n'ai pas compris votre question.\n\n" +
                "Tapez **'aide'** pour voir la liste des commandes disponibles.";
    }

    private String handleDeleteConfirmation(String response) {
        if (response.equalsIgnoreCase("oui") || response.equalsIgnoreCase("o")) {
            boolean deleted = userService.deleteUser(pendingDeleteUserId);
            awaitingDeleteConfirmation = false;
            String email = pendingDeleteEmail;
            pendingDeleteUserId = -1;
            pendingDeleteEmail = "";

            if (deleted) {
                return "✅ Utilisateur **" + email + "** supprimé avec succès !";
            } else {
                return "❌ Échec de la suppression. Vérifiez que l'utilisateur existe.";
            }
        } else {
            awaitingDeleteConfirmation = false;
            pendingDeleteUserId = -1;
            pendingDeleteEmail = "";
            return "❌ Suppression annulée.";
        }
    }

    private String extractEmail(String text) {
        // Regex simple pour trouver un email dans le texte
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String generateGlobalStats() {
        int totalUsers = userService.getTotalParticipantsCount();
        int totalEvents = eventService.countEvents();

        int totalSponsors = 0;
        try {
            totalSponsors = sponsorService.getTotalSponsors();
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalSponsors: " + e.getMessage());
        }

        int totalResources = 0;
        totalResources = resourceService.getTotalResources();

        int upcomingEvents = eventService.getUpcomingEvents().size();
        int ongoingEvents = eventService.getEventsByStatus("ONGOING").size();
        int completedEvents = eventService.getEventsByStatus("COMPLETED").size();

        return "📊 **STATISTIQUES GLOBALES**\n\n" +
                "**👥 Utilisateurs** : " + totalUsers + "\n" +
                "**📅 Événements** : " + totalEvents + "\n" +
                "   • À venir : " + upcomingEvents + "\n" +
                "   • En cours : " + ongoingEvents + "\n" +
                "   • Terminés : " + completedEvents + "\n" +
                "**💼 Sponsors** : " + totalSponsors + "\n" +
                "**📦 Ressources** : " + totalResources;
    }

    private String generateActivityReport() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int newUsersThisMonth = userService.getNewUsersThisMonthCount();

        int newEventsThisMonth = 0;
        try {
            newEventsThisMonth = eventService.getNewEventsThisMonth();
        } catch (Exception e) {
            System.err.println("❌ Erreur getNewEventsThisMonth: " + e.getMessage());
        }

        int newSponsorsThisMonth = 0;
        newSponsorsThisMonth = sponsorService.getNewSponsorsThisMonth();

        double avgRate = 0.0;
        try {
            avgRate = eventService.getAverageParticipationRate();
        } catch (Exception e) {
            System.err.println("❌ Erreur getAverageParticipationRate: " + e.getMessage());
        }

        int freeEvents = 0;
        try {
            freeEvents = eventService.getFreeEvents().size();
        } catch (Exception e) {
            System.err.println("❌ Erreur getFreeEvents: " + e.getMessage());
        }

        int paidEvents = 0;
        try {
            paidEvents = eventService.getPaidEvents().size();
        } catch (Exception e) {
            System.err.println("❌ Erreur getPaidEvents: " + e.getMessage());
        }

        return "📈 **RAPPORT D'ACTIVITÉ - " + now.format(formatter) + "**\n\n" +
                "**Nouveautés du mois** :\n" +
                "• 👥 " + newUsersThisMonth + " nouveaux utilisateurs\n" +
                "• 📅 " + newEventsThisMonth + " nouveaux événements\n" +
                "• 💼 " + newSponsorsThisMonth + " nouveaux sponsors\n\n" +
                "**Tendances** :\n" +
                "• Taux de participation moyen : " + String.format("%.1f", avgRate) + "%\n" +
                "• Événements gratuits : " + freeEvents + "\n" +
                "• Événements payants : " + paidEvents;
    }

    private String getHelpMessage() {
        return "📚 **AIDE - Commandes disponibles**\n\n" +
                "**👥 Utilisateurs**\n" +
                "• Combien d'utilisateurs sont inscrits ?\n" +
                "• Liste des administrateurs\n" +
                "• Nouveaux utilisateurs ce mois\n" +
                "• Rôle de [email]\n" +
                "• Utilisateurs par rôle\n" +
                "• Supprimer utilisateur [email]\n\n" +
                "**📅 Événements**\n" +
                "• Événements à venir\n" +
                "• Événements en cours\n" +
                "• Événements terminés\n" +
                "• Événements gratuits\n" +
                "• Événements payants\n" +
                "• Prochain événement\n" +
                "• Capacité totale\n\n" +
                "**💼 Sponsors**\n" +
                "• Nombre de sponsors\n" +
                "• Sponsors actifs\n" +
                "• Budget total\n\n" +
                "**📦 Ressources**\n" +
                "• Ressources disponibles\n" +
                "• Équipements disponibles\n" +
                "• Salles disponibles\n" +
                "• Réservations en cours\n\n" +
                "**📊 Statistiques**\n" +
                "• Statistiques globales\n" +
                "• Rapport d'activité\n" +
                "• Taux de participation\n\n" +
                "**Autres**\n" +
                "• Aide\n" +
                "• Commandes";
    }

    private void addMessageToChat(String sender, String message, Pos alignment) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox();
            messageBox.setAlignment(alignment);
            messageBox.setPadding(new Insets(5, 10, 5, 10));

            Label senderLabel = new Label(sender + ": ");
            senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                    (sender.equals("Vous") ? "#4CAF50" : "#2196F3") + ";");

            Text messageText = new Text(message);
            TextFlow messageFlow = new TextFlow(senderLabel, messageText);
            messageFlow.setStyle("-fx-background-color: " +
                    (sender.equals("Vous") ? "#E8F5E8" : "#E3F2FD") +
                    "; -fx-background-radius: 15; -fx-padding: 12;");
            messageFlow.setMaxWidth(500);

            messageBox.getChildren().add(messageFlow);
            chatBox.getChildren().add(messageBox);

            // Scroll vers le bas
            scrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void handleClearChat() {
        chatBox.getChildren().clear();
        addWelcomeMessage();
    }
}
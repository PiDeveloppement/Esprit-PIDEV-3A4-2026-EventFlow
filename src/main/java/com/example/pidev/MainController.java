package com.example.pidev;

import com.example.pidev.controller.sponsor.SponsorAdminController;
import com.example.pidev.controller.sponsor.SponsorPortalController;
import com.example.pidev.controller.event.EventFormController;
import com.example.pidev.controller.event.CategoryFormController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.model.role.Role;
import com.example.pidev.model.user.UserModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class MainController {

    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    private String lastSponsorPortalEmail;
    public void setLastSponsorPortalEmail(String email) { this.lastSponsorPortalEmail = email; }
    public String getLastSponsorPortalEmail() { return lastSponsorPortalEmail; }

    // ===================== CHEMINS FXML =====================
    // Pages fonctionnelles (sponsors, budget, dépenses, dashboard)
    private static final String SPONSOR_PORTAL_FXML = "/com/example/pidev/fxml/Sponsor/sponsor_portal.fxml";
    private static final String SPONSOR_ADMIN_FXML  = "/com/example/pidev/fxml/Sponsor/sponsor_admin.fxml";
    private static final String BUDGET_LIST_FXML    = "/com/example/pidev/fxml/Budget/budget.fxml";
    private static final String DEPENSE_LIST_FXML   = "/com/example/pidev/fxml/Depense/depense-modern.fxml";
    private static final String DASHBOARD_FXML      = "/com/example/pidev/fxml/dashboard/dashboard.fxml";

    // Événements
    private static final String EVENT_LIST_FXML     = "/com/example/pidev/fxml/event/event-list.fxml";
    private static final String EVENT_FORM_FXML     = "/com/example/pidev/fxml/event/event-form.fxml";
    private static final String EVENT_VIEW_FXML     = "/com/example/pidev/fxml/event/event-view.fxml";
    private static final String CATEGORY_LIST_FXML  = "/com/example/pidev/fxml/event/category-list.fxml";
    private static final String CATEGORY_FORM_FXML  = "/com/example/pidev/fxml/event/category-form.fxml";
    private static final String CATEGORY_VIEW_FXML  = "/com/example/pidev/fxml/event/category-view.fxml";
    private static final String TICKET_LIST_FXML    = "/com/example/pidev/fxml/event/ticket-list.fxml";

    // Rôles et utilisateurs
    private static final String ROLE_LIST_FXML      = "/com/example/pidev/fxml/role/role.fxml";
    private static final String USER_LIST_FXML      = "/com/example/pidev/fxml/user/user.fxml";
    private static final String PROFILE_FXML        = "/com/example/pidev/fxml/user/profil.fxml";
    private static final String EDIT_USER_FXML      = "/com/example/pidev/fxml/user/editUser.fxml";
    private static final String FORGOT_PWD_FXML     = "/com/example/pidev/fxml/user/forgot_password.fxml";
    private static final String RESET_PWD_FXML      = "/com/example/pidev/fxml/user/reset_password.fxml";

    // Ressources
    private static final String SALLE_LIST_FXML     = "/com/example/pidev/fxml/resource/salle.fxml";
    private static final String EQUIPEMENT_LIST_FXML = "/com/example/pidev/fxml/resource/equipement.fxml";
    private static final String RESERVATION_LIST_FXML = "/com/example/pidev/fxml/resource/reservation.fxml";

    // Questionnaires
    private static final String QUESTION_LIST_FXML  = "/com/example/pidev/fxml/questionnaire/list_question.fxml";
    private static final String REPONSE_LIST_FXML   = "/com/example/pidev/fxml/questionnaire/Resultat.fxml";

    // Paramètres
    private static final String SETTINGS_FXML       = "/com/example/pidev/fxml/settings/settings.fxml";

    // ===================== ÉLÉMENTS FXML =====================
    @FXML private VBox pageContentContainer;
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;

    @FXML private Label navDateLabel;
    @FXML private Label navTimeLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Text userInitialsText;
    @FXML private ImageView profileImageView;
    @FXML private StackPane initialsContainer;
    @FXML private StackPane avatarContainer;
    @FXML private MenuButton profileMenu;

    @FXML private Button dashboardBtn;
    @FXML private Button eventsToggleBtn;
    @FXML private Button usersToggleBtn;
    @FXML private Button sponsorsBtn;
    @FXML private Button resourcesToggleBtn;
    @FXML private Button questionnairesToggleBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    @FXML private VBox eventsSubmenu;
    @FXML private VBox usersSubmenu;
    @FXML private VBox sponsorsSubmenu;
    @FXML private VBox resourcesSubmenu;
    @FXML private VBox questionnairesSubmenu;

    @FXML private Text eventsArrow;
    @FXML private Text usersArrow;
    @FXML private Text sponsorsArrow;
    @FXML private Text resourcesArrow;
    @FXML private Text questionnairesArrow;

    @FXML private Button eventsListBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button ticketsBtn;
    @FXML private Button rolesBtn;
    @FXML private Button inscriptionsBtn;
    @FXML private Button sponsorsListBtn;
    @FXML private Button sponsorPortalBtn;
    @FXML private Button budgetBtn;
    @FXML private Button contratsBtn;
    @FXML private Button sallesBtn;
    @FXML private Button equipementsBtn;
    @FXML private Button reservationsBtn;
    @FXML private Button questionsBtn;
    @FXML private Button reponsesBtn;

    // ===================== INITIALISATION =====================
    @FXML
    public void initialize() {
        instance = this;
        configureDateTime();
        loadSessionUserInHeader();
        onDashboard(); // Page par défaut
    }

    private void loadSessionUserInHeader() {
        // À remplacer par les données de session réelles (UserSession)
        String fullName = "Maryem Manai";
        String role = "Sponsor";
        String initials = "MM";
        if (userNameLabel != null) userNameLabel.setText(fullName);
        if (userRoleLabel != null) userRoleLabel.setText(role);
        if (userInitialsText != null) userInitialsText.setText(initials);
    }

    private void configureDateTime() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        LocalDateTime now = LocalDateTime.now();
        if (navDateLabel != null) navDateLabel.setText(now.format(dateFmt));
        if (navTimeLabel != null) navTimeLabel.setText(now.format(timeFmt));

        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        LocalDateTime n = LocalDateTime.now();
                        if (navTimeLabel != null) navTimeLabel.setText(n.format(timeFmt));
                        if (navDateLabel != null) navDateLabel.setText(n.format(dateFmt));
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ===================== GESTION DES SOUS-MENUS =====================
    @FXML private void toggleEvents()         { toggleSubmenu(eventsSubmenu, eventsArrow, eventsToggleBtn); }
    @FXML private void toggleUsers()          { toggleSubmenu(usersSubmenu, usersArrow, usersToggleBtn); }
    @FXML private void toggleSponsors()       { toggleSubmenu(sponsorsSubmenu, sponsorsArrow, sponsorsBtn); }
    @FXML private void toggleResources()      { toggleSubmenu(resourcesSubmenu, resourcesArrow, resourcesToggleBtn); }
    @FXML private void toggleQuestionnaires() { toggleSubmenu(questionnairesSubmenu, questionnairesArrow, questionnairesToggleBtn); }

    private void toggleSubmenu(VBox submenu, Text arrow, Button active) {
        if (submenu == null) return;
        boolean isVisible = submenu.isVisible();
        submenu.setVisible(!isVisible);
        submenu.setManaged(!isVisible);
        if (arrow != null) arrow.setText(!isVisible ? "▼" : "▶");
        setActiveButton(active);
    }

    private void openSponsorsSubmenu() {
        if (sponsorsSubmenu != null) {
            sponsorsSubmenu.setVisible(true);
            sponsorsSubmenu.setManaged(true);
        }
        if (sponsorsArrow != null) sponsorsArrow.setText("▼");
    }

    // ===================== CHARGEMENT GÉNÉRIQUE =====================
    public void loadPage(String fxmlPath, String title, String subtitle) {
        System.out.println("loadPage: " + fxmlPath);
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("❌ FXML non trouvé : " + fxmlPath);
                showEmptyPage("Page non trouvée", "Le fichier " + fxmlPath + " est introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Object controller = loader.getController();

            // Injecter la référence du MainController si le contrôleur a une méthode setMainController
            injectMainController(controller);

            setContent(root);
            updateHeader(title, subtitle);
            System.out.println("✅ Page chargée : " + fxmlPath);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPage("Erreur de chargement", fxmlPath, e);
        }
    }

    public <T> void loadIntoCenter(String fxmlPath, Consumer<T> controllerConsumer) {
        System.out.println("loadIntoCenter: " + fxmlPath);
        try {
            if (pageContentContainer == null) {
                throw new IllegalStateException("pageContentContainer est null.");
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            Object controller = loader.getController();

            // Injecter la référence du MainController
            injectMainController(controller);

            pageContentContainer.getChildren().setAll(page);

            if (controllerConsumer != null) {
                @SuppressWarnings("unchecked")
                T ctrl = (T) controller;
                controllerConsumer.accept(ctrl);
            }
            System.out.println("✅ Page chargée avec contrôleur : " + fxmlPath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorPage("Erreur de chargement", fxmlPath, e);
        }
    }

    /**
     * Injecte la référence de ce MainController dans le contrôleur de la page
     * si celui-ci possède une méthode setMainController(MainController).
     */
    private void injectMainController(Object controller) {
        if (controller == null) return;
        try {
            controller.getClass().getMethod("setMainController", MainController.class).invoke(controller, this);
            System.out.println("✅ Injection de MainController réussie dans " + controller.getClass().getSimpleName());
        } catch (NoSuchMethodException e) {
            // ignore, le contrôleur n'a pas besoin de cette méthode
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de l'injection de MainController : " + e.getMessage());
        }
    }

    public void setContent(Parent node) {
        if (pageContentContainer == null) {
            System.err.println("❌ pageContentContainer est NULL !");
            return;
        }
        pageContentContainer.getChildren().setAll(node);
    }

    public void setContent(Parent node, String title) {
        setContent(node);
        updateHeader(title, "");
    }

    public void updateHeader(String title, String subtitle) {
        if (pageTitle != null) pageTitle.setText(title);
        if (pageSubtitle != null) pageSubtitle.setText(subtitle);
    }

    private void showEmptyPage(String title, String message) {
        updateHeader(title, "");
        VBox empty = new VBox();
        empty.setStyle("-fx-alignment: center; -fx-padding: 40;");
        Label msg = new Label(message);
        msg.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
        empty.getChildren().add(msg);
        setContent(empty);
    }

    private void showErrorPage(String title, String fxmlPath, Exception e) {
        updateHeader(title, "");
        VBox errorBox = new VBox(15);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40));
        errorBox.setStyle("-fx-background-color: #fee2e2; -fx-border-color: #fecaca; -fx-border-width: 1; -fx-background-radius: 12;");

        Label errorTitle = new Label("❌ " + title);
        errorTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");

        Label errorPath = new Label("FXML: " + fxmlPath);
        errorPath.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f1d1d;");

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        TextArea stackTrace = new TextArea(sw.toString());
        stackTrace.setEditable(false);
        stackTrace.setPrefHeight(200);
        stackTrace.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        errorBox.getChildren().addAll(errorTitle, errorPath, stackTrace);
        setContent(errorBox);
    }

    // ===================== NAVIGATION - DASHBOARD =====================
    @FXML public void onDashboard() {
        setActiveButton(dashboardBtn);
        loadIntoCenter(DASHBOARD_FXML, null);
        updateHeader("Tableau de bord", "Aperçu général de votre activité");
    }

    // ===================== NAVIGATION - SPONSORS =====================
    @FXML public void onSponsorsList() {
        openSponsorsSubmenu();
        setActiveButton(sponsorsListBtn);
        showSponsorsAdmin();
    }

    @FXML public void onSponsorPortal() {
        openSponsorsSubmenu();
        setActiveButton(sponsorPortalBtn);
        showSponsorPortal(lastSponsorPortalEmail);
    }

    @FXML public void onBudget() {
        openSponsorsSubmenu();
        setActiveButton(budgetBtn);
        loadIntoCenter(BUDGET_LIST_FXML, null);
        updateHeader("Budget", "Gestion budgétaire");
    }

    @FXML public void onDepenses() {
        openSponsorsSubmenu();
        setActiveButton(contratsBtn);
        loadIntoCenter(DEPENSE_LIST_FXML, null);
        updateHeader("Dépenses", "Suivi des dépenses");
    }

    public void showSponsorPortal(String email) {
        openSponsorsSubmenu();
        setActiveButton(sponsorPortalBtn);
        if (email != null && !email.isBlank()) setLastSponsorPortalEmail(email);
        loadIntoCenter(SPONSOR_PORTAL_FXML, (SponsorPortalController ctrl) -> {
            String e = getLastSponsorPortalEmail();
            if (e != null && !e.isBlank()) ctrl.setInitialEmail(e);
        });
        updateHeader("Portail Sponsor", "Espace dédié aux sponsors");
    }

    public void showSponsorsAdmin() {
        openSponsorsSubmenu();
        setActiveButton(sponsorsListBtn);
        loadIntoCenter(SPONSOR_ADMIN_FXML, (SponsorAdminController ctrl) -> {});
        updateHeader("Administration Sponsors", "Gestion des sponsors");
    }

    // ===================== NAVIGATION - ÉVÉNEMENTS =====================
    @FXML public void onEventsList() {
        setActiveButton(eventsListBtn);
        loadPage(EVENT_LIST_FXML, "Liste des événements", "Gérez tous vos événements");
    }

    @FXML public void onCategories() {
        setActiveButton(categoriesBtn);
        loadPage(CATEGORY_LIST_FXML, "Catégories", "Gestion des catégories d'événements");
    }

    @FXML public void onTickets() {
        setActiveButton(ticketsBtn);
        loadPage(TICKET_LIST_FXML, "Billets", "Gestion des billets");
    }

    // Méthodes pour les formulaires d'événements
    public void showEventForm() {
        loadPage(EVENT_FORM_FXML, "Nouvel Événement", "Créez un nouvel événement");
    }

    public void showEventForm(Event event) {
        loadIntoCenter(EVENT_FORM_FXML, (EventFormController ctrl) -> {
            ctrl.setEvent(event);
        });
    }

    public void showEventView(Event event) {
        loadIntoCenter(EVENT_VIEW_FXML, (EventFormController ctrl) -> {
            // Si vous avez un contrôleur de vue, utilisez-le
            ctrl.setEvent(event);
        });
    }

    // Méthodes pour les formulaires de catégories
    public void showCategoryForm() {
        loadPage(CATEGORY_FORM_FXML, "Nouvelle Catégorie", "Créez une nouvelle catégorie");
    }

    public void showCategoryForm(EventCategory category) {
        loadIntoCenter(CATEGORY_FORM_FXML, (CategoryFormController ctrl) -> {
            ctrl.setCategory(category);
        });
    }

    public void showCategoryView(EventCategory category) {
        loadIntoCenter(CATEGORY_VIEW_FXML, (CategoryFormController ctrl) -> {
            ctrl.setCategory(category);
        });
    }

    // ===================== NAVIGATION - PARTICIPANTS =====================
    @FXML public void onRoles() {
        setActiveButton(rolesBtn);
        loadPage(ROLE_LIST_FXML, "Rôles", "Gestion des rôles utilisateurs");
    }

    @FXML public void onInscriptions() {
        setActiveButton(inscriptionsBtn);
        loadPage(USER_LIST_FXML, "Participants", "Gestion des participants");
    }

    // ===================== NAVIGATION - RESSOURCES =====================
    @FXML public void onSalles() {
        setActiveButton(sallesBtn);
        loadPage(SALLE_LIST_FXML, "Salles", "Gestion des salles");
    }

    @FXML public void onEquipements() {
        setActiveButton(equipementsBtn);
        loadPage(EQUIPEMENT_LIST_FXML, "Équipements", "Inventaire du matériel");
    }

    @FXML public void onReservations() {
        setActiveButton(reservationsBtn);
        loadPage(RESERVATION_LIST_FXML, "Réservations", "Gestion des réservations");
    }

    // ===================== NAVIGATION - QUESTIONNAIRES =====================
    @FXML public void onQuestions() {
        setActiveButton(questionsBtn);
        loadPage(QUESTION_LIST_FXML, "Questions", "Gestion des questions");
    }

    @FXML public void onReponses() {
        setActiveButton(reponsesBtn);
        loadPage(REPONSE_LIST_FXML, "Réponses", "Consultation des réponses");
    }

    // ===================== AUTRES =====================
    @FXML public void showSettings() {
        setActiveButton(settingsBtn);
        loadPage(SETTINGS_FXML, "Paramètres", "Configuration de l'application");
    }

    @FXML public void showProfile() {
        loadPage(PROFILE_FXML, "Mon Profil", "Informations personnelles");
    }

    @FXML public void logout() {
        System.out.println("Déconnexion...");
        HelloApplication.loadLoginPage(); // Retour à la page de connexion
    }

    // ===================== MÉTHODES DE COMPATIBILITÉ (pour ne pas casser les anciens appels) =====================
    public void showEventsList() { onEventsList(); }
    public void showCategories() { onCategories(); }
    public void showTicketsList() { onTickets(); }
    public void loadRoleView() { onRoles(); }
    public void loadAddRolePage() { onRoles(); }
    public void loadEditRolePage(Role role) { onRoles(); }
    public void loadUserView() { onInscriptions(); }
    public void loadEditUserPage(UserModel user) { onInscriptions(); }
    public void refreshHeaderProfile() { loadSessionUserInHeader(); }
    public void loadEquipementsView() { onEquipements(); }
    public void showReservations() { onReservations(); }
    public void showSalles() { onSalles(); }

    // ===================== GESTION DU BOUTON ACTIF =====================
    private void setActiveButton(Button button) {
        Button[] all = {
                dashboardBtn,
                eventsToggleBtn, eventsListBtn, categoriesBtn, ticketsBtn,
                usersToggleBtn, rolesBtn, inscriptionsBtn,
                sponsorsBtn, sponsorsListBtn, sponsorPortalBtn, budgetBtn, contratsBtn,
                resourcesToggleBtn, sallesBtn, equipementsBtn, reservationsBtn,
                questionnairesToggleBtn, questionsBtn, reponsesBtn,
                settingsBtn
        };

        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove("sidebar-button-active");

            boolean isSub = b == eventsListBtn || b == categoriesBtn || b == ticketsBtn ||
                    b == rolesBtn || b == inscriptionsBtn ||
                    b == sponsorsListBtn || b == sponsorPortalBtn || b == budgetBtn || b == contratsBtn ||
                    b == sallesBtn || b == equipementsBtn || b == reservationsBtn ||
                    b == questionsBtn || b == reponsesBtn;

            b.getStyleClass().removeAll("main-menu-button", "submenu-button");
            b.getStyleClass().add(isSub ? "submenu-button" : "main-menu-button");
        }

        if (button != null) button.getStyleClass().add("sidebar-button-active");
    }
}
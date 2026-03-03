package com.example.pidev;

import com.example.pidev.controller.dashboard.DashboardController;
import com.example.pidev.controller.event.*;
import com.example.pidev.controller.role.RoleController;
import com.example.pidev.controller.user.EditUserController;
import com.example.pidev.controller.user.ProfilController;
import com.example.pidev.controller.user.UserController;
import com.example.pidev.controller.sponsor.SponsorAdminController;
import com.example.pidev.controller.sponsor.SponsorPortalController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.model.role.Role;
import com.example.pidev.service.event.EventCategoryService;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventTicketService;
import com.example.pidev.service.role.RoleService;
import com.example.pidev.service.user.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.example.pidev.utils.UserSession;
import com.example.pidev.model.user.UserModel;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MainController {

    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    // ===================== SPONSOR SPECIFIC FIELDS =====================
    private String lastSponsorPortalEmail;
    public void setLastSponsorPortalEmail(String email) { this.lastSponsorPortalEmail = email; }
    public String getLastSponsorPortalEmail() { return lastSponsorPortalEmail; }

    // ===================== FXML PATHS =====================
    private static final String SPONSOR_PORTAL_FXML = "/com/example/pidev/fxml/Sponsor/sponsor_portal.fxml";
    private static final String SPONSOR_ADMIN_FXML  = "/com/example/pidev/fxml/Sponsor/sponsor_admin.fxml";
    private static final String BUDGET_LIST_FXML    = "/com/example/pidev/fxml/budget/budget.fxml";
    private static final String DEPENSE_LIST_FXML   = "/com/example/pidev/fxml/depense/depense-modern.fxml";

    // ===================== CENTER CONTENT =====================
    @FXML private VBox pageContentContainer;

    // ===================== PAGE HEADER =====================
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private Button chatFloatingButton;
    @FXML private HBox kpiContainer;

    // ===================== TOP BAR =====================
    @FXML private Label navDateLabel;
    @FXML private Label navTimeLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Text userInitialsText;
    @FXML private ImageView profileImageView;
    @FXML private StackPane initialsContainer;
    @FXML private MenuButton profileMenu;

    // ===================== SIDEBAR =====================
    @FXML private Button dashboardBtn;

    // Events
    @FXML private Button eventsToggleBtn;
    @FXML private VBox eventsSubmenu;
    @FXML private Text eventsArrow;
    @FXML private Button eventsListBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button ticketsBtn;

    // Participants
    @FXML private Button usersToggleBtn;
    @FXML private VBox usersSubmenu;
    @FXML private Text usersArrow;
    @FXML private Button rolesBtn;
    @FXML private Button inscriptionsBtn;

    // Sponsors
    @FXML private Button sponsorsBtn;
    @FXML private VBox sponsorsSubmenu;
    @FXML private Text sponsorsArrow;
    @FXML private Button sponsorsListBtn;
    @FXML private Button sponsorPortalBtn;
    @FXML private Button budgetBtn;
    @FXML private Button contratsBtn;

    // Ressources
    @FXML private Button resourcesToggleBtn;
    @FXML private VBox resourcesSubmenu;
    @FXML private Text resourcesArrow;
    @FXML private Button sallesBtn;
    @FXML private Button equipementsBtn;
    @FXML private Button reservationsBtn;

    // Questionnaires
    @FXML private Button questionnairesToggleBtn;
    @FXML private VBox questionnairesSubmenu;
    @FXML private Text questionnairesArrow;
    @FXML private Button questionsBtn;
    @FXML private Button reponsesBtn;

    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    @FXML private TextField globalSearchField;

    private final Map<String, PageInfo> pageInfoMap = new HashMap<>();
    private Button activeButton;
    private DashboardController dashboardController;

    // Services pour les KPI
    private EventService eventService;
    private EventCategoryService categoryService;
    private EventTicketService ticketService;
    private UserService userService;
    private RoleService roleService;

    private static class PageInfo {
        String title;
        String subtitle;
        PageInfo(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    // ===================== INIT =====================
    @FXML
    public void initialize() {
        instance = this;
        System.out.println("✅ MainController initialisé");

        try {
            // Initialiser les services pour les KPI avec gestion des exceptions
            eventService = new EventService();
            categoryService = new EventCategoryService();
            ticketService = new EventTicketService();
            userService = new UserService();

            // Initialiser RoleService avec try-catch
            try {
                roleService = new RoleService();
                System.out.println("✅ RoleService initialisé avec succès");
            } catch (SQLException e) {
                System.err.println("⚠️ Erreur lors de l'initialisation de RoleService: " + e.getMessage());
                roleService = null; // Mettre à null en cas d'erreur
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de l'initialisation des services: " + e.getMessage());
        }

        UserSession session = UserSession.getInstance();
        System.out.println("👤 Rôle connecté dans MainController: " + session.getRole());

        initializePageInfo();
        configureSidebarButtons();
        hideAllButtons();
        configureSidebarByRole();
        configureDateTime();
        loadUserProfileInHeader();
        setupGlobalSearch();

        // Initialiser les KPI (cachés par défaut)
        hideKPIs();

        // Page par défaut : Dashboard
        if (dashboardBtn != null) {
            setActiveButton(dashboardBtn);
            loadDashboardView();
        }

        // Animer le bouton flottant au démarrage
        animateFloatingButton();

        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> logout());
        }
    }

    // ===================== PAGE INFO =====================
    private void initializePageInfo() {
        pageInfoMap.put("dashboard", new PageInfo("Tableau de bord", "Aperçu général de votre activité"));
        pageInfoMap.put("events", new PageInfo("Gestion des événements", "Consultez et gérez tous vos événements"));
        pageInfoMap.put("categories", new PageInfo("Gestion des catégories", "Gérez les catégories d'événements"));
        pageInfoMap.put("tickets", new PageInfo("Gestion des billets", "Gérez les billets et inscriptions"));
        pageInfoMap.put("users", new PageInfo("Gestion des participants", "Gérez les participants"));
        pageInfoMap.put("roles", new PageInfo("Gestion des rôles", "Gérez les différents rôles"));
        pageInfoMap.put("inscriptions", new PageInfo("Gestion des inscriptions", "Gérez les inscriptions"));
        pageInfoMap.put("sponsors", new PageInfo("Gestion des sponsors", "Gérez vos partenaires"));
        pageInfoMap.put("sponsorsList", new PageInfo("Liste des sponsors", "Consultez tous les sponsors"));
        pageInfoMap.put("sponsorPortal", new PageInfo("Portail Sponsor", "Accédez à votre espace sponsor"));
        pageInfoMap.put("budget", new PageInfo("Gestion du budget", "Suivez vos finances"));
        pageInfoMap.put("depenses", new PageInfo("Gestion des dépenses", "Suivez vos dépenses"));
        pageInfoMap.put("salles", new PageInfo("Gestion des salles", "Gérez les salles et espaces"));
        pageInfoMap.put("equipements", new PageInfo("Gestion des équipements", "Gérez le matériel"));
        pageInfoMap.put("reservations", new PageInfo("Gestion des réservations", "Gérez les réservations"));
        pageInfoMap.put("questions", new PageInfo("Gestion des questions", "Gérez les questions"));
        pageInfoMap.put("reponses", new PageInfo("Gestion des réponses", "Consultez les réponses"));
        pageInfoMap.put("resultats", new PageInfo("Résultats", "Statistiques et aperçu global"));
        pageInfoMap.put("historique", new PageInfo("Historique", "Consultation des anciens scores"));
        pageInfoMap.put("participantQuiz", new PageInfo("Passer le Quiz", "Interface d'examen"));
        pageInfoMap.put("settings", new PageInfo("Paramètres", "Configurez l'application"));
        pageInfoMap.put("profile", new PageInfo("Mon profil", "Consultez et modifiez vos informations"));
    }

    // ===================== HEADER USER =====================
    private void loadUserProfileInHeader() {
        UserSession session = UserSession.getInstance();
        UserModel currentUser = session.getCurrentUser();

        if (currentUser != null) {
            if (userNameLabel != null) userNameLabel.setText(session.getFullName());
            if (userRoleLabel != null) userRoleLabel.setText(session.getRole());

            String photoUrl = currentUser.getProfilePictureUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                try {
                    Image image = new Image(photoUrl, 28, 28, true, true);
                    profileImageView.setImage(image);
                    profileImageView.setVisible(true);
                    Circle clip = new Circle(14, 14, 14);
                    profileImageView.setClip(clip);
                    if (initialsContainer != null) initialsContainer.setVisible(false);
                } catch (Exception e) {
                    showInitials(session.getInitials());
                }
            } else {
                showInitials(session.getInitials());
            }
        } else {
            if (userNameLabel != null) userNameLabel.setText("Invité");
            if (userRoleLabel != null) userRoleLabel.setText("Non connecté");
            showInitials("?");
        }
    }

    public void refreshHeaderProfile() {
        loadUserProfileInHeader();
    }

    private void showInitials(String initials) {
        if (profileImageView != null) profileImageView.setVisible(false);
        if (initialsContainer != null) {
            initialsContainer.setVisible(true);
            if (userInitialsText != null) userInitialsText.setText(initials);
        }
    }

    // ===================== DATE/TIME =====================
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

    // ===================== TOGGLES =====================
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

    private void collapseOtherSubmenus(String currentMenu) {
        if (!"events".equals(currentMenu) && eventsSubmenu != null) { eventsSubmenu.setVisible(false); eventsSubmenu.setManaged(false); if (eventsArrow != null) eventsArrow.setText("▶"); }
        if (!"users".equals(currentMenu) && usersSubmenu != null) { usersSubmenu.setVisible(false); usersSubmenu.setManaged(false); if (usersArrow != null) usersArrow.setText("▶"); }
        if (!"sponsors".equals(currentMenu) && sponsorsSubmenu != null) { sponsorsSubmenu.setVisible(false); sponsorsSubmenu.setManaged(false); if (sponsorsArrow != null) sponsorsArrow.setText("▶"); }
        if (!"resources".equals(currentMenu) && resourcesSubmenu != null) { resourcesSubmenu.setVisible(false); resourcesSubmenu.setManaged(false); if (resourcesArrow != null) resourcesArrow.setText("▶"); }
        if (!"questionnaires".equals(currentMenu) && questionnairesSubmenu != null) { questionnairesSubmenu.setVisible(false); questionnairesSubmenu.setManaged(false); if (questionnairesArrow != null) questionnairesArrow.setText("▶"); }
    }

    private void collapseAllSubmenus() {
        if (eventsSubmenu != null) { eventsSubmenu.setVisible(false); eventsSubmenu.setManaged(false); if (eventsArrow != null) eventsArrow.setText("▶"); }
        if (usersSubmenu != null) { usersSubmenu.setVisible(false); usersSubmenu.setManaged(false); if (usersArrow != null) usersArrow.setText("▶"); }
        if (sponsorsSubmenu != null) { sponsorsSubmenu.setVisible(false); sponsorsSubmenu.setManaged(false); if (sponsorsArrow != null) sponsorsArrow.setText("▶"); }
        if (resourcesSubmenu != null) { resourcesSubmenu.setVisible(false); resourcesSubmenu.setManaged(false); if (resourcesArrow != null) resourcesArrow.setText("▶"); }
        if (questionnairesSubmenu != null) { questionnairesSubmenu.setVisible(false); questionnairesSubmenu.setManaged(false); if (questionnairesArrow != null) questionnairesArrow.setText("▶"); }
    }

    private void openSponsorsSubmenu() {
        if (sponsorsSubmenu != null) { sponsorsSubmenu.setVisible(true); sponsorsSubmenu.setManaged(true); if (sponsorsArrow != null) sponsorsArrow.setText("▼"); }
    }

    private void setActiveButton(Button button) {
        Button[] allButtons = {
                dashboardBtn, eventsToggleBtn, eventsListBtn, categoriesBtn, ticketsBtn,
                usersToggleBtn, rolesBtn, inscriptionsBtn,
                sponsorsBtn, sponsorsListBtn, sponsorPortalBtn, budgetBtn, contratsBtn,
                resourcesToggleBtn, sallesBtn, equipementsBtn, reservationsBtn,
                questionnairesToggleBtn, questionsBtn, reponsesBtn, settingsBtn
        };
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().removeAll("main-menu-button", "submenu-button", "sidebar-button-active");
                boolean isSub = btn == eventsListBtn || btn == categoriesBtn || btn == ticketsBtn ||
                        btn == rolesBtn || btn == inscriptionsBtn ||
                        btn == sponsorsListBtn || btn == sponsorPortalBtn || btn == budgetBtn || btn == contratsBtn ||
                        btn == sallesBtn || btn == equipementsBtn || btn == reservationsBtn ||
                        btn == questionsBtn || btn == reponsesBtn;
                btn.getStyleClass().add(isSub ? "submenu-button" : "main-menu-button");
            }
        }
        if (button != null) {
            for (Button btn : allButtons) { if (btn != null) btn.getStyleClass().remove("sidebar-button-active"); }
            button.getStyleClass().add("sidebar-button-active");
        }
    }

    // ===================== CONFIGURATION SIDEBAR =====================
    private void configureSidebarButtons() {
        if (dashboardBtn != null) dashboardBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(dashboardBtn); loadDashboardView(); });
        if (eventsToggleBtn != null) eventsToggleBtn.setOnAction(e -> { toggleEvents(); collapseOtherSubmenus("events"); setActiveButton(eventsToggleBtn); });
        if (usersToggleBtn != null) usersToggleBtn.setOnAction(e -> { toggleUsers(); collapseOtherSubmenus("users"); setActiveButton(usersToggleBtn); });
        if (sponsorsBtn != null) sponsorsBtn.setOnAction(e -> { toggleSponsors(); collapseOtherSubmenus("sponsors"); setActiveButton(sponsorsBtn); });
        if (resourcesToggleBtn != null) resourcesToggleBtn.setOnAction(e -> { toggleResources(); collapseOtherSubmenus("resources"); setActiveButton(resourcesToggleBtn); });
        if (questionnairesToggleBtn != null) questionnairesToggleBtn.setOnAction(e -> { toggleQuestionnaires(); collapseOtherSubmenus("questionnaires"); setActiveButton(questionnairesToggleBtn); });
        if (eventsListBtn != null) eventsListBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(eventsListBtn); showEventsList(); });
        if (categoriesBtn != null) categoriesBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(categoriesBtn); showCategories(); });
        if (ticketsBtn != null) ticketsBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(ticketsBtn); showTicketsList(); });
        if (rolesBtn != null) rolesBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(rolesBtn); loadRoleView(); });
        if (inscriptionsBtn != null) inscriptionsBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(inscriptionsBtn); loadUserView(); });
        if (sponsorsListBtn != null) sponsorsListBtn.setOnAction(e -> { openSponsorsSubmenu(); setActiveButton(sponsorsListBtn); showSponsorsAdmin(); });
        if (sponsorPortalBtn != null) sponsorPortalBtn.setOnAction(e -> { openSponsorsSubmenu(); setActiveButton(sponsorPortalBtn); showSponsorPortal(lastSponsorPortalEmail); });
        if (budgetBtn != null) budgetBtn.setOnAction(e -> { openSponsorsSubmenu(); setActiveButton(budgetBtn); showBudget(); });
        if (contratsBtn != null) contratsBtn.setOnAction(e -> { openSponsorsSubmenu(); setActiveButton(contratsBtn); showDepenses(); });
        if (sallesBtn != null) sallesBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(sallesBtn); loadSallesView(); });
        if (equipementsBtn != null) equipementsBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(equipementsBtn); loadEquipementsView(); });
        if (reservationsBtn != null) reservationsBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(reservationsBtn); loadReservationsView(); });
        if (questionsBtn != null) questionsBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(questionsBtn); showQuestionEditor(); });
        if (reponsesBtn != null) reponsesBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(reponsesBtn); showResultats(); });
        if (settingsBtn != null) settingsBtn.setOnAction(e -> { collapseAllSubmenus(); setActiveButton(settingsBtn); loadSettingsView(); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> logout());
    }

    // ===================== SIDEBAR ROLE CONFIG =====================
    private void configureSidebarByRole() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole();
        if (role == null) { hideAllButtons(); return; }
        role = role.trim().toLowerCase();
        hideAllButtons();
        switch (role) {
            case "admin": case "admin2": case "admin3": case "admin4":
                showAllButtons(); break;
            case "organisateur": case "organisateur2":
                showAllButtons(); hideNode(usersToggleBtn); hideNode(usersSubmenu); break;
            case "sponsor": case "sponsor2": case "sponsor3":
                showOnlySponsorButtons(); break;
            case "participant": case "default": case "invité":
                showOnlyParticipantButtons(); break;
            default:
                hideAllButtons(); break;
        }
    }

    private void showOnlyParticipantButtons() {
        showNode(dashboardBtn); hideNode(eventsToggleBtn); hideNode(usersToggleBtn);
        hideNode(sponsorsBtn); hideNode(resourcesToggleBtn); hideNode(questionnairesToggleBtn);
        hideNode(settingsBtn); hideNode(budgetBtn); hideAllSubmenus();
    }

    private void showOnlySponsorButtons() {
        showNode(dashboardBtn); showNode(sponsorsBtn); showNode(budgetBtn);
        hideNode(eventsToggleBtn); hideNode(usersToggleBtn); hideNode(resourcesToggleBtn);
        hideNode(questionnairesToggleBtn); hideNode(settingsBtn); hideAllSubmenus();
    }

    private void hideAllButtons() {
        hideNode(dashboardBtn); hideNode(sponsorsBtn); hideNode(eventsToggleBtn);
        hideNode(usersToggleBtn); hideNode(resourcesToggleBtn); hideNode(questionnairesToggleBtn);
        hideNode(settingsBtn); hideNode(budgetBtn);
    }

    private void showAllButtons() {
        Node[] main = { dashboardBtn, eventsToggleBtn, usersToggleBtn, sponsorsBtn, resourcesToggleBtn, questionnairesToggleBtn, settingsBtn, budgetBtn };
        for (Node n : main) showNode(n);
        collapseAllSubmenus();
    }

    private void hideNode(Node node) { if (node != null) { node.setVisible(false); node.setManaged(false); } }
    private void showNode(Node node) { if (node != null) { node.setVisible(true); node.setManaged(true); } }

    private void hideAllSubmenus() {
        VBox[] submenus = {eventsSubmenu, usersSubmenu, sponsorsSubmenu, resourcesSubmenu, questionnairesSubmenu};
        Text[] arrows = {eventsArrow, usersArrow, sponsorsArrow, resourcesArrow, questionnairesArrow};
        for (VBox submenu : submenus) { if (submenu != null) { submenu.setVisible(false); submenu.setManaged(false); } }
        for (Text arrow : arrows) { if (arrow != null) arrow.setText("▶"); }
    }

    // ===================== RECHERCHE GLOBALE =====================
    private void setupGlobalSearch() {
        if (globalSearchField != null) {
            globalSearchField.setOnAction(event -> { String query = globalSearchField.getText().trim(); if (!query.isEmpty()) performSimpleSearch(query); });
            globalSearchField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ESCAPE) globalSearchField.clear(); });
        }
    }

    private void performSimpleSearch(String query) {
        String lowerQuery = query.toLowerCase();
        ObservableList<String> results = FXCollections.observableArrayList();
        if ("dashboard".contains(lowerQuery)) results.add("📊 Dashboard");
        if ("événements".contains(lowerQuery) || "events".contains(lowerQuery)) { results.add("📅 Événements"); results.add("   📋 Liste des événements"); results.add("   🏷️ Catégories"); results.add("   🎫 Billets"); }
        if ("participants".contains(lowerQuery) || "users".contains(lowerQuery)) { results.add("👥 Participants"); results.add("   👤 Rôles"); results.add("   📝 Inscriptions"); }
        if ("sponsors".contains(lowerQuery)) { results.add("💼 Sponsors"); results.add("   📋 Liste sponsors"); results.add("   🔑 Portail Sponsor"); results.add("   💰 Budget"); results.add("   📄 Dépenses"); }
        if ("ressources".contains(lowerQuery)) { results.add("📦 Ressources"); results.add("   💻 Équipements"); results.add("   🏢 Salles"); results.add("   📅 Réservations"); }
        if ("questionnaires".contains(lowerQuery)) { results.add("📝 Questionnaires"); results.add("   ❓ Questions"); results.add("   📊 Résultats"); results.add("   📜 Historique"); results.add("   🎯 Passer le Quiz"); }
        if ("paramètres".contains(lowerQuery) || "settings".contains(lowerQuery)) results.add("⚙️ Paramètres");
        if (results.isEmpty()) showSimpleAlert("Aucun résultat", "Aucun résultat trouvé pour: " + query);
        else showSimpleResultsPopup(query, results);
    }

    private void showSimpleResultsPopup(String query, ObservableList<String> results) {
        Stage stage = new Stage();
        stage.setTitle("Résultats de recherche");
        stage.initModality(Modality.APPLICATION_MODAL);
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20; -fx-background-color: white;");
        root.setPrefWidth(400); root.setMaxWidth(400);
        Label titleLabel = new Label("Résultats pour: \"" + query + "\"");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        ListView<String> listView = new ListView<>(results);
        listView.setPrefHeight(Math.min(300, results.size() * 40));
        listView.setStyle("-fx-background-color: transparent;");
        listView.setOnMouseClicked(event -> { String selected = listView.getSelectionModel().getSelectedItem(); if (selected != null) { navigateFromSearch(selected); stage.close(); } });
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());
        HBox buttonBar = new HBox(); buttonBar.setAlignment(Pos.CENTER_RIGHT); buttonBar.getChildren().add(closeBtn);
        root.getChildren().addAll(titleLabel, listView, buttonBar);
        stage.setScene(new Scene(root)); stage.show();
    }

    private void navigateFromSearch(String selected) {
        if (selected.contains("Dashboard")) dashboardBtn.fire();
        else if (selected.contains("Événements") && !selected.contains("  ")) eventsToggleBtn.fire();
        else if (selected.contains("Liste événements")) eventsListBtn.fire();
        else if (selected.contains("Catégories")) categoriesBtn.fire();
        else if (selected.contains("Billets")) ticketsBtn.fire();
        else if (selected.contains("Participants") && !selected.contains("  ")) usersToggleBtn.fire();
        else if (selected.contains("Rôles")) rolesBtn.fire();
        else if (selected.contains("Inscriptions")) inscriptionsBtn.fire();
        else if (selected.contains("Sponsors") && !selected.contains("  ")) sponsorsBtn.fire();
        else if (selected.contains("Liste sponsors")) sponsorsListBtn.fire();
        else if (selected.contains("Portail Sponsor")) sponsorPortalBtn.fire();
        else if (selected.contains("Budget")) budgetBtn.fire();
        else if (selected.contains("Dépenses")) contratsBtn.fire();
        else if (selected.contains("Ressources") && !selected.contains("  ")) resourcesToggleBtn.fire();
        else if (selected.contains("Équipements")) equipementsBtn.fire();
        else if (selected.contains("Salles")) sallesBtn.fire();
        else if (selected.contains("Réservations")) reservationsBtn.fire();
        else if (selected.contains("Questionnaires") && !selected.contains("  ")) questionnairesToggleBtn.fire();
        else if (selected.contains("Questions")) questionsBtn.fire();
        else if (selected.contains("Résultats")) reponsesBtn.fire();
        else if (selected.contains("Historique")) showHistorique();
        else if (selected.contains("Passer le Quiz")) showParticipantQuiz();
        else if (selected.contains("Paramètres")) settingsBtn.fire();
    }

    // ===================== NAVIGATION - SPONSORS =====================
    @FXML public void showSponsorsAdmin() { openSponsorsSubmenu(); setActiveButton(sponsorsListBtn); loadIntoCenter(SPONSOR_ADMIN_FXML, (SponsorAdminController ctrl) -> {}); updatePageHeader("sponsorsList"); }
    @FXML public void showSponsorPortal(String email) { openSponsorsSubmenu(); setActiveButton(sponsorPortalBtn); if (email != null && !email.isBlank()) setLastSponsorPortalEmail(email); loadIntoCenter(SPONSOR_PORTAL_FXML, (SponsorPortalController ctrl) -> { String e = getLastSponsorPortalEmail(); if (e != null && !e.isBlank()) ctrl.setInitialEmail(e); }); updatePageHeader("sponsorPortal"); }
    @FXML public void showSponsorPortal() { showSponsorPortal(lastSponsorPortalEmail); }
    @FXML public void showBudget() { openSponsorsSubmenu(); setActiveButton(budgetBtn); loadIntoCenter(BUDGET_LIST_FXML, null); updatePageHeader("budget"); }
    @FXML public void showDepenses() { openSponsorsSubmenu(); setActiveButton(contratsBtn); loadIntoCenter(DEPENSE_LIST_FXML, null); updatePageHeader("depenses"); }
    public void showSponsors() { showSponsorsAdmin(); }

    // ===================== CHARGEMENT DYNAMIQUE =====================
    public <T> void loadIntoCenter(String fxmlPath, Consumer<T> controllerConsumer) {
        try {
            if (pageContentContainer == null) throw new IllegalStateException("pageContentContainer est null.");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            pageContentContainer.getChildren().setAll(page);
            if (controllerConsumer != null) { @SuppressWarnings("unchecked") T ctrl = (T) loader.getController(); controllerConsumer.accept(ctrl); }
        } catch (Exception e) { e.printStackTrace(); showEmptyPage("Erreur", "Impossible de charger : " + fxmlPath + "\n" + e.getMessage()); }
    }

    public void loadPage(String fxmlPath, String pageKey) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            if ("profile".equals(pageKey) && loader.getController() instanceof ProfilController) ((ProfilController) loader.getController()).setMainController(this);
            if ("dashboard".equals(pageKey) && loader.getController() instanceof DashboardController) { dashboardController = (DashboardController) loader.getController(); dashboardController.setMainController(this); }
            updatePageHeader(pageKey);
            pageContentContainer.getChildren().setAll(page);
        } catch (IOException e) { e.printStackTrace(); showSimpleAlert("Erreur", "Impossible de charger la page: " + fxmlPath); }
    }

    public void loadPage(String fxmlPath) { loadPage(fxmlPath, extractPageKeyFromPath(fxmlPath)); }

    private String extractPageKeyFromPath(String fxmlPath) {
        if (fxmlPath.contains("dashboard")) return "dashboard";
        if (fxmlPath.contains("event")) return "events";
        if (fxmlPath.contains("categorie")) return "categories";
        if (fxmlPath.contains("ticket")) return "tickets";
        if (fxmlPath.contains("user")) return "users";
        if (fxmlPath.contains("role")) return "roles";
        if (fxmlPath.contains("inscription")) return "inscriptions";
        if (fxmlPath.contains("salle")) return "salles";
        if (fxmlPath.contains("equipement")) return "equipements";
        if (fxmlPath.contains("reservation")) return "reservations";
        if (fxmlPath.contains("sponsor_admin")) return "sponsorsList";
        if (fxmlPath.contains("sponsor_portal")) return "sponsorPortal";
        if (fxmlPath.contains("budget")) return "budget";
        if (fxmlPath.contains("depense")) return "depenses";
        if (fxmlPath.contains("form_question")) return "questions";
        if (fxmlPath.contains("Resultat")) return "resultats";
        if (fxmlPath.contains("Participant")) return "participantQuiz";
        if (fxmlPath.contains("Historique")) return "historique";
        if (fxmlPath.contains("question")) return "questions";
        if (fxmlPath.contains("reponse")) return "reponses";
        if (fxmlPath.contains("settings")) return "settings";
        if (fxmlPath.contains("profil")) return "profile";
        if (fxmlPath.contains("editUser")) return "editUsers";
        if (fxmlPath.contains("editRole")) return "editRoles";
        return "dashboard";
    }

    private void updatePageHeader(String pageKey) {
        PageInfo pageInfo = pageInfoMap.get(pageKey);
        if (pageInfo != null) { if (pageTitle != null) pageTitle.setText(pageInfo.title); if (pageSubtitle != null) pageSubtitle.setText(pageInfo.subtitle); }
    }

    // ===================== NAVIGATION - DASHBOARD =====================
    public void loadDashboardView() {
        UserSession session = UserSession.getInstance();
        if (session.getRole() != null) {
            loadPage("/com/example/pidev/fxml/dashboard/dashboard.fxml", "dashboard");
            hideKPIs(); // Cacher les KPI sur le dashboard
        }
    }

    // ===================== NAVIGATION - USERS =====================
    public void loadUserView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/user/user.fxml"));
            Parent root = loader.load();
            UserController controller = loader.getController();
            controller.setMainController(this);

            // Mettre à jour l'en-tête
            updatePageHeader("users");

            // Afficher uniquement le KPI des participants
            showParticipantKPIs();

            // Charger le contenu
            pageContentContainer.getChildren().setAll(root);

        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des utilisateurs: " + e.getMessage());
        }
    }

    // ===================== NAVIGATION - ROLES =====================
    public void loadRoleView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/role/role.fxml"));
            Parent root = loader.load();
            RoleController controller = loader.getController();
            controller.setMainController(this);

            // Mettre à jour l'en-tête
            updatePageHeader("roles");

            // Afficher uniquement le KPI des rôles
            showRoleKPIs();

            // Charger le contenu
            pageContentContainer.getChildren().setAll(root);

        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des rôles: " + e.getMessage());
        }
    }

    public void loadEditUserPage(UserModel user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/user/editUser.fxml"));
            Parent root = loader.load();
            EditUserController controller = loader.getController();
            controller.setMainController(this);
            controller.setUser(user);
            pageContentContainer.getChildren().setAll(root);
            hideKPIs(); // Cacher les KPI sur la page d'édition
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadEditRolePage(Role role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/role/editRole.fxml"));
            Parent root = loader.load();
            com.example.pidev.controller.role.EditRoleController controller = loader.getController();
            controller.setEditMode(role);
            controller.setMainController(this);
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("roles");
            hideKPIs(); // Cacher les KPI sur la page d'édition
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page de modification: " + e.getMessage());
        }
    }

    public void loadAddRolePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/role/editRole.fxml"));
            Parent root = loader.load();
            com.example.pidev.controller.role.EditRoleController controller = loader.getController();
            controller.setAddMode();
            controller.setMainController(this);
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("roles");
            hideKPIs(); // Cacher les KPI sur la page d'ajout
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page d'ajout: " + e.getMessage());
        }
    }

    // ===================== NAVIGATION - RESSOURCES =====================
    public void loadSallesView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/resource/salle.fxml"));
            Parent root = loader.load();
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("salles");
            hideKPIs(); // Cacher les KPI sur la page des salles
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des salles: " + e.getMessage());
        }
    }

    public void loadEquipementsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/resource/equipement.fxml"));
            Parent root = loader.load();
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("equipements");
            hideKPIs(); // Cacher les KPI sur la page des équipements
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des équipements: " + e.getMessage());
        }
    }

    public void loadReservationsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/resource/reservation.fxml"));
            Parent root = loader.load();
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("reservations");
            hideKPIs(); // Cacher les KPI sur la page des réservations
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des réservations: " + e.getMessage());
        }
    }

    @FXML public void showSalles() { collapseAllSubmenus(); setActiveButton(sallesBtn); loadSallesView(); }
    @FXML public void showEquipements() { collapseAllSubmenus(); setActiveButton(equipementsBtn); loadEquipementsView(); }
    @FXML public void showReservations() { collapseAllSubmenus(); setActiveButton(reservationsBtn); loadReservationsView(); }

    // ===================== NAVIGATION - EVENTS =====================
    public void showEventsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/event-list.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventListController) ((EventListController) controller).setMainController(this);
            updatePageHeader("events");
            hideKPIs(); // Cacher les KPI sur la liste des événements
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement liste événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showEventForm(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/event-form.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventFormController) {
                ((EventFormController) controller).setMainController(this);
                if (event != null) ((EventFormController) controller).setEvent(event);
            }
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
            hideKPIs(); // Cacher les KPI sur le formulaire
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showEventView(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/event-view.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventViewController) {
                ((EventViewController) controller).setMainController(this);
                ((EventViewController) controller).setEvent(event);
            }
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
            hideKPIs(); // Cacher les KPI sur la vue détaillée
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/category-list.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof CategoryListController) ((CategoryListController) controller).setMainController(this);
            updatePageHeader("categories");
            hideKPIs(); // Cacher les KPI sur la liste des catégories
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement liste catégories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showCategoryForm(EventCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/category-form.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof CategoryFormController) {
                ((CategoryFormController) controller).setMainController(this);
                if (category != null) ((CategoryFormController) controller).setCategory(category);
            }
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
            hideKPIs(); // Cacher les KPI sur le formulaire
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showCategoryView(EventCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/category-view.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof CategoryViewController) {
                ((CategoryViewController) controller).setMainController(this);
                ((CategoryViewController) controller).setCategory(category);
            }
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
            hideKPIs(); // Cacher les KPI sur la vue détaillée
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showTicketsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/ticket-list.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventTicketListController) ((EventTicketListController) controller).setMainController(this);
            updatePageHeader("tickets");
            hideKPIs(); // Cacher les KPI sur la liste des tickets
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement liste tickets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showTicketView(EventTicket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/event/ticket-view.fxml"));
            Parent page = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventTicketViewController) {
                ((EventTicketViewController) controller).setMainController(this);
                ((EventTicketViewController) controller).setTicket(ticket);
            }
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);
            hideKPIs(); // Cacher les KPI sur la vue détaillée
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showTickets() { showTicketsList(); }

    // ===================== NAVIGATION - QUESTIONNAIRES =====================
    public void loadQuestionEditor() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/form_question.fxml", "Gestion des questions", "Gestion de la banque de données");
        hideKPIs(); // Cacher les KPI sur l'éditeur de questions
    }

    public void loadResultatsView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Resultat.fxml", "Résultats", "Statistiques et aperçu global");
        hideKPIs(); // Cacher les KPI sur la page des résultats
    }

    public void loadParticipantQuizView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Participant.fxml", "Passer le Quiz", "Interface d'examen");
        hideKPIs(); // Cacher les KPI sur la page du quiz
    }

    public void loadHistoriqueView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Historique.fxml", "Historique", "Consultation des anciens scores");
        hideKPIs(); // Cacher les KPI sur la page d'historique
    }

    private void loadQuestionnairePage(String fxmlPath, String title, String subtitle) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) { showSimpleAlert("Erreur", "Fichier introuvable: " + fxmlPath); return; }
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();
            setContent(root);
            if (pageTitle != null) pageTitle.setText(title);
            if (pageSubtitle != null) pageSubtitle.setText(subtitle);
        } catch (IOException e) { e.printStackTrace(); showSimpleAlert("Erreur", "Impossible de charger la page: " + e.getMessage()); }
    }

    @FXML public void showQuestionEditor() { collapseAllSubmenus(); setActiveButton(questionsBtn); loadQuestionEditor(); }
    @FXML public void showResultats() { collapseAllSubmenus(); setActiveButton(reponsesBtn); loadResultatsView(); }
    @FXML public void showParticipantQuiz() { collapseAllSubmenus(); loadParticipantQuizView(); }
    @FXML public void showHistorique() { collapseAllSubmenus(); loadHistoriqueView(); }

    // ===================== NAVIGATION - SETTINGS & PROFILE =====================
    public void loadSettingsView() {
        loadPage("/com/example/pidev/fxml/settings/settings.fxml", "settings");
        hideKPIs(); // Cacher les KPI sur la page des paramètres
    }

    @FXML public void showProfile() {
        collapseAllSubmenus();
        loadPage("/com/example/pidev/fxml/user/profil.fxml", "profile");
        hideKPIs(); // Cacher les KPI sur la page de profil
    }

    @FXML public void showSettings() {
        collapseAllSubmenus();
        loadPage("/com/example/pidev/fxml/settings/settings.fxml", "settings");
        hideKPIs(); // Cacher les KPI sur la page des paramètres
    }

    // ===================== LOGOUT =====================
    @FXML
    public void logout() {
        try {
            if (dashboardController != null) dashboardController.cleanup();
            UserSession.getInstance().clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Stage stage = HelloApplication.getPrimaryStage();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("EventFlow - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleExit() { System.exit(0); }

    // ===================== REFRESH METHODS =====================
    public void refreshDashboard() { if (dashboardController != null) dashboardController.refreshData(); }
    public void onEventSaved() { refreshDashboard(); }
    public void onParticipantSaved() { refreshDashboard(); }
    public void onInscriptionSaved() { refreshDashboard(); }
    public void cleanup() { if (dashboardController != null) dashboardController.cleanup(); }
    public void refreshSidebarForRole() { configureSidebarByRole(); collapseAllSubmenus(); }

    // ===================== CONTENT SETTERS =====================
    public void setContent(Parent node) { if (pageContentContainer != null) pageContentContainer.getChildren().setAll(node); }
    public void setContent(Parent root, String title) { setContent(root, title, ""); }
    public void setContent(Parent root, String title, String subtitle) { if (pageTitle != null) pageTitle.setText(title); if (pageSubtitle != null) pageSubtitle.setText(subtitle); setContent(root); }

    // ===================== PAGE DE SECOURS =====================
    private void showEmptyPage(String title, String subtitle) {
        if (pageContentContainer == null) return;
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #e2e8f0; -fx-border-width: 1;");
        Label t = new Label(title); t.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label s = new Label(subtitle); s.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        box.getChildren().addAll(t, s);
        pageContentContainer.getChildren().setAll(box);
    }

    private void showSimpleAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }

    @FXML
    public void onBudget() {
        collapseAllSubmenus();
        setActiveButton(budgetBtn);
        showBudget();
    }

    // ===================== CHAT ASSISTANT =====================
    @FXML
    private void openChatAssistant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/chat/chat_view.fxml"));
            Parent root = loader.load();
            Stage chatStage = new Stage();
            chatStage.setTitle("Assistant IA - EventFlow");
            chatStage.setScene(new Scene(root, 550, 600));
            chatStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'assistant: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }

    private void animateFloatingButton() {
        if (chatFloatingButton != null) {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(1500), chatFloatingButton);
            st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.15); st.setToY(1.15);
            st.setCycleCount(3); st.setAutoReverse(true); st.play();
        }
    }

    // ===========================================
    // SECTION KPI - GESTION DYNAMIQUE DES KPI
    // pour Participants, Rôles, Catégories, Événements et Billets
    // ===========================================

    private void hideKPIs() {
        if (kpiContainer != null) {
            kpiContainer.setVisible(false);
            kpiContainer.setManaged(false);
            kpiContainer.getChildren().clear();
        }
    }

    /**
     * Affiche uniquement le KPI des participants (pour la page Gestion des participants)
     */
    public void showParticipantKPIs() {
        if (kpiContainer == null) return;

        // Vider le conteneur
        kpiContainer.getChildren().clear();

        // Créer uniquement la carte des participants
        VBox participantCard = createKPICard(
                "#dbeafe", "#93c5fd", "#1e40af", "#2563eb",
                "👥", "totalParticipantsKPILabel", "Total Participants"
        );

        kpiContainer.getChildren().add(participantCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);

        // Charger le nombre de participants
        loadParticipantData();
    }

    /**
     * Affiche uniquement le KPI des rôles (pour la page Gestion des rôles)
     */
    public void showRoleKPIs() {
        if (kpiContainer == null) return;

        // Vider le conteneur
        kpiContainer.getChildren().clear();

        // Créer uniquement la carte des rôles
        VBox roleCard = createKPICard(
                "#f0fdf4", "#bbf7d0", "#16a34a", "#22c55e",
                "📋", "totalRolesKPILabel", "Total Rôles"
        );

        kpiContainer.getChildren().add(roleCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);

        // Charger le nombre de rôles
        loadRoleData();
    }

    /**
     * Charge les données des participants
     */
    /**
     * Charge les données des participants (uniquement ceux avec le rôle Participant)
     */
    /**
     * Charge les données des participants (TOUS les utilisateurs)
     */
    private void loadParticipantData() {
        try {
            int totalUsers = 0;

            if (userService != null) {
                // Récupérer TOUS les utilisateurs
                java.util.List<UserModel> users = userService.getAllUsers();
                totalUsers = users.size(); // Compter TOUS les utilisateurs, pas seulement les participants

                System.out.println("📊 Nombre TOTAL d'utilisateurs: " + totalUsers);

                // DEBUG: Afficher le détail des utilisateurs
                for (UserModel user : users) {
                    String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "Aucun rôle";
                    System.out.println("   - " + user.getFirst_Name() + " " + user.getLast_Name() + " (" + roleName + ")");
                }
            }

            // Mettre à jour le label avec le nombre total
            updateLabel("totalParticipantsKPILabel", String.valueOf(totalUsers));

            // Forcer le rafraîchissement visuel
            Platform.runLater(() -> {
                if (kpiContainer != null) {
                    kpiContainer.requestLayout();
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
            updateLabel("totalParticipantsKPILabel", "0");
        }
    }
    /**
     * Charge les données des rôles
     */
    private void loadRoleData() {
        try {
            int totalRoles = 0;

            // Utiliser RoleService pour compter les rôles
            if (roleService != null) {
                totalRoles = roleService.getTotalRolesCount();
            } else {
                // Si RoleService n'est pas disponible, utiliser une valeur par défaut
                System.out.println("⚠️ RoleService non disponible, utilisation de la valeur par défaut");
                totalRoles = 5;
            }

            updateLabel("totalRolesKPILabel", String.valueOf(totalRoles));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des rôles: " + e.getMessage());
            updateLabel("totalRolesKPILabel", "0");
        }
    }

    /**
     * Affiche les KPI pour les catégories (ancienne méthode, conservée mais non utilisée)
     */
    public void showCategoryKPIs() {
        if (kpiContainer == null) return;
        kpiContainer.getChildren().clear();
        VBox categoryCard = createKPICard("#d1f4e0", "#95d5b2", "#1b5e20", "#2d6a4f", "📁", "totalCategoriesLabel", "Catégories");
        VBox eventCard = createKPICard("#cfe2ff", "#9ec5fe", "#004085", "#0056b3", "📅", "totalEventsLabel", "Événements");
        kpiContainer.getChildren().addAll(categoryCard, eventCard);
        kpiContainer.setVisible(true); kpiContainer.setManaged(true);
        loadCategoryData();
    }

    /**
     * Affiche les KPI pour les événements (ancienne méthode, conservée mais non utilisée)
     */
    public void showEventKPIs() {
        if (kpiContainer == null) return;
        kpiContainer.getChildren().clear();
        VBox eventCard = createKPICard("#cfe2ff", "#9ec5fe", "#004085", "#0056b3", "📅", "totalEventsLabel", "Événements");
        VBox ticketCard = createKPICard("#fff3cd", "#ffecb5", "#856404", "#856404", "🎫", "totalTicketsLabel", "Billets");
        kpiContainer.getChildren().addAll(eventCard, ticketCard);
        kpiContainer.setVisible(true); kpiContainer.setManaged(true);
        loadEventData();
    }

    /**
     * Affiche les KPI pour les tickets (ancienne méthode, conservée mais non utilisée)
     */
    public void showTicketKPIs() {
        if (kpiContainer == null) return;
        kpiContainer.getChildren().clear();
        VBox ticketCard = createKPICard("#fff3cd", "#ffecb5", "#856404", "#856404", "🎫", "totalTicketsLabel", "Billets");
        VBox eventCard = createKPICard("#cfe2ff", "#9ec5fe", "#004085", "#0056b3", "📅", "totalEventsLabel", "Événements");
        kpiContainer.getChildren().addAll(ticketCard, eventCard);
        kpiContainer.setVisible(true); kpiContainer.setManaged(true);
        loadTicketData();
    }

    private VBox createKPICard(String bgColor, String borderColor, String valueColor, String labelColor, String icon, String labelId, String text) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 16; -fx-padding: 8 20; -fx-border-color: %s; -fx-border-radius: 16; -fx-border-width: 1;", bgColor, borderColor));
        Label iconLabel = new Label(icon); iconLabel.setStyle("-fx-font-size: 20px;");
        Label valueLabel = new Label("0"); valueLabel.setId(labelId); valueLabel.setStyle(String.format("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: %s;", valueColor));
        Label textLabel = new Label(text); textLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 10px;", labelColor));
        card.getChildren().addAll(iconLabel, valueLabel, textLabel);
        return card;
    }

    private void loadCategoryData() {
        try {
            java.util.List<EventCategory> categories = categoryService.getAllCategoriesWithCount();
            int totalCategories = categories.size();
            int totalEvents = categories.stream().mapToInt(EventCategory::getEventCount).sum();
            updateLabel("totalCategoriesLabel", String.valueOf(totalCategories));
            updateLabel("totalEventsLabel", String.valueOf(totalEvents));
        } catch (Exception e) {
            updateLabel("totalCategoriesLabel", "0");
            updateLabel("totalEventsLabel", "0");
        }
    }

    private void loadEventData() {
        try {
            java.util.List<Event> events = eventService.getAllEvents();
            java.util.List<EventTicket> tickets = ticketService.getAllTickets();
            updateLabel("totalEventsLabel", String.valueOf(events.size()));
            updateLabel("totalTicketsLabel", String.valueOf(tickets.size()));
        } catch (Exception e) {
            updateLabel("totalEventsLabel", "0");
            updateLabel("totalTicketsLabel", "0");
        }
    }

    private void loadTicketData() {
        try {
            java.util.List<EventTicket> tickets = ticketService.getAllTickets();
            long totalEvents = tickets.stream().map(EventTicket::getEventId).distinct().count();
            updateLabel("totalTicketsLabel", String.valueOf(tickets.size()));
            updateLabel("totalEventsLabel", String.valueOf(totalEvents));
        } catch (Exception e) {
            updateLabel("totalTicketsLabel", "0");
            updateLabel("totalEventsLabel", "0");
        }
    }

    private void updateLabel(String id, String value) {
        if (kpiContainer == null || id == null) return;
        for (Node node : kpiContainer.getChildren()) {
            if (node instanceof VBox) {
                for (Node child : ((VBox) node).getChildren()) {
                    if (child instanceof Label && id.equals(((Label) child).getId())) {
                        ((Label) child).setText(value);
                        return;
                    }
                }
            }
        }
    }

    public void refreshKPIs() {
        if (kpiContainer == null || !kpiContainer.isVisible()) return;
        String currentTitle = pageTitle.getText().toLowerCase();
        if (currentTitle.contains("participant")) {
            loadParticipantData();
        } else if (currentTitle.contains("rôle") || currentTitle.contains("role")) {
            loadRoleData();
        } else if (currentTitle.contains("catégorie")) {
            loadCategoryData();
        } else if (currentTitle.contains("événement")) {
            loadEventData();
        } else if (currentTitle.contains("billet") || currentTitle.contains("ticket")) {
            loadTicketData();
        }
    }
}
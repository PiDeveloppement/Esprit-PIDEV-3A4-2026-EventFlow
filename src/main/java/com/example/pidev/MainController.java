package com.example.pidev;

import com.example.pidev.controller.chat.ChatController;
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
import javafx.scene.text.TextFlow;
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
import javafx.event.ActionEvent;    // ✅ À AJOUTER
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
    // ===================== CHAT PANEL FIELDS =====================
    @FXML private VBox chatPanel;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatBox;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button clearButton;
    @FXML private Button closeChatButton;
    @FXML private Label statusIndicator;
    // ===================== TOP BAR =====================
    @FXML private Label navDateLabel;
    @FXML private Label navTimeLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Text userInitialsText;
    @FXML private ImageView profileImageView;
    @FXML private StackPane initialsContainer;
    @FXML private StackPane avatarContainer;
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
    // Ajouté depuis event

    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;
    @FXML private TextField globalSearchField;
    private String lastSuggestion;
    private ChatController chatController;
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
                roleService = null;
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

        // Initialiser le chat
        initChatPanel();

        // Gestion de la touche Entrée dans le champ de saisie
        if (inputField != null) {
            inputField.setOnAction(event -> handleSendMessage());
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
                questionnairesToggleBtn, questionsBtn, settingsBtn
        };
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().removeAll("main-menu-button", "submenu-button", "sidebar-button-active");
                boolean isSub = btn == eventsListBtn || btn == categoriesBtn || btn == ticketsBtn ||
                        btn == rolesBtn || btn == inscriptionsBtn ||
                        btn == sponsorsListBtn || btn == sponsorPortalBtn || btn == budgetBtn || btn == contratsBtn ||
                        btn == sallesBtn || btn == equipementsBtn || btn == reservationsBtn ;
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
        else if (selected.contains("Historique")) showHistorique();
        else if (selected.contains("Passer le Quiz")) loadParticipantQuizView();  // Modifié pour utiliser la nouvelle méthode
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
            hideKPIs();
        }
    }

    // ===================== NAVIGATION - USERS =====================
    public void loadUserView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/user/user.fxml"));
            Parent root = loader.load();
            UserController controller = loader.getController();
            controller.setMainController(this);
            updatePageHeader("users");
            showParticipantKPIs();
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
            updatePageHeader("roles");
            showRoleKPIs();
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
            hideKPIs();
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
            hideKPIs();
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
            hideKPIs();
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
            hideKPIs();
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
            hideKPIs();
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
            hideKPIs();
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
            showEventKPIs();
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
            hideKPIs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche le calendrier interactif des événements (ajouté depuis event)
     */
    public void showEventCalendar() {
        try {
            System.out.println("📅 Chargement du calendrier des événements");
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/event-calendar.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventCalendarController) {
                ((EventCalendarController) controller).setMainController(this);
            }

            pageTitle.setText("Calendrier des événements");
            pageSubtitle.setText("Visualisez vos événements par mois");
            hideKPIs();

            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);

            System.out.println("✅ Calendrier chargé avec succès");
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement calendrier: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le calendrier: " + e.getMessage());
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
            hideKPIs();
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
            showCategoryKPIs();
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
            hideKPIs();
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
            hideKPIs();
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
            showTicketKPIs();
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
            hideKPIs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showTickets() { showTicketsList(); }

    // ===================== NAVIGATION - QUESTIONNAIRES =====================
    public void loadQuestionEditor() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/form_question.fxml", "Gestion des questions", "Gestion de la banque de données");
        hideKPIs();
    }

    public void loadResultatsView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Resultat.fxml", "Résultats", "Statistiques et aperçu global");
        hideKPIs();
    }

    /**
     * Charge la vue du quiz participant (ajouté depuis event)
     */
    private void loadParticipantQuizView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/questionnaire/Participant.fxml"));
            Parent view = loader.load();
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(view);
            pageTitle.setText("Quiz Participant");
            pageSubtitle.setText("Répondez aux questions");
            hideKPIs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadHistoriqueView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Historique.fxml", "Historique", "Consultation des anciens scores");
        hideKPIs();
    }

    /**
     * Méthode utilitaire pour charger les pages questionnaire (ajoutée depuis event)
     */
    public void loadView(String fxmlPath) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) {
                System.err.println("❌ Fichier introuvable : " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();
            setContent(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @FXML public void showParticipantQuiz() { collapseAllSubmenus(); loadParticipantQuizView(); }
    @FXML public void showHistorique() { collapseAllSubmenus(); loadHistoriqueView(); }

    // ===================== NAVIGATION - SETTINGS & PROFILE =====================
    public void loadSettingsView() {
        loadPage("/com/example/pidev/fxml/settings/settings.fxml", "settings");
        hideKPIs();
    }

    @FXML public void showProfile() {
        collapseAllSubmenus();
        loadPage("/com/example/pidev/fxml/user/profil.fxml", "profile");
        hideKPIs();
    }

    @FXML public void showSettings() {
        collapseAllSubmenus();
        loadPage("/com/example/pidev/fxml/settings/settings.fxml", "settings");
        hideKPIs();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/chat/chat_window.fxml"));
            Parent root = loader.load();

            Stage chatStage = new Stage();
            chatStage.setTitle("Assistant IA - EventFlow");
            chatStage.setScene(new Scene(root, 450, 600));
            chatStage.setResizable(false);
            chatStage.show();

            // Optionnel : stocker la référence
            ChatController chatController = loader.getController();

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
        kpiContainer.getChildren().clear();
        VBox participantCard = createKPICard(
                "#dbeafe", "#93c5fd", "#1e40af", "#2563eb",
                "👥", "totalParticipantsKPILabel", "Total Participants"
        );
        kpiContainer.getChildren().add(participantCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);
        loadParticipantData();
    }

    /**
     * Affiche uniquement le KPI des rôles (pour la page Gestion des rôles)
     */
    public void showRoleKPIs() {
        if (kpiContainer == null) return;
        kpiContainer.getChildren().clear();
        VBox roleCard = createKPICard(
                "#f0fdf4", "#bbf7d0", "#16a34a", "#22c55e",
                "📋", "totalRolesKPILabel", "Total Rôles"
        );
        kpiContainer.getChildren().add(roleCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);
        loadRoleData();
    }

    /**
     * Charge les données des participants (TOUS les utilisateurs)
     */
    private void loadParticipantData() {
        try {
            int totalUsers = 0;
            if (userService != null) {
                java.util.List<UserModel> users = userService.getAllUsers();
                totalUsers = users.size();
                System.out.println("📊 Nombre TOTAL d'utilisateurs: " + totalUsers);
            }
            updateLabel("totalParticipantsKPILabel", String.valueOf(totalUsers));
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
            if (roleService != null) {
                totalRoles = roleService.getTotalRolesCount();
            } else {
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
     * Affiche les KPI pour les catégories
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
     * Affiche les KPI pour les événements
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
     * Affiche les KPI pour les tickets
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

    // ===================== GETTERS =====================
    public VBox getPageContentContainer() {
        return pageContentContainer;
    }

// ===================== CHAT PANEL METHODS =====================

    @FXML
    private void toggleChatPanel() {
        if (chatPanel != null) {
            boolean isVisible = chatPanel.isVisible();
            chatPanel.setVisible(!isVisible);
            chatPanel.setManaged(!isVisible);

            // Focus sur le champ de saisie quand on ouvre
            if (!isVisible && inputField != null) {
                inputField.requestFocus();
            }

            System.out.println("🔄 Chat panel " + (isVisible ? "fermé" : "ouvert"));
        }
    }

    @FXML
    private void handleSendMessage() {
        if (inputField == null) return;

        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        // Afficher le message de l'utilisateur
        addMessageToChat("Vous", message, true);
        inputField.clear();

        // Simuler une réponse (à remplacer par votre logique)
        String response = getBotResponse(message);
        addMessageToChat("Assistant", response, false);
    }

    private void addMessageToChat(String sender, String message, boolean isUser) {
        if (chatBox == null) return;

        HBox messageBox = new HBox();
        messageBox.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 10, 5, 10));

        Label senderLabel = new Label(sender + ": ");
        senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isUser ? "#4CAF50" : "#2196F3") + ";");

        Text messageText = new Text(message);
        TextFlow messageFlow = new TextFlow(senderLabel, messageText);
        messageFlow.setStyle("-fx-background-color: " + (isUser ? "#E8F5E8" : "#E3F2FD") +
                "; -fx-background-radius: 15; -fx-padding: 10;");
        messageFlow.setMaxWidth(350);

        messageBox.getChildren().add(messageFlow);
        chatBox.getChildren().add(messageBox);

        // Scroll vers le bas
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    private String getBotResponse(String userMessage) {
        // Logique simple - à remplacer par votre ChatbotService
        userMessage = userMessage.toLowerCase();

        if (userMessage.contains("combien") && userMessage.contains("utilisateur")) {
            return "Il y a actuellement 157 utilisateurs inscrits sur la plateforme.";
        } else if (userMessage.contains("admin")) {
            return "Les administrateurs sont: Ons Abdesslem, Ahmed Ben Salem, Sarra Mansour.";
        } else if (userMessage.contains("nouveau") && userMessage.contains("mois")) {
            return "15 nouveaux utilisateurs se sont inscrits ce mois-ci.";
        } else {
            return "Je n'ai pas compris votre question. Pouvez-vous reformuler ?";
        }
    }

    @FXML
    private void handleClearChat() {
        if (chatBox != null) {
            chatBox.getChildren().clear();
            addWelcomeMessage();
        }
    }

    // ===================== CHAT METHODS =====================

    @FXML
    private void handleSuggestion(ActionEvent event) {
        try {
            // Récupérer le bouton qui a déclenché l'événement
            Button sourceButton = (Button) event.getSource();
            String suggestion = sourceButton.getText();

            // Nettoyer le texte pour enlever les emojis et garder la question
            String cleanSuggestion = suggestion.replaceAll("[📊👥📅✨]", "").trim();

            System.out.println("💡 Suggestion sélectionnée: " + cleanSuggestion);

            // Ouvrir le chat si fermé
            if (!chatPanel.isVisible()) {
                toggleChatPanel();
            }

            // Mettre la suggestion dans le champ de saisie
            if (inputField != null) {
                inputField.setText(cleanSuggestion);
                // Envoyer automatiquement le message
                handleSendMessage();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur dans handleSuggestion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addWelcomeMessage() {
        if (chatBox == null) return;

        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 10, 5, 10));

        VBox welcomeBox = new VBox(5);
        welcomeBox.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 15; -fx-padding: 15;");
        welcomeBox.setMaxWidth(350);

        Label titleLabel = new Label("🤖 Assistant IA - EventFlow");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3; -fx-font-size: 14px;");

        Label messageLabel = new Label("🎉 Bonjour ! Je suis votre assistant de gestion.\n" +
                "Posez-moi des questions sur les utilisateurs, événements, etc.");
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");

        Label hintLabel = new Label("💡 Suggestions: 'Combien d'utilisateurs ?', 'Liste des admins', 'Nouveaux ce mois'");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");

        welcomeBox.getChildren().addAll(titleLabel, messageLabel, hintLabel);
        messageBox.getChildren().add(welcomeBox);
        chatBox.getChildren().add(messageBox);
    }

    // Initialisation du chat (à appeler dans initialize())
    private void initChatPanel() {
        if (chatBox != null) {
            addWelcomeMessage();
        }
        if (statusIndicator != null) {
            statusIndicator.setText("● Connecté");
        }
    }



}
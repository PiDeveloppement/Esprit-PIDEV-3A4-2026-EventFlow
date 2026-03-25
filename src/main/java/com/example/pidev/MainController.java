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
    // Sponsor paths
    private static final String SPONSOR_PORTAL_FXML = "/com/example/pidev/fxml/Sponsor/sponsor_portal.fxml";
    private static final String SPONSOR_ADMIN_FXML  = "/com/example/pidev/fxml/Sponsor/sponsor_admin.fxml";

    // Budget/Depense paths (adaptés selon votre structure)
    private static final String BUDGET_LIST_FXML    = "/com/example/pidev/fxml/budget/budget.fxml";
    private static final String DEPENSE_LIST_FXML   = "/com/example/pidev/fxml/depense/depense-modern.fxml";

    // ===================== CENTER CONTENT =====================
    @FXML private VBox pageContentContainer;

    // ===================== PAGE HEADER =====================
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private HBox kpiContainer;

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
    @FXML private Button sponsorsListBtn;       // Admin Sponsors
    @FXML private Button sponsorPortalBtn;      // Portail Sponsor
    @FXML private Button budgetBtn;              // Budget
    @FXML private Button contratsBtn;            // Dépenses/Contrats

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
    @FXML private Button participantQuizBtn;

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

        // Initialiser les services pour les KPI
        eventService = new EventService();
        categoryService = new EventCategoryService();
        ticketService = new EventTicketService();

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
        participantQuizBtn.setOnAction(event -> {
            loadParticipantQuizView();
        });
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
                    System.out.println("✅ Image de profil chargée");
                } catch (Exception e) {
                    System.err.println("Erreur chargement photo: " + e.getMessage());
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
        if (!"events".equals(currentMenu) && eventsSubmenu != null) {
            eventsSubmenu.setVisible(false);
            eventsSubmenu.setManaged(false);
            if (eventsArrow != null) eventsArrow.setText("▶");
        }
        if (!"users".equals(currentMenu) && usersSubmenu != null) {
            usersSubmenu.setVisible(false);
            usersSubmenu.setManaged(false);
            if (usersArrow != null) usersArrow.setText("▶");
        }
        if (!"sponsors".equals(currentMenu) && sponsorsSubmenu != null) {
            sponsorsSubmenu.setVisible(false);
            sponsorsSubmenu.setManaged(false);
            if (sponsorsArrow != null) sponsorsArrow.setText("▶");
        }
        if (!"resources".equals(currentMenu) && resourcesSubmenu != null) {
            resourcesSubmenu.setVisible(false);
            resourcesSubmenu.setManaged(false);
            if (resourcesArrow != null) resourcesArrow.setText("▶");
        }
        if (!"questionnaires".equals(currentMenu) && questionnairesSubmenu != null) {
            questionnairesSubmenu.setVisible(false);
            questionnairesSubmenu.setManaged(false);
            if (questionnairesArrow != null) questionnairesArrow.setText("▶");
        }
    }

    private void collapseAllSubmenus() {
        if (eventsSubmenu != null) {
            eventsSubmenu.setVisible(false);
            eventsSubmenu.setManaged(false);
            if (eventsArrow != null) eventsArrow.setText("▶");
        }
        if (usersSubmenu != null) {
            usersSubmenu.setVisible(false);
            usersSubmenu.setManaged(false);
            if (usersArrow != null) usersArrow.setText("▶");
        }
        if (sponsorsSubmenu != null) {
            sponsorsSubmenu.setVisible(false);
            sponsorsSubmenu.setManaged(false);
            if (sponsorsArrow != null) sponsorsArrow.setText("▶");
        }
        if (resourcesSubmenu != null) {
            resourcesSubmenu.setVisible(false);
            resourcesSubmenu.setManaged(false);
            if (resourcesArrow != null) resourcesArrow.setText("▶");
        }
        if (questionnairesSubmenu != null) {
            questionnairesSubmenu.setVisible(false);
            questionnairesSubmenu.setManaged(false);
            if (questionnairesArrow != null) questionnairesArrow.setText("▶");
        }
    }

    private void openSponsorsSubmenu() {
        if (sponsorsSubmenu != null) {
            sponsorsSubmenu.setVisible(true);
            sponsorsSubmenu.setManaged(true);
            if (sponsorsArrow != null) sponsorsArrow.setText("▼");
        }
    }

    private void setActiveButton(Button button) {
        Button[] allButtons = {
                dashboardBtn, eventsToggleBtn, eventsListBtn, categoriesBtn, ticketsBtn,
                usersToggleBtn, rolesBtn, inscriptionsBtn,
                sponsorsBtn, sponsorsListBtn, sponsorPortalBtn, budgetBtn, contratsBtn,
                resourcesToggleBtn, sallesBtn, equipementsBtn, reservationsBtn,
                questionnairesToggleBtn, questionsBtn, reponsesBtn,
                settingsBtn
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
            for (Button btn : allButtons) {
                if (btn != null) btn.getStyleClass().remove("sidebar-button-active");
            }
            button.getStyleClass().add("sidebar-button-active");
        }
    }

    // ===================== CONFIGURATION SIDEBAR =====================
    private void configureSidebarButtons() {
        // Dashboard
        if (dashboardBtn != null) {
            dashboardBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(dashboardBtn);
                loadDashboardView();
            });
        }

        // Événements toggle
        if (eventsToggleBtn != null) {
            eventsToggleBtn.setOnAction(e -> {
                toggleEvents();
                collapseOtherSubmenus("events");
                setActiveButton(eventsToggleBtn);
            });
        }

        // Participants toggle
        if (usersToggleBtn != null) {
            usersToggleBtn.setOnAction(e -> {
                toggleUsers();
                collapseOtherSubmenus("users");
                setActiveButton(usersToggleBtn);
            });
        }

        // Sponsors toggle
        if (sponsorsBtn != null) {
            sponsorsBtn.setOnAction(e -> {
                toggleSponsors();
                collapseOtherSubmenus("sponsors");
                setActiveButton(sponsorsBtn);
            });
        }

        // Ressources toggle
        if (resourcesToggleBtn != null) {
            resourcesToggleBtn.setOnAction(e -> {
                toggleResources();
                collapseOtherSubmenus("resources");
                setActiveButton(resourcesToggleBtn);
            });
        }

        // Questionnaires toggle
        if (questionnairesToggleBtn != null) {
            questionnairesToggleBtn.setOnAction(e -> {
                toggleQuestionnaires();
                collapseOtherSubmenus("questionnaires");
                setActiveButton(questionnairesToggleBtn);
            });
        }

        // Sous-menus Événements
        if (eventsListBtn != null) {
            eventsListBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(eventsListBtn);
                showEventsList();
            });
        }

        if (categoriesBtn != null) {
            categoriesBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(categoriesBtn);
                showCategories();
            });
        }

        if (ticketsBtn != null) {
            ticketsBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(ticketsBtn);
                showTicketsList();
            });
        }

        // Sous-menus Participants
        if (rolesBtn != null) {
            rolesBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(rolesBtn);
                loadRoleView();
            });
        }

        if (inscriptionsBtn != null) {
            inscriptionsBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(inscriptionsBtn);
                loadUserView();
            });
        }

        // ===================== SPONSORS SUBMENU =====================
        if (sponsorsListBtn != null) {
            sponsorsListBtn.setOnAction(e -> {
                openSponsorsSubmenu();
                setActiveButton(sponsorsListBtn);
                showSponsorsAdmin();
            });
        }

        if (sponsorPortalBtn != null) {
            sponsorPortalBtn.setOnAction(e -> {
                openSponsorsSubmenu();
                setActiveButton(sponsorPortalBtn);
                showSponsorPortal(lastSponsorPortalEmail);
            });
        }

        if (budgetBtn != null) {
            budgetBtn.setOnAction(e -> {
                openSponsorsSubmenu();
                setActiveButton(budgetBtn);
                showBudget();
            });
        }

        if (contratsBtn != null) {
            contratsBtn.setOnAction(e -> {
                openSponsorsSubmenu();
                setActiveButton(contratsBtn);
                showDepenses();
            });
        }

        // Sous-menus Ressources
        if (sallesBtn != null) {
            sallesBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(sallesBtn);
                loadSallesView();
            });
        }

        if (equipementsBtn != null) {
            equipementsBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(equipementsBtn);
                loadEquipementsView();
            });
        }

        if (reservationsBtn != null) {
            reservationsBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(reservationsBtn);
                loadReservationsView();
            });
        }

        // Sous-menus Questionnaires
        if (questionsBtn != null) {
            questionsBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(questionsBtn);
                showQuestionEditor();
            });
        }

        if (reponsesBtn != null) {
            reponsesBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(reponsesBtn);
                showResultats();
            });
        }

        // Settings
        if (settingsBtn != null) {
            settingsBtn.setOnAction(e -> {
                collapseAllSubmenus();
                setActiveButton(settingsBtn);
                loadSettingsView();
            });
        }

        // Logout
        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> logout());
        }
    }

    // ===================== SIDEBAR ROLE CONFIG =====================
    private void configureSidebarByRole() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole();

        if (role == null) {
            hideAllButtons();
            return;
        }

        role = role.trim().toLowerCase();
        System.out.println("🔧 Configuration sidebar pour le rôle: " + role);

        hideAllButtons();

        switch (role) {
            case "admin":
            case "admin2":
            case "admin3":
            case "admin4":
                showAllButtons();
                System.out.println("✅ Admin: tous les boutons affichés");
                break;

            case "organisateur":
            case "organisateur2":
                showAllButtons();
                hideNode(usersToggleBtn);
                hideNode(usersSubmenu);
                System.out.println("✅ Organisateur: tous sauf gestion utilisateurs");
                break;

            case "sponsor":
            case "sponsor2":
            case "sponsor3":
                showOnlySponsorButtons();
                System.out.println("✅ Sponsor: dashboard, sponsors, budget");
                break;

            case "participant":
            case "default":
            case "invité":
                showOnlyParticipantButtons();
                System.out.println("✅ Participant/Default: uniquement dashboard");
                break;

            default:
                hideAllButtons();
                System.out.println("⚠️ Rôle inconnu: " + role);
                break;
        }
    }

    private void showOnlyParticipantButtons() {
        showNode(dashboardBtn);
        hideNode(eventsToggleBtn);
        hideNode(usersToggleBtn);
        hideNode(sponsorsBtn);
        hideNode(resourcesToggleBtn);
        hideNode(questionnairesToggleBtn);
        hideNode(settingsBtn);
        hideNode(budgetBtn);
        hideAllSubmenus();
    }

    private void showOnlySponsorButtons() {
        showNode(dashboardBtn);
        showNode(sponsorsBtn);
        showNode(budgetBtn);
        hideNode(eventsToggleBtn);
        hideNode(usersToggleBtn);
        hideNode(resourcesToggleBtn);
        hideNode(questionnairesToggleBtn);
        hideNode(settingsBtn);
        hideAllSubmenus();
    }

    private void hideAllButtons() {
        hideNode(dashboardBtn);
        hideNode(sponsorsBtn);
        hideNode(eventsToggleBtn);
        hideNode(usersToggleBtn);
        hideNode(resourcesToggleBtn);
        hideNode(questionnairesToggleBtn);
        hideNode(settingsBtn);
        hideNode(budgetBtn);
    }

    private void showAllButtons() {
        Node[] main = {
                dashboardBtn, eventsToggleBtn, usersToggleBtn, sponsorsBtn,
                resourcesToggleBtn, questionnairesToggleBtn,
                settingsBtn, budgetBtn
        };
        for (Node n : main) showNode(n);
        collapseAllSubmenus();
    }

    private void hideNode(Node node) {
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }

    private void showNode(Node node) {
        if (node != null) {
            node.setVisible(true);
            node.setManaged(true);
        }
    }

    private void hideAllSubmenus() {
        VBox[] submenus = {eventsSubmenu, usersSubmenu, sponsorsSubmenu, resourcesSubmenu, questionnairesSubmenu};
        Text[] arrows = {eventsArrow, usersArrow, sponsorsArrow, resourcesArrow, questionnairesArrow};

        for (VBox submenu : submenus) {
            if (submenu != null) {
                submenu.setVisible(false);
                submenu.setManaged(false);
            }
        }
        for (Text arrow : arrows) {
            if (arrow != null) arrow.setText("▶");
        }
    }

    // ===================== RECHERCHE GLOBALE =====================
    private void setupGlobalSearch() {
        if (globalSearchField != null) {
            globalSearchField.setOnAction(event -> {
                String query = globalSearchField.getText().trim();
                if (!query.isEmpty()) {
                    performSimpleSearch(query);
                }
            });

            globalSearchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    globalSearchField.clear();
                }
            });
        }
    }

    private void performSimpleSearch(String query) {
        String lowerQuery = query.toLowerCase();
        ObservableList<String> results = FXCollections.observableArrayList();

        if ("dashboard".contains(lowerQuery)) results.add("📊 Dashboard");
        if ("événements".contains(lowerQuery) || "events".contains(lowerQuery)) {
            results.add("📅 Événements");
            results.add("   📋 Liste des événements");
            results.add("   🏷️ Catégories");
            results.add("   🎫 Billets");
        }
        if ("participants".contains(lowerQuery) || "users".contains(lowerQuery)) {
            results.add("👥 Participants");
            results.add("   👤 Rôles");
            results.add("   📝 Inscriptions");
        }
        if ("sponsors".contains(lowerQuery)) {
            results.add("💼 Sponsors");
            results.add("   📋 Liste sponsors");
            results.add("   🔑 Portail Sponsor");
            results.add("   💰 Budget");
            results.add("   📄 Dépenses");
        }
        if ("ressources".contains(lowerQuery)) {
            results.add("📦 Ressources");
            results.add("   💻 Équipements");
            results.add("   🏢 Salles");
            results.add("   📅 Réservations");
        }
        if ("questionnaires".contains(lowerQuery)) {
            results.add("📝 Questionnaires");
            results.add("   ❓ Questions");
            results.add("   📊 Résultats");
            results.add("   📜 Historique");
            results.add("   🎯 Passer le Quiz");
        }
        if ("paramètres".contains(lowerQuery) || "settings".contains(lowerQuery)) {
            results.add("⚙️ Paramètres");
        }

        if (results.isEmpty()) {
            showSimpleAlert("Aucun résultat", "Aucun résultat trouvé pour: " + query);
        } else {
            showSimpleResultsPopup(query, results);
        }
    }

    private void showSimpleResultsPopup(String query, ObservableList<String> results) {
        Stage stage = new Stage();
        stage.setTitle("Résultats de recherche");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20; -fx-background-color: white;");
        root.setPrefWidth(400);
        root.setMaxWidth(400);

        Label titleLabel = new Label("Résultats pour: \"" + query + "\"");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");

        ListView<String> listView = new ListView<>(results);
        listView.setPrefHeight(Math.min(300, results.size() * 40));
        listView.setStyle("-fx-background-color: transparent;");

        listView.setOnMouseClicked(event -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigateFromSearch(selected);
                stage.close();
            }
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.getChildren().add(closeBtn);

        root.getChildren().addAll(titleLabel, listView, buttonBar);
        stage.setScene(new Scene(root));
        stage.show();
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
    @FXML
    public void showSponsorsAdmin() {
        openSponsorsSubmenu();
        setActiveButton(sponsorsListBtn);
        loadIntoCenter(SPONSOR_ADMIN_FXML, (SponsorAdminController ctrl) -> {
            // Initialisation si nécessaire
        });
        updatePageHeader("sponsorsList");
    }

    @FXML
    public void showSponsorPortal(String email) {
        openSponsorsSubmenu();
        setActiveButton(sponsorPortalBtn);
        if (email != null && !email.isBlank()) setLastSponsorPortalEmail(email);

        loadIntoCenter(SPONSOR_PORTAL_FXML, (SponsorPortalController ctrl) -> {
            String e = getLastSponsorPortalEmail();
            if (e != null && !e.isBlank()) ctrl.setInitialEmail(e);
        });
        updatePageHeader("sponsorPortal");
    }

    @FXML
    public void showSponsorPortal() {
        showSponsorPortal(lastSponsorPortalEmail);
    }

    @FXML
    public void showBudget() {
        openSponsorsSubmenu();
        setActiveButton(budgetBtn);
        loadIntoCenter(BUDGET_LIST_FXML, null);
        updatePageHeader("budget");
    }

    @FXML
    public void showDepenses() {
        openSponsorsSubmenu();
        setActiveButton(contratsBtn);
        loadIntoCenter(DEPENSE_LIST_FXML, null);
        updatePageHeader("depenses");
    }

    // Méthodes de compatibilité (pour les anciens appels)
    public void showSponsors() { showSponsorsAdmin(); }

    // ===================== CHARGEMENT DYNAMIQUE =====================
    public <T> void loadIntoCenter(String fxmlPath, Consumer<T> controllerConsumer) {
        try {
            if (pageContentContainer == null) {
                throw new IllegalStateException("pageContentContainer est null. Vérifiez fx:id dans MainLayout.fxml");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            pageContentContainer.getChildren().setAll(page);

            if (controllerConsumer != null) {
                @SuppressWarnings("unchecked")
                T ctrl = (T) loader.getController();
                controllerConsumer.accept(ctrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showEmptyPage("Erreur", "Impossible de charger : " + fxmlPath + "\n" + e.getMessage());
        }
    }

    public void loadPage(String fxmlPath, String pageKey) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();

            if ("profile".equals(pageKey) && loader.getController() instanceof ProfilController) {
                ((ProfilController) loader.getController()).setMainController(this);
            }

            if ("dashboard".equals(pageKey) && loader.getController() instanceof DashboardController) {
                dashboardController = (DashboardController) loader.getController();
                dashboardController.setMainController(this);
            }

            updatePageHeader(pageKey);
            pageContentContainer.getChildren().setAll(page);

        } catch (IOException e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page: " + fxmlPath);
        }
    }

    public void loadPage(String fxmlPath) {
        String pageKey = extractPageKeyFromPath(fxmlPath);
        loadPage(fxmlPath, pageKey);
    }

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
        if (pageInfo != null) {
            if (pageTitle != null) pageTitle.setText(pageInfo.title);
            if (pageSubtitle != null) pageSubtitle.setText(pageInfo.subtitle);
        }
    }

    // ===================== NAVIGATION - DASHBOARD =====================
    public void loadDashboardView() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole();

        if (role != null) {
            loadPage("/com/example/pidev/fxml/dashboard/dashboard.fxml", "dashboard");
        }
    }

    // ===================== NAVIGATION - USERS =====================
    public void loadUserView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/user/user.fxml")
            );
            Parent root = loader.load();
            UserController controller = loader.getController();
            controller.setMainController(this);

            updatePageHeader("users");
            pageContentContainer.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des utilisateurs: " + e.getMessage());
        }
    }

    public void loadRoleView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/role/role.fxml")
            );
            Parent root = loader.load();

            RoleController controller = loader.getController();
            controller.setMainController(this);

            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("roles");

        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des rôles: " + e.getMessage());
        }
    }

    public void loadEditUserPage(UserModel user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/user/editUser.fxml")
            );
            Parent root = loader.load();
            EditUserController controller = loader.getController();
            controller.setMainController(this);
            controller.setUser(user);
            pageContentContainer.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadEditRolePage(Role role) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/role/editRole.fxml")
            );
            Parent root = loader.load();

            com.example.pidev.controller.role.EditRoleController controller = loader.getController();
            controller.setEditMode(role);
            controller.setMainController(this);

            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("roles");

        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page de modification: " + e.getMessage());
        }
    }

    public void loadAddRolePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/role/editRole.fxml")
            );
            Parent root = loader.load();

            com.example.pidev.controller.role.EditRoleController controller = loader.getController();
            controller.setAddMode();
            controller.setMainController(this);

            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("roles");

        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page d'ajout: " + e.getMessage());
        }
    }

    // ===================== NAVIGATION - RESSOURCES =====================
    public void loadSallesView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/resource/salle.fxml")
            );
            Parent root = loader.load();
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("salles");
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des salles: " + e.getMessage());
        }
    }

    public void loadEquipementsView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/resource/equipement.fxml")
            );
            Parent root = loader.load();
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("equipements");
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des équipements: " + e.getMessage());
        }
    }

    public void loadReservationsView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/resource/reservation.fxml")
            );
            Parent root = loader.load();
            pageContentContainer.getChildren().setAll(root);
            updatePageHeader("reservations");
        } catch (Exception e) {
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page des réservations: " + e.getMessage());
        }
    }

    @FXML
    public void showSalles() {
        collapseAllSubmenus();
        setActiveButton(sallesBtn);
        loadSallesView();
    }

    @FXML
    public void showEquipements() {
        collapseAllSubmenus();
        setActiveButton(equipementsBtn);
        loadEquipementsView();
    }

    @FXML
    public void showReservations() {
        collapseAllSubmenus();
        setActiveButton(reservationsBtn);
        loadReservationsView();
    }

    // ===================== NAVIGATION - EVENTS =====================
    public void showEventsList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/event-list.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventListController) {
                ((EventListController) controller).setMainController(this);
            }

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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/event-form.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventFormController) {
                ((EventFormController) controller).setMainController(this);
                if (event != null) ((EventFormController) controller).setEvent(event);
            }

            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement formulaire événement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche le calendrier interactif des événements
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/event-view.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventViewController) {
                ((EventViewController) controller).setMainController(this);
                ((EventViewController) controller).setEvent(event);
            }

            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement vue événement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/category-list.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof CategoryListController) {
                ((CategoryListController) controller).setMainController(this);
            }

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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/category-form.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof CategoryFormController) {
                ((CategoryFormController) controller).setMainController(this);
                if (category != null) ((CategoryFormController) controller).setCategory(category);
            }

            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement formulaire catégorie: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void showCategoryView(EventCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/category-view.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof CategoryViewController) {
                ((CategoryViewController) controller).setMainController(this);
                ((CategoryViewController) controller).setCategory(category);
            }

            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement vue catégorie: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showTicketsList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/ticket-list.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventTicketListController) {
                ((EventTicketListController) controller).setMainController(this);
            }

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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/event/ticket-view.fxml")
            );
            Parent page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventTicketViewController) {
                ((EventTicketViewController) controller).setMainController(this);
                ((EventTicketViewController) controller).setTicket(ticket);
            }

            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(page);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement vue ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showTickets() {
        showTicketsList();
    }

    // ===================== NAVIGATION - QUESTIONNAIRES =====================
    public void loadQuestionEditor() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/form_question.fxml",
                "Gestion des questions", "Gestion de la banque de données");
    }

    public void loadResultatsView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Resultat.fxml",
                "Résultats", "Statistiques et aperçu global");
    }
    // 3. Méthode pour charger la vue
    private void loadParticipantQuizView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/questionnaire/Participant.fxml"));
            Parent view = loader.load();

            // Nettoyer et afficher dans le conteneur principal
            pageContentContainer.getChildren().clear();
            pageContentContainer.getChildren().add(view);

            // Optionnel : Mettre à jour le header
            pageTitle.setText("Quiz Participant");
            pageSubtitle.setText("Répondez aux questions");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadHistoriqueView() {
        loadQuestionnairePage("/com/example/pidev/fxml/questionnaire/Historique.fxml",
                "Historique", "Consultation des anciens scores");
    }
    // Ajoutez cette méthode dans MainController.java
    public void loadView(String fxmlPath) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) {
                System.err.println("❌ Fichier introuvable : " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();

            // On réutilise la méthode setContent que nous avons corrigée ensemble
            setContent(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadQuestionnairePage(String fxmlPath, String title, String subtitle) {
        try {
            URL fileUrl = getClass().getResource(fxmlPath);
            if (fileUrl == null) {
                System.err.println("❌ Fichier FXML introuvable: " + fxmlPath);
                showSimpleAlert("Erreur", "Fichier introuvable: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fileUrl);
            Parent root = loader.load();

            setContent(root);
            if (pageTitle != null) pageTitle.setText(title);
            if (pageSubtitle != null) pageSubtitle.setText(subtitle);

            System.out.println("✅ Page chargée: " + title);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            showSimpleAlert("Erreur", "Impossible de charger la page: " + e.getMessage());
        }
    }
    public VBox getPageContentContainer() {
        return pageContentContainer;
    }

    /**
     * Rafraîchit la page des événements publics (vitrine participant)
     */
    public void refreshEventsFrontPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/front/events.fxml")
            );
            Parent page = loader.load();

            if (pageContentContainer != null) {
                pageContentContainer.getChildren().clear();
                pageContentContainer.getChildren().add(page);
                System.out.println("✅ Page événements publics rafraîchie");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur rafraîchissement page événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showQuestionEditor() {
        collapseAllSubmenus();
        setActiveButton(questionsBtn);
        loadQuestionEditor();
    }

    @FXML
    public void showResultats() {
        collapseAllSubmenus();
        setActiveButton(reponsesBtn);
        loadResultatsView();
    }

    @FXML
    public void showParticipantQuiz() {
        collapseAllSubmenus();
        // Assurez-vous que le chemin est exact par rapport au dossier resources
        loadView("/com/example/pidev/fxml/questionnaire/Participant.fxml");
    }

    @FXML
    public void showHistorique() {
        collapseAllSubmenus();
        loadHistoriqueView();
    }


    // ===================== NAVIGATION - SETTINGS & PROFILE =====================
    public void loadSettingsView() {
        loadPage("/com/example/pidev/fxml/settings/settings.fxml", "settings");
    }

    @FXML
    public void showProfile() {
        collapseAllSubmenus();
        loadPage("/com/example/pidev/fxml/user/profil.fxml", "profile");
    }

    @FXML
    public void showSettings() {
        collapseAllSubmenus();
        loadPage("/com/example/pidev/fxml/settings/settings.fxml", "settings");
    }

    // ===================== LOGOUT =====================
    @FXML
    public void logout() {
        try {
            if (dashboardController != null) {
                dashboardController.cleanup();
            }

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

    @FXML
    private void handleExit() {
        System.out.println("🚪 Fermeture de l'application");
        System.exit(0);
    }

    // ===================== REFRESH METHODS =====================
    public void refreshDashboard() {
        if (dashboardController != null) {
            System.out.println("🔄 Rafraîchissement du dashboard depuis MainController");
            dashboardController.refreshData();
        }
    }

    public void onEventSaved() {
        refreshDashboard();
    }

    public void onParticipantSaved() {
        refreshDashboard();
    }

    public void onInscriptionSaved() {
        refreshDashboard();
    }

    public void cleanup() {
        if (dashboardController != null) {
            dashboardController.cleanup();
        }
    }

    public void refreshSidebarForRole() {
        System.out.println("🔄 Rafraîchissement de la sidebar");
        configureSidebarByRole();
        collapseAllSubmenus();
    }

    // ===================== CONTENT SETTERS =====================
    // 1. La méthode de base (celle qui fait le travail réel)
    public void setContent(Parent node) {
        if (pageContentContainer != null) {
            pageContentContainer.getChildren().setAll(node);
        } else {
            System.err.println("❌ pageContentContainer est null dans setContent()");
        }
    }

    // 2. La méthode pour ajouter le titre (elle appelle la méthode #1)
    public void setContent(Parent root, String title) {
        setContent(root, title, "");
    }

    // 3. La méthode pour ajouter titre + sous-titre (elle met à jour l'UI et appelle la méthode #1)
    public void setContent(Parent root, String title, String subtitle) {
        if (pageTitle != null) pageTitle.setText(title);
        if (pageSubtitle != null) pageSubtitle.setText(subtitle);
        setContent(root);
    }

    // ===================== PAGE DE SECOURS =====================
    private void showEmptyPage(String title, String subtitle) {
        if (pageContentContainer == null) return;

        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(24));
        box.setStyle("""        
                -fx-background-color: white;
                -fx-background-radius: 14;
                -fx-border-radius: 14;
                -fx-border-color: #e2e8f0;
                -fx-border-width: 1;
                """);

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        box.getChildren().addAll(t, s);
        pageContentContainer.getChildren().setAll(box);
    }

    private void showSimpleAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    /**
     * Méthode appelée depuis BudgetListController pour naviguer vers la page budget
     */
    @FXML
    public void onBudget() {
        System.out.println("💰 Navigation vers la page budget");
        collapseAllSubmenus();
        setActiveButton(budgetBtn);
        showBudget();
    }

    // ===========================================
    // SECTION KPI - GESTION DYNAMIQUE DES KPI
    // pour Catégories, Événements et Billets
    // ===========================================

    /**
     * Cache tous les KPI
     */
    private void hideKPIs() {
        if (kpiContainer != null) {
            kpiContainer.setVisible(false);
            kpiContainer.setManaged(false);
            kpiContainer.getChildren().clear();
        }
    }

    /**
     * Affiche les KPI pour la page des CATÉGORIES
     * Cartes : [Catégories] [Événements]
     */
    public void showCategoryKPIs() {
        if (kpiContainer == null) return;

        kpiContainer.getChildren().clear();

        VBox categoryCard = createKPICard(
                "#d1f4e0", "#95d5b2", "#1b5e20", "#2d6a4f",
                "📁", "totalCategoriesLabel", "Catégories"
        );

        VBox eventCard = createKPICard(
                "#cfe2ff", "#9ec5fe", "#004085", "#0056b3",
                "📅", "totalEventsLabel", "Événements"
        );

        kpiContainer.getChildren().addAll(categoryCard, eventCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);

        loadCategoryData();
    }

    /**
     * Affiche les KPI pour la page des ÉVÉNEMENTS
     * Cartes : [Événements] [Tickets]
     */
    public void showEventKPIs() {
        if (kpiContainer == null) return;

        kpiContainer.getChildren().clear();

        VBox eventCard = createKPICard(
                "#cfe2ff", "#9ec5fe", "#004085", "#0056b3",
                "📅", "totalEventsLabel", "Événements"
        );

        VBox ticketCard = createKPICard(
                "#fff3cd", "#ffecb5", "#856404", "#856404",
                "🎫", "totalTicketsLabel", "Billets"
        );

        kpiContainer.getChildren().addAll(eventCard, ticketCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);

        loadEventData();
    }

    /**
     * Affiche les KPI pour la page des TICKETS
     * Cartes : [Tickets] [Événements concernés]
     */
    public void showTicketKPIs() {
        if (kpiContainer == null) return;

        kpiContainer.getChildren().clear();

        VBox ticketCard = createKPICard(
                "#fff3cd", "#ffecb5", "#856404", "#856404",
                "🎫", "totalTicketsLabel", "Billets"
        );

        VBox eventCard = createKPICard(
                "#cfe2ff", "#9ec5fe", "#004085", "#0056b3",
                "📅", "totalEventsLabel", "Événements"
        );

        kpiContainer.getChildren().addAll(ticketCard, eventCard);
        kpiContainer.setVisible(true);
        kpiContainer.setManaged(true);

        loadTicketData();
    }

    /**
     * Crée une carte KPI (méthode utilitaire)
     */
    private VBox createKPICard(String bgColor, String borderColor,
                               String valueColor, String labelColor,
                               String icon, String labelId, String text) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 16; -fx-padding: 8 20; " +
                        "-fx-border-color: %s; -fx-border-radius: 16; -fx-border-width: 1;",
                bgColor, borderColor
        ));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label valueLabel = new Label("0");
        valueLabel.setId(labelId);
        valueLabel.setStyle(String.format(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: %s;", valueColor
        ));

        Label textLabel = new Label(text);
        textLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 10px;", labelColor
        ));

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
            System.out.println("✅ KPI Catégories mis à jour: " + totalCategories + " catégories, " + totalEvents + " événements");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement KPI catégories: " + e.getMessage());
            updateLabel("totalCategoriesLabel", "0");
            updateLabel("totalEventsLabel", "0");
        }
    }

    private void loadEventData() {
        try {
            java.util.List<Event> events = eventService.getAllEvents();
            int totalEvents = events.size();

            java.util.List<EventTicket> tickets = ticketService.getAllTickets();
            int totalTickets = tickets.size();

            updateLabel("totalEventsLabel", String.valueOf(totalEvents));
            updateLabel("totalTicketsLabel", String.valueOf(totalTickets));
            System.out.println("✅ KPI Événements mis à jour: " + totalEvents + " événements, " + totalTickets + " tickets");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement KPI événements: " + e.getMessage());
            updateLabel("totalEventsLabel", "0");
            updateLabel("totalTicketsLabel", "0");
        }
    }

    private void loadTicketData() {
        try {
            java.util.List<EventTicket> tickets = ticketService.getAllTickets();
            int totalTickets = tickets.size();

            long totalEvents = tickets.stream()
                    .map(EventTicket::getEventId)
                    .distinct()
                    .count();

            updateLabel("totalTicketsLabel", String.valueOf(totalTickets));
            updateLabel("totalEventsLabel", String.valueOf(totalEvents));
            System.out.println("✅ KPI Tickets mis à jour: " + totalTickets + " tickets, " + totalEvents + " événements");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement KPI tickets: " + e.getMessage());
            updateLabel("totalTicketsLabel", "0");
            updateLabel("totalEventsLabel", "0");
        }
    }

    private void updateLabel(String id, String value) {
        if (kpiContainer == null || id == null) {
            return;
        }

        for (Node node : kpiContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                for (Node child : card.getChildren()) {
                    if (child instanceof Label) {
                        Label label = (Label) child;
                        if (id.equals(label.getId())) {
                            label.setText(value);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Rafraîchit les KPI de la page actuelle
     * Appelé après ajout/modification/suppression
     */
    public void refreshKPIs() {
        if (kpiContainer == null || !kpiContainer.isVisible()) {
            return;
        }

        // Déterminer quelle page est actuellement affichée
        String currentTitle = pageTitle.getText().toLowerCase();

        if (currentTitle.contains("catégorie")) {
            System.out.println("🔄 Rafraîchissement KPI Catégories...");
            loadCategoryData();
        } else if (currentTitle.contains("événement")) {
            System.out.println("🔄 Rafraîchissement KPI Événements...");
            loadEventData();
        } else if (currentTitle.contains("billet") || currentTitle.contains("ticket")) {
            System.out.println("🔄 Rafraîchissement KPI Billets...");
            loadTicketData();
        }
    }

    /**
     * Affiche une alerte simple
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== GETTERS =====================
    /**
     * Retourne le conteneur principal du contenu pour navigation depuis d'autres contrôleurs
     */
    public VBox getPageContentContainer() {
        return pageContentContainer;
    }

}

package com.example.pidev.controller.depense;

import com.example.pidev.model.depense.Depense;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.budget.BudgetService;
import com.example.pidev.service.depense.DepenseService;
import com.example.pidev.service.currency.CurrencyService;
import com.example.pidev.service.event.EventCategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DepenseFormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> budgetComboBox;
    @FXML private TextField descField;
    @FXML private ComboBox<String> categoryComboBox; // nouveau
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label convertedAmountLabel;

    private Depense depense;
    private boolean saved = false;

    private final BudgetService budgetService = new BudgetService();
    private final DepenseService depenseService = new DepenseService();
    private final EventCategoryService categoryService = new EventCategoryService();

    private Runnable onFormDone;

    private static final DateTimeFormatter DB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void setOnFormDone(Runnable callback) { this.onFormDone = callback; }

    public boolean isSaved() { return saved; }
    public Depense getDepense() { return depense; }

    @FXML
    private void initialize() {
        if (datePicker != null) {
            datePicker.setConverter(new StringConverter<>() {
                @Override public String toString(LocalDate date) {
                    return date == null ? "" : DB_FMT.format(date);
                }
                @Override public LocalDate fromString(String s) {
                    if (s == null || s.trim().isEmpty()) return null;
                    return LocalDate.parse(s.trim(), DB_FMT);
                }
            });
            datePicker.setEditable(false);
        }

        if (errorLabel != null) errorLabel.setText("");

        if (budgetComboBox != null) {
            try {
                budgetComboBox.setItems(budgetService.getAllBudgetNames());
            } catch (Exception e) {
                if (errorLabel != null) errorLabel.setText("⚠️ Erreur chargement budgets");
            }
        }

        // Charger les catégories depuis event_category
        if (categoryComboBox != null) {
            try {
                List<EventCategory> categories = categoryService.getAllCategories();
                ObservableList<String> categoryNames = FXCollections.observableArrayList();
                for (EventCategory cat : categories) {
                    categoryNames.add(cat.getName() + " - " + (cat.getDescription() != null ? cat.getDescription() : ""));
                }
                categoryComboBox.setItems(categoryNames);
                categoryComboBox.setPromptText("Choisissez une catégorie");

                // Personnalisation de l'affichage pour montrer nom + description
                categoryComboBox.setCellFactory(lv -> new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }
                });
            } catch (Exception e) {
                if (errorLabel != null) errorLabel.setText("⚠️ Erreur chargement catégories");
            }
        }

        // Initialisation de la devise avec une large sélection
        if (currencyComboBox != null) {
            currencyComboBox.getItems().addAll(
                    "TND", "USD", "EUR", "GBP", "CHF", "CAD", "JPY", "CNY", "AUD", "NZD",
                    "DKK", "NOK", "SEK", "TRY", "SAR", "AED", "KWD", "BHD", "QAR", "MAD",
                    "EGP", "ZAR", "INR", "PKR", "BDT", "LKR", "MYR", "SGD", "HKD", "KRW",
                    "RUB", "BRL", "MXN", "PLN", "CZK", "HUF", "ILS", "THB", "VND", "PHP"
            );
            currencyComboBox.setValue("TND");
            currencyComboBox.valueProperty().addListener((obs, old, n) -> updateConvertedAmount());
        }

        if (amountField != null) {
            amountField.textProperty().addListener((obs, old, n) -> updateConvertedAmount());
        }

        // Validations
        if (descField != null) {
            descField.textProperty().addListener((obs, old, n) -> {
                if (n == null) return;
                String filtered = n.replaceAll("\\d", "");
                if (filtered.length() > 255) filtered = filtered.substring(0, 255);
                if (!filtered.equals(n)) descField.setText(filtered);
            });
        }

        if (amountField != null) {
            amountField.textProperty().addListener((obs, old, n) -> {
                if (n == null || n.isEmpty()) return;
                String cleaned = n.replaceAll("[^0-9.,]", "");
                if (!cleaned.equals(n)) amountField.setText(cleaned);
            });
        }
    }

    private void updateConvertedAmount() {
        if (currencyComboBox == null || amountField == null || convertedAmountLabel == null) return;
        String currency = currencyComboBox.getValue();
        String amountText = amountField.getText().trim().replace(",", ".");
        if (amountText.isEmpty()) {
            convertedAmountLabel.setText("");
            return;
        }
        try {
            double amount = Double.parseDouble(amountText);
            if (currency == null || currency.equals("TND")) {
                convertedAmountLabel.setText(String.format("= %,.2f TND", amount));
            } else {
                double converted = CurrencyService.convert(amount, currency, "TND");
                if (converted >= 0) {
                    convertedAmountLabel.setText(String.format("≈ %,.2f TND", converted));
                } else {
                    convertedAmountLabel.setText("Erreur conversion");
                }
            }
        } catch (NumberFormatException e) {
            convertedAmountLabel.setText("Montant invalide");
        }
    }

    public void setDepense(Depense existing) {
        this.depense = existing;

        if (existing == null) {
            if (titleLabel != null) titleLabel.setText("➕ Nouvelle Dépense");
            if (budgetComboBox != null) budgetComboBox.setValue(null);
            if (descField != null) descField.clear();
            if (categoryComboBox != null) categoryComboBox.setValue(null);
            if (amountField != null) amountField.clear();
            if (datePicker != null) datePicker.setValue(LocalDate.now());
            if (currencyComboBox != null) currencyComboBox.setValue("TND");
            if (convertedAmountLabel != null) convertedAmountLabel.setText("");
            return;
        }

        if (titleLabel != null) titleLabel.setText("✏ Modifier Dépense");

        if (budgetComboBox != null) {
            String budgetName = budgetService.getBudgetNameById(existing.getBudget_id());
            budgetComboBox.setValue(budgetName);
        }
        if (descField != null) descField.setText(existing.getDescription());
        if (categoryComboBox != null) {
            // On cherche la catégorie correspondante dans la liste
            String category = existing.getCategory();
            if (category != null) {
                categoryComboBox.setValue(category + " - ..."); // À améliorer : il faudrait récupérer la description
                // En pratique, on pourrait stocker l'ID de catégorie, mais ici on utilise juste le nom
                // On va parcourir les items pour trouver celui qui commence par le nom
                for (String item : categoryComboBox.getItems()) {
                    if (item.startsWith(category + " - ")) {
                        categoryComboBox.setValue(item);
                        break;
                    }
                }
            }
        }
        if (amountField != null) amountField.setText(String.valueOf(existing.getAmount()));
        if (datePicker != null) datePicker.setValue(existing.getExpense_date());
        if (currencyComboBox != null) currencyComboBox.setValue("TND");
        if (convertedAmountLabel != null) convertedAmountLabel.setText("");
    }

    @FXML
    private void onCancel() {
        saved = false;
        closeWindow();
    }

    @FXML
    private void onSave() {
        clearError();

        String budgetName = (budgetComboBox == null) ? null : budgetComboBox.getValue();
        if (budgetName == null || budgetName.trim().isEmpty()) { showError("Sélectionnez un budget"); return; }

        int budgetId = budgetService.getBudgetIdByName(budgetName);
        if (budgetId <= 0) { showError("Budget invalide"); return; }

        String desc = descField.getText() == null ? "" : descField.getText().trim();
        if (desc.isEmpty()) { showError("Description obligatoire"); return; }
        if (desc.length() > 255) { showError("Description trop longue (max 255)"); return; }

        String catSelection = (categoryComboBox == null) ? null : categoryComboBox.getValue();
        if (catSelection == null || catSelection.trim().isEmpty()) { showError("Catégorie obligatoire"); return; }
        // Extraire le nom de la catégorie (avant le " - ")
        String catName = catSelection.split(" - ")[0].trim();
        if (catName.isEmpty()) { showError("Catégorie invalide"); return; }

        String currency = (currencyComboBox == null) ? "TND" : currencyComboBox.getValue();
        double amount;
        try {
            String raw = amountField.getText() == null ? "" : amountField.getText().trim().replace(',', '.');
            if (raw.isEmpty()) { showError("Montant obligatoire"); return; }
            amount = Double.parseDouble(raw);
            if (amount <= 0) { showError("Montant doit être > 0"); return; }
        } catch (NumberFormatException e) {
            showError("Montant invalide (ex: 150.50)");
            return;
        }

        double amountInTND;
        if (currency.equals("TND")) {
            amountInTND = amount;
        } else {
            amountInTND = CurrencyService.convert(amount, currency, "TND");
            if (amountInTND < 0) {
                showError("Erreur de conversion de devise. Vérifiez votre connexion ou les codes.");
                return;
            }
        }

        LocalDate dt = (datePicker == null) ? null : datePicker.getValue();
        if (dt == null) { showError("Date obligatoire"); return; }
        if (dt.isAfter(LocalDate.now())) { showError("Date ne peut pas être dans le futur"); return; }

        boolean isNew = (depense == null || depense.getId() <= 0);
        if (depense == null) depense = new Depense();

        int oldBudgetId = isNew ? 0 : depense.getBudget_id();

        depense.setBudget_id(budgetId);
        depense.setDescription(desc);
        depense.setCategory(catName); // On enregistre seulement le nom, pas la description
        depense.setAmount(amountInTND);
        depense.setExpense_date(dt);

        try {
            if (isNew) depenseService.addDepense(depense);
            else depenseService.updateDepense(depense, oldBudgetId);

            saved = true;
            closeWindow();
        } catch (Exception ex) {
            showError("Erreur sauvegarde: " + ex.getMessage());
        }
    }

    private void closeWindow() {
        if (onFormDone != null) { onFormDone.run(); return; }
        try {
            if (budgetComboBox != null && budgetComboBox.getScene() != null) {
                Stage stage = (Stage) budgetComboBox.getScene().getWindow();
                if (stage != null && stage.isShowing()) stage.close();
            }
        } catch (Exception ignored) {}
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText("❌ " + msg);
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }
}
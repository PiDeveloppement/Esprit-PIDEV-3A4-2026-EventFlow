package com.example.pidev.controller.depense;

import com.example.pidev.model.depense.Depense;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.budget.BudgetService;
import com.example.pidev.service.currency.CurrencyService;
import com.example.pidev.service.depense.DepenseService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class DepenseFormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> budgetComboBox;
    @FXML private TextField descField;
    @FXML private ComboBox<EventCategory> categoryComboBox;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label convertedAmountLabel;
    @FXML private DatePicker datePicker;
    @FXML private Label errorLabel;

    private Depense editing;
    private boolean saved = false;
    private Runnable onFormDone;
    private Consumer<Depense> onSaved;

    private final DepenseService depenseService = new DepenseService();
    private final BudgetService budgetService = new BudgetService();

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    @FXML
    private void initialize() {
        try {
            budgetComboBox.setItems(budgetService.getBudgetDisplayNames()); // ✅ sans ID
        } catch (Exception e) {
            showError("Erreur chargement budgets : " + e.getMessage());
        }

        try {
            ObservableList<EventCategory> categories = depenseService.getEventCategories();
            categoryComboBox.setItems(categories);

            categoryComboBox.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(EventCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            categoryComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(EventCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

        } catch (Exception e) {
            showError("Erreur chargement catégories : " + e.getMessage());
        }

        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getDescription() != null) {
                descField.setText(newVal.getDescription());
            }
        });

        if (currencyComboBox != null) {
            currencyComboBox.getItems().addAll(
                    "TND","USD","EUR","GBP","CHF","CAD","JPY","CNY","AUD","NZD",
                    "DKK","NOK","SEK","TRY","SAR","AED","KWD","BHD","QAR","MAD",
                    "EGP","ZAR","INR","PKR","BDT","LKR","MYR","SGD","HKD","KRW",
                    "RUB","BRL","MXN","PLN","CZK","HUF","ILS","THB","VND","PHP"
            );
            currencyComboBox.setValue("TND");
            currencyComboBox.valueProperty().addListener((obs, old, n) -> updateConvertedAmount());
        }

        if (amountField != null) {
            amountField.textProperty().addListener((obs, old, n) -> updateConvertedAmount());
        }

        if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
    }

    private void updateConvertedAmount() {
        if (currencyComboBox == null || amountField == null || convertedAmountLabel == null) return;

        String currency = currencyComboBox.getValue();
        String amountText = amountField.getText() == null ? "" : amountField.getText().trim().replace(",", ".");
        if (amountText.isEmpty()) {
            convertedAmountLabel.setText("");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (currency == null || currency.equals("TND")) {
                convertedAmountLabel.setText(String.format(Locale.US, "= %.2f TND", amount));
            } else {
                double converted = CurrencyService.convert(amount, currency, "TND");
                convertedAmountLabel.setText(converted >= 0
                        ? String.format(Locale.US, "≈ %.2f TND", converted)
                        : "Erreur conversion");
            }
        } catch (NumberFormatException e) {
            convertedAmountLabel.setText("Montant invalide");
        }
    }

    public void setDepense(Depense d) {
        this.editing = d;

        if (d == null) {
            if (titleLabel != null) titleLabel.setText("➕ Nouvelle Dépense");
            return;
        }

        if (titleLabel != null) titleLabel.setText("✏ Modifier Dépense");

        // ✅ IMPORTANT: récupérer l'affichage exact depuis la DB (même format que la ComboBox)
        String expectedDisplay = budgetService.getBudgetDisplayNameById(d.getBudget_id());

        Platform.runLater(() -> {
            if (expectedDisplay == null) return;

            // si la liste contient déjà l'item -> setValue direct
            if (budgetComboBox.getItems().contains(expectedDisplay)) {
                budgetComboBox.setValue(expectedDisplay);
            } else {
                // sinon on l'ajoute (au cas où) puis set
                budgetComboBox.getItems().add(0, expectedDisplay);
                budgetComboBox.setValue(expectedDisplay);
            }
        });

        if (descField != null) descField.setText(d.getDescription());

        if (categoryComboBox != null && categoryComboBox.getItems() != null) {
            for (EventCategory cat : categoryComboBox.getItems()) {
                if (cat != null && cat.getName() != null && cat.getName().equals(d.getCategory())) {
                    categoryComboBox.setValue(cat);
                    break;
                }
            }
        }

        if (amountField != null) amountField.setText(String.format(Locale.US, "%.2f", d.getAmount()));
        if (currencyComboBox != null) currencyComboBox.setValue("TND");
        if (datePicker != null && d.getExpense_date() != null) datePicker.setValue(d.getExpense_date());

        updateConvertedAmount();
    }

    public void setOnFormDone(Runnable callback) { this.onFormDone = callback; }
    public void setOnSaved(Consumer<Depense> callback) { this.onSaved = callback; }
    public boolean isSaved() { return saved; }
    public Depense getDepense() { return editing; }

    @FXML
    private void onSave() {
        if (errorLabel != null) errorLabel.setText("");

        String budgetDisplay = budgetComboBox == null ? null : budgetComboBox.getValue();
        String description = descField == null ? null : descField.getText();
        EventCategory selectedCat = categoryComboBox == null ? null : categoryComboBox.getValue();
        String amountText = amountField == null ? "" : amountField.getText().trim().replace(",", ".");
        String currency = currencyComboBox == null ? null : currencyComboBox.getValue();
        LocalDate date = datePicker == null ? null : datePicker.getValue();

        if (budgetDisplay == null || budgetDisplay.isBlank()) { showError("Veuillez sélectionner un budget."); return; }
        if (description == null || description.trim().isEmpty()) { showError("La description est obligatoire."); return; }
        if (selectedCat == null) { showError("Veuillez sélectionner une catégorie."); return; }
        if (amountText.isEmpty()) { showError("Le montant est obligatoire."); return; }
        if (!AMOUNT_PATTERN.matcher(amountText).matches()) { showError("Format de montant invalide (ex: 150.50)"); return; }
        if (date == null) { showError("Veuillez sélectionner une date."); return; }
        if (currency == null || currency.isBlank()) { showError("Veuillez sélectionner une devise."); return; }

        double amount;
        try { amount = Double.parseDouble(amountText); }
        catch (NumberFormatException e) { showError("Montant invalide"); return; }

        double amountInTND;
        if ("TND".equals(currency)) {
            amountInTND = amount;
        } else {
            amountInTND = CurrencyService.convert(amount, currency, "TND");
            if (amountInTND < 0) { showError("Erreur de conversion de devise."); return; }
        }

        int budgetId = budgetService.getBudgetIdByDisplayName(budgetDisplay);
        if (budgetId == -1) { showError("Budget invalide."); return; }

        Depense depense = (editing == null) ? new Depense() : editing;
        int oldBudgetId = (editing == null) ? -1 : editing.getBudget_id();

        depense.setBudget_id(budgetId);
        depense.setDescription(description);
        depense.setCategory(selectedCat.getName());
        depense.setAmount(amountInTND);
        depense.setExpense_date(date);
        depense.setOriginalCurrency(currency);
        depense.setOriginalAmount(amount);

        try {
            if (editing == null) depenseService.addDepense(depense);
            else depenseService.updateDepense(depense, oldBudgetId);

            saved = true;
            if (onSaved != null) onSaved.accept(depense);
            if (onFormDone != null) onFormDone.run();
            closeWindow();

        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (onFormDone != null) onFormDone.run();
        closeWindow();
    }

    private void closeWindow() {
        try {
            Stage stage = (Stage) budgetComboBox.getScene().getWindow();
            stage.close();
        } catch (Exception ignored) {}
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText("❌ " + msg);
    }
}
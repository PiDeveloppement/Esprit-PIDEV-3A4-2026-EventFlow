package com.example.pidev.service.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetService {

    private Connection cnx() {
        return DBConnection.getInstance().getConnection();
    }

    // ===================== EVENT =====================

    /** title depuis event.id */
    public String getEventTitleById(int eventId) {
        String sql = "SELECT title FROM event WHERE id = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("title");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération titre événement", e);
        }
        return "Événement inconnu";
    }

    /** ✅ title depuis budget.id (JOIN budget -> event) */
    public String getEventTitleByBudgetId(int budgetId) {
        String sql = """
            SELECT e.title
            FROM budget b
            JOIN event e ON b.event_id = e.id
            WHERE b.id = ?
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération titre événement par budgetId", e);
        }
        return "Événement inconnu";
    }

    /** ✅ Utilisé par BudgetFormController/BudgetListController */
    public ObservableList<String> getAllEventTitles() {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT title FROM event ORDER BY title";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("title"));
        } catch (SQLException e) {
            throw new RuntimeException("getAllEventTitles failed", e);
        }
        return list;
    }

    /** ✅ Utilisé par BudgetFormController/BudgetListController */
    public int getEventIdByTitle(String title) throws SQLException {
        String sql = "SELECT id FROM event WHERE title = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Événement introuvable : " + title);
    }

    // ===================== CRUD BUDGET =====================

    public boolean eventExists(int eventId) {
        String sql = "SELECT 1 FROM event WHERE id=? LIMIT 1";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("eventExists failed", e);
        }
    }

    public ObservableList<Budget> getAllBudgets() {
        ObservableList<Budget> list = FXCollections.observableArrayList();
        String sql = "SELECT id, event_id, initial_budget, total_expenses, total_revenue, rentabilite " +
                "FROM budget ORDER BY id DESC";

        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Budget(
                        rs.getInt("id"),
                        rs.getInt("event_id"),
                        rs.getDouble("initial_budget"),
                        rs.getDouble("total_expenses"),
                        rs.getDouble("total_revenue"),
                        rs.getDouble("rentabilite")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getAllBudgets failed", e);
        }
    }

    public List<Budget> getTop5Budgets() {
        List<Budget> list = new ArrayList<>();
        String sql = "SELECT b.id, b.event_id, b.initial_budget, b.total_expenses, b.total_revenue, b.rentabilite " +
                "FROM budget b ORDER BY b.id DESC LIMIT 5";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Budget(
                        rs.getInt("id"),
                        rs.getInt("event_id"),
                        rs.getDouble("initial_budget"),
                        rs.getDouble("total_expenses"),
                        rs.getDouble("total_revenue"),
                        rs.getDouble("rentabilite")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("getTop5Budgets failed", e);
        }
        return list;
    }

    public ObservableList<Integer> getEventIdsFromBudgets() {
        ObservableList<Integer> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT event_id FROM budget ORDER BY event_id";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getInt(1));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getEventIdsFromBudgets failed", e);
        }
    }

    public void addBudget(Budget b) {
        if (!eventExists(b.getEvent_id())) {
            throw new IllegalArgumentException("event_id n'existe pas: " + b.getEvent_id());
        }

        double rent = b.getTotal_revenue() - b.getTotal_expenses();

        String sql = "INSERT INTO budget (event_id, initial_budget, total_expenses, total_revenue, rentabilite) " +
                "VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getEvent_id());
            ps.setDouble(2, b.getInitial_budget());
            ps.setDouble(3, b.getTotal_expenses());
            ps.setDouble(4, b.getTotal_revenue());
            ps.setDouble(5, rent);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert budget failed", e);
        }
    }

    public void updateBudget(Budget b) {
        if (!eventExists(b.getEvent_id())) {
            throw new IllegalArgumentException("event_id n'existe pas: " + b.getEvent_id());
        }

        double rent = b.getTotal_revenue() - b.getTotal_expenses();

        String sql = "UPDATE budget SET event_id=?, initial_budget=?, total_expenses=?, total_revenue=?, rentabilite=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, b.getEvent_id());
            ps.setDouble(2, b.getInitial_budget());
            ps.setDouble(3, b.getTotal_expenses());
            ps.setDouble(4, b.getTotal_revenue());
            ps.setDouble(5, rent);
            ps.setInt(6, b.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) throw new RuntimeException("Aucune ligne modifiée (id introuvable): " + b.getId());
        } catch (SQLException e) {
            throw new RuntimeException("update budget failed", e);
        }
    }

    public void deleteBudget(int id) {
        String delDep = "DELETE FROM depense WHERE budget_id=?";
        String delBud = "DELETE FROM budget WHERE id=?";

        try (PreparedStatement ps1 = cnx().prepareStatement(delDep);
             PreparedStatement ps2 = cnx().prepareStatement(delBud)) {

            ps1.setInt(1, id);
            ps1.executeUpdate();

            ps2.setInt(1, id);
            ps2.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("delete budget failed", e);
        }
    }

    // KPI
    public int countBudgets() {
        String sql = "SELECT COUNT(*) FROM budget";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("countBudgets failed", e);
        }
    }

    public double sumInitial() {
        String sql = "SELECT COALESCE(SUM(initial_budget),0) FROM budget";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            throw new RuntimeException("sumInitial failed", e);
        }
    }

    public double globalRentability() {
        String sql = "SELECT COALESCE(SUM(total_revenue - total_expenses),0) FROM budget";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            throw new RuntimeException("globalRentability failed", e);
        }
    }

    public int countDeficitBudgets() {
        String sql = "SELECT COUNT(*) FROM budget WHERE (total_revenue - total_expenses) < 0";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("countDeficitBudgets failed", e);
        }
    }

    // ==================== DEPENSE FORM / EXPORT ====================

    /**
     * ✅ Affichage ComboBox Dépense (SANS ID):
     * "Hackathon 2024 (200.00 DT)"
     */
    public ObservableList<String> getBudgetDisplayNames() {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = """
            SELECT e.title, b.initial_budget
            FROM budget b
            JOIN event e ON b.event_id = e.id
            ORDER BY b.id DESC
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString("title");
                double initial = rs.getDouble("initial_budget");
                String initialStr = String.format(Locale.US, "%.2f", initial);
                list.add(title + " (" + initialStr + " DT)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("getBudgetDisplayNames failed", e);
        }
        return list;
    }

    /**
     * ✅ Pour pré-sélectionner correctement en mode Modifier (même format exact)
     */
    public String getBudgetDisplayNameById(int budgetId) {
        String sql = """
            SELECT e.title, b.initial_budget
            FROM budget b
            JOIN event e ON b.event_id = e.id
            WHERE b.id = ?
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString(1);
                    double initial = rs.getDouble(2);
                    String initialStr = String.format(Locale.US, "%.2f", initial);
                    return title + " (" + initialStr + " DT)";
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getBudgetDisplayNameById failed", e);
        }
        return null;
    }

    /**
     * ✅ Parsing robuste pour retrouver budget.id depuis "title (amount DT)"
     */
    public int getBudgetIdByDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return -1;

        try {
            int lastParen = displayName.lastIndexOf('(');
            if (lastParen == -1) return -1;

            String title = displayName.substring(0, lastParen).trim();

            String amountStr = displayName
                    .substring(lastParen + 1, displayName.length() - 1)
                    .replace("DT", "")
                    .replace(",", "")
                    .trim();

            double amount = Double.parseDouble(amountStr);

            String sql = """
                SELECT b.id
                FROM budget b
                JOIN event e ON b.event_id = e.id
                WHERE e.title = ? AND b.initial_budget = ?
                ORDER BY b.id DESC
                LIMIT 1
            """;
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setDouble(2, amount);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            return -1;
        }

        return -1;
    }

    /**
     * ✅ Utilisé par ExcelExportService (au lieu de getBudgetNameById manquant)
     * Retourne: "Hackathon 2024 (200.00 DT)"
     */
    public String getBudgetNameById(int budgetId) {
        String s = getBudgetDisplayNameById(budgetId);
        return (s == null || s.isBlank()) ? ("Budget " + budgetId) : s;
    }

    public double getInitialBudgetById(int budgetId) {
        String sql = "SELECT initial_budget FROM budget WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("initial_budget");
            }
        } catch (SQLException e) {
            throw new RuntimeException("getInitialBudgetById failed", e);
        }
        return 0;
    }
}
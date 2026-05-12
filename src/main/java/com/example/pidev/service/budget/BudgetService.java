package com.example.pidev.service.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetService {

    private Connection cnx() {
        return DBConnection.getInstance().getConnection();
    }

    // ===================== ACCÈS DIRECT À LA TABLE EVENT =====================
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

    // Methods for Depense form - budget selection by name
    public ObservableList<String> getAllBudgetNames() {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT id, initial_budget FROM budget ORDER BY id DESC";

        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int budgetId = rs.getInt("id");
                double initial = rs.getDouble("initial_budget");
                String name = String.format("Budget N°%d (%.2f DT)", budgetId, initial);
                list.add(name);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getAllBudgetNames failed", e);
        }
    }

    public int getBudgetIdByName(String name) {
        if (name == null || name.isEmpty()) return -1;
        try {
            int startIdx = name.indexOf("°") + 1;
            int endIdx = name.indexOf("(");
            if (startIdx <= 0 || endIdx <= 0) return -1;
            String idStr = name.substring(startIdx, endIdx).trim();
            return Integer.parseInt(idStr);
        } catch (Exception e) {
            System.err.println("getBudgetIdByName error: " + e.getMessage());
            return -1;
        }
    }

    public String getBudgetNameById(int budgetId) {
        String sql = "SELECT initial_budget FROM budget WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double initial = rs.getDouble("initial_budget");
                    return String.format("Budget N°%d (%.2f DT)", budgetId, initial);
                }
            }
        } catch (SQLException e) {
            System.err.println("getBudgetNameById error: " + e.getMessage());
        }
        return "Budget N°" + budgetId;
    }
}
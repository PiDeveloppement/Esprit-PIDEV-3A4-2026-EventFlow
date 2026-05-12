package com.example.pidev.service.depense;

import com.example.pidev.model.depense.Depense;
import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class DepenseService {

    private Connection cnx() {
        return DBConnection.getInstance().getConnection();
    }

    // ---------- validations ----------
    public boolean budgetExists(int budgetId) {
        String sql = "SELECT 1 FROM budget WHERE id=? LIMIT 1";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("budgetExists failed", e);
        }
    }

    // ---------- list ----------
    public ObservableList<Depense> getAllDepenses() {
        ObservableList<Depense> list = FXCollections.observableArrayList();
        String sql = "SELECT id, budget_id, description, amount, category, expense_date " +
                "FROM depense ORDER BY expense_date DESC, id DESC";

        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Date d = rs.getDate("expense_date");
                LocalDate ld = (d == null) ? null : d.toLocalDate();

                list.add(new Depense(
                        rs.getInt("id"),
                        rs.getInt("budget_id"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        ld
                ));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("getAllDepenses failed", e);
        }
    }

    // ---------- filtres (combobox) ----------
    public ObservableList<Integer> getBudgetIdsFromDepenses() {
        ObservableList<Integer> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT budget_id FROM depense ORDER BY budget_id";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getInt(1));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getBudgetIdsFromDepenses failed", e);
        }
    }

    public ObservableList<String> getCategories() {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT category FROM depense " +
                "WHERE category IS NOT NULL AND category <> '' ORDER BY category";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("getCategories failed", e);
        }
    }

    // ---------- KPI ----------
    public int countDepenses() {
        String sql = "SELECT COUNT(*) FROM depense";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("countDepenses failed", e);
        }
    }

    public double sumDepenses() {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM depense";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            throw new RuntimeException("sumDepenses failed", e);
        }
    }

    public double sumDepensesBetween(LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM depense WHERE expense_date BETWEEN ? AND ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("sumDepensesBetween failed", e);
        }
    }

    public String topCategorie() {
        String sql = "SELECT category, COUNT(*) AS c " +
                "FROM depense WHERE category IS NOT NULL AND category <> '' " +
                "GROUP BY category ORDER BY c DESC LIMIT 1";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("category");
            return "—";
        } catch (SQLException e) {
            throw new RuntimeException("topCategorie failed", e);
        }
    }

    // ---------- CHART DATA ----------
    public Map<String, Double> getSumByCategory() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT category, COALESCE(SUM(amount),0) FROM depense GROUP BY category ORDER BY category";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException("getSumByCategory failed", e);
        }
        return map;
    }

    // ---------- CRUD ----------
    public void addDepense(Depense d) {
        if (!budgetExists(d.getBudget_id())) {
            throw new IllegalArgumentException("budget_id n'existe pas: " + d.getBudget_id());
        }

        String sql = "INSERT INTO depense (budget_id, description, amount, category, expense_date) " +
                "VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getBudget_id());
            ps.setString(2, d.getDescription());
            ps.setDouble(3, d.getAmount());
            ps.setString(4, d.getCategory());
            ps.setDate(5, d.getExpense_date() == null ? null : Date.valueOf(d.getExpense_date()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setId(keys.getInt(1));
            }

            recomputeBudget(d.getBudget_id());

        } catch (SQLException e) {
            throw new RuntimeException("insert depense failed", e);
        }
    }

    public void updateDepense(Depense d, int oldBudgetId) {
        if (!budgetExists(d.getBudget_id())) {
            throw new IllegalArgumentException("budget_id n'existe pas: " + d.getBudget_id());
        }

        String sql = "UPDATE depense SET budget_id=?, description=?, amount=?, category=?, expense_date=? WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, d.getBudget_id());
            ps.setString(2, d.getDescription());
            ps.setDouble(3, d.getAmount());
            ps.setString(4, d.getCategory());
            ps.setDate(5, d.getExpense_date() == null ? null : Date.valueOf(d.getExpense_date()));
            ps.setInt(6, d.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) throw new RuntimeException("Aucune ligne modifiée (id introuvable): " + d.getId());

            recomputeBudget(oldBudgetId);
            if (oldBudgetId != d.getBudget_id()) recomputeBudget(d.getBudget_id());

        } catch (SQLException e) {
            throw new RuntimeException("update depense failed", e);
        }
    }

    public void deleteDepense(int id, int budgetId) {
        String sql = "DELETE FROM depense WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            recomputeBudget(budgetId);
        } catch (SQLException e) {
            throw new RuntimeException("delete depense failed", e);
        }
    }

    // ---------- recalcul Budget ----------
    private void recomputeBudget(int budgetId) {
        String sumSql = "SELECT COALESCE(SUM(amount),0) FROM depense WHERE budget_id=?";
        String revSql = "SELECT COALESCE(total_revenue,0) FROM budget WHERE id=?";
        String updSql = "UPDATE budget SET total_expenses=?, rentabilite=? WHERE id=?";

        try {
            double totalExpenses;
            try (PreparedStatement ps = cnx().prepareStatement(sumSql)) {
                ps.setInt(1, budgetId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    totalExpenses = rs.getDouble(1);
                }
            }

            double totalRevenue;
            try (PreparedStatement ps = cnx().prepareStatement(revSql)) {
                ps.setInt(1, budgetId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    totalRevenue = rs.getDouble(1);
                }
            }

            double rentabilite = totalRevenue - totalExpenses;

            try (PreparedStatement ps = cnx().prepareStatement(updSql)) {
                ps.setDouble(1, totalExpenses);
                ps.setDouble(2, rentabilite);
                ps.setInt(3, budgetId);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("recomputeBudget failed", e);
        }
    }
}
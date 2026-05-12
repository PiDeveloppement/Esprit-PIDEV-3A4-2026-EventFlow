package com.example.pidev.service.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SponsorService {

    private final Connection cnx;

    public SponsorService() {
        cnx = DBConnection.getInstance().getConnection();
    }

    // ===================== ACCÈS DIRECT À LA TABLE EVENT =====================
    public String getEventTitleById(int eventId) {
        String sql = "SELECT title FROM event WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
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
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("title"));
        } catch (SQLException e) {
            throw new RuntimeException("getAllEventTitles failed", e);
        }
        return list;
    }

    public int getEventIdByTitle(String title) throws SQLException {
        String sql = "SELECT id FROM event WHERE title = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Événement introuvable : " + title);
    }

    // ===================== CRUD SPONSOR =====================
    public void addSponsor(Sponsor s) throws SQLException {
        String sql = """
            INSERT INTO sponsor(event_id, company_name, contact_email, logo_url, contribution_name, contract_url, access_code, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getEvent_id());
            ps.setString(2, s.getCompany_name());
            ps.setString(3, s.getContact_email());
            ps.setString(4, s.getLogo_url());
            ps.setDouble(5, s.getContribution_name());
            ps.setString(6, s.getContract_url());
            ps.setString(7, s.getAccess_code());
            if (s.getUser_id() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, s.getUser_id());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) s.setId(rs.getInt(1));
            }
        }
    }

    public void updateSponsor(Sponsor s) throws SQLException {
        String sql = """
            UPDATE sponsor
            SET event_id=?, company_name=?, contact_email=?, logo_url=?, contribution_name=?, contract_url=?, access_code=?, user_id=?
            WHERE id=?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getEvent_id());
            ps.setString(2, s.getCompany_name());
            ps.setString(3, s.getContact_email());
            ps.setString(4, s.getLogo_url());
            ps.setDouble(5, s.getContribution_name());
            ps.setString(6, s.getContract_url());
            ps.setString(7, s.getAccess_code());
            if (s.getUser_id() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, s.getUser_id());
            ps.setInt(9, s.getId());

            ps.executeUpdate();
        }
    }

    public void deleteSponsor(int sponsorId) throws SQLException {
        String sql = "DELETE FROM sponsor WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sponsorId);
            ps.executeUpdate();
        }
    }

    public void updateContractUrl(int sponsorId, String url) throws SQLException {
        String sql = "UPDATE sponsor SET contract_url=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setInt(2, sponsorId);
            ps.executeUpdate();
        }
    }

    public List<Sponsor> getAllSponsors() throws SQLException {
        String sql = "SELECT * FROM sponsor ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Sponsor> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    public Sponsor getSponsorById(int id) throws SQLException {
        String sql = "SELECT * FROM sponsor WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ===================== ADMIN FILTERS =====================
    public ObservableList<String> getCompanyNamesAll() throws SQLException {
        String sql = """
            SELECT DISTINCT company_name
            FROM sponsor
            WHERE company_name IS NOT NULL AND TRIM(company_name) <> ''
            ORDER BY company_name
        """;

        ObservableList<String> names = FXCollections.observableArrayList();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    public ObservableList<Integer> getEventIdsAll() throws SQLException {
        String sql = """
            SELECT DISTINCT event_id
            FROM sponsor
            ORDER BY event_id
        """;

        ObservableList<Integer> ids = FXCollections.observableArrayList();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt(1));
        }
        return ids;
    }

    // ===================== ADMIN STATS =====================
    public int getTotalSponsors() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sponsor";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public double getTotalContribution() throws SQLException {
        String sql = "SELECT COALESCE(SUM(contribution_name),0) FROM sponsor";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    public double getAverageContribution() throws SQLException {
        String sql = "SELECT COALESCE(AVG(contribution_name),0) FROM sponsor";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    // ===================== CHART DATA =====================
    public Map<String, Double> getTotalContributionByEvent() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT e.title, COALESCE(SUM(s.contribution_name),0) " +
                "FROM sponsor s JOIN event e ON s.event_id = e.id " +
                "GROUP BY e.id, e.title ORDER BY e.title";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getDouble(2));
            }
        }
        return map;
    }

    public Map<String, Double> getMyContributionsByCompany(String email) throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT company_name, contribution_name FROM sponsor WHERE LOWER(contact_email)=LOWER(?) ORDER BY company_name";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("company_name"), rs.getDouble("contribution_name"));
                }
            }
        }
        return map;
    }

    // ===================== MODE DEMO (SponsorPortalController) =====================
    public ObservableList<String> getDemoEmailsFromSponsor() throws SQLException {
        String sql = """
            SELECT DISTINCT contact_email
            FROM sponsor
            WHERE contact_email IS NOT NULL AND TRIM(contact_email) <> ''
            ORDER BY contact_email
        """;

        ObservableList<String> emails = FXCollections.observableArrayList();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) emails.add(rs.getString(1));
        }
        return emails;
    }

    public List<Sponsor> getSponsorsByContactEmail(String email) throws SQLException {
        String sql = """
            SELECT *
            FROM sponsor
            WHERE LOWER(contact_email)=LOWER(?)
            ORDER BY id DESC
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                List<Sponsor> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public int getMySponsorsCountDemo(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sponsor WHERE LOWER(contact_email)=LOWER(?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public double getMyTotalContributionDemo(String email) throws SQLException {
        String sql = "SELECT COALESCE(SUM(contribution_name),0) FROM sponsor WHERE LOWER(contact_email)=LOWER(?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    public int getMySponsoredEventsCountDemo(String email) throws SQLException {
        String sql = """
            SELECT COUNT(DISTINCT event_id)
            FROM sponsor
            WHERE LOWER(contact_email)=LOWER(?)
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public ObservableList<String> getCompanyNamesByContactEmail(String email) throws SQLException {
        String sql = """
            SELECT DISTINCT company_name
            FROM sponsor
            WHERE LOWER(contact_email)=LOWER(?)
              AND company_name IS NOT NULL AND TRIM(company_name) <> ''
            ORDER BY company_name
        """;

        ObservableList<String> names = FXCollections.observableArrayList();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString(1));
            }
        }
        return names;
    }

    public ObservableList<Integer> getEventIdsByContactEmail(String email) throws SQLException {
        String sql = """
            SELECT DISTINCT event_id
            FROM sponsor
            WHERE LOWER(contact_email)=LOWER(?)
            ORDER BY event_id
        """;

        ObservableList<Integer> ids = FXCollections.observableArrayList();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    // ===================== MAPPING =====================
    private Sponsor map(ResultSet rs) throws SQLException {
        Sponsor s = new Sponsor();
        s.setId(rs.getInt("id"));
        s.setEvent_id(rs.getInt("event_id"));
        s.setCompany_name(rs.getString("company_name"));
        s.setContact_email(rs.getString("contact_email"));
        s.setLogo_url(rs.getString("logo_url"));
        s.setContribution_name(rs.getDouble("contribution_name"));
        s.setContract_url(rs.getString("contract_url"));
        s.setAccess_code(rs.getString("access_code"));

        int uid = rs.getInt("user_id");
        s.setUser_id(rs.wasNull() ? null : uid);

        return s;
    }
    // ==================== MÉTHODES POUR LE CHATBOT ====================



    /**
     * Récupère le nombre de sponsors actifs
     */
    public int getActiveSponsors() {
        String sql = "SELECT COUNT(DISTINCT sponsor_id) FROM budget WHERE amount > 0";
        // Ou selon votre logique métier
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getActiveSponsors: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Récupère le budget total des sponsors
     */
    public double getTotalBudget() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM budget WHERE type = 'SPONSOR'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalBudget: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Récupère le nombre de nouveaux sponsors ce mois
     */
    public int getNewSponsorsThisMonth() {
        String sql = "SELECT COUNT(*) FROM sponsor " +
                "WHERE MONTH(created_at) = MONTH(CURRENT_DATE()) " +
                "AND YEAR(created_at) = YEAR(CURRENT_DATE())";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNewSponsorsThisMonth: " + e.getMessage());
        }
        return 0;
    }
}
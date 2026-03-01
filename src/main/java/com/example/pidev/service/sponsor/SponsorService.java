package com.example.pidev.service.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SponsorService {

    private Connection connection;

    public SponsorService() {
        this.connection = DBConnection.getConnection();
    }

    // ==================== CRUD ====================
    public boolean addSponsor(Sponsor sponsor) {
        String sql = "INSERT INTO sponsor (event_id, company_name, logo_url, contribution_name, contact_email, contract_url, industry, phone, tax_id, document_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sponsor.getEvent_id());
            pstmt.setString(2, sponsor.getCompany_name());
            pstmt.setString(3, sponsor.getLogo_url());
            pstmt.setDouble(4, sponsor.getContribution_name());
            pstmt.setString(5, sponsor.getContact_email());
            pstmt.setString(6, sponsor.getContract_url());
            pstmt.setString(7, sponsor.getIndustry());
            pstmt.setString(8, sponsor.getPhone());
            pstmt.setString(9, sponsor.getTax_id());
            pstmt.setString(10, sponsor.getDocument_url());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        sponsor.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Sponsor> getAllSponsors() throws SQLException {
        List<Sponsor> list = new ArrayList<>();
        String sql = "SELECT * FROM sponsor";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractSponsorFromResultSet(rs));
            }
        }
        return list;
    }

    public Sponsor getSponsorById(int id) throws SQLException {
        String sql = "SELECT * FROM sponsor WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractSponsorFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Sponsor> getSponsorsByContactEmail(String email) throws SQLException {
        List<Sponsor> list = new ArrayList<>();
        String sql = "SELECT * FROM sponsor WHERE contact_email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractSponsorFromResultSet(rs));
                }
            }
        }
        return list;
    }

    public boolean updateSponsor(Sponsor sponsor) throws SQLException {
        String sql = "UPDATE sponsor SET event_id=?, company_name=?, logo_url=?, contribution_name=?, contact_email=?, contract_url=?, industry=?, phone=?, tax_id=?, document_url=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, sponsor.getEvent_id());
            pstmt.setString(2, sponsor.getCompany_name());
            pstmt.setString(3, sponsor.getLogo_url());
            pstmt.setDouble(4, sponsor.getContribution_name());
            pstmt.setString(5, sponsor.getContact_email());
            pstmt.setString(6, sponsor.getContract_url());
            pstmt.setString(7, sponsor.getIndustry());
            pstmt.setString(8, sponsor.getPhone());
            pstmt.setString(9, sponsor.getTax_id());
            pstmt.setString(10, sponsor.getDocument_url());
            pstmt.setInt(11, sponsor.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteSponsor(int id) throws SQLException {
        String sql = "DELETE FROM sponsor WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Sponsor extractSponsorFromResultSet(ResultSet rs) throws SQLException {
        Sponsor s = new Sponsor();
        s.setId(rs.getInt("id"));
        s.setEvent_id(rs.getInt("event_id"));
        s.setCompany_name(rs.getString("company_name"));
        s.setLogo_url(rs.getString("logo_url"));
        s.setContribution_name(rs.getDouble("contribution_name"));
        s.setContact_email(rs.getString("contact_email"));
        s.setContract_url(rs.getString("contract_url"));
        s.setIndustry(rs.getString("industry"));
        s.setPhone(rs.getString("phone"));
        s.setTax_id(rs.getString("tax_id"));
        s.setDocument_url(rs.getString("document_url"));
        try {
            s.setUser_id(rs.getInt("user_id"));
            s.setAccess_code(rs.getString("access_code"));
        } catch (SQLException ignored) {}
        return s;
    }

    // ==================== MÉTHODES POUR LE CONTROLEUR ADMIN ====================
    public int getTotalSponsors() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sponsor";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public double getTotalContribution() throws SQLException {
        String sql = "SELECT SUM(contribution_name) FROM sponsor";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public double getAverageContribution() throws SQLException {
        String sql = "SELECT AVG(contribution_name) FROM sponsor";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public Map<String, Double> getTotalContributionByEvent() throws SQLException {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT e.title, SUM(s.contribution_name) as total " +
                "FROM sponsor s JOIN event e ON s.event_id = e.id " +
                "GROUP BY e.title";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("title"), rs.getDouble("total"));
            }
        }
        return map;
    }

    public ObservableList<String> getDemoEmailsFromSponsor() throws SQLException {
        ObservableList<String> emails = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT contact_email FROM sponsor WHERE contact_email IS NOT NULL";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                emails.add(rs.getString("contact_email"));
            }
        }
        return emails;
    }

    public ObservableList<String> getAllCompanyNames() throws SQLException {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT company_name FROM sponsor WHERE company_name IS NOT NULL ORDER BY company_name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("company_name"));
            }
        }
        return list;
    }

    public int getMySponsorsCountDemo(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sponsor WHERE contact_email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public double getMyTotalContributionDemo(String email) throws SQLException {
        String sql = "SELECT SUM(contribution_name) FROM sponsor WHERE contact_email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public int getMySponsoredEventsCountDemo(String email) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT event_id) FROM sponsor WHERE contact_email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public ObservableList<String> getCompanyNamesByContactEmail(String email) throws SQLException {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT company_name FROM sponsor WHERE contact_email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("company_name"));
                }
            }
        }
        return list;
    }

    public ObservableList<Integer> getEventIdsByContactEmail(String email) throws SQLException {
        ObservableList<Integer> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT event_id FROM sponsor WHERE contact_email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("event_id"));
                }
            }
        }
        return list;
    }

    public String getEventTitleById(int eventId) throws SQLException {
        String sql = "SELECT title FROM event WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("title");
            }
        }
        return null;
    }

    public int getEventIdByTitle(String title) throws SQLException {
        String sql = "SELECT id FROM event WHERE title = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    public ObservableList<String> getAllEventTitles() throws SQLException {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT title FROM event";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("title"));
            }
        }
        return list;
    }

    // ==================== MÉTHODES SUPPLÉMENTAIRES ====================
    public void updateContractUrl(int sponsorId, String contractUrl) throws SQLException {
        String sql = "UPDATE sponsor SET contract_url = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, contractUrl);
            pstmt.setInt(2, sponsorId);
            pstmt.executeUpdate();
        }
    }

    public Map<String, Double> getMyContributionsByCompany(String email) throws SQLException {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT company_name, SUM(contribution_name) as total FROM sponsor WHERE contact_email = ? GROUP BY company_name";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("company_name"), rs.getDouble("total"));
                }
            }
        }
        return map;
    }
}
module com.example.pidev {
 requires javafx.controls;
 requires org.slf4j;
 requires javafx.fxml;
 requires atlantafx.base;
 requires java.sql;
 requires mysql.connector.j;
 requires jakarta.persistence;
 requires java.prefs;
 requires javafx.web;
 requires java.desktop;
 requires java.net.http;
 requires kernel;
 requires layout;
 requires itextpdf;
 requires java.mail;
 requires org.apache.pdfbox;
 requires org.json;


 // Exportez tous les packages nécessaires
 exports com.example.pidev;
 exports com.example.pidev.model.event;
 exports com.example.pidev.model.resource;
 exports com.example.pidev.model.user;
 exports com.example.pidev.model.role;
 exports com.example.pidev.model.sponsor;
 exports com.example.pidev.controller.event;
 exports com.example.pidev.controller.sponsor;
 exports com.example.pidev.controller.auth;
 exports com.example.pidev.controller.user;
 exports com.example.pidev.controller.role;
 exports com.example.pidev.controller.questionnaire;
 exports com.example.pidev.service.user;
 // Ouvrez tous les packages à javafx.fxml

 opens com.example.pidev to javafx.fxml;
 opens com.example.pidev.controller.dashboard to javafx.fxml;
 opens com.example.pidev.controller.event to javafx.fxml;
 opens com.example.pidev.controller.auth to javafx.fxml;
 opens com.example.pidev.controller.user to javafx.fxml;
 opens com.example.pidev.controller.role to javafx.fxml;
 opens com.example.pidev.controller.resource to javafx.fxml;
 opens com.example.pidev.controller.questionnaire to javafx.fxml;
 opens com.example.pidev.controller.sponsor to javafx.fxml;
 opens com.example.pidev.controller.budget to javafx.fxml;
 opens com.example.pidev.controller.depense to javafx.fxml;
 opens com.example.pidev.service.user to javafx.fxml;
}
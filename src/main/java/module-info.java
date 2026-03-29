/**
 * Module descriptor for PiDev application
 * Fixed: Added proper module exports and opens for JavaFX FXML controller access
 */
module com.example.pidev {

    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.sql;
    requires mysql.connector.j;
    requires jakarta.persistence;
    requires java.prefs;
    requires javafx.web; // On la garde ici
    requires java.desktop;
    requires java.net.http;
    requires com.google.gson;
    requires commons.math3;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    requires kernel;
    requires layout;
    requires io;
    requires itextpdf;

    requires java.mail;
    requires twilio;

    requires com.fasterxml.jackson.databind;
    requires org.hibernate.orm.core;


    requires org.apache.pdfbox;


    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires org.bytedeco.javacpp;

    // --- Ouvertures pour JavaFX (FXMLLoader) ---

    requires org.json;
    requires vosk;

    // --- ACCÈS AUX RESSOURCES JS ---
    // Cette ligne permet à la WebView de lire ton fichier apexcharts.min.js
    opens com.example.pidev.js to javafx.web;

    // --- EXPORTS ---

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
    exports com.example.pidev.controller.front;
    exports com.example.pidev.service.user;

    exports com.example.pidev.service.resource;
    exports com.example.pidev.controller.resource;



    // Open all controller packages to javafx.fxml for FXML controller instantiation
    opens com.example.pidev to javafx.fxml;

    // Sous-packages de contrôleurs (ceux-ci contiennent des fichiers)
    opens com.example.pidev.controller.auth to javafx.fxml;
    opens com.example.pidev.controller.user to javafx.fxml;
    opens com.example.pidev.controller.facial to javafx.fxml;
    opens com.example.pidev.controller.chat to javafx.fxml;
    opens com.example.pidev.controller.dashboard to javafx.fxml;
    opens com.example.pidev.controller.event to javafx.fxml;
    opens com.example.pidev.controller.role to javafx.fxml;
    opens com.example.pidev.controller.resource to javafx.fxml;
    opens com.example.pidev.controller.questionnaire to javafx.fxml;
    opens com.example.pidev.controller.sponsor to javafx.fxml;
    opens com.example.pidev.controller.front to javafx.fxml;
    opens com.example.pidev.controller.budget to javafx.fxml;
    opens com.example.pidev.controller.depense to javafx.fxml;

    // Sous-packages de services
    opens com.example.pidev.service.user to javafx.fxml;
    opens com.example.pidev.service.questionnaire to javafx.fxml;

    opens com.example.pidev.service.facial to javafx.fxml;
    opens com.example.pidev.service.chat to javafx.fxml;

    // Sous-packages de modèles
    opens com.example.pidev.fxml.facial to javafx.fxml;
    opens com.example.pidev.fxml.role to javafx.fxml;
    opens com.example.pidev.model.user to javafx.fxml;
    opens com.example.pidev.model.facial to javafx.fxml;
    opens com.example.pidev.model.event to javafx.fxml;
    opens com.example.pidev.model.sponsor to javafx.fxml;
    opens com.example.pidev.model.resource to javafx.base, javafx.fxml;
    opens com.example.pidev.model.role to javafx.base, javafx.fxml;
    // --- Exportations ---

    exports com.example.pidev.model.facial;

    opens com.example.pidev.service.resource to javafx.fxml;









}

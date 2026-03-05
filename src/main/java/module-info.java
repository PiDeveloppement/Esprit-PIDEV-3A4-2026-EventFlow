module com.example.pidev {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires atlantafx.base;

    requires java.sql;
    requires java.prefs;
    requires java.desktop;
    requires java.net.http;
    requires java.mail;

    requires mysql.connector.j;
    requires jakarta.persistence;
    requires com.google.gson;
    requires okhttp3;

    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires org.apache.lucene.core;
    requires org.apache.lucene.queryparser;
    requires org.apache.lucene.analysis.common;
    requires commons.math3;

    requires org.slf4j;
    requires kernel;
    requires layout;
    requires io;
    requires itextpdf;

    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.mail;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpmime;
    requires google.cloud.vision;

    exports com.example.pidev;
    exports com.example.pidev.model.event;
    exports com.example.pidev.model.resource;
    exports com.example.pidev.model.user;
    exports com.example.pidev.model.role;
    exports com.example.pidev.model.sponsor;
    exports com.example.pidev.model.budget;
    exports com.example.pidev.model.depense;

    exports com.example.pidev.controller.event;
    exports com.example.pidev.controller.sponsor;
    exports com.example.pidev.controller.auth;
    exports com.example.pidev.controller.user;
    exports com.example.pidev.controller.role;
    exports com.example.pidev.controller.questionnaire;
    exports com.example.pidev.controller.front;
    exports com.example.pidev.controller.dashboard;
    exports com.example.pidev.controller.resource;
    exports com.example.pidev.controller.budget;
    exports com.example.pidev.controller.depense;

    exports com.example.pidev.service.user;
    exports com.example.pidev.service.event;
    exports com.example.pidev.service.role;
    exports com.example.pidev.service.sponsor;
    exports com.example.pidev.service.budget;
    exports com.example.pidev.service.depense;
    exports com.example.pidev.service.forecast;
    exports com.example.pidev.service.currency;
    exports com.example.pidev.service.whatsapp;
    exports com.example.pidev.service.external;
    exports com.example.pidev.service.upload;
    exports com.example.pidev.service.search;
    exports com.example.pidev.service.pdf;
    exports com.example.pidev.service.chart;
    exports com.example.pidev.service.vision;
    exports com.example.pidev.utils;

    opens com.example.pidev to javafx.fxml;
    opens com.example.pidev.controller.dashboard to javafx.fxml;
    opens com.example.pidev.controller.event to javafx.fxml;
    opens com.example.pidev.controller.auth to javafx.fxml;
    opens com.example.pidev.controller.user to javafx.fxml;
    opens com.example.pidev.controller.role to javafx.fxml;
    opens com.example.pidev.controller.resource to javafx.fxml;
    opens com.example.pidev.controller.questionnaire to javafx.fxml;
    opens com.example.pidev.controller.sponsor to javafx.fxml;
    opens com.example.pidev.controller.front to javafx.fxml;
    opens com.example.pidev.controller.budget to javafx.fxml;
    opens com.example.pidev.controller.depense to javafx.fxml;
    opens com.example.pidev.service.user to javafx.fxml;
}

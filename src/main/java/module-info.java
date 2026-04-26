module com.edukids.edukids3a {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires org.apache.pdfbox;

    exports com.edukids.edukids3a;
    exports com.edukids.edukids3a.controller;
    exports com.edukids.edukids3a.model;
    exports com.edukids.edukids3a.persistence;
    exports com.edukids.edukids3a.security;
    exports com.edukids.edukids3a.service;
    exports com.edukids.edukids3a.ui;
    exports com.edukids.edukids3a.utils;
    exports com.edukids.edukids3a.validation;

    opens com.edukids.edukids3a.controller to javafx.fxml;
    opens com.edukids.edukids3a.ui to javafx.fxml;
    opens com.edukids.edukids3a.model to org.hibernate.orm.core, javafx.base;
    opens com.edukids.edukids3a.persistence to org.hibernate.orm.core;
}

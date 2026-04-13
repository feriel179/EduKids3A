module com.ecom {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.ecom to javafx.fxml;
    opens com.ecom.model to javafx.base;
    opens com.ecom.ui to javafx.fxml;

    exports com.ecom;
    exports com.ecom.model;
    exports com.ecom.ui;
}

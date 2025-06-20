// Letakkan file ini di: src/main/java/module-info.java
module com.example.ismoney {
    // Required modules
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    // PostgreSQL driver
    requires org.postgresql.jdbc;
    requires jbcrypt;

    // Export packages
    exports com.example.ismoney;
    exports com.example.ismoney.controller;
    exports com.example.ismoney.model;
    exports com.example.ismoney.database;

    // Open for JavaFX reflection
    opens com.example.ismoney to javafx.fxml;
    opens com.example.ismoney.controller to javafx.fxml;
    opens com.example.ismoney.model to javafx.fxml;
}
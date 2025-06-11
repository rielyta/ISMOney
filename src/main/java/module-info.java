module com.example.ismoney {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.ismoney to javafx.fxml;
    exports com.example.ismoney;
}
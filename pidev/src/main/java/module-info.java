module com.melocode.pidev {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.melocode.pidev to javafx.fxml;
    exports com.melocode.pidev;
}
module com.rst.outspelled {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires com.almasb.fxgl.all;

    requires java.prefs;

    opens com.rst.outspelled to javafx.fxml;
    opens com.rst.outspelled.ui to javafx.fxml;
    opens com.rst.outspelled.util to javafx.fxml;

    exports com.rst.outspelled;
    exports com.rst.outspelled.ui;
    exports com.rst.outspelled.model;
    exports com.rst.outspelled.network;
    exports com.rst.outspelled.util;
}
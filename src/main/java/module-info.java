module com.github.elektronenbindung.scheduling {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.miachm.sods;
    requires org.jgrapht.core;

    opens scheduling.ui to javafx.fxml;

    exports scheduling.ui;
    // exports scheduling; // Falls du das übergeordnete Paket exportieren möchtest
}


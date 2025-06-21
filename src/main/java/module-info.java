module com.github.elektronenbindung.scheduling {
    // Required JavaFX modules, now with transitive for graphics and base
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics; // Made transitive for Stage visibility
    requires transitive javafx.base; // Good practice to make base transitive too

    // Required external libraries, if they are modular or used across module
    // boundaries
    // The jgrapht.core is handled via classpath for jlink, so it should NOT be
    // required here.
    // requires org.jgrapht.core; // This line should remain commented out or
    // removed for jlink handling
    requires com.github.miachm.sods; // Module name for SODS

    // Open packages for reflection (e.g., FXML and Spring)
    // The package containing your main application class and FXML controllers needs
    // to be opened
    opens scheduling.ui to javafx.fxml;

    // Export packages if other modules or applications outside this one need to
    // access them
    // It's good practice to export packages that contain public APIs.
    exports scheduling; // If scheduling.Main or other classes in 'scheduling' package are entry points
                        // or public APIs
    exports scheduling.ui; // Export the UI package if other parts of your application or external code
                           // needs access
}

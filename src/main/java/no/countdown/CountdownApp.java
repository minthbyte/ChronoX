package no.countdown;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import no.countdown.ui.TimerController;

public class CountdownApp extends Application {

    private TimerController controller;

    @Override
    public void start(Stage primaryStage) {
        controller = new TimerController();
        Scene scene = new Scene(controller, 900, 650);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle("ChronoX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

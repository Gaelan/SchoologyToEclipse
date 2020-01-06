import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class App extends Application {

    private final ZipConverter zipConverter = new ZipConverter();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            Path inputZipPath = getInputZipPath(primaryStage);
            if (inputZipPath == null) {
                return;
            }

            Path outputZipPath = zipConverter.convertZip(inputZipPath);

            if (zipConverter.getUnknownTypeSubmissions().size() > 0) {
                String message = "The following students uploaded something other than a zip file or a java file. You'll have to look at these manually:\n";
                new Alert(Alert.AlertType.WARNING, message + String.join("\n", zipConverter.getUnknownTypeSubmissions())).showAndWait();
            }

            if (zipConverter.getMultipleProjectsSubmissions().size() > 0) {
                String message = "The following students uploaded a zip file containing more than one Eclipse project. The projects will be included in the zip, but their names are unchanged.\n";
                new Alert(Alert.AlertType.WARNING, message + String.join("\n", zipConverter.getMultipleProjectsSubmissions())).showAndWait();
            }

            new Alert(Alert.AlertType.INFORMATION, "Created a new zip file called " + outputZipPath.getFileName() + ". Import that into Eclipse.").showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "An error occurred: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    private Path getInputZipPath(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select zip file downloaded from Schoology.");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("ZIP files", "*.zip")
        );
        File inputZipFile = fileChooser.showOpenDialog(primaryStage);
        return (inputZipFile != null) ? inputZipFile.toPath() : null;
    }
}

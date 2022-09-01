package nz.ac.auckland.se206;

import static nz.ac.auckland.se206.App.loadFxml;

import ai.djl.ModelException;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.net.URISyntaxException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainMenuController {

  @FXML private Button toCanvasButton;
  @FXML private Label wordLabel;

  public void initialise() throws IOException {}

  @FXML
  private void onSwitchToCanvas(ActionEvent event)
      throws ModelException, IOException, URISyntaxException, CsvException {
    // load canvas and switch the scene to canvas when push start button on menu
    SceneManager.addUi(SceneManager.AppUi.CANVAS, loadFxml("canvas"));
    Button button = (Button) event.getSource();
    Scene sceneButtonIsIn = button.getScene();
    sceneButtonIsIn.setRoot(SceneManager.getUiRoot(SceneManager.AppUi.CANVAS));
  }
}

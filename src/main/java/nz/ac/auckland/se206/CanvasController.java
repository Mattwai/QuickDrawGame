package nz.ac.auckland.se206;

import static nz.ac.auckland.se206.ml.DoodlePrediction.printPredictions;
import static nz.ac.auckland.se206.ml.DoodlePrediction.returnPredictions;

import ai.djl.ModelException;
import ai.djl.modality.Classifications;
import ai.djl.translate.TranslateException;
import com.opencsv.exceptions.CsvException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import nz.ac.auckland.se206.ml.DoodlePrediction;
import nz.ac.auckland.se206.speech.TextToSpeech;
import nz.ac.auckland.se206.words.CategorySelector;

/**
 * This is the controller of the canvas. You are free to modify this class and the corresponding
 * FXML file as you see fit. For example, you might no longer need the "Predict" button because the
 * DL model should be automatically queried in the background every second.
 *
 * <p>!! IMPORTANT !!
 *
 * <p>Although we added the scale of the image, you need to be careful when changing the size of the
 * drawable canvas and the brush size. If you make the brush too big or too small with respect to
 * the canvas size, the ML model will not work correctly. So be careful. If you make some changes in
 * the canvas and brush sizes, make sure that the prediction works fine.
 */
public class CanvasController {
  @FXML private Button backButton;
  @FXML private VBox canvasWindow;
  @FXML private Button startButton;
  @FXML private Button saveImageButton;
  @FXML private Label printResult;
  @FXML private Button playAgainButton;
  @FXML private Button penButton;
  @FXML private Button eraseButton;
  @FXML private Label timerLabel;
  @FXML private Button clearButton;
  @FXML private Canvas canvas;

  @FXML private Label wordLabel;

  private GraphicsContext graphic;

  private DoodlePrediction model;

  private String currentWord;
  @FXML private Text predictions;

  private double lastX;
  private double lastY;

  private final int SECONDS = 60;
  private int secondsLeft;

  /**
   * JavaFX calls this method once the GUI elements are loaded. In our case we create a listener for
   * the drawing, and we load the ML model.
   *
   * @throws ModelException If there is an error in reading the input/output of the DL model.
   * @throws IOException If the model cannot be found on the file system.
   */
  public void initialize() throws ModelException, IOException, URISyntaxException, CsvException {
    // When canvas loads disable drawing and other buttons
    stopDrawing();
    startButton.setDisable(false);
    penButton.setDisable(true);
    eraseButton.setDisable(true);
    clearButton.setDisable(true);
    saveImageButton.setVisible(false);
    playAgainButton.setVisible(false);
    printResult.setVisible(false);

    // no predictions have been generated yet
    predictions.setText("==== PREDICTION  ====");

    // create drawing canvas
    graphic = canvas.getGraphicsContext2D();

    // start ml drawing model
    model = new DoodlePrediction();

    // get new word and speak word
    speak();
  }

  /** This method is called when the "Clear" button is pressed. */
  @FXML
  private void onClear() {
    // button to clear drawings from canvas
    graphic.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
  }

  /**
   * This method executes when the user clicks the "Predict" button. It gets the current drawing,
   * queries the DL model and prints on the console the top 5 predictions of the DL model and the
   * elapsed time of the prediction in milliseconds.
   *
   * @throws TranslateException If there is an error in reading the input/output of the DL model.
   */
  @FXML
  private boolean onPredict() throws TranslateException {
    System.out.println("==== PREDICTION  ====");
    System.out.println("Top 10 predictions");

    final long start = System.currentTimeMillis();
    // gets the top 10 prediction from the ml model
    List<Classifications.Classification> predictionResult =
        model.getPredictions(getCurrentSnapshot(), 10);
    printPredictions(predictionResult);
    System.out.println(
        isWin(predictionResult.stream().limit(3).collect(Collectors.toList())) ? "WIN" : "LOST");
    System.out.println("prediction performed in " + (System.currentTimeMillis() - start) + " ms");

    // prints the top predictions to the gui
    predictions.setText("Top 10 Predictions:\n" + returnPredictions(predictionResult));

    // check if drawing is in the top 3 ml model predictions and if it is return true
    if (isWin(predictionResult.stream().limit(3).collect(Collectors.toList()))) {
      printResult.setText("WINNER!!!");
      return true;

    } else {
      printResult.setText("LOST: TRY AGAIN!");
      return false;
    }
  }

  private void end() {
    // when the timer runs out or player wins end game by disabling the canvas and buttons
    saveImageButton.setVisible(true);
    stopDrawing();
    penButton.setDisable(true);
    eraseButton.setDisable(true);
    clearButton.setDisable(true);
    playAgainButton.setVisible(true);
    printResult.setVisible(true);
    startButton.setDisable(true);
  }

  private boolean isWin(List<Classifications.Classification> classifications) {
    // checks if player wins by checking if the word is in the ml predictions
    for (Classifications.Classification classification : classifications) {
      String word = classification.getClassName().replace("_", " ");
      if (word.equals(currentWord)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the current snapshot of the canvas.
   *
   * @return The BufferedImage corresponding to the current canvas content.
   */
  private BufferedImage getCurrentSnapshot() {
    final Image snapshot = canvas.snapshot(null, null);
    final BufferedImage image = SwingFXUtils.fromFXImage(snapshot, null);

    // Convert into a binary image.
    final BufferedImage imageBinary =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

    final Graphics2D graphics = imageBinary.createGraphics();

    graphics.drawImage(image, 0, 0, null);

    // To release memory we dispose.
    graphics.dispose();

    return imageBinary;
  }

  /**
   * Save the current snapshot on a bitmap file.
   *
   * @return The file of the saved image.
   * @throws IOException If the image cannot be saved.
   */
  private File saveCurrentSnapshotOnFile() throws IOException {
    // Open file dialog box
    FileChooser fileChooser = new FileChooser();

    // You can change the location as you see fit.
    final File tmpFolder = new File("tmp");

    // sets initial folder to the tmpfolder
    fileChooser.setInitialDirectory(tmpFolder);

    // make a tmp folder if it doesnt exist
    if (!tmpFolder.exists()) {
      tmpFolder.mkdir();
    }

    // We save the image to a file in the tmp folder.
    fileChooser.setTitle("Save Dialog");
    fileChooser.setInitialFileName("MyDrawing" + ".bmp");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("img", "*.bmp"),
            new FileChooser.ExtensionFilter("img", "*.png"),
            new FileChooser.ExtensionFilter("img", "*.jpeg"));
    // open file dialog box
    Window stage = canvasWindow.getScene().getWindow();
    File file = fileChooser.showSaveDialog(stage);

    // Save the image to a file.
    ImageIO.write(getCurrentSnapshot(), "bmp", file);

    return file;
  }

  @FXML
  private void onSaveImage(ActionEvent event) throws IOException {
    // button to save the canvas drawing file
    saveCurrentSnapshotOnFile();
  }

  @FXML
  private void onSwitchToMenu(ActionEvent event) {
    // button to switch scene back to menu page
    Button button = (Button) event.getSource();
    Scene sceneButtonIsIn = button.getScene();
    sceneButtonIsIn.setRoot(SceneManager.getUiRoot(SceneManager.AppUi.MENU));
  }

  @FXML
  private void onErase() {
    // set on mouse drag to clear the canvas over that area
    canvas.setOnMouseDragged(
        e -> {
          // Brush size (you can change this, it should not be too small or too large).
          final double size = 7.0;

          final double x = e.getX() - size / 2;
          final double y = e.getY() - size / 2;

          // This is the clear brush.
          graphic.clearRect(x, y, size, size);
        });
  }

  @FXML
  private void onPen() {
    // Get co-ordinates of where the mouse is
    canvas.setOnMousePressed(
        e -> {
          lastX = e.getX();
          lastY = e.getY();
        });
    // When mouse is dragged, draw with the pen
    canvas.setOnMouseDragged(
        e -> {
          double size = 5;
          double x = e.getX();
          double y = e.getY();

          graphic.setLineWidth(size);
          graphic.setStroke(Color.BLACK);
          graphic.strokeLine(lastX, lastY, x, y);
          lastX = x;
          lastY = y;
        });
  }

  private void stopDrawing() {
    // disable drawing or erasing when mouse is dragged
    canvas.setOnMouseDragged(e -> {});
  }

  private void speak() throws IOException, URISyntaxException, CsvException {
    // gets a new word of given difficulty and displays the word on the gui
    CategorySelector categorySelector = new CategorySelector();
    String randomWord = categorySelector.getRandomCategory(CategorySelector.Difficulty.E);
    wordLabel.setText(randomWord);
    currentWord = randomWord;
    // create a new task to speak the chosen word
    Task<Void> speakTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            try {
              // in the task using text to speech, speak the chosen word
              TextToSpeech textToSpeech = new TextToSpeech();
              textToSpeech.speak(currentWord);

            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }
        };
    // start the speak word thread
    Thread speakThread = new Thread(speakTask);
    speakThread.start();
  }

  @FXML
  private void onPlayAgain(ActionEvent event)
      throws ModelException, IOException, URISyntaxException, CsvException {
    // button to play again clears the canvas and restarts the game and timer
    onClear();
    initialize();
    timerLabel.setText(String.valueOf(SECONDS));
  }

  @FXML
  private void onStartDrawing(ActionEvent event) {
    // button to start drawing undisables the buttons and starts the timer
    penButton.setDisable(false);
    eraseButton.setDisable(false);
    clearButton.setDisable(false);
    startButton.setDisable(true);

    // displays the timer on the gui
    secondsLeft = SECONDS;
    timerLabel.setText(String.valueOf(SECONDS));
    // creates a new timer and timer task
    Timer timer = new Timer();
    TimerTask timerTask =
        new TimerTask() {
          @Override
          public void run() {
            // when the timer runs decrement the time every second and display new time left
            timerLabel.setText(String.valueOf(secondsLeft));
            secondsLeft--;
            Platform.runLater(
                () -> {
                  // display time left on timer
                  timerLabel.setText(String.valueOf(secondsLeft));

                  try {
                    if (onPredict()) {
                      // check the predictions ever second and if win, stop timer and game
                      timer.cancel();
                      end();
                    }
                  } catch (TranslateException e) {
                    throw new RuntimeException(e);
                  }
                  ;
                });
            if (secondsLeft == 0) {
              // if time to draw runs out, stop timer and game
              timer.cancel();
              end();
            }
          }
        };

    Thread timerThread = new Thread(timerTask);
    timerThread.start();
    // decrement the timer ever second and enables the pen to draw on canvas
    timer.scheduleAtFixedRate(timerTask, 1000, 1000);
    onPen();
  }
}

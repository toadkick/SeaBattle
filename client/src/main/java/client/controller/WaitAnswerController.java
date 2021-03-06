package client.controller;

import client.controller.utils.ProgressAnimation;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
/**
 *class controller for working with a WaitAnswer form
 *@author Dmytro Cherevko
 *@version 1.0
 */

public class WaitAnswerController {
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label lblStatus;

    /**
     * Called to initialize a controller after its root element has been completely processed.
     */
    @FXML
    private void initialize(){
        lblStatus.setText("Waiting for answer from " + CommonWindowController.getCwController().getEnemy());
        ProgressAnimation progressAnimation = new ProgressAnimation();
        progressBar.setProgress(0.0);
        progressBar.progressProperty().bind(progressAnimation.progressProperty());
        new Thread(progressAnimation).start();
    }
}

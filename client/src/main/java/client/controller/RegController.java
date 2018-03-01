package client.controller;

import client.MainLauncher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegController implements Initializable {
    final static Logger logger = Logger.getLogger(ServerListener.class);

    @FXML
    private PasswordField passField;
    @FXML
    private TextField loginField;
    @FXML
    private TextField txtServerPort;
    @FXML
    private TextField txtServerHostname;
    @FXML
    private Button regButton;
    @FXML
    private Button signButton;
    @FXML
    private Label inputStatus;
    public static CommonWindowController cwController;
    private Scene scene;
    private String hostname;
    private String username;
    private String password;
    private int port;

    private static RegController regController;

    public RegController() {
        regController = this;
    }

    public static RegController getRegController() {
        return regController;
    }

    private String status = "Input status: ";

    @FXML
    private void pressRegButton(ActionEvent event){
        if(isNotEmptyFields()){
            checkStatus();
            ServerListener listener = new ServerListener(hostname, port, username, password, cwController, "REG");
            Thread x = new Thread(listener);
            x.start();
        }
    }

    @FXML
    private void pressSignButton(ActionEvent event) throws IOException {
        if(isNotEmptyFields()){
            checkStatus();
            initializeUserInput();

            //FXMLLoader fmxlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
            //Parent window = (Pane) fmxlLoader.load();
            //cwController = fmxlLoader.<CommonWindowController>getController();
            ServerListener listener = new ServerListener(hostname, port, username, password, cwController, "AUTH");
            Thread x = new Thread(listener);
            x.start();

        }
    }
    protected void showCommonWindow() {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            Parent root = null;
            try {
                root = FXMLLoader.load(getClass().getResource("/views/commonWindow.fxml"));
                logger.info("Load commonWindow.fxml is successfully");
            } catch (IOException e) {
                logger.info("Can not load commonWindow.fxml", e);
                logger.error("Can not load commonWindow.fxml", e);
            }
            stage.setOnCloseRequest((WindowEvent e) -> {
                MainLauncher.getPrimaryStageObj().show();
           });
            stage.setTitle("Sea Battle 2018");
            Scene scene = new Scene(root,640,360);
            //scene.getStylesheets().add(0, "resources/css/main.css");
            stage.setScene(scene);
            stage.setMinHeight(360);
            stage.setMinWidth(640);
            stage.show();
            clearUserInput();
            MainLauncher.getPrimaryStageObj().hide();

        });
    }

    private boolean isNotEmptyFields(){
        if(loginField.getText().isEmpty()) {
            inputStatus.setText(status + "Please, enter the login");
            return false;
        }

        if(passField.getText().isEmpty()) {
            inputStatus.setText(status + "Please,enter the password");
            return false;
        }
        if(txtServerPort.getText().isEmpty()) {
            inputStatus.setText(status + "Please,enter the server port");
            return false;
        }
        if(txtServerHostname.getText().isEmpty()) {
            inputStatus.setText(status + "Please,enter the server host");
            return false;
        }
        return true;
    }

    private void checkStatus(){
        inputStatus.setText(status + "Cheking your input...");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    private void initializeUserInput() {
        hostname = txtServerHostname.getText();
        port = Integer.parseInt(txtServerPort.getText());
        username = loginField.getText();
        password = passField.getText();
    }

    private void clearUserInput(){
        loginField.setText("");
        passField.setText("");
    }

    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }

    public void showErrorDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning!");
            alert.setHeaderText(message);
            alert.setContentText("Please check server info and try again.");
            alert.showAndWait();
        });

    }
}
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

public class LogController {
    private TextField nickname;
    private PasswordField password;
    private Stage stage;
    private String msg;
    private ClientConnection clientConnection;
    private Label message;
    private String pw;
    private String nick;
    private Feedback feedback;
    private int i=0;

    public void setClientConnection(ClientConnection clientConnection){
        this.clientConnection=clientConnection;
    }
    public void setStage(Stage stage){
        this.stage=stage;
    }

    public void setFeedback(Feedback feedback){
        this.feedback=feedback;
    }

    public void setAnchor(Parent root){
        nickname=(TextField) root.lookup("#nickname");
        password=(PasswordField) root.lookup("#password");
        message=(Label) root.lookup("#message");
        message.setText("");
    }

    @FXML
    public void sign_up(ActionEvent click){
        nick=nickname.getText();
        pw=password.getText();
        boolean result=clientConnection.my_registration(nick, pw);
        if(!result){
            msg=clientConnection.getMsgAlert();
            feedback.showAlert(Alert.AlertType.ERROR, "Sign-up Error", msg);
            message.setText("Close and reopen the application");
        }
        else{
            message.setText("Now Login");
        }
        try{
            Thread.sleep(3000);
            message.setText("");
            nickname.clear();
            password.clear();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }

    @FXML
    public void login(ActionEvent click){
        message.setText("");
        nick=nickname.getText();
        pw=password.getText();
        boolean result=clientConnection.my_log(nick,pw);
        if(!result){
            msg=clientConnection.getMsgAlert();
            feedback.showAlert(Alert.AlertType.ERROR, "Login Error", msg);
            i++;
            if(i==5) {
                message.setText("You've finished your chances");
                stage.close();
            }
            else {
                message.setText("You have " + (5 - i) + " chances");
            }
            password.clear();
            nickname.clear();
        } else {
            try {
                FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./GUI/Main.fxml"));
                Parent root=loader.load();

                MainController mainController=loader.getController();
                mainController.setAnchor(root);
                mainController.setStage(stage);
                mainController.setClientConnection(clientConnection);
                mainController.setFeedback(feedback);

                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("GUI/style.css").toExternalForm());
                stage.setScene(scene);
                stage.show();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}

package Client;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

public class LogController {
    private ClientGui clientGui;
    private TextField nickname;
    private PasswordField password;
    private String msg;
    private Label message;
    private String pw;
    private String nick;
    private int i=0;

    public void setClientGui(ClientGui client){
        clientGui=client;
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
        boolean result=clientGui.clientConnection.my_registration(nick, pw);
        if(!result){
            msg=clientGui.clientConnection.getMsgAlert();
            clientGui.feedback.showAlert(Alert.AlertType.ERROR, "Sign-up Error", msg);
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
        boolean result=clientGui.clientConnection.my_log(nick,pw);
        if(!result){
            msg=clientGui.clientConnection.getMsgAlert();
            clientGui.feedback.showAlert(Alert.AlertType.ERROR, "Login Error", msg);
            i++;
            if(i==5) {
                message.setText("You've finished your chances");
                clientGui.stage.close();
            }
            else {
                message.setText("You have " + (5 - i) + " chances");
            }
            password.clear();
            nickname.clear();
        } else {
            clientGui.launchMainGui();
        }
    }


}

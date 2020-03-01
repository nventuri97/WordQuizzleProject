package Client;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.event.ActionEvent;

public class ResultController {
    private ClientGui clientGui;
    private Label result;
    private Button home;

    public void setClientGui(ClientGui client){
        clientGui=client;
    }

    public void setAnchor(Parent root){
        result=(Label) root.lookup("#result");
        result.setText("");
    }

    public void showResult(){
        String answer=clientGui.clientConnection.receiveResponse();
        result.setText(answer);
    }

    @FXML
    public void home(ActionEvent click){
        clientGui.launchMainGui();
    }
}

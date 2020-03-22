package Client;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.event.ActionEvent;

public class ResultController {
    private ClientGui clientGui;
    private Label result1;
    private Label result2;
    private Label result3;
    private Label result4;
    private Button home;

    public void setClientGui(ClientGui client){
        clientGui=client;
    }

    public void setAnchor(Parent root){
        result1=(Label) root.lookup("#result1");
        result1.setText("");
        result2=(Label) root.lookup("#result2");
        result2.setText("");
        result3=(Label) root.lookup("#result3");
        result3.setText("");
        result4=(Label) root.lookup("#result4");
        result4.setText("");
    }

    public void showResult(){
        String answer=clientGui.clientConnection.receiveResponse();
        String[] substr=answer.split("\\s+");
        if(substr[1].equals("won")){
            result1.setText(substr[0]+" "+substr[1]+". "+substr[5]+" "+substr[6]+" "+substr[7]+" "+substr[8]+" "+substr[9]);
            result2.setText(substr[2]);
            result3.setText(substr[3]);
            result4.setText(substr[4]);
        }else{
            result1.setText(substr[0]+" "+substr[1]);
            result2.setText(substr[2]);
            result3.setText(substr[3]);
            result4.setText(substr[4]);
        }
    }

    @FXML
    public void home(ActionEvent click){
        clientGui.launchMainGui();
    }
}

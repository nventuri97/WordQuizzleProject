import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

public class GameController {
    private ClientGui clientGui;
    private String msg;
    private Label lblword;
    private TextField wordfield;
    private Button sendWord;
    public void setClientGui(ClientGui client){
        clientGui=client;
    }

    public void setAnchor(Parent root,String newWord){
        wordfield=(TextField) root.lookup("#wordfield");
        lblword=(Label) root.lookup("#lblword");
        lblword.setText(newWord);
    }

    @FXML
    public void sendWord(ActionEvent click){
        msg=wordfield.getText();
        clientGui.clientConnection.sendNewWord(msg);
        lblword.setText("");
        wordfield.clear();
        String word=clientGui.clientConnection.receiveNewWord();
        lblword.setText(word);
        if(word.contains("Time's up") || word.contains("You have finished"))
            clientGui.launchResultGui();
    }


}

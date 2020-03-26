package Client;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

public class GameController {
    private ClientGui clientGui;
    private String msg;
    private Label lblword;
    private TextField wordfield;
    @FXML
    private Button sendWord;
    @FXML
    private Button ok;
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
        if(!msg.equals("")) {
            lblword.setText("");
            clientGui.clientConnection.sendNewWord(msg);
            wordfield.clear();
            String word = clientGui.clientConnection.receiveNewWord();
            lblword.setText(word);
            if (word.contains("Time's up") || word.contains("You have finished")) {
                sendWord.setDisable(true);
                sendWord.setVisible(false);
                ok.setVisible(true);
                ok.setDisable(false);
            }
        }
    }

    @FXML
    public void ok(ActionEvent click){
        ok.setDisable(true);
        ok.setOpacity(0.5);
        clientGui.launchResultGui();
    }


}

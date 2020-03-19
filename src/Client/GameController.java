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
        if(!msg.equals("")) {
            lblword.setText("");
            clientGui.clientConnection.sendNewWord(msg);
            wordfield.clear();
            String word = clientGui.clientConnection.receiveNewWord();
            lblword.setText(word);
            //Stampa di debug
            System.out.println(word);
            if (word.contains("Time's up") || word.contains("You have finished"))
                clientGui.launchResultGui();
        }
    }


}

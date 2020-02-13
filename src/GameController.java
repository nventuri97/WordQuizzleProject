import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

public class GameController {
    private ClientConnection clientConnection;
    private Stage stage;
    private Feedback feedback;
    private String msg;
    private Label lblword;
    private TextField wordfield;
    private Button sendWord;

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
        wordfield=(TextField) root.lookup("#wordfield");
        lblword=(Label) root.lookup("#lblword");
        lblword.setText(clientConnection.receiveNewWord());
    }

    @FXML
    public void sendWord(ActionEvent click){
        msg=wordfield.getText();
        clientConnection.sendNewWord(msg);
        lblword.setText("");
        wordfield.clear();
        lblword.setText(clientConnection.receiveNewWord());
    }
}

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainController {
    private ClientConnection clientConnection;
    private Stage stage;
    private TextField friend;
    private Button add_friend;
    private Button score;
    private Button ranking;
    private Button logout;
    private Button friends;
    private Button new_game;
    private ListView list;
    private Feedback feedback;
    private String msg;
    private Label lblscore;

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
        friend=(TextField) root.lookup("#friend");
        list=(ListView) root.lookup("#list");
        lblscore=(Label) root.lookup("#lblscore");
    }

    @FXML
    public void addFriend(ActionEvent click){
        String friend_nick=friend.getText();
        clientConnection.addFriends(friend_nick);
        if(!(msg=clientConnection.getAdditionalMsg()).equals(""))
            feedback.showAlert(Alert.AlertType.CONFIRMATION, "New friend added", msg);
        else
            feedback.showAlert(Alert.AlertType.ERROR, "Friendship Errore", clientConnection.getMsgAlert());
    }

    @FXML
    public void view_ranking(ActionEvent click){
        list.getItems().clear();
        clientConnection.my_ranking();
        Parser parser=new Parser();
        List<Map.Entry<String, Integer>> ranking=parser.parseRankFromJSON(clientConnection.getAdditionalMsg());
        for(Map.Entry<String, Integer> entry: ranking){
            list.getItems().add(entry.getKey()+" "+ entry.getValue());
        }
    }

    @FXML
    public void view_friends(ActionEvent click){
        list.getItems().clear();
        String result=clientConnection.showFriends();
        Parser parser=new Parser();
        Set<String> setOfFriend=parser.parseFriFromJSON(result);
        for(String s: setOfFriend){
            list.getItems().add(s);
        }
    }

    @FXML
    public void logout(ActionEvent click){
        if(!clientConnection.my_logout()) {
            feedback.showAlert(Alert.AlertType.ERROR, "Logout Error", clientConnection.getMsgAlert());
            stage.close();
        }
    }

    @FXML
    public void view_score(ActionEvent click){
        clientConnection.score();
        msg=clientConnection.getAdditionalMsg();
        lblscore.setText("Your score is "+msg);
    }

    @FXML
    public void newGame(ActionEvent click){
        String nickFriend=friend.getText();
        clientConnection.newGame(nickFriend);
        msg=clientConnection.getAdditionalMsg();
        lblscore.setText(msg);
    }

    @FXML
    public void clean(ActionEvent click){
        list.getItems().clear();
        friend.clear();
        lblscore.setText("");
    }
}

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainController {
    private ClientGui clientGui;
    private TextField friend;
    private Button add_friend;
    private Button score;
    private Button ranking;
    private Button logout;
    private Button friends;
    private Button new_game;
    private ListView list;
    private String msg;
    private Label lblscore;

    public void setClientGui(ClientGui client){
        clientGui=client;
    }

    public void setAnchor(Parent root){
        friend=(TextField) root.lookup("#friend");
        list=(ListView) root.lookup("#list");
        lblscore=(Label) root.lookup("#lblscore");
    }

    @FXML
    public void addFriend(ActionEvent click){
        String friend_nick=friend.getText();
        msg=clientGui.clientConnection.addFriends(friend_nick);
        if(!msg.equals(""))
            lblscore.setText(msg);
        else
            clientGui.feedback.showAlert(Alert.AlertType.ERROR, "Friendship Errore", clientGui.clientConnection.getMsgAlert());
    }

    @FXML
    public void view_ranking(ActionEvent click){
        list.getItems().clear();
        msg=clientGui.clientConnection.my_ranking();
        Parser parser=new Parser();
        List<Map.Entry<String, Integer>> ranking=parser.parseRankFromJSON(msg);
        for(Map.Entry<String, Integer> entry: ranking){
            list.getItems().add(entry.getKey()+" "+ entry.getValue());
        }
    }

    @FXML
    public void view_friends(ActionEvent click){
        list.getItems().clear();
        String result=clientGui.clientConnection.showFriends();
        if(result!=null) {
            Parser parser = new Parser();
            Set<String> setOfFriend = parser.parseFriFromJSON(result);
            for (String s : setOfFriend) {
                list.getItems().add(s);
            }
        } else
            lblscore.setText("You don't already have friends");
    }

    @FXML
    public void logout(ActionEvent click){
        if(!clientGui.clientConnection.my_logout()) {
            clientGui.feedback.showAlert(Alert.AlertType.ERROR, "Logout Error", clientGui.clientConnection.getMsgAlert());
        }else
            clientGui.stage.close();
    }

    @FXML
    public void view_score(ActionEvent click){
        msg=clientGui.clientConnection.score();
        lblscore.setText("Your score is "+msg);
    }

    @FXML
    public void newGame(ActionEvent click){
        String nickFriend=friend.getText();
        msg=clientGui.clientConnection.newGame(nickFriend);
        lblscore.setText(msg);
        if(msg.contains("accepted")) {
            waitTime(clientGui);
        }
    }

    @FXML
    public void clean(ActionEvent click){
        list.getItems().clear();
        friend.clear();
        lblscore.setText("");
    }

    public void waitTime(ClientGui cGui){
        clientGui=cGui;
        clientGui.clientConnection.newGameConnection();
        String word=clientGui.clientConnection.receiveNewWord();
        clientGui.launchGameGUI(word);
    }


}

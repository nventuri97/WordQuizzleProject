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
        msg=clientConnection.addFriends(friend_nick);
        if(!msg.equals(""))
            lblscore.setText(msg);
        else
            feedback.showAlert(Alert.AlertType.ERROR, "Friendship Errore", clientConnection.getMsgAlert());
    }

    @FXML
    public void view_ranking(ActionEvent click){
        list.getItems().clear();
        msg=clientConnection.my_ranking();
        Parser parser=new Parser();
        List<Map.Entry<String, Integer>> ranking=parser.parseRankFromJSON(msg);
        for(Map.Entry<String, Integer> entry: ranking){
            list.getItems().add(entry.getKey()+" "+ entry.getValue());
        }
    }

    @FXML
    public void view_friends(ActionEvent click){
        list.getItems().clear();
        String result=clientConnection.showFriends();
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
        if(!clientConnection.my_logout()) {
            feedback.showAlert(Alert.AlertType.ERROR, "Logout Error", clientConnection.getMsgAlert());
        }else
            stage.close();
    }

    @FXML
    public void view_score(ActionEvent click){
        msg=clientConnection.score();
        lblscore.setText("Your score is "+msg);
    }

    @FXML
    public void newGame(ActionEvent click){
        String nickFriend=friend.getText();
        msg=clientConnection.newGame(nickFriend);
        lblscore.setText(msg);
        if(msg.contains("accepted")) {
            launchGameGUI();
        }
    }

    @FXML
    public void clean(ActionEvent click){
        list.getItems().clear();
        friend.clear();
        lblscore.setText("");
    }

    public void launchGameGUI(){
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./GUI/Game.fxml"));
            Parent root=loader.load();

            GameController gameController=loader.getController();
            gameController.setClientConnection(clientConnection);
            gameController.setNewConnection();
            gameController.setAnchor(root);
            gameController.setStage(stage);
            gameController.setFeedback(feedback);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("GUI/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

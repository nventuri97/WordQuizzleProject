import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;

public class WaitingController{

    private static ClientConnection clientConnection;
    private static Stage stage;
    private Label waiting;
    private Feedback feedback;

    public void setClientConnection(ClientConnection clientConnection){
        this.clientConnection=clientConnection;
    }

    public static void setNewConnection() {
        clientConnection.newGameConnection();
    }

    public void setStage(Stage stage){
        this.stage=stage;
    }

    public void setAnchor(Parent root){
        waiting=(Label) root.lookup("#waiting");
    }

    public void waitTime(){
        String word;
        while(true){
            word=clientConnection.receiveNewWord();
            if(word!="")
                break;
        }
        launchGameGUI(clientConnection, word);
    }

    public void setFeedback(Feedback feedback){
        this.feedback=feedback;
    }


    public static Stage getStage(){
        return stage;
    }

    public void launchGameGUI(ClientConnection connection, String word){
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./GUI/Game.fxml"));
            Parent root=loader.load();

            GameController gameController=loader.getController();
            gameController.setClientConnection(connection);
            gameController.setFeedback(feedback);
            stage=getStage();
            gameController.setStage(stage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("GUI/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            gameController.setAnchor(root, word);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

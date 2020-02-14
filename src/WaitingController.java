import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;

public class WaitingController{

    private ClientConnection clientConnection;
    private Stage stage;
    private Label waiting;

    public void setClientConnection(ClientConnection clientConnection){
        this.clientConnection=clientConnection;
    }

    public void setStage(Stage stage){
        this.stage=stage;
    }

    public void setAnchor(Parent root){
        waiting=(Label) root.lookup("#waiting");
    }

    public void waitTime(){
        try{
            Thread.sleep(3000);
        }catch (InterruptedException ie){
            ie.printStackTrace();
        }
        launchGameGUI();
    }

    public void launchGameGUI(){
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./GUI/Game.fxml"));
            Parent root=loader.load();

            GameController gameController=loader.getController();
            gameController.setClientConnection(clientConnection);
            gameController.setStage(stage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("GUI/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            gameController.setAnchor(root);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientGui extends Application {
    static LogController logController;
    Parent root;
    FXMLLoader loader;
    ClientConnection clientConnection;
    Feedback feedback;

    @Override
    public void start(Stage stage) throws Exception {
        loader= new FXMLLoader(getClass().getClassLoader().getResource("./GUI/Log.fxml"));
        root = loader.load();
        clientConnection=new ClientConnection();

        //Set controller
        logController = loader.getController();
        logController.setAnchor(root);
        logController.setStage(stage);
        logController.setClientConnection(clientConnection);
        feedback=new Feedback();
        logController.setFeedback(feedback);

        //Log window
        stage.setTitle("WORD QUIZZLE");
        stage.setResizable(false);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("GUI/style.css").toExternalForm());
        stage.getIcons().add(new Image("./GUI/lg.png"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

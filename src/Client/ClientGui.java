package Client;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientGui extends Application {
    static LogController logController;                         //Controller della pagina di login
    Parent root;
    FXMLLoader loader;
    ClientConnection clientConnection;                          //Istanza dell'oggetto condiviso per la connessione con il server
    Feedback feedback;                                          //Istanza della classe che produce gli Alert quando necessario
    Stage stage;                                                //Stage principale

    @Override
    public void start(Stage primaryStage) throws Exception {
        loader= new FXMLLoader(getClass().getClassLoader().getResource("./Client/GUI/Log.fxml"));
        root = loader.load();
        clientConnection=new ClientConnection(this);
        stage=primaryStage;
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        //Set LogController
        logController = loader.getController();
        logController.setAnchor(root);
        feedback=new Feedback();
        logController.setClientGui(this);

        //set login window
        stage.setTitle("WORD QUIZZLE");
        stage.setResizable(false);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/Client/GUI/style.css").toExternalForm());
        stage.getIcons().add(new Image("/Client/GUI/lg.png"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void launchMainGui(){
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./Client/GUI/Main.fxml"));
            Parent root=loader.load();

            MainController mainController=loader.getController();
            mainController.setAnchor(root);
            mainController.setClientGui(this);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Client/GUI/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void launchGameGUI(String word){
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./Client/GUI/Game.fxml"));
            Parent root=loader.load();

            GameController gameController=loader.getController();
            gameController.setClientGui(this);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Client/GUI/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            gameController.setAnchor(root, word);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void launchResultGui(){
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getClassLoader().getResource("./Client/GUI/Result.fxml"));
            Parent root=loader.load();

            ResultController resultController=loader.getController();
            resultController.setClientGui(this);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Client/GUI/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            resultController.setAnchor(root);
            resultController.showResult();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

package Client;
import javafx.scene.control.Alert;

public class Feedback {

    public void showAlert(Alert.AlertType type, String title,String message) {
        if(title==null || message ==null)
            return;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

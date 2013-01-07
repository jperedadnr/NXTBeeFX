package nxtbeefx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author José Pereda Llamas
 * Created on 27-dic-2012 - 19:32:10
 */
public class NXTBeeFX extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("NXTBeeFX.fxml"));
        Parent root = (Parent) loader.load();
            
        Scene scene = new Scene(root);
        String css = this.getClass().getResource("NXTBeeFX.css").toExternalForm();
        scene.getStylesheets().add(css);
		
        stage.setWidth(700);
        stage.setHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

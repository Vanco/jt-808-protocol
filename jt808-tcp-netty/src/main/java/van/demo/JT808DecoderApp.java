package van.demo;/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2018/4/11.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class JT808DecoderApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));

        Scene scene = new Scene(root);

        primaryStage.setTitle("JT 808 Decoder Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}

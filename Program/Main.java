package Program;
import GUI.MainUI; import Program.utils.FileManager; import javafx.application.Application; import javafx.stage.Stage;
public class Main extends Application { @Override public void start(Stage stage){FileManager.initialize();new MainUI(stage).showLogin();} public static void main(String[] args){launch(args);} }

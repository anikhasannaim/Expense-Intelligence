package GUI.components; import javafx.geometry.Insets; import javafx.scene.control.Label; import javafx.scene.layout.VBox;
public class AlertCard extends VBox {public AlertCard(String text){setPadding(new Insets(10,12,10,12));getStyleClass().add("alert-card");Label l=new Label(text);l.setWrapText(true);getChildren().add(l);}}

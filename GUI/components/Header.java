package GUI.components;

import GUI.UIFactory;
import Program.model.User;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

public class Header extends HBox {
    public Header(User user) {
        setAlignment(Pos.CENTER_LEFT); setPadding(new Insets(16, 32, 16, 32)); getStyleClass().add("header");
        Label name = new Label("EXPENSE INTELLIGENCE"); name.getStyleClass().add("brand");
        HBox title = new HBox(9, UIFactory.logoBadge(), name); title.setAlignment(Pos.CENTER_LEFT);
        VBox brand = new VBox(title, new Label("Personal Finance Management")); brand.getChildren().get(1).getStyleClass().add("muted");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        SVGPath profileIcon = new SVGPath(); profileIcon.setContent("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-4.4 0-8 2.2-8 5v1h16v-1c0-2.8-3.6-5-8-5z"); profileIcon.getStyleClass().add("header-profile-icon");
        Label username = new Label(user.getUsername()); username.getStyleClass().add("account-name");
        Label chevron = new Label("▾"); chevron.getStyleClass().add("account-chevron");
        HBox account = new HBox(9, profileIcon, username, chevron); account.setAlignment(Pos.CENTER_RIGHT); account.getStyleClass().add("account");
        getChildren().addAll(brand, gap, account);
    }
}

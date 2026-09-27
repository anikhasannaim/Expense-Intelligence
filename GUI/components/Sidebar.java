package GUI.components;

import GUI.UIFactory;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Consumer;

/** Minimal, text-first navigation; all actions remain selectable. */
public class Sidebar extends VBox {
    private final Map<String, Button> navigation = new HashMap<>();
    public Sidebar(Consumer<String> navigate) {
        setSpacing(6); setPadding(new Insets(24, 14, 18, 14)); setPrefWidth(250); getStyleClass().add("sidebar");
        Label name = new Label("EXPENSE\nINTELLIGENCE"); name.getStyleClass().add("logo");
        HBox logo = new HBox(9, UIFactory.logoBadge(), name); getChildren().addAll(logo, new Separator());
        addItem(this, "Dashboard", "Dashboard", "nav-button", navigate);
        addItem(this, "Expenses", "Expenses", "nav-button", navigate);
        addItem(this, "Income", "Income", "nav-button", navigate);
        addItem(this, "Budgets", "Budgets", "nav-button", navigate);
        addItem(this, "Transactions", "Transactions", "nav-button", navigate);
        addItem(this, "Reports", "Reports", "nav-button", navigate);

        addItem(this, "AI INTELLIGENCE", "AI Intelligence", "nav-ai-parent", navigate);
        VBox aiChildren = new VBox(3); aiChildren.getStyleClass().add("ai-submenu");
        addItem(aiChildren, "Spending Analysis", "Spending Analysis", "nav-sub-item", navigate);
        addItem(aiChildren, "Financial Advisor", "Financial Advisor", "nav-sub-item", navigate);
        addItem(aiChildren, "Budget Recommendation", "Budget Recommendation", "nav-sub-item", navigate);
        getChildren().add(aiChildren);
        Region fill = new Region(); VBox.setVgrow(fill, Priority.ALWAYS); getChildren().addAll(fill, new Separator());
        addItem(this, "Settings", "Settings", "nav-button", navigate);
        Button logout = UIFactory.button("Logout", "nav-button"); logout.setMaxWidth(Double.MAX_VALUE); logout.setOnAction(e -> navigate.accept("Logout")); getChildren().add(logout);
    }
    private void addItem(Pane parent, String label, String page, String style, Consumer<String> navigate) {
        Button item = UIFactory.button(label, style); item.setMaxWidth(Double.MAX_VALUE);
        item.setOnAction(e -> navigate.accept(page)); navigation.put(page, item); parent.getChildren().add(item);
    }
    public void select(String page) { navigation.values().forEach(item -> item.getStyleClass().remove("nav-active")); Button current = navigation.get(page); if (current != null) current.getStyleClass().add("nav-active"); }
}

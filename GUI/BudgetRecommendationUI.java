package GUI;

import Program.model.*;
import Program.service.*;
import Program.utils.AIResponseFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Displays AI-selected category budgets while enforcing the user's spending ceiling. */
public class BudgetRecommendationUI extends ScrollPane {
    private static final List<String> FALLBACK_CATEGORIES = List.of("Food", "Bills", "Transport", "Shopping", "Entertainment", "Other");
    private final User user;
    private final BudgetService budgets = new BudgetService();
    private final TransactionService transactions = new TransactionService();
    private final TableView<Row> table = new TableView<>();
    private Map<String, Double> recommended = new LinkedHashMap<>();

    public BudgetRecommendationUI(User user, MainUI app) {
        this.user = user; setFitToWidth(true);
        VBox page = UIFactory.page("Budget Recommendation", "Set your income and savings target, then receive an AI-guided category budget.");
        Label title = (Label) page.getChildren().get(0);
        SVGPath icon = new SVGPath(); icon.setContent("M12,3 A9,9 0 1,0 21,12 L12,12 Z M13,3 L13,11 L21,11 A9,9 0 0,0 13,3 Z");
        icon.getStyleClass().add("ai-heading-icon"); title.setGraphic(icon); title.setContentDisplay(ContentDisplay.LEFT); title.setGraphicTextGap(10);
        Button back = UIFactory.button("← Back", "secondary-button"); back.setOnAction(e -> app.navigate("AI Intelligence"));
        TextField income = UIFactory.field("Monthly income (৳)"), savings = UIFactory.field("Desired monthly savings (৳)");
        income.getStyleClass().add("budget-input"); savings.getStyleClass().add("budget-input");
        Button generate = UIFactory.button("Generate Recommendation", "ai-button"), apply = UIFactory.button("Apply Budget", "primary-button");
        Label note = UIFactory.muted("AI will use your recorded history, active categories, and configured budgets. Recommendations are capped at your spending ceiling."); note.setWrapText(true);
        setup(); populate(Map.of(), 0, 0);
        generate.setOnAction(e -> generate(income, savings, note, generate));
        apply.setOnAction(e -> { if (recommended.isEmpty()) UIFactory.error("Generate an AI recommendation first."); else { budgets.apply(user.getId(), new LinkedHashMap<>(recommended)); UIFactory.info("Budget applied", "The AI recommendation has been saved to budgets.txt."); } });
        page.getChildren().addAll(back, new HBox(10, income, savings), note, table, new HBox(10, generate, apply)); setContent(page);
    }

    private void setup() { table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN); column("Category", row -> row.category); column("Current Budget", row -> money(row.current)); column("AI Recommended", row -> money(row.recommended)); }
    private void column(String title, java.util.function.Function<Row, String> value) { TableColumn<Row, String> column = new TableColumn<>(title); column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue()))); table.getColumns().add(column); }

    private void generate(TextField income, TextField savings, Label note, Button button) {
        try {
            double monthlyIncome = Program.utils.Validator.positiveAmount(income.getText()), targetSavings = Program.utils.Validator.positiveAmount(savings.getText());
            if (targetSavings > monthlyIncome) throw new IllegalArgumentException("Desired savings cannot exceed monthly income.");
            long ceiling = Math.max(0, Math.round(monthlyIncome - targetSavings)); List<String> categories = activeCategories();
            recommended.clear(); populate(Map.of(), monthlyIncome, targetSavings);
            note.setText("AI is analyzing your history and active categories. Spend ceiling: " + money(ceiling) + "."); button.setDisable(true);
            Task<String> task = new Task<>() { @Override protected String call() { return new AIService().budgetRecommendation(user.getId(), monthlyIncome, targetSavings, categories); } };
            task.setOnSucceeded(e -> { try {
                Recommendation result = parseRecommendation(task.getValue(), categories, ceiling); recommended = result.breakdown; populate(recommended, monthlyIncome, targetSavings);
                note.setText(result.summary.isBlank() ? "AI recommendation generated for " + recommended.size() + " active categories. Total: " + money(sum(recommended)) + "." : result.summary);
            } catch (IllegalArgumentException exception) { recommended.clear(); populate(Map.of(), monthlyIncome, targetSavings); note.setText("The AI response could not be safely applied. Please generate the recommendation again."); } finally { button.setDisable(false); } });
            task.setOnFailed(e -> { note.setText("AI recommendation is currently unavailable. Please check your connection and try again."); button.setDisable(false); });
            Thread thread = new Thread(task, "ai-budget-recommendation"); thread.setDaemon(true); thread.start();
        } catch (Exception exception) { UIFactory.error(exception.getMessage()); }
    }

    private List<String> activeCategories() {
        LinkedHashSet<String> categories = new LinkedHashSet<>(); budgets.load(user.getId()).forEach(b -> categories.add(b.getCategory()));
        categories.addAll(transactions.expenseByCategory(user.getId(), LocalDate.now().minusMonths(6), LocalDate.now()).keySet());
        if (categories.isEmpty()) categories.addAll(FALLBACK_CATEGORIES); return new ArrayList<>(categories);
    }

    private Recommendation parseRecommendation(String response, List<String> categories, long ceiling) {
        String json = AIResponseFormatter.sanitizeAIResponse(response).replaceAll("(?s)^\\s*```(?:json)?\\s*|\\s*```\\s*$", "").trim();
        Map<String, Double> proposed = new LinkedHashMap<>();
        Matcher objects = Pattern.compile("(?s)\\{([^{}]*\\\"category\\\"[^{}]*)}").matcher(json);
        while (objects.find()) {
            String object = objects.group(1);
            Matcher name = Pattern.compile("\\\"category\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(object);
            Matcher amount = Pattern.compile("\\\"amount\\\"\\s*:\\s*\\\"?([^\\\",}]+)").matcher(object);
            if (!name.find() || !amount.find()) continue;
            String category = matchingCategory(name.group(1).trim(), categories);
            String numericAmount = amount.group(1).replaceAll("[^0-9.-]", "");
            if (category == null || numericAmount.isBlank() || proposed.putIfAbsent(category, Double.parseDouble(numericAmount)) != null) throw new IllegalArgumentException("Invalid category data");
        }
        if (proposed.size() != categories.size() || proposed.values().stream().anyMatch(value -> value < 0)) throw new IllegalArgumentException("Incomplete AI breakdown");
        Map<String, Double> cleaned = normalizeAiAllocation(categories, proposed, ceiling);
        Matcher summary = Pattern.compile("(?s)\\\"summary\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
        String text = summary.find() ? summary.group(1).replace("\\n", " ").replace("\\\"", "\"").trim() : "";
        boolean changed = categories.stream().anyMatch(category -> Math.round(proposed.get(category)) != Math.round(cleaned.get(category)));
        if (changed) text = "AI allocation has been rounded to clean ৳100 amounts where possible while keeping the exact " + money(ceiling) + " spending ceiling.";
        return new Recommendation(cleaned, text);
    }

    private String matchingCategory(String candidate, List<String> categories) {
        return categories.stream().filter(category -> category.equalsIgnoreCase(candidate)).findFirst().orElse(null);
    }

    /** Preserves AI priorities but turns them into clean increments and an exact total. */
    private Map<String, Double> normalizeAiAllocation(List<String> categories, Map<String, Double> proposed, long ceiling) {
        Map<String, Double> result = new LinkedHashMap<>(); if (ceiling == 0) { categories.forEach(category -> result.put(category, 0d)); return result; }
        double totalWeight = proposed.values().stream().mapToDouble(Double::doubleValue).sum(); if (totalWeight <= 0) throw new IllegalArgumentException("Empty AI allocation");
        long increment = ceiling >= categories.size() * 100L ? 100L : 1L, units = ceiling / increment, remainder = ceiling % increment, used = 0; Map<String, Double> fractions = new HashMap<>();
        for (String category : categories) { double exact = units * proposed.get(category) / totalWeight; long base = (long) Math.floor(exact); result.put(category, (double) (base * increment)); fractions.put(category, exact - base); used += base; }
        List<String> order = new ArrayList<>(categories); order.sort(Comparator.comparingDouble((String category) -> fractions.get(category)).reversed().thenComparing(String::compareTo));
        for (long i = used; i < units; i++) { String category = order.get((int) ((i - used) % order.size())); result.put(category, result.get(category) + increment); }
        if (remainder > 0) { String category = order.get(0); result.put(category, result.get(category) + remainder); } return result;
    }

    private void populate(Map<String, Double> values, double monthlyIncome, double targetSavings) {
        Map<String, Double> current = new LinkedHashMap<>(); budgets.load(user.getId()).forEach(b -> current.put(b.getCategory(), b.getLimit()));
        LinkedHashSet<String> categories = new LinkedHashSet<>(activeCategories()); categories.addAll(values.keySet()); List<Row> rows = new ArrayList<>();
        for (String category : categories) rows.add(new Row(category, current.getOrDefault(category, 0d), values.getOrDefault(category, 0d)));
        rows.add(new Row("TOTAL", sum(current), sum(values))); rows.add(new Row("TARGET SAVINGS", Math.max(0, monthlyIncome - sum(current)), targetSavings)); table.setItems(FXCollections.observableArrayList(rows));
    }
    private double sum(Map<String, Double> values) { return values.values().stream().mapToDouble(Double::doubleValue).sum(); }
    private String money(double value) { return String.format("৳%,.0f", value); }
    private record Recommendation(Map<String, Double> breakdown, String summary) { }
    private static class Row { final String category; final double current, recommended; Row(String category, double current, double recommended) { this.category = category; this.current = current; this.recommended = recommended; } }
}

package GUI;

import Program.model.*;
import Program.service.BudgetService;
import Program.service.TransactionService;
import Program.service.AIService;
import Program.utils.AIResponseFormatter;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SpendingAnalysisUI extends ScrollPane {
    private final TransactionService transactions = new TransactionService();
    public SpendingAnalysisUI(User user, MainUI app) {
        setFitToWidth(true); Analysis data = new Analysis(user.getId());
        VBox page = UIFactory.page("Spending Analysis", "A focused review of your recorded financial data."); Label title = (Label) page.getChildren().get(0);
        SVGPath chartIcon = new SVGPath(); chartIcon.setContent("M3,20 L3,11 L7,11 L7,20 Z M10,20 L10,5 L14,5 L14,20 Z M17,20 L17,2 L21,2 L21,20 Z"); chartIcon.getStyleClass().add("analysis-title-icon"); title.setGraphic(chartIcon); title.setContentDisplay(ContentDisplay.LEFT); title.setGraphicTextGap(10);
        Button back = UIFactory.button("← Back", "secondary-button"); back.setOnAction(e -> app.navigate("AI Intelligence"));
        HBox stats = new HBox(12, new GUI.components.StatCard("TOTAL SPENDING", money(data.historyExpenses), "#EF4444"), new GUI.components.StatCard("TOP CATEGORY", data.topCategory(), "#A78BFA"), new GUI.components.StatCard("SAVINGS OPPORTUNITY", data.savingsOpportunity(), "#22C55E")); stats.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        TextFlow patternsFlow = textFlow("Loading AI key patterns…");
        TextFlow summaryFlow = textFlow("Loading AI financial analysis…");
        page.getChildren().addAll(back, stats, card("KEY PATTERNS", patternsFlow, "analysis-card"), card("AI SUMMARY", summaryFlow, "summary-callout")); setContent(page);
        loadAiAnalysis(user.getId(), patternsFlow, summaryFlow);
    }
    private VBox card(String heading, javafx.scene.Node content, String style) { Label badge = new Label(heading); badge.getStyleClass().add("analysis-badge"); VBox card = new VBox(12, badge, content); card.setPadding(new Insets(20)); card.getStyleClass().add(style); return card; }
    private TextFlow textFlow(String value) { TextFlow flow = new TextFlow(); flow.setLineSpacing(5); flow.setMaxWidth(Double.MAX_VALUE); flow.getStyleClass().add("analysis-text-flow"); for (String line : value.split("\\n", -1)) addLine(flow, line); return flow; }
    private void addLine(TextFlow flow, String line) { boolean heading = line.startsWith("## "); String content = heading ? line.substring(3) : line; if (content.isBlank()) { flow.getChildren().add(text("\n", "analysis-summary-spacer")); return; } int cursor = 0; while (cursor < content.length()) { int start = content.indexOf("**", cursor); if (start < 0) { flow.getChildren().add(text(content.substring(cursor), heading ? "analysis-summary-heading" : "analysis-text")); break; } if (start > cursor) flow.getChildren().add(text(content.substring(cursor, start), heading ? "analysis-summary-heading" : "analysis-text")); int end = content.indexOf("**", start + 2); if (end < 0) { flow.getChildren().add(text(content.substring(start), heading ? "analysis-summary-heading" : "analysis-text")); break; } flow.getChildren().add(text(content.substring(start + 2, end), "analysis-summary-strong")); cursor = end + 2; } flow.getChildren().add(text("\n", "analysis-text")); }
    private Text text(String value, String style) { Text text = new Text(value); text.getStyleClass().add(style); return text; }
    private void populateTextFlow(TextFlow flow, String value) { flow.getChildren().clear(); for (String line : value.split("\\n", -1)) addLine(flow, line); }
    private void loadAiAnalysis(String userId, TextFlow patternsFlow, TextFlow summaryFlow) {
        AIService ai = new AIService();
        Task<String> patternsTask = new Task<>() { @Override protected String call() { return ai.keyPatterns(userId); } };
        patternsTask.setOnSucceeded(e -> populateTextFlow(patternsFlow, AIResponseFormatter.sanitizeRichAIResponse(patternsTask.getValue())));
        patternsTask.setOnFailed(e -> populateTextFlow(patternsFlow, "• AI key patterns are currently unavailable.\n• Your recorded transactions remain available in the summary below."));
        Task<String> summaryTask = new Task<>() { @Override protected String call() { return ai.spendingAnalysis(userId); } };
        summaryTask.setOnSucceeded(e -> populateTextFlow(summaryFlow, AIResponseFormatter.sanitizeRichAIResponse(summaryTask.getValue())));
        summaryTask.setOnFailed(e -> populateTextFlow(summaryFlow, "AI analysis is currently unavailable. Please try again after checking the AI connection."));
        Thread patternsThread = new Thread(patternsTask, "ai-key-patterns"); patternsThread.setDaemon(true); patternsThread.start();
        Thread summaryThread = new Thread(summaryTask, "ai-spending-analysis"); summaryThread.setDaemon(true); summaryThread.start();
    }
    private static String money(double amount) { return String.format("৳%,.0f", amount); }
    private static String percent(double value) { return String.format("%.1f%%", value); }

    private final class Analysis {
        final String userId; final LocalDate today = LocalDate.now(), monthStart = today.withDayOfMonth(1), previousStart = monthStart.minusMonths(1), previousEnd = previousStart.plusDays(Math.min(today.getDayOfMonth(), previousStart.lengthOfMonth()) - 1L);
        final List<Transaction> all; final List<Budget> budgets; final Map<String, Double> currentSpending, previousSpending;
        final double currentIncome, currentExpenses, previousIncome, previousExpenses, historyIncome, historyExpenses, currentBalance, historyBalance, currentRate, historyRate, totalBudget;
        Analysis(String userId) { this.userId = userId; all = transactions.all(userId); budgets = new BudgetService().load(userId); currentSpending = transactions.expenseByCategory(userId, monthStart, today); previousSpending = transactions.expenseByCategory(userId, previousStart, previousEnd); currentIncome = total(Income.class, monthStart, today); currentExpenses = total(Expense.class, monthStart, today); previousIncome = total(Income.class, previousStart, previousEnd); previousExpenses = total(Expense.class, previousStart, previousEnd); historyIncome = total(Income.class, null, null); historyExpenses = total(Expense.class, null, null); currentBalance = currentIncome-currentExpenses; historyBalance = historyIncome-historyExpenses; currentRate = rate(currentIncome,currentExpenses); historyRate = rate(historyIncome,historyExpenses); totalBudget = budgets.stream().mapToDouble(Budget::getLimit).sum(); }
        private double total(Class<?> kind, LocalDate from, LocalDate to) { return all.stream().filter(kind::isInstance).filter(t -> from == null || (!t.getDate().isBefore(from) && !t.getDate().isAfter(to))).mapToDouble(Transaction::getAmount).sum(); }
        private double rate(double income, double expense) { return income == 0 ? 0 : (income-expense)*100/income; }
        String topCategory() { return currentSpending.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("No spending yet"); }
        double topAmount() { return currentSpending.values().stream().mapToDouble(Double::doubleValue).max().orElse(0); }
        double limit(String category) { return budgets.stream().filter(b -> b.getCategory().equals(category)).mapToDouble(Budget::getLimit).findFirst().orElse(0); }
        String priorityCategory() { return currentSpending.keySet().stream().filter(c -> limit(c)>0 && currentSpending.get(c)/limit(c)>=.8).max(Comparator.comparingDouble(c -> currentSpending.get(c)/limit(c))).orElse(null); }
        String savingsOpportunity() { String c = priorityCategory(); return c == null ? (currentBalance >= 0 ? "Protect " + money(currentBalance) : "Review cash flow") : "Monitor " + c; }
        String keyPatterns() { double top = topAmount(), share = currentExpenses == 0 ? 0 : top*100/currentExpenses; String first = top == 0 ? "• No expense transactions have been recorded for the current month yet." : "• **"+topCategory()+"** is the largest contributor to current-month spending at **"+money(top)+"**, or **"+percent(share)+"** of expenses."; String focus = priorityCategory(); String second = focus != null ? "• **"+focus+"** has used **"+percent(currentSpending.get(focus)*100/limit(focus))+"** of its budget, so it should be monitored for the remainder of the month." : currentBalance >= 0 ? "• Income is currently covering recorded spending, leaving a **"+money(currentBalance)+"** balance to protect." : "• Recorded expenses exceed income by **"+money(-currentBalance)+"**, making near-term spending control important."; return first+"\n"+second; }
        String summary() { StringBuilder s = new StringBuilder("## Current Month Financial Snapshot\n• Income: **"+money(currentIncome)+"**\n• Expenses: **"+money(currentExpenses)+"**\n• Balance: **"+money(currentBalance)+"**\n• Savings Rate: **"+percent(currentRate)+"**\n\n"+monthExplanation()+"\n\n## Spending Summary\n"); for (String c : relevantCategories()) { double spent=currentSpending.getOrDefault(c,0d), budget=limit(c); s.append("• **").append(c).append("**: ").append(money(spent)); if(budget>0)s.append(" / ").append(money(budget)).append(" budget — **").append(percent(spent*100/budget)).append(" used**"); else s.append(" / No budget set"); s.append("\n"); } double usage=totalBudget==0?0:currentExpenses*100/totalBudget; s.append("• **Total**: ").append(money(currentExpenses)).append(" / ").append(money(totalBudget)).append(" budget — **").append(percent(usage)).append(" used**\n• Remaining budget: **").append(money(totalBudget-currentExpenses)).append("**\n"); if(topAmount()>0)s.append("\n## Top Expense of This Month\nYour highest expense this month is **").append(topCategory()).append(" at ").append(money(topAmount())).append("**. It represents **").append(percent(topAmount()*100/currentExpenses)).append("** of recorded monthly spending").append(limit(topCategory())>0 ? " and has used **"+percent(topAmount()*100/limit(topCategory()))+"** of its budget." : ".").append("\n"); s.append("\n## Overall Financial Insight\n• Total Income: **").append(money(historyIncome)).append("**\n• Total Expenses: **").append(money(historyExpenses)).append("**\n• Balance: **").append(money(historyBalance)).append("**\n• Overall Savings Rate: **").append(percent(historyRate)).append("**\n• Average Monthly Income: **").append(money(historyIncome/activeMonths())).append("**\n• Average Monthly Expenses: **").append(money(historyExpenses/activeMonths())).append("**\n\nOverall, ").append(historyBalance>=0?"income has remained above expenses across the recorded history.":"recorded expenses are higher than income and need attention."); s.append("\n\n## Compared With Overall History\n").append(historyComparison()).append("\n\n## Compared With Previous Month\n").append(previousComparison()).append("\n\n## Key Insights\n").append(keyInsights()).append("\n\n## Recommendations\n").append(recommendations()).append("\n\n## Improvement Outlook\n").append(outlook()); return s.toString(); }
        List<String> relevantCategories() { return currentSpending.keySet().stream().sorted(Comparator.<String>comparingDouble(c -> limit(c)>0 ? currentSpending.get(c)/limit(c) : 0).reversed().thenComparing(Comparator.comparingDouble((String c) -> currentSpending.get(c)).reversed())).limit(4).toList(); }
        double activeMonths() { return Math.max(1, all.stream().map(t -> YearMonth.from(t.getDate())).distinct().count()); }
        String monthExplanation() { return currentIncome==0&&currentExpenses==0 ? "No income or expense activity has been recorded this month, so there is not yet a meaningful cash-flow position to assess." : currentBalance>=0 ? "Income currently exceeds recorded expenses, leaving a positive balance. Monitoring the highest-use budget categories will help preserve this position." : "Recorded expenses currently exceed income, which makes near-term spending discipline important."; }
        String historyComparison() { return "• Current expenses are **"+relation(currentExpenses,historyExpenses/activeMonths())+"** your average monthly expenses.\n• Current income is **"+relation(currentIncome,historyIncome/activeMonths())+"** your average monthly income.\n• Current savings rate is **"+percent(currentRate)+"** versus a historical **"+percent(historyRate)+"**."; }
        String previousComparison() { String window=today.getDayOfMonth()==today.lengthOfMonth()?"full-month":"same-day partial-month"; return "• This comparison uses a **"+window+"** window: "+monthStart+" to "+today+" versus "+previousStart+" to "+previousEnd+".\n• Expenses are **"+relation(currentExpenses,previousExpenses)+"** the comparable previous-month period.\n• Income is **"+relation(currentIncome,previousIncome)+"** the comparable previous-month period.\n• Current balance is **"+money(currentBalance)+"**."; }
        String relation(double current,double baseline) { if(baseline==0)return current==0?"similar to":"higher than"; double change=(current-baseline)*100/baseline; return Math.abs(change)<5?"similar to":(change>0?"above":"below")+" by **"+percent(Math.abs(change))+"**"; }
        String keyInsights() { List<String> p=new ArrayList<>(); if(currentBalance>=0)p.add("• Your current financial position is **positive**, with a balance of **"+money(currentBalance)+"**."); String c=priorityCategory(); if(c!=null)p.add("• **"+c+"** is the main budget-pressure category at **"+percent(currentSpending.get(c)*100/limit(c))+"** used."); if(topAmount()>0)p.add("• **"+topCategory()+"** is the largest spending driver this month."); p.add("• Current savings rate is **"+percent(currentRate)+"**, compared with **"+percent(historyRate)+"** across your recorded history."); return p.stream().limit(4).collect(Collectors.joining("\n")); }
        String recommendations() { List<String> p=new ArrayList<>(); String c=priorityCategory(); if(c!=null)p.add("1. Keep **"+c+"** spending within its remaining budget to avoid an overrun this month."); if(topAmount()>0)p.add((p.size()+1)+". Review recent **"+topCategory()+"** transactions and retain only necessary spending for the rest of the month."); if(currentBalance>0)p.add((p.size()+1)+". Reserve part of the **"+money(currentBalance)+"** balance for savings or known upcoming bills."); while(p.size()<2)p.add((p.size()+1)+". Record upcoming income and expenses so the next review has a complete picture."); return String.join("\n",p); }
        String outlook() { return currentBalance>=0?"Your current month is financially sustainable so far. Keeping close watch on the highest-use categories can help protect or improve the savings rate.":"This month needs closer spending control. Reducing pressure in the highest-use categories can help restore a positive balance."; }
    }
}

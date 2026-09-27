package Program.service;

import Program.api.GroqClient;
import Program.interfaces.Analyzable;
import Program.model.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class AIService {
    private final GroqClient client = new GroqClient();
    private final TransactionService transactions = new TransactionService();
    private final BudgetService budgets = new BudgetService();

    public String keyPatterns(String userId) {
        return ask("Act as a senior personal-finance analyst. Using only the supplied records, return exactly two distinct, short, financially meaningful bullet points for a Key Patterns section. Prioritize the largest spending driver and the most important budget, savings-rate, historical, or unusual-spending signal. Do not invent facts, do not add a heading, recommendations, disclaimer, table, HTML, or any extra text. Every money value must use ৳.", analysisContext(userId));
    }

    public String spendingAnalysis(String userId) {
        String format = "Act as a senior personal-finance analyst. Give a professional, thoughtful, evidence-led analysis of only the supplied records. Be precise, calm and constructive: explain the implication of each important finding, distinguish facts from recommendations, and never invent data. Use this exact plain-text section order: Current Month Financial Snapshot with bullets for Income, Expenses, Balance and Savings Rate, followed by a short interpretation; Spending Summary with the important current-month categories, total/total budget/remaining budget/overall usage and a Top Expense of This Month interpretation; Overall Financial Insight with total income, total expenses, balance, overall savings rate, average monthly income and average monthly expenses; Compared With Overall History; Compared With Previous Month; Key Insights; Recommendations; Improvement Outlook. Do not show Net Cash Flow. Current versus previous month already uses a like-for-like day range; do not compare a partial month with a full previous month. Every monetary value must use ৳. Never use Tk, BDT, dollars, Markdown tables, ASCII tables, pipes or HTML. Avoid filler, repetition and generic advice.";
        return ask(format, analysisContext(userId));
    }

    public String advisor(String userId, String question) {
        if (question == null || question.isBlank())
            throw new IllegalArgumentException("Ask a financial question first.");
        String instructions = "You are a data-aware personal financial advisor. Answer only from the supplied financial context; never invent transactions, categories, budgets, dates, or trends. Date-scope rules are strict: when the user asks about 'this month', 'current month', spending, income, balance, or budget without another period, use ONLY the Current Month section. Current-month spending must never be replaced with overall totals. For current versus previous month, use ONLY the supplied like-for-like comparison period; never compare a partial current month with a full previous month. For current versus history, compare the Current Month section to the supplied overall historical monthly averages. If required data is missing, state that plainly. Explain calculations briefly when helpful and answer the question directly before recommendations. Use ৳ for every monetary amount. Never use Tk, BDT, Rs, INR, ₹, ₨, dollars, tables, pipes, or HTML. Keep spending/budget responses readable with short bullets.";
        return ask(instructions, analysisContext(userId) + "\n\nUser question: " + question);
    }

    public String budgetRecommendation(String userId, double monthlyIncome, double desiredSavings, List<String> activeCategories) {
        long ceiling = Math.max(0, Math.round(monthlyIncome - desiredSavings));
        String categoryList = String.join(", ", activeCategories);
        return ask("You are an expert Bangladeshi personal-finance budgeting advisor. You—not the application—must choose the recommendation using the supplied transaction history, spending patterns, configured budgets, and category names. If relevant history exists, infer realistic priorities from it. If no history exists for a category, use its name and configured budget as sensible evidence. The spend ceiling is absolute: recommendations must total exactly that amount and never exceed it. Allocate only across the listed active categories, including every one exactly once. Use clean whole-Taka amounts, preferably multiples of ৳100; avoid awkward values such as ৳1,895, ৳6,316, or ৳789. Return VALID JSON ONLY in this exact shape: {\"breakdown\":[{\"category\":\"Food\",\"amount\":5000}],\"summary\":\"short explanation using ৳\"}. The summary must match the returned category values and explain the pattern briefly. Do not use Markdown, code fences, Tk, BDT, Rs, INR, or any currency marker other than ৳.", analysisContext(userId) + "\n\nMonthly income: ৳" + Math.round(monthlyIncome) + "\nTarget savings: ৳" + Math.round(desiredSavings) + "\nABSOLUTE SPEND CEILING: ৳" + ceiling + "\nACTIVE CATEGORIES (return each exactly once, no others): " + categoryList + "\nGenerate the budget JSON now.");
    }

    public Map<String, Double> localRecommendation(String userId, double income, double savings) {
        long ceiling = Math.max(0, Math.round(income - savings));
        Map<String, Double> spend = transactions.expenseByCategory(userId, LocalDate.now().minusMonths(3), LocalDate.now());
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        budgets.load(userId).forEach(b -> categories.add(b.getCategory()));
        categories.addAll(spend.keySet());
        if (categories.isEmpty())
            categories.addAll(List.of("Food", "Bills", "Transport", "Shopping", "Entertainment", "Other"));
        Map<String, Double> raw = new LinkedHashMap<>();
        double historical = categories.stream().mapToDouble(c -> spend.getOrDefault(c, 0d)).sum();
        Map<String, Double> defaults = Map.of("Food", .35, "Bills", .25, "Transport", .15, "Shopping", .10, "Entertainment", .10, "Other", .05);
        for (String c : categories)
            raw.put(c, historical > 0 ? spend.getOrDefault(c, 0d) : defaults.getOrDefault(c, 1d));
        double total = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) {
            raw.replaceAll((c, v) -> 1d);
            total = raw.size();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        Map<String, Double> fractions = new LinkedHashMap<>();
        long allocated = 0;
        for (String c : categories) {
            double exact = ceiling * raw.get(c) / total;
            long base = (long) Math.floor(exact);
            result.put(c, (double) base);
            fractions.put(c, exact - base);
            allocated += base;
        }
        List<String> remainderOrder = new ArrayList<>(categories);
        remainderOrder.sort(Comparator.comparingDouble((String c) -> fractions.get(c)).reversed().thenComparing(c -> c));
        for (long i = 0; i < ceiling - allocated; i++) {
            String c = remainderOrder.get((int) (i % remainderOrder.size()));
            result.put(c, result.get(c) + 1);
        }
        return result;
    }

    public List<String> localAlerts(String userId) {
        List<String> alerts = new ArrayList<>();
        Map<String, Double> spending = transactions.expenseByCategory(userId, LocalDate.now().withDayOfMonth(1), LocalDate.now());
        for (Budget b : budgets.load(userId)) {
            double used = spending.getOrDefault(b.getCategory(), 0d);
            if (used > b.getLimit()) alerts.add("🔴 " + b.getCategory() + " spending exceeds its budget.");
            else if (b.getLimit() > 0 && used / b.getLimit() >= .8)
                alerts.add("🟠 " + b.getCategory() + " budget is almost reached.");
        }
        String top = spending.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (top != null && !alerts.stream().anyMatch(a -> a.contains(top)))
            alerts.add("💡 Review " + top + " spending for a savings opportunity.");
        return alerts.isEmpty() ? List.of("💡 Add a budget or transactions to receive data-based financial alerts.") : alerts;
    }

    private String context(String user) {
        List<Transaction> list = transactions.all(user);
        double income = list.stream().filter(t -> t instanceof Income).mapToDouble(Transaction::getAmount).sum();
        double expense = list.stream().filter(t -> t instanceof Expense).mapToDouble(Transaction::getAmount).sum();
        Map<String, Double> categoryMap = transactions.expenseByCategory(user, LocalDate.now().minusMonths(3), LocalDate.now());
        String categories = categoryMap.entrySet().stream().map(e -> e.getKey() + "=৳" + Math.round(e.getValue())).collect(Collectors.joining(", "));
        String budget = budgets.load(user).stream().map(b -> b.getCategory() + "=৳" + Math.round(b.getLimit())).collect(Collectors.joining(", "));
        Analyzable insight = new SpendingInsight(categoryMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("none"), expense);
        return "Financial context (no personal credentials): total income=৳" + Math.round(income) + ", total expenses=৳" + Math.round(expense) + ", balance=৳" + Math.round(income - expense) + ", " + insight.analyze() + ", recent expense categories=" + categories + ", budgets=" + budget + ", recent transactions=" + list.stream().limit(8).map(t -> t.getDate() + " " + t.getType() + " " + t.getCategory() + " ৳" + Math.round(t.getAmount())).collect(Collectors.joining("; "));
    }

    private String analysisContext(String user) {
        LocalDate today = LocalDate.now(), start = today.withDayOfMonth(1), previous = start.minusMonths(1), previousEnd = previous.plusDays(Math.min(today.getDayOfMonth(), previous.lengthOfMonth()) - 1L);
        List<Transaction> list = transactions.all(user);
        java.util.function.BiFunction<LocalDate, LocalDate, String> totals = (from, to) -> {
            double income = list.stream().filter(t -> t instanceof Income && !t.getDate().isBefore(from) && !t.getDate().isAfter(to)).mapToDouble(Transaction::getAmount).sum(), expense = list.stream().filter(t -> t instanceof Expense && !t.getDate().isBefore(from) && !t.getDate().isAfter(to)).mapToDouble(Transaction::getAmount).sum();
            double rate = income == 0 ? 0 : (income - expense) * 100 / income;
            return "income=৳" + Math.round(income) + ", expenses=৳" + Math.round(expense) + ", balance=৳" + Math.round(income - expense) + ", savings rate=" + String.format("%.1f%%", rate);
        };
        Map<String, Double> current = transactions.expenseByCategory(user, start, today), prior = transactions.expenseByCategory(user, previous, previousEnd);
        String currentCategories = current.entrySet().stream().map(e -> e.getKey() + "=৳" + Math.round(e.getValue())).collect(Collectors.joining(", "));
        String priorCategories = prior.entrySet().stream().map(e -> e.getKey() + "=৳" + Math.round(e.getValue())).collect(Collectors.joining(", "));
        String budget = budgets.load(user).stream().map(b -> b.getCategory() + "=৳" + Math.round(b.getLimit())).collect(Collectors.joining(", "));
        double totalIncome = list.stream().filter(t -> t instanceof Income).mapToDouble(Transaction::getAmount).sum(), totalExpense = list.stream().filter(t -> t instanceof Expense).mapToDouble(Transaction::getAmount).sum();
        List<YearMonth> dataMonths = list.stream().map(t -> YearMonth.from(t.getDate())).distinct().sorted().toList();
        long months = Math.max(1, dataMonths.size());
        String monthLabels = dataMonths.stream().map(YearMonth::toString).collect(Collectors.joining(", "));
        String records = list.stream().sorted(Comparator.comparing(Transaction::getDate)).map(t -> t.getDate() + " | " + t.getType() + " | " + t.getCategory() + " | ৳" + Math.round(t.getAmount()) + (t.getDescription().isBlank() ? "" : " | " + t.getDescription())).collect(Collectors.joining("; "));
        return "Analysis date: " + today + "\nCURRENT MONTH ONLY (" + start + " to " + today + "): " + totals.apply(start, today) + "; category expenses=" + currentCategories + "; monthly budgets=" + budget + ".\nPREVIOUS MONTH, LIKE-FOR-LIKE (" + previous + " to " + previousEnd + "): " + totals.apply(previous, previousEnd) + "; category expenses=" + priorCategories + ".\nOVERALL HISTORY (only months with recorded transactions: " + monthLabels + "; " + months + " data months): total income=৳" + Math.round(totalIncome) + ", total expenses=৳" + Math.round(totalExpense) + ", total balance=৳" + Math.round(totalIncome - totalExpense) + ", average monthly income=৳" + Math.round(totalIncome / months) + ", average monthly expenses=৳" + Math.round(totalExpense / months) + ", overall savings rate=" + String.format("%.1f%%", totalIncome == 0 ? 0 : (totalIncome - totalExpense) * 100 / totalIncome) + ". Do not divide by calendar months without data.\nALL RECORDED TRANSACTIONS: " + records;
    }

    private String ask(String sys, String prompt) {
        try {
            return client.chat(sys, prompt);
        } catch (Exception e) {
            System.err.println("AI error: " + e.getMessage());
            throw new IllegalStateException("Unable to connect to AI service. Please check your internet connection and API key.");
        }
    }

    private double round(double d) {
        return Math.round(d / 50) * 50;
    }
}

package Program.utils;

import Program.model.Budget;
import java.util.*;

/** Cleans model output and embeds real spending data into an existing chat message. */
public final class AIResponseFormatter {
    private AIResponseFormatter() { }

    public static String sanitizeAIResponse(String response) {
        return sanitize(response, true);
    }

    /** Cleans AI output while retaining bold markers for rich TextFlow rendering. */
    public static String sanitizeRichAIResponse(String response) {
        return sanitize(response, false);
    }

    private static String sanitize(String response, boolean stripMarkdown) {
        if (response == null || response.isBlank()) return "";
        String decoded = decodeUnicode(response).replace("\\n", "\n").replace("&nbsp;", " ");
        decoded = decoded.replace("US$", "৳").replace("$", "৳").replace("€", "৳")
                .replace("₹", "৳").replace("₨", "৳")
                .replaceAll("(?i)\\b(?:USD|BDT|Tk|INR|Rs\\.?)\\s*", "৳")
                .replaceAll("(?i)\\bdollars?\\b", "৳");
        decoded = decoded.replaceAll("(?i)<br\\s*/?>", "\n");
        decoded = decoded.replaceAll("(?s)<[^>]*>", "");
        // Remove incomplete tags created by malformed sequences such as u003ce.
        decoded = decoded.replaceAll("(?im)<[a-z/][^>\\n]*(?:>|$)", "");
        decoded = decoded.replaceAll("(?m)^\\s*---+\\s*$", "");
        if (stripMarkdown) decoded = decoded.replaceAll("\\*{1,3}", "");
        return decoded.replaceAll("\\n{3,}", "\n\n").trim();
    }

    /** Replaces an AI pipe table with concise, real-data spending lines in the same chat message. */
    public static String formatSpendingSummary(String response, Map<String, Double> spending, List<Budget> budgets, double balance) {
        String clean = sanitizeAIResponse(response);
        if (!containsPipeTable(clean)) return clean;
        String summary = createSummaryLines(spending, budgets, balance);
        if (summary.isBlank()) return removePipeTable(clean);

        StringBuilder result = new StringBuilder();
        boolean inserted = false;
        for (String line : clean.split("\\R", -1)) {
            if (isTableLine(line)) {
                if (!inserted) { result.append(summary).append('\n'); inserted = true; }
                continue;
            }
            result.append(line).append('\n');
        }
        return result.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    /** Keeps a single, application-calculated spending summary in the AI analysis. */
    public static String replaceSpendingSummary(String response, Map<String, Double> spending, List<Budget> budgets, double balance) {
        String clean = sanitizeAIResponse(response);
        String summary = createSummaryLines(spending, budgets, balance);
        if (clean.isBlank()) return summary;

        java.util.regex.Pattern block = java.util.regex.Pattern.compile(
                "(?ims)^\\s*Spending Summary\\s*$.*?(?=^\\s*(?:Key Insights & Alerts|Practical next steps)\\b|\\z)");
        java.util.regex.Matcher matcher = block.matcher(clean);
        if (matcher.find()) {
            return matcher.replaceFirst(java.util.regex.Matcher.quoteReplacement(summary + "\n\n"))
                    .replaceAll("\\n{3,}", "\n\n").trim();
        }

        int insights = clean.indexOf("Key Insights & Alerts");
        if (insights >= 0) return (clean.substring(0, insights).trim() + "\n\n" + summary + "\n\n" + clean.substring(insights).trim())
                .replaceAll("\\n{3,}", "\n\n").trim();
        return (clean + "\n\n" + summary).replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static String createSummaryLines(Map<String, Double> spending, List<Budget> budgets, double balance) {
        Map<String, Double> safeSpending = spending == null ? Map.of() : spending;
        List<Budget> safeBudgets = budgets == null ? List.of() : budgets;
        Map<String, Double> limits = new LinkedHashMap<>();
        for (Budget budget : safeBudgets) if (budget != null) limits.put(budget.getCategory(), budget.getLimit());
        Set<String> categories = new LinkedHashSet<>(limits.keySet()); categories.addAll(safeSpending.keySet());
        if (categories.isEmpty()) return "Spending Summary\n• No spending or category budgets have been recorded this month.\n\nBalance after recent expenses: " + money(balance);

        StringBuilder summary = new StringBuilder("Spending Summary");
        double totalAmount = 0, totalBudget = 0;
        for (String category : categories) {
            double amount = safeSpending.getOrDefault(category, 0d);
            double budget = limits.getOrDefault(category, 0d);
            totalAmount += amount; totalBudget += budget;
            summary.append("\n• ").append(category).append(": ").append(money(amount)).append(" spent / ");
            if (budget > 0) summary.append(money(budget)).append(" budget — ").append(percentUsed(amount, budget)).append(" used");
            else summary.append("No budget set");
        }
        summary.append("\n• Total: ").append(money(totalAmount)).append(" spent / ");
        if (totalBudget > 0) summary.append(money(totalBudget)).append(" budget — ").append(percentUsed(totalAmount, totalBudget)).append(" used");
        else summary.append("No budget set");
        return summary.append("\n\nBalance after recent expenses: ").append(money(balance)).toString();
    }

    private static boolean containsPipeTable(String text) {
        return Arrays.stream(text.split("\\R")).filter(AIResponseFormatter::isTableLine).count() >= 2;
    }
    private static boolean isTableLine(String line) { return line.chars().filter(character -> character == '|').count() >= 2 || line.trim().matches("[-| :]+$"); }
    private static String removePipeTable(String text) { return text.lines().filter(line -> !isTableLine(line)).reduce("", (a, b) -> a + b + "\n").trim(); }
    private static String money(double amount) { return String.format("৳%,.0f", amount); }
    private static String percentUsed(double amount, double budget) { return String.format("%.0f%%", amount * 100 / budget); }

    private static String decodeUnicode(String text) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length();) {
            boolean slashEscaped = index + 5 < text.length() && text.charAt(index) == '\\' && text.charAt(index + 1) == 'u';
            boolean bareEscaped = index + 4 < text.length() && text.charAt(index) == 'u';
            int digitsAt = slashEscaped ? index + 2 : index + 1;
            if ((slashEscaped || bareEscaped) && digitsAt + 4 <= text.length()) {
                try { result.append((char) Integer.parseInt(text.substring(digitsAt, digitsAt + 4), 16)); index = digitsAt + 4; continue; }
                catch (NumberFormatException ignored) { }
            }
            result.append(text.charAt(index++));
        }
        return result.toString();
    }
}

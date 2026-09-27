package Program.service;
import Program.model.User;
import Program.utils.FileManager;
import java.util.ArrayList;
import java.util.List;

/** User-facing local data operations, kept out of the GUI. */
public class DataService {
    public void exportUserData(User user) {
        List<String> lines = new ArrayList<>();
        lines.add("EXPENSE INTELLIGENCE DATA EXPORT");
        lines.add("User: " + user.getUsername());
        lines.add("EXPENSES");
        FileManager.read("expenses.txt").stream().filter(s -> s.startsWith(user.getId() + "|", s.indexOf('|') + 1)).forEach(lines::add);
        lines.add("INCOMES");
        FileManager.read("incomes.txt").stream().filter(s -> s.startsWith(user.getId() + "|", s.indexOf('|') + 1)).forEach(lines::add);
        lines.add("BUDGETS");
        FileManager.read("budgets.txt").stream().filter(s -> s.startsWith(user.getId() + "|", s.indexOf('|') + 1)).forEach(lines::add);
        FileManager.write("reports/" + user.getId() + "_export.txt", lines);
    }
    public void clearUserData(User user) {
        for (String file : List.of("expenses.txt", "incomes.txt", "budgets.txt")) {
            FileManager.delete(file, line -> line.split("\\|", -1).length > 1 && line.split("\\|", -1)[1].equals(user.getId()));
        }
    }
}

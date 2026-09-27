package Program.service;
import Program.model.*; import Program.utils.FileManager; import java.time.LocalDate; import java.util.*; import java.util.stream.*;
public class TransactionService {
 public List<Transaction> all(String userId){List<Transaction> all=new ArrayList<>();all.addAll(loadExpenses(userId));all.addAll(loadIncomes(userId));all.sort(Comparator.comparing(Transaction::getDate).reversed());return all;}
 public List<Expense> loadExpenses(String userId){return FileManager.read("expenses.txt").stream().map(this::expense).filter(Objects::nonNull).filter(x->x.getUserId().equals(userId)).sorted(Comparator.comparing(Expense::getDate).reversed()).toList();}
 public List<Income> loadIncomes(String userId){return FileManager.read("incomes.txt").stream().map(this::income).filter(Objects::nonNull).filter(x->x.getUserId().equals(userId)).sorted(Comparator.comparing(Income::getDate).reversed()).toList();}
 protected Expense expense(String s){try{String[] p=s.split("\\|",-1);return p.length==6?new Expense(p[0],p[1],Double.parseDouble(p[2]),p[3],LocalDate.parse(p[4]),p[5]):null;}catch(Exception e){return null;}}
 protected Income income(String s){try{String[] p=s.split("\\|",-1);return p.length==6?new Income(p[0],p[1],Double.parseDouble(p[2]),p[3],LocalDate.parse(p[4]),p[5]):null;}catch(Exception e){return null;}}
 protected String record(Transaction t){return String.join("|",t.getId(),t.getUserId(),String.valueOf(t.getAmount()),FileManager.clean(t.getCategory()),t.getDate().toString(),FileManager.clean(t.getDescription()));}
 public double totalImpact(String userId,LocalDate from,LocalDate to){return all(userId).stream().filter(t->!t.getDate().isBefore(from)&&!t.getDate().isAfter(to)).mapToDouble(Transaction::calculateImpact).sum();}
 public Map<String,Double> expenseByCategory(String userId,LocalDate from,LocalDate to){return loadExpenses(userId).stream().filter(e->!e.getDate().isBefore(from)&&!e.getDate().isAfter(to)).collect(Collectors.groupingBy(Expense::getCategory,LinkedHashMap::new,Collectors.summingDouble(Expense::getAmount)));}
}

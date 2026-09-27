package Program.model;
import Program.interfaces.Reportable;
import java.time.LocalDate;
public class FinancialReport implements Reportable { private final LocalDate from,to; private final double income,expenses;
 public FinancialReport(LocalDate from,LocalDate to,double income,double expenses){this.from=from;this.to=to;this.income=income;this.expenses=expenses;} public double getIncome(){return income;} public double getExpenses(){return expenses;} public double getBalance(){return income-expenses;} public double getSavingsRate(){return income==0?0:(income-expenses)*100/income;} @Override public String generateReport(){return String.format("Report %s to %s | Income: ৳%,.2f | Expenses: ৳%,.2f | Balance: ৳%,.2f",from,to,income,expenses,getBalance());}}

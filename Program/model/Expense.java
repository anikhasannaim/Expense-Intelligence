package Program.model;
import java.time.LocalDate;
public class Expense extends Transaction { public Expense(String id,String userId,double amount,String category,LocalDate date,String description){super(id,userId,amount,category,date,description);} @Override public String getType(){return "Expense";} @Override public double calculateImpact(){return -getAmount();} }

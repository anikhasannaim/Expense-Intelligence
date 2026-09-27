package Program.model;
import java.time.LocalDate;
public class Income extends Transaction { public Income(String id,String userId,double amount,String category,LocalDate date,String description){super(id,userId,amount,category,date,description);} @Override public String getType(){return "Income";} @Override public double calculateImpact(){return getAmount();} }

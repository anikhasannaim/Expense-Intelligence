package Program.model;
import java.time.LocalDate;
/** Common base for cash movements. A List<Transaction> can contain both subclasses. */
public abstract class Transaction {
    private final String id,userId; private double amount; private String category,description; private LocalDate date;
    protected Transaction(String id,String userId,double amount,String category,LocalDate date,String description){this.id=id;this.userId=userId;setAmount(amount);this.category=category;this.date=date;this.description=description==null?"":description;}
    public String getId(){return id;} public String getUserId(){return userId;} public double getAmount(){return amount;} public void setAmount(double amount){if(amount<=0) throw new IllegalArgumentException("Amount must be positive");this.amount=amount;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public String getDescription(){return description;} public void setDescription(String v){description=v==null?"":v;}
    public abstract String getType(); public abstract double calculateImpact();
}

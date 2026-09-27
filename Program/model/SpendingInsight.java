package Program.model;
import Program.interfaces.Analyzable;
/** Local data summary that gives the analysis interface a real, reusable purpose. */
public class SpendingInsight implements Analyzable {
    private final String topCategory; private final double totalSpending;
    public SpendingInsight(String topCategory,double totalSpending){this.topCategory=topCategory;this.totalSpending=totalSpending;}
    @Override public String analyze(){return "Local spending summary: total="+totalSpending+", largest category="+topCategory+".";}
}

package Program.enums;
public enum IncomeCategory { SALARY("Salary"), FREELANCE("Freelance"), BUSINESS("Business"), INVESTMENT("Investment"), GIFT("Gift"), OTHER("Other");
    private final String label; IncomeCategory(String label){this.label=label;} public String getLabel(){return label;} @Override public String toString(){return label;}}

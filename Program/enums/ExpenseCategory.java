package Program.enums;
public enum ExpenseCategory { FOOD("Food"), SHOPPING("Shopping"), BILLS("Bills"), TRANSPORT("Transport"), ENTERTAINMENT("Entertainment"), OTHER("Other");
    private final String label; ExpenseCategory(String label){this.label=label;} public String getLabel(){return label;} @Override public String toString(){return label;}}

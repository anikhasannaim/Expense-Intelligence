package Program.utils;
public final class Validator { private Validator(){} public static boolean blank(String s){return s==null||s.trim().isEmpty();} public static double positiveAmount(String s){try{double v=Double.parseDouble(s.trim());if(v<=0)throw new NumberFormatException();return v;}catch(Exception e){throw new IllegalArgumentException("Enter a valid positive amount.");}} }

package miniJava.SyntacticAnalyzer;

public enum TokenKind {

   // Grouping & Separation
   OPEN_PAREN("("),
   CLOSE_PAREN(")"),
   OPEN_BRACKET("["),
   CLOSE_BRACKET("]"),
   OPEN_CURLY("{"),
   CLOSE_CURLY("}"),
   SEMICOLON(";"),
   COMMA(","),
   EOF(Character.toString(CharUtil.EOF)),

   // Operators
   // -- Relational, Logical, & Assignment
   GREATER_THAN(">"),
   GT_EQUAL(">="),
   LESS_THAN("<"),
   LT_EQUAL("<="),
   ASSIGN("="),
   EQUALITY("=="),
   NOT("!"),
   NOT_EQUAL("!="),
   AND("&&"),
   OR("||"),
   // -- Arithmetic
   ADD("+"),
   SUB("-"),
   MULTI("*"),
   DIVIDE("/"),
   // -- Other
   DOT("."),

   // Keywords
   CLASS("class"),
   RETURN("return"),
   PUBLIC("public"),
   PRIVATE("private"),
   STATIC("static"),
   INT("int"),
   BOOLEAN("boolean"),
   VOID("void"),
   THIS("this"),
   IF("if"),
   ELSE("else"),
   WHILE("while"),
   TRUE("true"),
   FALSE("false"),
   NEW("new"),

   // Other
   IDENTIFIER("<identifier>"),
   NUMBER("<number>"),

   // Pseudo-Kinds
   ERROR("<error>") // Indicates scanning error.

   ;

   public final String value;

   private TokenKind(String tokenVal) {
      this.value = tokenVal;
   }

   public boolean isClassDeclarationStarter() {
      return (this == CLASS);
   }

   public boolean isDeclarationStarter() {
      boolean output = false;
      output |= (this == PUBLIC);
      output |= (this == PRIVATE);
      output |= (this == STATIC);
      output |= isTypeStarter();
      return output;
   }

   public boolean isTypeStarter() {
      return isPrimitiveTypeStarter() || isReferenceTypeStarter();
   }

   public boolean isPrimitiveTypeStarter() {
      boolean output = false;
      output |= (this == INT);
      output |= (this == BOOLEAN);
      output |= (this == VOID);
      return output;
   }

   public boolean isArgsStarter() {
      return (this == OPEN_PAREN);
   }

   public boolean isReferenceTypeStarter() {
      return (this == IDENTIFIER);
   }

   public boolean isReferenceStarter() {
      boolean output = false;
      output |= (this == IDENTIFIER);
      output |= (this == THIS);
      return output;
   }

   public boolean isReferenceTailStarter() {
      return (this == DOT);
   }

   public boolean isRefSegmentStarter() {
      return (this == IDENTIFIER);
   }

   public boolean isStatementStarter() {
      boolean output = false;
      output |= (this == OPEN_CURLY);
      output |= (this.isReferenceStatementStarter());
      output |= (this.isPrimitiveTypeStarter());
      output |= (this == IF);
      output |= (this == WHILE);
      return output;
   }

   public boolean isReferenceStatementStarter() {
      boolean output = false;
      output |= (this == IDENTIFIER);
      output |= (this == THIS);
      return output;
   }

   public boolean isReferenceStatementTailStarter() {
      boolean output = false;
      output |= (this == ASSIGN);
      output |= (this.isArgsStarter());
      return output;
   }

   public boolean isBinaryOperator() {
      boolean output = false;
      output |= (this == GREATER_THAN);
      output |= (this == GT_EQUAL);
      output |= (this == LESS_THAN);
      output |= (this == LT_EQUAL);
      output |= (this == EQUALITY);
      output |= (this == NOT_EQUAL);
      output |= (this == AND);
      output |= (this == OR);
      output |= (this == ADD);
      output |= (this == SUB);
      output |= (this == MULTI);
      output |= (this == DIVIDE);
      return output;
   }

   public boolean isDisjunctionOperator() {
      return (this == OR);
   }

   public boolean isConjunctionOperator() {
      return (this == AND);
   }

   public boolean isEqualityOperator() {
      boolean output = false;
      output |= (this == EQUALITY);
      output |= (this == NOT_EQUAL);
      return output;
   }

   public boolean isRelationalOperator() {
      boolean output = false;
      output |= (this == GREATER_THAN);
      output |= (this == GT_EQUAL);
      output |= (this == LESS_THAN);
      output |= (this == LT_EQUAL);
      return output;
   }

   public boolean isAdditiveOperator() {
      boolean output = false;
      output |= (this == ADD);
      output |= (this == SUB);
      return output;
   }

   public boolean isMultiplicativeOperator() {
      boolean output = false;
      output |= (this == MULTI);
      output |= (this == DIVIDE);
      return output;
   }

   public boolean isUnaryOperator() {
      boolean output = false;
      output |= (this == SUB);
      output |= (this == NOT);
      return output;
   }

}

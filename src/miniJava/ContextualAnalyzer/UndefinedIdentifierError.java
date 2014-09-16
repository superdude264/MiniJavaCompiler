package miniJava.ContextualAnalyzer;

public class UndefinedIdentifierError extends IdentifierError {

   private static final long serialVersionUID = 1L;

   public UndefinedIdentifierError() {
      super();
   }

   public UndefinedIdentifierError(String message) {
      super(message);
   }

}

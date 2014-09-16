package miniJava.ContextualAnalyzer;

public class IdentifierError extends RuntimeException {

   private static final long serialVersionUID = 1L;

   public IdentifierError() {
      super("");
   }

   public IdentifierError(String message) {
      super(message);
   }

}

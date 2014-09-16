package miniJava;

public class Logger {

   private static final String ERROR_FMT = "ERROR: %s";

   private int                 numErrors;

   Logger() {
      numErrors = 0;
   }

   public boolean hasErrors() {
      return numErrors > 0;
   }

   public void error(String message) {
      recordError(ERROR_FMT, message);
   }

   public void contextError(String message) {
      recordError("*** %s", message);
   }

   private void recordError(String formatStr, String message) {
      System.out.println(String.format(formatStr, message));
      numErrors++;
   }

}

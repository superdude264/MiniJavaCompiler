package miniJava.SyntacticAnalyzer;

import java.util.Arrays;
import java.util.List;

public class CharUtil {

   // Whitespace
   public static final char            SPACE            = ' ';
   public static final char            TAB              = '\t';
   public static final char            EOL              = '\n';
   public static final char            RET              = '\r';

   // End of text
   public static final char            EOF              = (char) -1;

   public static final List<Character> WHITESPACE_CHARS = Arrays.asList(SPACE, TAB, EOL, RET);

   public static boolean isWhitespaceChar(char c) {
      return WHITESPACE_CHARS.contains(c);
   }

   public static boolean isLetter(char c) {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
   }

   public static boolean isDigit(char c) {
      return (c >= '0' && c <= '9');
   }

   private CharUtil() {
      // Do nothing.
   }
}

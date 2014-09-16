package miniJava.SyntacticAnalyzer;

import static miniJava.SyntacticAnalyzer.TokenKind.*;
import miniJava.Logger;

public class Scanner {

   private SourceFile    source;
   private Logger        logger;
   private char          currentChar;
   private StringBuilder currentToken;
   private boolean       isScanningToken;

   public Scanner(SourceFile source, Logger errorReporter) {
      this.source = source;
      this.logger = errorReporter;

      // Read the first char.
      currentChar = source.readChar();
   }

   public Token scan() throws SyntaxError {
      // Separators or division.
      isScanningToken = false;
      while (currentChar == '/' || CharUtil.isWhitespaceChar(currentChar)) {
         if (currentChar == '/') {
            takeIt();
            // Start of a comment.
            if (currentChar == '/' || currentChar == '*') {
               scanComments();
            }
            // Division.
            else {
               SourcePosition pos = new SourcePosition(source.getCurrentLine(), source.getCurrentLine());
               return new Token(DIVIDE, "/", pos);
            }
         }
         else {
            scanWhitespace();
         }
      }

      // Tokens
      isScanningToken = true;
      currentToken = new StringBuilder();
      SourcePosition pos = new SourcePosition();
      pos.start = source.getCurrentLine();

      // Scan for tokens.
      TokenKind kind = scanToken();
      String tokenVal = currentToken.toString();
      pos.finish = source.getCurrentLine();

      // Map identifiers to their respective tokens.
      if (kind == IDENTIFIER) {
         kind = selectIdentifierTokenKind(tokenVal);
      }

      Token token = new Token(kind, tokenVal, pos);
      return token;
   }

   /**
    * Scans whitespace, throwing it away.
    */
   private void scanWhitespace() {
      while (CharUtil.isWhitespaceChar(currentChar)) {
         takeIt();
      }
   }

   /**
    * Scans through comments, ignoring their content.
    * 
    * @throws SyntaxError
    */
   private void scanComments() throws SyntaxError {
      // Single line comment.
      if (currentChar == '/') {
         takeIt();
         while (currentChar != CharUtil.RET && currentChar != CharUtil.EOL && currentChar != CharUtil.EOF) {
            takeIt();
         }
      }

      // Multi-line comment.
      else if (currentChar == '*') {
         takeIt();
         while (true) {
            if (currentChar == '*') {
               takeIt();
               if (currentChar == '/') {
                  takeIt();
                  break;
               }
            }
            else if (currentChar == CharUtil.EOF) {
               scanError("Reached end of file without seeing close to multi-line comment ('*/').");
            }
            else {
               takeIt();
            }
         }
      }

      // End of file.
      else if (currentChar == CharUtil.EOF) {
         scanError("Reached end of file with trailing slash ('/').");
      }

      // Take care of endings that may occur after comments.
      if (currentChar == CharUtil.RET) {
         takeIt();
      }
      if (currentChar == CharUtil.EOL) {
         takeIt();
      }
      if (currentChar == CharUtil.EOF) {
         takeIt();
      }
   }

   private TokenKind scanToken() throws SyntaxError {
      switch (currentChar) {
         case '(':
            takeIt();
            return OPEN_PAREN;

         case ')':
            takeIt();
            return CLOSE_PAREN;

         case '[':
            takeIt();
            return OPEN_BRACKET;

         case ']':
            takeIt();
            return CLOSE_BRACKET;

         case '{':
            takeIt();
            return OPEN_CURLY;

         case '}':
            takeIt();
            return CLOSE_CURLY;

         case ';':
            takeIt();
            return SEMICOLON;

         case ',':
            takeIt();
            return COMMA;

         case CharUtil.EOF:
            takeIt();
            return EOF;

         case '>':
            takeIt();

            // case '>=':
            if (currentChar == '=') {
               takeIt();
               return GT_EQUAL;
            }

            return GREATER_THAN;

         case '<':
            takeIt();

            // case '<=':
            if (currentChar == '=') {
               takeIt();
               return LT_EQUAL;
            }

            return LESS_THAN;

         case '=':
            takeIt();

            // case '==':
            if (currentChar == '=') {
               takeIt();
               return EQUALITY;
            }

            return ASSIGN;

         case '!':
            takeIt();

            // case '!=':
            if (currentChar == '=') {
               takeIt();
               return NOT_EQUAL;
            }

            return NOT;

         case '&':
            takeIt();
            if (currentChar == '&') {
               takeIt();
               return AND;
            }
            else {
               scanError("Found individual character '&' on line " + source.getCurrentLine() + ".");
               return ERROR; // Unreachable.
            }

         case '|':
            takeIt();
            if (currentChar == '|') {
               takeIt();
               return OR;
            }
            else {
               scanError("Found individual character '|' on line " + source.getCurrentLine() + ".");
               return ERROR; // Unreachable.
            }

         case '+':
            takeIt();
            return ADD;

         case '-':
            takeIt();
            if (currentChar == '-') {
               scanError("Found invalid operator '--' on line " + source.getCurrentLine() + ".");
               return ERROR; // Unreachable.
            }
            return SUB;

         case '*':
            takeIt();
            return MULTI;

            // case '/': Handled elsewhere.

         case '.':
            takeIt();
            return DOT;

            //@formatter:off
         case 'a':  case 'b':  case 'c':  case 'd':  case 'e':
         case 'f':  case 'g':  case 'h':  case 'i':  case 'j':
         case 'k':  case 'l':  case 'm':  case 'n':  case 'o':
         case 'p':  case 'q':  case 'r':  case 's':  case 't':
         case 'u':  case 'v':  case 'w':  case 'x':  case 'y':
         case 'z':
         case 'A':  case 'B':  case 'C':  case 'D':  case 'E':
         case 'F':  case 'G':  case 'H':  case 'I':  case 'J':
         case 'K':  case 'L':  case 'M':  case 'N':  case 'O':
         case 'P':  case 'Q':  case 'R':  case 'S':  case 'T':
         case 'U':  case 'V':  case 'W':  case 'X':  case 'Y':
         case 'Z': //@formatter:on
            takeIt();
            while (CharUtil.isLetter(currentChar) || CharUtil.isDigit(currentChar) || currentChar == '_') {
               takeIt();
            }
            return IDENTIFIER;

            //@formatter:off
         case '0':  case '1':  case '2':  case '3':  case '4':
         case '5':  case '6':  case '7':  case '8':  case '9': //@formatter:on
            takeIt();
            while (CharUtil.isDigit(currentChar)) {
               takeIt();
            }
            return NUMBER;

         default:
            scanError("Could not match token starting with '" + currentChar + "' on line " + source.getCurrentLine() + ".");
            return ERROR;
      }
   }

   /**
    * Map identifiers that are keywords to their respective token kinds based on the scanned value.
    * 
    * @param tokenVal
    * @return
    */
   private TokenKind selectIdentifierTokenKind(String tokenVal) {
      // If the identifier isn't a keyword, leave it as an identifier.
      TokenKind outputKind = IDENTIFIER;

      if (CLASS.value.equals(tokenVal)) {
         outputKind = CLASS;
      }
      else if (RETURN.value.equals(tokenVal)) {
         outputKind = RETURN;
      }
      else if (PUBLIC.value.equals(tokenVal)) {
         outputKind = PUBLIC;
      }
      else if (PRIVATE.value.equals(tokenVal)) {
         outputKind = PRIVATE;
      }
      else if (STATIC.value.equals(tokenVal)) {
         outputKind = STATIC;
      }
      else if (INT.value.equals(tokenVal)) {
         outputKind = INT;
      }
      else if (BOOLEAN.value.equals(tokenVal)) {
         outputKind = BOOLEAN;
      }
      else if (VOID.value.equals(tokenVal)) {
         outputKind = VOID;
      }
      else if (THIS.value.equals(tokenVal)) {
         outputKind = THIS;
      }
      else if (IF.value.equals(tokenVal)) {
         outputKind = IF;
      }
      else if (ELSE.value.equals(tokenVal)) {
         outputKind = ELSE;
      }
      else if (WHILE.value.equals(tokenVal)) {
         outputKind = WHILE;
      }
      else if (TRUE.value.equals(tokenVal)) {
         outputKind = TRUE;
      }
      else if (FALSE.value.equals(tokenVal)) {
         outputKind = FALSE;
      }
      else if (NEW.value.equals(tokenVal)) {
         outputKind = NEW;
      }

      return outputKind;
   }

   private void takeIt() {
      if (isScanningToken) {
         currentToken.append(currentChar);
      }
      currentChar = source.readChar();
   }

   private void scanError(String msg) throws SyntaxError {
      logger.error("SCANNER: " + msg);
      throw new SyntaxError();
   }

}

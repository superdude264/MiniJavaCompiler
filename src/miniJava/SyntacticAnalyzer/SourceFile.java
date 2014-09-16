package miniJava.SyntacticAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * An input wrapper to keep track of line numbers.
 */
public class SourceFile {

   private File            sourceFile;
   private FileInputStream iStream;
   private int             currentLine;

   public SourceFile(String filename) throws FileNotFoundException {
      sourceFile = new File(filename);
      iStream = new FileInputStream(sourceFile);
      currentLine = 1;
   }

   /**
    * Reads a char from the source file.
    * 
    * @return
    */
   char readChar() {
      try {
         int c = iStream.read();

         if (c == -1) {
            c = CharUtil.EOF;
         }
         else if (c == CharUtil.EOL) {
            currentLine++;
         }
         return (char) c;
      }
      catch (IOException s) {
         return CharUtil.EOF;
      }
   }

   /**
    * Gets the current line number being read from the source file.
    * 
    * @return
    */
   int getCurrentLine() {
      return currentLine;
   }
}

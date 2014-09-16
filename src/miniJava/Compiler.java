package miniJava;

import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.Encoder;
import miniJava.ContextualAnalyzer.IdentificationChecker;
import miniJava.ContextualAnalyzer.TypeChecker;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;

@SuppressWarnings("unused")
public class Compiler {

   public static void main(String[] args) {
      try {
         if (args.length != 1) {
            System.err.println("Must input a name of '.java' or '.mjava' file for compilation.");
            System.exit(4);
         }

         String sourceName = args[0];

         // Check file extension.
         String extension = getExtension(sourceName);
         if (!(extension.equals("java") || extension.equals("mjava"))) {
            System.err.println("File must hava extension of '.java' or '.mjava'.");
            System.exit(4);
         }

         // Generate source & object file handles.
         SourceFile sourceFile = new SourceFile(sourceName);
         String objName = removeExtension(sourceName) + ".mJAM";
         ObjectFile objectFile = new ObjectFile(objName);

         boolean hasErrors = compileProgram(sourceFile, objectFile);
         if (hasErrors) {
            System.exit(4);
         }

         System.exit(0);

      }
      catch (Exception e) {
         System.err.println(e.getMessage());
         System.exit(4);
      }

   }

   public static String getExtension(String fileName) {
      String extension = "";

      int i = fileName.lastIndexOf('.');
      int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

      if (i > p) {
         extension = fileName.substring(i + 1);
      }

      return extension;
   }

   private static String removeExtension(String fileName) {
      int extension = fileName.lastIndexOf('.');
      if (extension >= 0) {
         return fileName.substring(0, extension);
      }
      return fileName;
   }

   /**
    * Compiles the program, returning true if the source file has errors and false otherwise.
    * 
    * @param source
    * @param objectFile
    * @return
    */
   private static boolean compileProgram(SourceFile source, ObjectFile objectFile) {
      Logger logger = new Logger();

      // Generate the AST.
      Scanner scanner = new Scanner(source, logger);
      Parser parser = new Parser(scanner, logger);
      AST ast = parser.parse();
      if (logger.hasErrors()) {
         return true;
      }

      // Contextual analysis - Identification.
      IdentificationChecker idChecker = new IdentificationChecker(logger);
      idChecker.check(ast);
      if (logger.hasErrors()) {
         return true;
      }

      // Contextual analysis - Type checking.
      TypeChecker typeChecker = new TypeChecker(logger);
      typeChecker.check(ast);
      if (logger.hasErrors()) {
         return true;
      }

      // Code generation.
      Encoder encoder = new Encoder(logger);
      encoder.encode(ast);
      if (logger.hasErrors()) {
         return true;
      }
      objectFile.write();

      // Show the AST.
      // ASTDisplay displayVisitor = new ASTDisplay();
      // displayVisitor.showTree(ast);

      return false;
   }

}

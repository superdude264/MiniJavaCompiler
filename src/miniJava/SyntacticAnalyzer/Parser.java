package miniJava.SyntacticAnalyzer;

import static miniJava.SyntacticAnalyzer.TokenKind.*;
import miniJava.Logger;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {

   private Scanner scanner;
   private Logger  logger;
   private Token   currentToken;

   public Parser(Scanner scanner, Logger errorReporter) {
      this.scanner = scanner;
      this.logger = errorReporter;
   }

   public Package parse() {
      Package pkg = null;

      try {
         currentToken = scanner.scan();
         pkg = parseProgram();
      }
      catch (SyntaxError e) {
         // Do nothing.
      }

      return pkg;
   }

   private Package parseProgram() throws SyntaxError {
      int start = currentToken.position.start;

      ClassDeclList classDecls = new ClassDeclList();
      while (currentToken.kind.isClassDeclarationStarter()) {
         ClassDecl cd = parseClassDeclaration();
         classDecls.add(cd);
      }

      accept(EOF);

      Package pkg = new Package(classDecls, getSourcePosition(start));
      return pkg;
   }

   private ClassDecl parseClassDeclaration() throws SyntaxError {
      int start = currentToken.position.start;
      accept(CLASS);

      String name = currentToken.spelling;
      accept(IDENTIFIER);

      accept(OPEN_CURLY);

      FieldDeclList fields = new FieldDeclList();
      MethodDeclList methods = new MethodDeclList();
      int fieldIndex = 0;
      while (currentToken.kind.isDeclarationStarter()) {
         MemberDecl member = parseMemberDeclaration();
         if (member instanceof FieldDecl) {
            member.index = fieldIndex;
            fields.add((FieldDecl) member);
            fieldIndex += 1;
         }
         else if (member instanceof MethodDecl) {
            methods.add((MethodDecl) member);
         }
         else {
            parseError("Could not identify member declaration type at " + member.posn + ".");
         }

      }

      accept(CLOSE_CURLY);

      ClassDecl cd = new ClassDecl(name, fields, methods, getSourcePosition(start));
      return cd;
   }

   private MemberDecl parseMemberDeclaration() throws SyntaxError {
      int start = currentToken.position.start;
      boolean isPrivate = false;
      boolean isStatic = false;

      if (currentToken.kind == PUBLIC) {
         isPrivate = false;
         acceptIt();
      }
      else if (currentToken.kind == PRIVATE) {
         isPrivate = true;
         acceptIt();
      }

      if (currentToken.kind == STATIC) {
         isStatic = true;
         acceptIt();
      }

      Type type = parseType();

      String name = currentToken.spelling;
      accept(IDENTIFIER);

      // Wrap the first part of the declaration.
      FieldDecl fd = new FieldDecl(isPrivate, isStatic, type, name, getSourcePosition(start));

      // Field declaration.
      if (currentToken.kind == SEMICOLON) {
         acceptIt();
         return fd;
      }

      // Method declaration.
      else if (currentToken.kind == OPEN_PAREN) {
         acceptIt();

         ParameterDeclList params = new ParameterDeclList();
         if (currentToken.kind != CLOSE_PAREN) {
            params = parseParameterList();
         }
         accept(CLOSE_PAREN);
         accept(OPEN_CURLY);

         StatementList statements = new StatementList();
         while (currentToken.kind.isStatementStarter()) {
            Statement s = parseStatement();
            statements.add(s);
         }

         Expression returnExp = null;
         if (currentToken.kind == RETURN) {
            acceptIt();
            returnExp = parseExpression();
            accept(SEMICOLON);
         }

         accept(CLOSE_CURLY);

         MethodDecl md = new MethodDecl(fd, params, statements, returnExp, getSourcePosition(start));
         return md;
      }

      // Error.
      else {
         parseError("Could not parse declaration at token " + currentToken.errorString() + ".");
         return null; // Unreachable.
      }
   }

   private Type parseType() throws SyntaxError {
      Type type = null;

      if (currentToken.kind.isPrimitiveTypeStarter()) {
         type = parsePrimitiveType();
      }
      else if (currentToken.kind.isReferenceTypeStarter()) {
         type = parseReferenceType();
      }
      else {
         parseError("Could not parse type at token " + currentToken.errorString() + ".");
      }

      return type;
   }

   private Type parsePrimitiveType() throws SyntaxError {
      int start = currentToken.position.start;
      Type type = null;

      if (currentToken.kind == INT) {
         // Int type.
         acceptIt();
         type = new BaseType(TypeKind.INT, getSourcePosition(start));

         // Int array.
         if (currentToken.kind == OPEN_BRACKET) {
            acceptIt();
            accept(CLOSE_BRACKET);
            type = new ArrayType(type, getSourcePosition(start));
         }
      }

      // Boolean.
      else if (currentToken.kind == BOOLEAN) {
         acceptIt();
         type = new BaseType(TypeKind.BOOLEAN, getSourcePosition(start));
      }

      // Void.
      else if (currentToken.kind == VOID) {
         acceptIt();
         type = new BaseType(TypeKind.VOID, getSourcePosition(start));
      }

      return type;
   }

   private Type parseReferenceType() throws SyntaxError {
      int start = currentToken.position.start;
      Type type = null;

      if (currentToken.kind == IDENTIFIER) {
         // Reference/Class type.
         String idStr = currentToken.spelling;
         Identifier id = new Identifier(idStr, currentToken.position);
         acceptIt();
         type = new ClassType(id, getSourcePosition(start));

         // Reference/Class array.
         if (currentToken.kind == OPEN_BRACKET) {
            acceptIt();
            accept(CLOSE_BRACKET);
            type = new ArrayType(type, getSourcePosition(start));
         }
      }

      return type;
   }

   private ParameterDeclList parseParameterList() throws SyntaxError {
      ParameterDeclList params = new ParameterDeclList();
      do {
         if (currentToken.kind == COMMA) {
            acceptIt();
         }

         int start = currentToken.position.start;
         Type type = parseType();
         String name = currentToken.spelling;

         accept(IDENTIFIER);

         ParameterDecl param = new ParameterDecl(type, name, getSourcePosition(start));
         params.add(param);
      }
      while (currentToken.kind == COMMA);

      return params;
   }

   private ExprList parseArgs() throws SyntaxError {
      ExprList args = new ExprList();
      accept(OPEN_PAREN);
      if (currentToken.kind != CLOSE_PAREN) {
         args = parseArgumentList();
      }
      accept(CLOSE_PAREN);
      return args;
   }

   private ExprList parseArgumentList() throws SyntaxError {
      ExprList expressions = new ExprList();
      do {
         if (currentToken.kind == COMMA) {
            acceptIt();
         }

         Expression exp = parseExpression();
         expressions.add(exp);
      }
      while (currentToken.kind == COMMA);

      return expressions;
   }

   private Reference parseReference() throws SyntaxError {
      Reference ref = parseBaseRef();
      ref = parseReferenceTail(ref);
      return ref;
   }

   private Reference parseBaseRef() throws SyntaxError {
      if (currentToken.kind == THIS) {
         ThisRef tr = new ThisRef(currentToken.position);
         acceptIt();
         return tr;
      }
      else if (currentToken.kind.isRefSegmentStarter()) {
         Reference r = parseRefSegment();
         return r;
      }
      else {
         parseError("Could not parse base reference at token " + currentToken.errorString() + ".");
         return null; // Unreachable.
      }
   }

   private Reference parseReferenceTail(Reference ref) throws SyntaxError {
      int start = currentToken.position.start;
      while (currentToken.kind == DOT) {
         acceptIt();

         // Qualified reference.
         Identifier id = new Identifier(currentToken.spelling, currentToken.position);
         accept(IDENTIFIER);
         QualifiedRef qr = new QualifiedRef(ref, id, getSourcePosition(start));
         ref = qr;

         // Indexed reference.
         if (currentToken.kind == OPEN_BRACKET) {
            acceptIt();
            Expression e = parseExpression();
            accept(CLOSE_BRACKET);

            IndexedRef ir = new IndexedRef(ref, e, getSourcePosition(start));
            ref = ir;
         }
      }

      return ref;
   }

   private Reference parseRefSegment() throws SyntaxError {
      int start = currentToken.position.start;

      Identifier id = new Identifier(currentToken.spelling, currentToken.position);
      accept(IDENTIFIER);
      IdRef idRef = new IdRef(id, getSourcePosition(start));

      // IndexedRef
      if (currentToken.kind == OPEN_BRACKET) {
         acceptIt();
         Expression e = parseExpression();
         accept(CLOSE_BRACKET);

         IndexedRef ir = new IndexedRef(idRef, e, getSourcePosition(start));
         return ir;
      }

      // IdRef
      else {
         return idRef;
      }
   }

   private Statement parseStatement() throws SyntaxError {
      int start = currentToken.position.start;

      // Block statement.
      if (currentToken.kind == OPEN_CURLY) {
         acceptIt();
         StatementList statements = new StatementList();
         while (currentToken.kind != CLOSE_CURLY) {
            Statement s = parseStatement();
            statements.add(s);
         }
         accept(CLOSE_CURLY);

         BlockStmt bs = new BlockStmt(statements, getSourcePosition(start));
         return bs;
      }

      // Call, Assign, and Reference variable declaration statements.
      else if (currentToken.kind.isReferenceStarter()) {
         Statement s = parseReferenceStatement();
         return s;
      }

      // Primitive variable declaration statement.
      else if (currentToken.kind.isPrimitiveTypeStarter()) {
         Type type = parsePrimitiveType();

         String name = currentToken.spelling;
         accept(IDENTIFIER);

         VarDecl vd = new VarDecl(type, name, getSourcePosition(start));
         accept(ASSIGN);

         Expression exp = parseExpression();
         accept(SEMICOLON);

         VarDeclStmt vds = new VarDeclStmt(vd, exp, getSourcePosition(start));
         return vds;
      }

      // If statement.
      else if (currentToken.kind == IF) {
         acceptIt();
         accept(OPEN_PAREN);
         Expression exp = parseExpression();
         accept(CLOSE_PAREN);
         Statement thenStmt = parseStatement();

         Statement elseStmt = null;
         if (currentToken.kind == ELSE) {
            acceptIt();
            elseStmt = parseStatement();
         }

         IfStmt ifst = new IfStmt(exp, thenStmt, elseStmt, getSourcePosition(start));
         return ifst;
      }

      // While statement.
      else if (currentToken.kind == WHILE) {
         acceptIt();
         accept(OPEN_PAREN);
         Expression exp = parseExpression();
         accept(CLOSE_PAREN);
         Statement stmt = parseStatement();

         WhileStmt whileStmt = new WhileStmt(exp, stmt, getSourcePosition(start));
         return whileStmt;
      }

      // Error.
      else {
         parseError("Could not parse statement at token " + currentToken.errorString() + ".");
         return null; // Unreachable.
      }
   }

   private Statement parseReferenceStatement() throws SyntaxError {
      int start = currentToken.position.start;

      // Call or assignment on 'this' (this ReferenceTail ReferenceStatementTail).
      if (currentToken.kind == THIS) {
         Reference ref = new ThisRef(currentToken.position);
         acceptIt();
         ref = parseReferenceTail(ref);
         Statement s = parseReferenceStatementTail(ref);
         return s;
      }

      else if (currentToken.kind == IDENTIFIER) {
         String idStr = currentToken.spelling;
         Identifier id = new Identifier(idStr, currentToken.position);
         acceptIt();

         // Object variable declaration (id id = Expression).
         if (currentToken.kind == IDENTIFIER) {
            Type type = new ClassType(id, getSourcePosition(start));
            String name = currentToken.spelling;
            acceptIt();
            accept(ASSIGN);
            Expression e = parseExpression();
            accept(SEMICOLON);

            VarDecl vd = new VarDecl(type, name, getSourcePosition(start));
            VarDeclStmt vds = new VarDeclStmt(vd, e, getSourcePosition(start));
            return vds;
         }

         // Object variable assignment or a procedure call (id ReferenceStatementTail).
         else if (currentToken.kind.isReferenceTailStarter() || currentToken.kind.isReferenceStatementTailStarter()) {
            Reference ref = new IdRef(id, getSourcePosition(start));
            ref = parseReferenceTail(ref);
            Statement s = parseReferenceStatementTail(ref);
            return s;
         }

         else if (currentToken.kind == OPEN_BRACKET) {
            acceptIt();

            // Object-array variable declaration (id[] id = Expression).
            if (currentToken.kind == CLOSE_BRACKET) {
               acceptIt();
               Type elementType = new ClassType(id, getSourcePosition(start));
               Type type = new ArrayType(elementType, getSourcePosition(start));

               String arrayVarId = currentToken.spelling;
               accept(IDENTIFIER);

               accept(ASSIGN);
               Expression e = parseExpression();
               accept(SEMICOLON);

               VarDecl vd = new VarDecl(type, arrayVarId, getSourcePosition(start));
               VarDeclStmt vds = new VarDeclStmt(vd, e, getSourcePosition(start));
               return vds;
            }

            // Object-array variable assignment or a procedure call (id[Expression] ReferenceStatementTail).
            else {
               Expression e = parseExpression();
               accept(CLOSE_BRACKET);
               Reference idRef = new IdRef(id, getSourcePosition(start));
               Reference ref = new IndexedRef(idRef, e, getSourcePosition(start));
               ref = parseReferenceTail(ref);
               Statement s = parseReferenceStatementTail(ref);
               return s;
            }
         }

         // Error.
         else {
            parseError("Could not parse reference statement at token " + currentToken.errorString() + ".");
            return null; // Unreachable.
         }
      }

      // Error.
      else {
         parseError("Could not parse reference statement at token " + currentToken.errorString() + ".");
         return null; // Unreachable.
      }
   }

   private Statement parseReferenceStatementTail(Reference ref) throws SyntaxError {
      int start = currentToken.position.start;

      // Variable assignment.
      if (currentToken.kind == ASSIGN) {
         acceptIt();
         Expression e = parseExpression();
         accept(SEMICOLON);

         AssignStmt as = new AssignStmt(ref, e, getSourcePosition(start));
         return as;
      }

      // Procedure call.
      else if (currentToken.kind.isArgsStarter()) {
         ExprList args = parseArgs();
         accept(SEMICOLON);

         CallStmt cs = new CallStmt(ref, args, getSourcePosition(start));
         return cs;
      }

      // Error.
      else {
         parseError("Could not parse reference statement tail at token " + currentToken.errorString() + ".");
         return null; // Unreachable.
      }
   }

   private Expression parseExpression() throws SyntaxError {
      Expression exp = parseDisjunctionExpression();
      return exp;
   }

   private Expression parseDisjunctionExpression() throws SyntaxError {
      int start = currentToken.position.start;
      Expression e1 = parseConjunctionExpression();

      while (currentToken.kind.isDisjunctionOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();
         Expression e2 = parseConjunctionExpression();
         e1 = new BinaryExpr(op, e1, e2, getSourcePosition(start));
      }

      return e1;
   }

   private Expression parseConjunctionExpression() throws SyntaxError {
      int start = currentToken.position.start;
      Expression e1 = parseEqualityExpression();

      while (currentToken.kind.isConjunctionOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();
         Expression e2 = parseEqualityExpression();
         e1 = new BinaryExpr(op, e1, e2, getSourcePosition(start));
      }

      return e1;
   }

   private Expression parseEqualityExpression() throws SyntaxError {
      int start = currentToken.position.start;
      Expression e1 = parseRelationalExpression();

      while (currentToken.kind.isEqualityOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();
         Expression e2 = parseRelationalExpression();
         e1 = new BinaryExpr(op, e1, e2, getSourcePosition(start));
      }

      return e1;
   }

   private Expression parseRelationalExpression() throws SyntaxError {
      int start = currentToken.position.start;
      Expression e1 = parseAdditiveExpression();

      while (currentToken.kind.isRelationalOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();
         Expression e2 = parseAdditiveExpression();
         e1 = new BinaryExpr(op, e1, e2, getSourcePosition(start));
      }

      return e1;
   }

   private Expression parseAdditiveExpression() throws SyntaxError {
      int start = currentToken.position.start;
      Expression e1 = parseMultiplicativeExpression();

      while (currentToken.kind.isAdditiveOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();
         Expression e2 = parseMultiplicativeExpression();
         e1 = new BinaryExpr(op, e1, e2, getSourcePosition(start));
      }

      return e1;
   }

   private Expression parseMultiplicativeExpression() throws SyntaxError {
      int start = currentToken.position.start;
      Expression e1 = parseTerminalExpression();

      while (currentToken.kind.isMultiplicativeOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();
         Expression e2 = parseTerminalExpression();
         e1 = new BinaryExpr(op, e1, e2, getSourcePosition(start));
      }

      return e1;
   }

   private Expression parseTerminalExpression() throws SyntaxError {
      int start = currentToken.position.start;

      if (currentToken.kind.isReferenceStarter()) {
         Reference ref = parseReference();

         // Call expression.
         if (currentToken.kind.isArgsStarter()) {
            ExprList args = parseArgs();
            CallExpr ce = new CallExpr(ref, args, getSourcePosition(start));
            return ce;
         }

         // Reference expression.
         else {
            RefExpr re = new RefExpr(ref, getSourcePosition(start));
            return re;
         }
      }

      // Unary expression.
      else if (currentToken.kind.isUnaryOperator()) {
         Operator op = new Operator(currentToken, currentToken.position);
         acceptIt();

         Expression e = parseTerminalExpression();

         UnaryExpr ue = new UnaryExpr(op, e, getSourcePosition(start));
         return ue;
      }

      // Parenthesized expression.
      else if (currentToken.kind == OPEN_PAREN) {
         acceptIt();
         Expression e = parseExpression();
         accept(CLOSE_PAREN);
         return e;
      }

      // 'new' expression.
      else if (currentToken.kind == NEW) {
         acceptIt();
         if (currentToken.kind == IDENTIFIER) {
            Identifier id = new Identifier(currentToken.spelling, currentToken.position);
            ClassType classType = new ClassType(id, id.posn);
            acceptIt();

            // new object.
            if (currentToken.kind == OPEN_PAREN) {
               acceptIt();
               accept(CLOSE_PAREN);

               NewObjectExpr noe = new NewObjectExpr(classType, getSourcePosition(start));
               return noe;
            }

            // new object array.
            else if (currentToken.kind == OPEN_BRACKET) {
               acceptIt();
               Expression exp = parseExpression();
               accept(CLOSE_BRACKET);

               Type type = new ArrayType(classType, getSourcePosition(start));
               NewArrayExpr nae = new NewArrayExpr(type, exp, getSourcePosition(start));
               return nae;
            }

            // Error.
            else {
               parseError("Could not parse new reference expression at token " + currentToken.errorString() + ".");
               return null; // Unreachable.
            }
         }

         // new int array.
         else if (currentToken.kind == INT) {
            acceptIt();
            Type elementType = new BaseType(TypeKind.INT, getSourcePosition(start));
            Type type = new ArrayType(elementType, getSourcePosition(start));
            accept(OPEN_BRACKET);
            Expression exp = parseExpression();
            accept(CLOSE_BRACKET);

            NewArrayExpr nae = new NewArrayExpr(type, exp, getSourcePosition(start));
            return nae;
         }

         // Error.
         else {
            parseError("Could not parse new expression at token " + currentToken.errorString() + ".");
            return null; // Unreachable.
         }
      }

      // int literal expression.
      else if (currentToken.kind == NUMBER) {
         Literal lit = new IntLiteral(currentToken.spelling, currentToken.position);
         acceptIt();

         LiteralExpr litExp = new LiteralExpr(lit, getSourcePosition(start));
         return litExp;
      }

      // boolean literal expression.
      else if (currentToken.kind == TRUE || currentToken.kind == FALSE) {
         Literal lit = new BooleanLiteral(currentToken.spelling, currentToken.position);
         acceptIt();

         LiteralExpr litExp = new LiteralExpr(lit, getSourcePosition(start));
         return litExp;
      }

      // Error.
      else {
         parseError("Could not parse term at token " + currentToken.errorString() + ".");
         return null; // Unreachable.
      }
   }

   /**
    * Checks whether the current token matches the expected token. If so, fetch the next token. If not, record an error.
    * 
    * @param expectedKind
    */
   private void accept(TokenKind expectedKind) throws SyntaxError {
      if (currentToken.kind == expectedKind) {
         currentToken = scanner.scan();
      }
      else {
         parseError(String.format("Expected token %s, but got %s at line(s) %s.", expectedKind, currentToken.kind, currentToken.position));
      }
   }

   /**
    * Scans for the next token.
    * 
    * @throws SyntaxError
    */
   private void acceptIt() throws SyntaxError {
      currentToken = scanner.scan();
   }

   /**
    * Record error, then throw exception.
    * 
    * @param msg
    */
   private void parseError(String msg) throws SyntaxError {
      logger.error("PARSER: " + msg);
      throw new SyntaxError();
   }

   /**
    * Returns a position with start equal to the given parameter and finish equal to the finish of the current token
    * when the method is called.
    * 
    * @param start
    * @return
    */
   private SourcePosition getSourcePosition(int start) {
      return new SourcePosition(start, currentToken.position.finish);
   }

}

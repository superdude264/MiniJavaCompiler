package miniJava.ContextualAnalyzer;

import miniJava.Logger;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.TokenKind;

public class TypeChecker implements Visitor<Void, Type> {

   private final Logger logger;

   public TypeChecker(Logger logger) {
      this.logger = logger;
   }

   private void log(String msg) {
      logger.contextError(msg);
   }

   public void check(AST ast) {
      try {
         ast.visit(this, null);
      }
      catch (Exception e) {
         log(e.getMessage());
      }
   }

   @Override
   public Type visitPackage(Package prog, Void none) {
      for (ClassDecl c : prog.classDeclList) {
         c.visit(this, null);
      }
      return null;
   }

   @Override
   public Type visitClassDecl(ClassDecl cd, Void none) {
      // Visit fields to keep with the pattern.
      for (FieldDecl f : cd.fieldDeclList) {
         f.visit(this, null);
      }
      for (MethodDecl m : cd.methodDeclList) {
         m.visit(this, null);
      }
      return null;
   }

   @Override
   public Type visitFieldDecl(FieldDecl fd, Void none) {
      Type t = fd.type;
      if (t.equals(BaseTypes.VOID)) {
         log("Field '" + fd.name + "' cannot be declared with type 'void' " + fd.posn + ".");
      }
      return fd.type;
   }

   @Override
   public Type visitMethodDecl(MethodDecl md, Void none) {
      // Parameters.
      for (ParameterDecl pd : md.parameterDeclList) {
         pd.visit(this, null);
      }

      // Body.
      for (Statement s : md.statementList) {
         s.visit(this, null);
      }

      // Return.
      if (md.type.equals(BaseTypes.VOID)) {
         // Void method cannot have return.
         if (md.returnExp != null) {
            log("Method '" + md.name + "' is of type void and cannot have a return statement " + md.posn + ".");
         }
      }
      else {
         // Non-void method.
         if (md.returnExp == null) {
            log("Method '" + md.name + "' does not have the required return statement " + md.posn + ".");
         }
         else {
            Type retType = md.returnExp.visit(this, null);
            if (!md.type.equals(retType)) {
               log("Method '" + md.name + "' is of type '" + md.type + "', but is attempting to return a value of type '" + retType + "' " + md.returnExp.posn
                     + ".");
            }
         }
      }

      return null;
   }

   @Override
   public Type visitParameterDecl(ParameterDecl pd, Void none) {
      if (pd.type.equals(BaseTypes.VOID)) {
         log("Method parameters cannot have type 'void' " + pd.posn + ".");
      }
      return pd.type;
   }

   @Override
   public Type visitVarDecl(VarDecl decl, Void none) {
      return decl.type;
   }

   @Override
   public Type visitBaseType(BaseType type, Void none) {
      return type;
   }

   @Override
   public Type visitClassType(ClassType type, Void none) {
      return type;
   }

   @Override
   public Type visitArrayType(ArrayType type, Void none) {
      return type;
   }

   @Override
   public Type visitBlockStmt(BlockStmt stmt, Void none) {
      for (Statement s : stmt.sl) {
         s.visit(this, null);
      }
      return null;
   }

   @Override
   public Type visitVardeclStmt(VarDeclStmt stmt, Void none) {
      Type exprType = stmt.initExp.visit(this, null);
      Type declType = stmt.varDecl.visit(this, null);

      if (!exprType.equals(declType)) {
         log("Type mistach.  Attempting to assign '" + exprType + "' to '" + declType + "' " + stmt.posn + ".");
      }

      return null;
   }

   @Override
   public Type visitAssignStmt(AssignStmt stmt, Void none) {
      Type refType = stmt.ref.visit(this, null);
      Type valType = stmt.val.visit(this, null);

      if (!refType.equals(valType)) {
         log("Type mistach.  Attempting to assign '" + valType + "' to '" + refType + "' " + stmt.posn + ".");
      }

      return null;
   }

   @Override
   public Type visitCallStmt(CallStmt stmt, Void none) {
      stmt.methodRef.visit(this, null);
      MethodDecl methodDecl = (MethodDecl) stmt.methodRef.decl;
      ExprList args = stmt.argList;

      checkArgs(methodDecl, args, stmt);

      return null;
   }

   @Override
   public Type visitIfStmt(IfStmt stmt, Void none) {
      Type condType = stmt.cond.visit(this, null);
      if (!condType.equals(BaseTypes.BOOLEAN)) {
         log("Conditional expression of an if-statement must be of type 'boolean'.  Detecting type '" + condType + "' " + stmt.cond.posn + ".");
      }
      stmt.thenStmt.visit(this, null);
      if (stmt.elseStmt != null) {
         stmt.elseStmt.visit(this, null);
      }
      return null;
   }

   @Override
   public Type visitWhileStmt(WhileStmt stmt, Void none) {
      Type condType = stmt.cond.visit(this, null);
      if (!condType.equals(BaseTypes.BOOLEAN)) {
         log("Conditional expression of an while-statement must be of type 'boolean'.  Detecting type '" + condType + "' " + stmt.cond.posn + ".");
      }
      stmt.body.visit(this, null);
      return null;
   }

   @Override
   public Type visitUnaryExpr(UnaryExpr expr, Void none) {
      expr.operator.visit(this, null);
      Type inputType = expr.expr.visit(this, null);
      Type outputType = BaseTypes.ERROR;

      // Numeric negation '-'.
      if (expr.operator.spelling.equals(TokenKind.SUB.value)) {
         if (!inputType.equals(BaseTypes.INT)) {
            log("The numeric negation operator '-' is only valid for type 'int'. Detected '" + inputType + "' " + expr.posn + ".");
         }
         else {
            outputType = BaseTypes.INT;
         }

      }

      // Logical negation '!'.
      else if (expr.operator.spelling.equals(TokenKind.NOT.value)) {
         if (!inputType.equals(BaseTypes.BOOLEAN)) {
            log("The logical negation operator '!' is only valid for type 'boolean'. Detected '" + inputType + "' " + expr.posn + ".");
         }
         else {
            outputType = BaseTypes.BOOLEAN;
         }
      }

      return outputType;
   }

   @Override
   public Type visitBinaryExpr(BinaryExpr expr, Void none) {
      expr.operator.visit(this, null);
      Type leftType = expr.left.visit(this, null);
      Type rightType = expr.right.visit(this, null);
      Type outputType = BaseTypes.ERROR;

      // Int operators.
      if (isNumericOperator(expr.operator)) {
         boolean areNums = leftType.equals(BaseTypes.INT) && rightType.equals(BaseTypes.INT);
         if (!areNums) {
            log("The '" + expr.operator.spelling + "' operator is only valid for type 'int'. Detected '" + leftType + "' and '" + rightType + "' " + expr.posn
                  + ".");
         }

         if (isRelationalOperator(expr.operator)) {
            outputType = BaseTypes.BOOLEAN;
         }
         else {
            outputType = BaseTypes.INT;
         }

      }

      // Boolean operators.
      else if (isBooleanOperator(expr.operator)) {
         boolean areBooleans = leftType.equals(BaseTypes.BOOLEAN) && rightType.equals(BaseTypes.BOOLEAN);
         if (!areBooleans) {
            log("The '" + expr.operator.spelling + "' operator is only valid for type 'boolean'. Detected '" + leftType + "' and '" + rightType + "' "
                  + expr.posn + ".");
         }

         outputType = BaseTypes.BOOLEAN;
      }

      // Other operators. Currently on '==' & '!='.
      else if (!leftType.equals(rightType)) {
         log("Types must match on either side of the '" + expr.operator.spelling + "' operator.  Detected '" + leftType + "' and '" + rightType + "' "
               + expr.posn + ".");
      }
      else {
         // The types are matching, but are not int or boolean. The output of the equality operators is a boolean.
         outputType = BaseTypes.BOOLEAN;
      }

      expr.type = outputType;
      return outputType;
   }

   /**
    * Returns true for operators that take two numbers as input.
    * 
    * @param operator
    * @return
    */
   private boolean isNumericOperator(Operator operator) {
      String op = operator.spelling;
      boolean output = false;
      output |= (op.equals(TokenKind.GREATER_THAN.value));
      output |= (op.equals(TokenKind.GT_EQUAL.value));
      output |= (op.equals(TokenKind.LESS_THAN.value));
      output |= (op.equals(TokenKind.LT_EQUAL.value));
      output |= (op.equals(TokenKind.ADD.value));
      output |= (op.equals(TokenKind.SUB.value));
      output |= (op.equals(TokenKind.MULTI.value));
      output |= (op.equals(TokenKind.DIVIDE.value));
      return output;
   }

   /**
    * Returns true for operators that take two ints as input and return a boolean value.
    * 
    * @param operator
    * @return
    */
   private boolean isRelationalOperator(Operator operator) {
      String op = operator.spelling;
      boolean output = false;
      output |= (op.equals(TokenKind.GREATER_THAN.value));
      output |= (op.equals(TokenKind.GT_EQUAL.value));
      output |= (op.equals(TokenKind.LESS_THAN.value));
      output |= (op.equals(TokenKind.LT_EQUAL.value));
      return output;
   }

   /**
    * Returns true for operators that take two booleans as input.
    * 
    * @param operator
    * @return
    */
   private boolean isBooleanOperator(Operator operator) {
      String op = operator.spelling;
      boolean output = false;
      output |= (op.equals(TokenKind.AND.value));
      output |= (op.equals(TokenKind.OR.value));
      output |= (op.equals(TokenKind.NOT.value));
      return output;
   }

   @Override
   public Type visitRefExpr(RefExpr expr, Void none) {
      Type t = expr.ref.visit(this, null);
      expr.type = t;
      return t;
   }

   @Override
   public Type visitCallExpr(CallExpr expr, Void none) {
      Type retType = expr.functionRef.visit(this, null);
      MethodDecl methodDecl = (MethodDecl) expr.functionRef.decl;
      ExprList args = expr.argList;

      checkArgs(methodDecl, args, expr);

      return retType;
   }

   private void checkArgs(MethodDecl methodDecl, ExprList args, AST astObj) {
      ParameterDeclList params = methodDecl.parameterDeclList;

      if (args.size() != params.size()) {
         log("Attempting to call method '" + methodDecl.name + "' " + "with improper number of arguments " + astObj.posn + ".");
      }

      // Don't check arg types unless the correct number are given.
      else {
         int i = 0;
         for (Expression e : args) {
            Type argType = e.visit(this, null);
            ParameterDecl param = params.get(i);
            Type paramType = param.type;
            if (!argType.equals(paramType)) {
               log("Improper argument type on call to '" + methodDecl.name + "'. Argument at index " + i + " is of type '" + argType
                     + "', but method requires '" + paramType + "' " + e.posn + ".");
            }
            i++;
         }

      }
   }

   @Override
   public Type visitLiteralExpr(LiteralExpr expr, Void none) {
      Type t = expr.literal.visit(this, null);
      expr.type = t;
      return t;
   }

   @Override
   public Type visitNewObjectExpr(NewObjectExpr expr, Void none) {
      Type t = expr.classtype.visit(this, null);
      expr.type = t;
      return t;
   }

   @Override
   public Type visitNewArrayExpr(NewArrayExpr expr, Void none) {
      expr.eltType.visit(this, null);
      expr.sizeExpr.visit(this, null);

      if (!expr.sizeExpr.type.equals(BaseTypes.INT)) {
         log("Array indexing expressions must be of type 'int'. Got '" + expr.sizeExpr.type + "' " + expr.sizeExpr.posn + ".");
      }

      expr.type = new ArrayType(expr.eltType, expr.posn);
      return expr.eltType;
   }

   @Override
   public Type visitQualifiedRef(QualifiedRef ref, Void none) {
      Type t = ref.decl.type;
      ref.type = t;
      return t;
   }

   @Override
   public Type visitIndexedRef(IndexedRef ref, Void none) {
      ArrayType at = (ArrayType) ref.decl.type;
      ref.type = at.eltType;
      return at.eltType;
   }

   @Override
   public Type visitIdRef(IdRef ref, Void none) {
      Type t = ref.decl.type;
      ref.type = t;
      return t;
   }

   @Override
   public Type visitThisRef(ThisRef ref, Void none) {
      Type t = ref.decl.type;
      ref.type = t;
      return t;
   }

   @Override
   public Type visitIdentifier(Identifier id, Void none) {
      return id.decl.type;
   }

   @Override
   public Type visitOperator(Operator op, Void none) {
      // Do nothing.
      return null;
   }

   @Override
   public Type visitIntLiteral(IntLiteral num, Void none) {
      return new BaseType(TypeKind.INT, num.posn);
   }

   @Override
   public Type visitBooleanLiteral(BooleanLiteral bool, Void none) {
      return new BaseType(TypeKind.BOOLEAN, bool.posn);
   }

}

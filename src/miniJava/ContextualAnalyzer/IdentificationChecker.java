package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.List;

import miniJava.Logger;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

/**
 * FIXME: The basic model used for the IdentificationTable and assumed here (Stack<Map<String, Declaration>>) is not
 * appropriate for higher scope levels (package, class, & method) because scope doesn't behave like a stack at those
 * levels. In addition, there is probably a way to include visibility, 'static-ness', and field/method split into a
 * single coherent model (i.e. one that contains scope, visibility, static-ness, & field/method to gauge accessiblity of
 * an item)
 */
public class IdentificationChecker implements Visitor<Void, Object> {

   private final Logger              logger;
   private final IdentificationTable idTable;
   private ClassDecl                 currentClass;
   private boolean                   visitingStaticMethod;
   private boolean                   visitingLeftValueOfAssignStmt;
   private int                       mainMethodCount;

   public IdentificationChecker(Logger logger) {
      this.logger = logger;
      idTable = new IdentificationTable();
      mainMethodCount = 0;
   }

   public void check(AST ast) {
      try {
         setupStandardEnvironment();
         ast.visit(this, null);
         if (mainMethodCount != 1) {
            logger.contextError("File must contain exactly one method with the signature 'public static void main(String[])'.");
         }
      }
      catch (Exception e) {
         logger.contextError(e.getMessage());
      }
   }

   private void putDecl(String key, Declaration value) throws IdentifierError {
      try {
         idTable.put(key, value);
      }
      catch (DuplicateIdentifierError e) {
         throw new IdentifierError("Identifier '" + key + "' is already in scope " + value.posn + ".");
      }
   }

   private Declaration getDecl(Identifier id) throws IdentifierError {
      try {
         return idTable.get(id.spelling);
      }
      catch (UndefinedIdentifierError e) {
         throw new IdentifierError("Identifier '" + id.spelling + "' is undefined " + id.posn + ".");
      }
   }

   private Declaration getDecl(Identifier id, boolean visitingLeftVal) throws IdentifierError {
      try {
         return idTable.get(id.spelling, visitingLeftVal);
      }
      catch (UndefinedIdentifierError e) {
         throw new IdentifierError("Identifier '" + id.spelling + "' is undefined " + id.posn + ".");
      }
   }

   private ClassDeclList getStandardEnvironment() {
      final SourcePosition dummyPos = SourcePosition.DUMMY_POSITION;
      final ClassDeclList standardClasses = new ClassDeclList();

      // String class.
      ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), dummyPos);
      standardClasses.add(stringClass);

      // _PrintStream class.
      Type voidType = new BaseType(TypeKind.VOID, dummyPos);
      FieldDecl printField = new FieldDecl(false, false, voidType, "println", dummyPos);

      Type intType = new BaseType(TypeKind.INT, dummyPos);
      ParameterDecl printParam = new ParameterDecl(intType, "n", dummyPos);
      ParameterDeclList printParams = new ParameterDeclList();
      printParams.add(printParam);

      MethodDecl printMethod = new MethodDecl(printField, printParams, new StatementList(), null, dummyPos);
      MethodDeclList methodDecls = new MethodDeclList();
      methodDecls.add(printMethod);

      ClassDecl printStreamClass = new ClassDecl("_PrintStream", new FieldDeclList(), methodDecls, dummyPos);
      standardClasses.add(printStreamClass);

      // System class.
      Identifier printStreamId = new Identifier(printStreamClass.name, dummyPos);
      printStreamId.decl = printStreamClass;
      Type printStreamType = new ClassType(printStreamId, dummyPos);
      FieldDecl outField = new FieldDecl(false, true, printStreamType, "out", dummyPos);
      FieldDeclList systemFields = new FieldDeclList();
      systemFields.add(outField);

      ClassDecl systemClass = new ClassDecl("System", systemFields, new MethodDeclList(), dummyPos);
      standardClasses.add(systemClass);

      return standardClasses;
   }

   private void setupStandardEnvironment() {
      ClassDeclList stdEnv = getStandardEnvironment();
      for (ClassDecl c : stdEnv) {
         putDecl(c.name, c);
         for (FieldDecl f : c.fieldDeclList) {
            putDecl(f.name, f);
         }
         for (MethodDecl m : c.methodDeclList) {
            putDecl(m.name, m);
         }
      }
   }

   @Override
   public Object visitPackage(Package prog, Void none) {
      // Scope level 1.
      idTable.openScope();

      // Add all class declarations to the ID table before visiting.
      for (ClassDecl c : prog.classDeclList) {
         putDecl(c.name, c);
      }

      for (ClassDecl c : prog.classDeclList) {
         currentClass = c;
         c.visit(this, null);
      }

      idTable.closeScope();
      return null;
   }

   @Override
   public Object visitClassDecl(ClassDecl cd, Void none) {
      // Scope level 2
      idTable.openScope();

      // Add all member declarations to the ID table before visiting.
      for (FieldDecl f : cd.fieldDeclList) {
         putDecl(f.name, f);
      }
      for (MethodDecl m : cd.methodDeclList) {
         putDecl(m.name, m);
      }

      for (FieldDecl f : cd.fieldDeclList) {
         f.visit(this, null);
      }
      for (MethodDecl m : cd.methodDeclList) {
         visitingStaticMethod = m.isStatic;
         if (isMainMethod(m)) {
            mainMethodCount += 1;
            m.isMain = true;
         }
         m.visit(this, null);
      }

      idTable.closeScope();
      return null;
   }

   private boolean isMainMethod(MethodDecl md) {
      boolean isPublic = !md.isPrivate;
      boolean isStatic = md.isStatic;
      boolean isVoid = md.type.typeKind.equals(TypeKind.VOID);
      boolean isCalledMain = md.name.equals("main");
      boolean hasArgParam = false;
      if (md.parameterDeclList.size() == 1) {
         ParameterDecl paramDecl = md.parameterDeclList.get(0);
         if (paramDecl.type instanceof ArrayType) {
            ArrayType paramType = (ArrayType) paramDecl.type;
            if (paramType.eltType instanceof ClassType) {
               ClassType arrayType = (ClassType) paramType.eltType;
               hasArgParam = arrayType.className.spelling.equals("String");
            }
         }
      }

      return isPublic && isStatic && isVoid && isCalledMain && hasArgParam;
   }

   @Override
   public Object visitFieldDecl(FieldDecl fd, Void none) {
      fd.type.visit(this, null);
      return null;
   }

   @Override
   public Object visitMethodDecl(MethodDecl md, Void none) {
      // Scope level 3.
      idTable.openScope();

      // Return type.
      md.type.visit(this, null);

      // Parameters.
      int offset = -md.parameterDeclList.size();
      for (ParameterDecl pd : md.parameterDeclList) {
         pd.visit(this, null);
         pd.offset = offset;
         offset += 1;
      }

      // Body.
      for (Statement s : md.statementList) {
         s.visit(this, null);
      }

      if (md.returnExp != null) {
         md.returnExp.visit(this, null);
      }

      idTable.closeScope();
      return null;
   }

   @Override
   public Object visitParameterDecl(ParameterDecl pd, Void none) {
      putDecl(pd.name, pd);
      pd.type.visit(this, null);
      return null;
   }

   @Override
   public Object visitVarDecl(VarDecl decl, Void none) {
      putDecl(decl.name, decl);
      decl.type.visit(this, null);
      return null;
   }

   @Override
   public Object visitBaseType(BaseType type, Void none) {
      // Do nothing.
      return null;
   }

   @Override
   public Object visitClassType(ClassType type, Void none) {
      type.className.decl = getDecl(type.className);
      return null;
   }

   @Override
   public Object visitArrayType(ArrayType type, Void none) {
      type.eltType.visit(this, null);
      return null;
   }

   @Override
   public Object visitBlockStmt(BlockStmt stmt, Void none) {
      // Scope level 4+
      idTable.openScope();

      for (Statement s : stmt.sl) {
         s.visit(this, null);
      }

      idTable.closeScope();
      return null;
   }

   @Override
   public Object visitVardeclStmt(VarDeclStmt stmt, Void none) {
      stmt.initExp.visit(this, null);
      stmt.varDecl.visit(this, null);
      return null;
   }

   @Override
   public Object visitAssignStmt(AssignStmt stmt, Void none) {
      this.visitingLeftValueOfAssignStmt = true;
      stmt.ref.visit(this, null);
      this.visitingLeftValueOfAssignStmt = false;

      stmt.val.visit(this, null);
      return null;
   }

   @Override
   public Object visitCallStmt(CallStmt stmt, Void none) {
      stmt.methodRef.visit(this, null);
      for (Expression e : stmt.argList) {
         e.visit(this, null);
      }
      return null;
   }

   @Override
   public Object visitIfStmt(IfStmt stmt, Void none) {
      if (stmt.thenStmt instanceof VarDeclStmt) {
         throw new IdentifierError("A variable declaration cannot be the only statement in the branch of a conditional " + stmt.thenStmt.posn + ".");
      }
      stmt.cond.visit(this, null);
      stmt.thenStmt.visit(this, null);
      if (stmt.elseStmt != null) {
         if (stmt.elseStmt instanceof VarDeclStmt) {
            throw new IdentifierError("A variable declaration cannot be the only statement in the branch of a conditional " + stmt.elseStmt.posn + ".");
         }
         stmt.elseStmt.visit(this, null);
      }
      return null;
   }

   @Override
   public Object visitWhileStmt(WhileStmt stmt, Void none) {
      stmt.cond.visit(this, null);

      if (stmt.body instanceof VarDeclStmt) {
         throw new IdentifierError("A variable declaration cannot be the only statement in the branch of a while-loop " + stmt.body.posn + ".");
      }
      stmt.body.visit(this, null);

      return null;
   }

   @Override
   public Object visitUnaryExpr(UnaryExpr expr, Void none) {
      expr.operator.visit(this, null);
      expr.expr.visit(this, null);
      return null;
   }

   @Override
   public Object visitBinaryExpr(BinaryExpr expr, Void none) {
      expr.operator.visit(this, null);
      expr.left.visit(this, null);
      expr.right.visit(this, null);
      return null;
   }

   @Override
   public Object visitRefExpr(RefExpr expr, Void none) {
      expr.ref.visit(this, null);
      return null;
   }

   @Override
   public Object visitCallExpr(CallExpr expr, Void none) {
      // HACK HACK HACK
      boolean temp = this.visitingLeftValueOfAssignStmt;
      this.visitingLeftValueOfAssignStmt = false;
      expr.functionRef.visit(this, null);
      this.visitingLeftValueOfAssignStmt = temp;

      for (Expression e : expr.argList) {
         e.visit(this, null);
      }
      return null;
   }

   @Override
   public Object visitLiteralExpr(LiteralExpr expr, Void none) {
      expr.literal.visit(this, null);
      return null;
   }

   @Override
   public Object visitNewObjectExpr(NewObjectExpr expr, Void none) {
      expr.classtype.visit(this, null);
      return null;
   }

   @Override
   public Object visitNewArrayExpr(NewArrayExpr expr, Void none) {
      expr.eltType.visit(this, null);
      expr.sizeExpr.visit(this, null);
      return null;
   }

   @Override
   public Object visitQualifiedRef(QualifiedRef ref, Void none) {
      ref.ref.visit(this, null);
      Declaration decl = ref.ref.decl;

      // Attempting to access non-static members of a class.
      if (decl.type instanceof ClassType) {
         ClassType classType = (ClassType) decl.type;
         classType.visit(this, null);
         ClassDecl classDecl = (ClassDecl) classType.className.decl;
         boolean sameClass = ((ClassType) decl.type).className.spelling.equals(this.currentClass.name);
         visitOtherClassMembers(ref, classDecl, sameClass, false);
      }

      // Attempting to access a static member of a class.
      else if (decl instanceof ClassDecl) {
         ClassDecl classDecl = (ClassDecl) ref.ref.decl;
         boolean sameClass = decl.name.equals(this.currentClass.name);
         visitOtherClassMembers(ref, classDecl, sameClass, true);
      }

      // Attempting to access array length.
      if (ref.ref.decl.type instanceof ArrayType && ref.id.spelling.equals("length")) {
         if (this.visitingLeftValueOfAssignStmt) {
            throw new IdentifierError("Cannot assign to 'length' field of an array " + ref.id.posn + ".");
         }
         ref.id.decl = BaseTypes.ARRAY_LENGTH_DECL;
         ref.decl = BaseTypes.ARRAY_LENGTH_DECL;
      }

      return null;
   }

   /***
    * HACK HACK HACK
    * 
    * This method is here because model of the IdentificationTable is a bit off. The table only keeps the members of the
    * current class in scope, preventing access to the public members of other classes. There is a cleaner (and faster)
    * way to handle this, which involves fixing the IdentificationTable.
    * 
    * @param ref
    * @param classDecl
    * @param onlyStaticMembers
    */
   private void visitOtherClassMembers(QualifiedRef ref, ClassDecl classDecl, boolean includePrivateMembers, boolean onlyStaticMembers) {
      // Get members.
      List<MemberDecl> members = new ArrayList<MemberDecl>();
      for (FieldDecl fd : classDecl.fieldDeclList) {
         members.add(fd);
      }
      for (MethodDecl md : classDecl.methodDeclList) {
         members.add(md);
      }

      boolean found = false;
      for (MemberDecl member : members) {
         if (ref.id.spelling.equals(member.name)) {
            found = true;
            if (!includePrivateMembers && member.isPrivate) {
               throw new IdentifierError("Identifier '" + ref.id.spelling + "' is inaccessible on class " + ref.ref.decl.name + " "
                     + " because it is declared 'private' " + ref.id.posn + ".");
            }
            else if (onlyStaticMembers && !member.isStatic) {
               throw new IdentifierError("Identifier '" + ref.id.spelling + "' is inaccessible on class " + ref.ref.decl.name + " "
                     + " because it is not declared 'static'  " + ref.id.posn + ".");
            }
            ref.id.decl = member;
            ref.decl = member;
         }
      }
      if (!found) {
         throw new IdentifierError("Identifier '" + ref.id.spelling + "' could not be found on class " + ref.ref.decl.name + " " + ref.id.posn + ".");
      }
   }

   @Override
   public Object visitIndexedRef(IndexedRef ref, Void none) {
      ref.indexExpr.visit(this, null);
      ref.ref.visit(this, null);
      ref.decl = ref.ref.decl;
      return null;
   }

   @Override
   public Object visitIdRef(IdRef ref, Void none) {
      ref.decl = getDecl(ref.id, visitingLeftValueOfAssignStmt);
      ref.id.visit(this, null);
      return null;
   }

   @Override
   public Object visitThisRef(ThisRef ref, Void none) {
      ref.decl = this.currentClass;
      return null;
   }

   @Override
   public Object visitIdentifier(Identifier id, Void none) {
      id.decl = getDecl(id);

      if (visitingStaticMethod && id.decl instanceof MemberDecl) {
         MemberDecl md = (MemberDecl) id.decl;
         if (!md.isStatic) {
            throw new IdentifierError("Cannot use non-static identifier '" + id.spelling + "'  in a static method " + id.posn + ".");
         }
      }

      return null;
   }

   @Override
   public Object visitOperator(Operator op, Void none) {
      // Do nothing.
      return null;
   }

   @Override
   public Object visitIntLiteral(IntLiteral num, Void none) {
      return null;
   }

   @Override
   public Object visitBooleanLiteral(BooleanLiteral bool, Void none) {
      return null;
   }
}

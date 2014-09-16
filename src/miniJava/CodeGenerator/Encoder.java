package miniJava.CodeGenerator;

import java.util.ArrayList;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.Logger;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.BaseTypes;
import miniJava.SyntacticAnalyzer.TokenKind;

/**
 * FIXME: Several issues stemming from (probable) lack of mental model & design. Probably needs to generate code from
 * standard environment classes instead faking it here. Additionally, a coherent method of differentiating when to
 * access vs. update an item is needed (e.g. a qualified method call within the index expression of an array
 * assignment).
 */
public class Encoder implements Visitor<Object, Void> {

   private final Logger logger;
   private int          lbOffset = 3;
   private boolean      visitingLeftValueOfAssignStmt;
   private int          mainAddr;

   public Encoder(Logger logger) {
      this.logger = logger;
   }

   private void log(String msg) {
      logger.error(msg);
   }

   public void encode(AST ast) {
      try {
         ast.visit(this, null);
      }
      catch (Exception e) {
         log(e.getMessage());
      }
   }

   @Override
   public Void visitPackage(Package prog, Object arg) {
      Machine.initCodeGen();

      // Array.
      int p_ArrayClass = Machine.nextInstrAddr();
      Machine.emit(Op.JUMP, Reg.CB, 0);
      // Array length method.
      int addr = Machine.nextInstrAddr();
      BaseTypes.ARRAY_LENGTH_DECL.address = addr;
      Machine.emit(Op.LOAD, Reg.OB, -1);
      Machine.emit(Op.RETURN, 1, 0, 0);
      // Array class object.
      int l_ArrayClass = Machine.nextInstrAddr();
      Machine.patch(p_ArrayClass, l_ArrayClass);
      Machine.emit(Op.LOADL, -1); // No super class.
      Machine.emit(Op.LOADL, 1); // Number of methods.
      Machine.emit(Op.LOADA, Reg.CB, addr); // Address of methods

      // Other classes.
      for (ClassDecl c : prog.classDeclList) {
         c.visit(this, null);
      }

      // Call to main.
      Machine.emit(Op.LOADL, Machine.nullRep); // 'args' param to main is null.
      Machine.emit(Op.CALL, Reg.CB, this.mainAddr); // call main.
      Machine.emit(Op.HALT);

      return null;
   }

   @Override
   public Void visitClassDecl(ClassDecl cd, Object arg) {
      for (FieldDecl f : cd.fieldDeclList) {
         f.visit(this, null);
      }

      int p_ClassObject = Machine.nextInstrAddr();
      Machine.emit(Op.JUMP, Reg.CB, 0);

      ArrayList<Integer> methodAddr = new ArrayList<Integer>();
      for (MethodDecl m : cd.methodDeclList) {
         int addr = Machine.nextInstrAddr();
         methodAddr.add(addr);
         m.address = addr;

         // Patch
         for (Integer p : m.patchList) {
            Machine.patch(p, m.address);
         }

         if (m.isMain) {
            this.mainAddr = addr;
         }

         m.visit(this, null);
      }

      // Build class object.
      int l_ClassObject = Machine.nextInstrAddr();
      Machine.patch(p_ClassObject, l_ClassObject);
      Machine.emit(Op.LOADL, -1); // No super class.
      Machine.emit(Op.LOADL, cd.methodDeclList.size()); // Number of methods.
      for (Integer addr : methodAddr) {
         Machine.emit(Op.LOADA, Reg.CB, addr); // Address of methods
      }

      return null;
   }

   @Override
   public Void visitFieldDecl(FieldDecl fd, Object arg) {
      return null;
   }

   @Override
   public Void visitMethodDecl(MethodDecl md, Object arg) {
      // Reset local base offset.
      this.lbOffset = 3;

      // Parameters.
      for (ParameterDecl pd : md.parameterDeclList) {
         pd.visit(this, null);
      }

      // Body.
      for (Statement s : md.statementList) {
         s.visit(this, null);
      }

      // Return.
      if (md.returnExp != null) {
         md.returnExp.visit(this, null);
         // Return 1 value & pop args.
         int args = md.parameterDeclList.size();
         Machine.emit(Op.RETURN, 1, 0, args);
      }
      else {
         // Implicit return.
         int args = md.parameterDeclList.size();
         Machine.emit(Op.RETURN, 0, 0, args);
      }

      return null;
   }

   @Override
   public Void visitParameterDecl(ParameterDecl pd, Object arg) {
      return null;
   }

   @Override
   public Void visitVarDecl(VarDecl decl, Object arg) {
      decl.address = this.lbOffset;
      this.lbOffset += 1;
      return null;
   }

   @Override
   public Void visitBaseType(BaseType type, Object arg) {
      return null;
   }

   @Override
   public Void visitClassType(ClassType type, Object arg) {
      return null;
   }

   @Override
   public Void visitArrayType(ArrayType type, Object arg) {
      return null;
   }

   @Override
   public Void visitBlockStmt(BlockStmt stmt, Object arg) {
      for (Statement s : stmt.sl) {
         s.visit(this, null);
      }
      return null;
   }

   @Override
   public Void visitVardeclStmt(VarDeclStmt stmt, Object arg) {
      stmt.varDecl.visit(this, null);
      stmt.initExp.visit(this, null);
      return null;
   }

   @Override
   public Void visitAssignStmt(AssignStmt stmt, Object arg) {
      if (stmt.ref instanceof IndexedRef) {
         this.visitingLeftValueOfAssignStmt = true;
         stmt.ref.visit(this, false);
         this.visitingLeftValueOfAssignStmt = false;
         stmt.val.visit(this, null);
         Machine.emit(Prim.arrayupd);
      }
      else if (stmt.ref instanceof QualifiedRef) {
         this.visitingLeftValueOfAssignStmt = true;
         stmt.ref.visit(this, false);
         this.visitingLeftValueOfAssignStmt = false;
         stmt.val.visit(this, null);
         Machine.emit(Prim.fieldupd);
      }
      else if (stmt.ref.decl instanceof FieldDecl) {
         this.visitingLeftValueOfAssignStmt = true;
         stmt.ref.visit(this, false);
         this.visitingLeftValueOfAssignStmt = false;
         stmt.val.visit(this, null);
         Machine.emit(Prim.fieldupd);
      }
      else {
         stmt.val.visit(this, null);
         this.visitingLeftValueOfAssignStmt = true;
         stmt.ref.visit(this, true);
         this.visitingLeftValueOfAssignStmt = false;
      }

      return null;
   }

   @Override
   public Void visitCallStmt(CallStmt stmt, Object arg) {
      for (Expression e : stmt.argList) {
         e.visit(this, null);
      }

      // If the call is not qualified add an implicit 'this' reference.
      if (stmt.methodRef instanceof IdRef) {
         Machine.emit(Op.LOADA, Reg.OB, 0);
      }

      stmt.methodRef.visit(this, null);
      return null;
   }

   @Override
   public Void visitIfStmt(IfStmt stmt, Object arg) {
      // Condition.
      stmt.cond.visit(this, null);
      int jumpIfAddr = Machine.nextInstrAddr();
      // If condition false, jump to the next label (either else or end of then block).
      Machine.emit(Op.JUMPIF, Machine.falseRep, Machine.CB, 0);

      // Then.
      stmt.thenStmt.visit(this, null);
      int jumpAddr = Machine.nextInstrAddr();
      // Jump to end of if/else block.
      Machine.emit(Op.JUMP, Reg.CB, 0);
      Machine.patch(jumpIfAddr, Machine.nextInstrAddr());

      // Else.
      if (stmt.elseStmt != null) {
         stmt.elseStmt.visit(this, null);
      }
      Machine.patch(jumpAddr, Machine.nextInstrAddr());

      return null;
   }

   @Override
   public Void visitWhileStmt(WhileStmt stmt, Object arg) {
      // Jump to condition.
      int jumpAddr = Machine.nextInstrAddr();
      Machine.emit(Op.JUMP, Reg.CB, 0);

      // Body.
      int loopAddr = Machine.nextInstrAddr();
      stmt.body.visit(this, null);
      Machine.patch(jumpAddr, Machine.nextInstrAddr());

      // Condition.
      stmt.cond.visit(this, null);
      // If condition is true, jump to the loop.
      Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, loopAddr);

      return null;
   }

   @Override
   public Void visitUnaryExpr(UnaryExpr expr, Object arg) {
      expr.expr.visit(this, null);
      expr.operator.visit(this, false);
      return null;
   }

   @Override
   public Void visitBinaryExpr(BinaryExpr expr, Object arg) {
      expr.left.visit(this, null);
      expr.right.visit(this, null);
      expr.operator.visit(this, true);
      return null;
   }

   @Override
   public Void visitRefExpr(RefExpr expr, Object arg) {
      expr.ref.visit(this, null);
      return null;
   }

   @Override
   public Void visitCallExpr(CallExpr expr, Object arg) {
      for (Expression e : expr.argList) {
         e.visit(this, null);
      }

      // Add implicit 'this' reference where appropriate.
      if (expr.functionRef instanceof QualifiedRef) {
         QualifiedRef qr = (QualifiedRef) expr.functionRef;
         if (!(qr.ref instanceof ThisRef) && !(qr.ref.decl instanceof VarDecl)) {
            Machine.emit(Op.LOADA, Reg.OB, 0);
         }
      }
      else {
         Machine.emit(Op.LOADA, Reg.OB, 0);
      }

      expr.functionRef.visit(this, null);
      return null;
   }

   @Override
   public Void visitLiteralExpr(LiteralExpr expr, Object arg) {
      expr.literal.visit(this, null);
      return null;
   }

   @Override
   public Void visitNewObjectExpr(NewObjectExpr expr, Object arg) {
      ClassDecl cd = (ClassDecl) expr.classtype.className.decl;
      Machine.emit(Op.LOADL, -1); // class object address
      Machine.emit(Op.LOADL, cd.fieldDeclList.size()); // num fields
      Machine.emit(Prim.newobj);
      return null;
   }

   @Override
   public Void visitNewArrayExpr(NewArrayExpr expr, Object arg) {
      expr.eltType.visit(this, null);
      expr.sizeExpr.visit(this, null);
      Machine.emit(Prim.newarr);
      return null;
   }

   @Override
   public Void visitQualifiedRef(QualifiedRef ref, Object arg) {
      if (ref.ref instanceof QualifiedRef) {
         ref.ref.visit(this, true);
      }
      else {
         ref.ref.visit(this, null);
      }

      // Field.
      if (ref.decl instanceof FieldDecl && !ref.id.spelling.equals("out")) {
         FieldDecl fd = (FieldDecl) ref.decl;

         // If first item of lvalue qualified ref is field, then get value of field.
         if (this.visitingLeftValueOfAssignStmt && ref.ref.decl instanceof FieldDecl) {
            Machine.emit(Prim.fieldref);
         }

         // Normal field.
         Machine.emit(Op.LOADL, fd.index);
         if (!this.visitingLeftValueOfAssignStmt) {
            Machine.emit(Prim.fieldref);
         }
      }

      // Method.
      else if (ref.decl instanceof MethodDecl && !ref.decl.name.equals("println")) {
         MethodDecl md = (MethodDecl) ref.decl;
         if (md.address == 0) {
            int tempAddr = Machine.nextInstrAddr();
            md.patchList.add(tempAddr);
            Machine.emit(Op.CALLI, Reg.CB, tempAddr);
         }
         else {
            Machine.emit(Op.CALLI, Reg.CB, md.address);
         }
      }

      return null;
   }

   @Override
   public Void visitIndexedRef(IndexedRef ref, Object arg) {
      ref.ref.visit(this, arg);
      ref.indexExpr.visit(this, null);

      if (!this.visitingLeftValueOfAssignStmt) {
         Machine.emit(Prim.arrayref);
      }

      return null;
   }

   @Override
   public Void visitIdRef(IdRef ref, Object arg) {
      ref.id.visit(this, arg);
      return null;
   }

   @Override
   public Void visitThisRef(ThisRef ref, Object arg) {
      Machine.emit(Op.LOADA, Reg.OB, 0);
      return null;
   }

   @Override
   public Void visitIdentifier(Identifier id, Object arg) {
      // HACK for println
      if (id.spelling.equals("System")) {
         Machine.emit(Prim.putintnl);
         return null;
      }

      // Determine operation.
      Op op = Op.LOAD;
      if (isTrue(arg)) {
         op = Op.STORE;
      }

      // Get address.
      if (id.decl instanceof FieldDecl) {
         FieldDecl fd = (FieldDecl) id.decl;
         if (this.visitingLeftValueOfAssignStmt) {
            Machine.emit(Op.LOADA, Reg.OB, 0);
            Machine.emit(Op.LOADL, fd.index);
         }
         else {
            // Load the value.
            Machine.emit(op, Reg.OB, fd.index);
         }
      }
      else if (id.decl instanceof MethodDecl && !id.decl.name.equals("println")) {
         MethodDecl md = (MethodDecl) id.decl;
         if (md.address == 0) {
            int tempAddr = Machine.nextInstrAddr();
            md.patchList.add(tempAddr);
            Machine.emit(Op.CALLI, Reg.CB, tempAddr);
         }
         else {
            Machine.emit(Op.CALLI, Reg.CB, md.address);
         }
      }
      else if (id.decl instanceof ParameterDecl) {
         ParameterDecl pd = (ParameterDecl) id.decl;
         Machine.emit(Op.LOAD, Reg.LB, pd.offset);
      }
      else {
         Machine.emit(op, Reg.LB, id.decl.address);
      }

      return null;
   }

   @Override
   public Void visitOperator(Operator op, Object arg) {
      boolean isBinary = (Boolean) arg;
      TokenKind kind = op.token.kind;
      Prim primOp = getPrimOp(kind, isBinary);
      Machine.emit(primOp);
      return null;
   }

   private Prim getPrimOp(TokenKind kind, boolean isBinary) {
      Prim primOp = null;

      //@formatter:off
      if (isBinary) {
         if      (kind == TokenKind.GREATER_THAN ) { primOp = Prim.gt;   }
         else if (kind == TokenKind.GT_EQUAL     ) { primOp = Prim.ge;   }
         else if (kind == TokenKind.LESS_THAN    ) { primOp = Prim.lt;   }
         else if (kind == TokenKind.LT_EQUAL     ) { primOp = Prim.le;   }
         else if (kind == TokenKind.EQUALITY     ) { primOp = Prim.eq;   }
         else if (kind == TokenKind.NOT_EQUAL    ) { primOp = Prim.ne;   }
         else if (kind == TokenKind.AND          ) { primOp = Prim.and;  }
         else if (kind == TokenKind.OR           ) { primOp = Prim.or;   }
         else if (kind == TokenKind.ADD          ) { primOp = Prim.add;  }
         else if (kind == TokenKind.SUB          ) { primOp = Prim.sub;  }
         else if (kind == TokenKind.MULTI        ) { primOp = Prim.mult; }
         else if (kind == TokenKind.DIVIDE       ) { primOp = Prim.div;  }
      }
      else {
         // Unary operators.
         if      (kind == TokenKind.NOT     ) { primOp = Prim.not;  }
         else if (kind == TokenKind.SUB     ) { primOp = Prim.neg;  }
      }
      //@formatter:on

      return primOp;
   }

   @Override
   public Void visitIntLiteral(IntLiteral num, Object arg) {
      int val = Integer.parseInt(num.spelling);
      Machine.emit(Op.LOADL, val);
      return null;
   }

   @Override
   public Void visitBooleanLiteral(BooleanLiteral bool, Object arg) {
      boolean val = Boolean.parseBoolean(bool.spelling);
      if (val) {
         Machine.emit(Op.LOADL, Machine.trueRep);
      }
      else {
         Machine.emit(Op.LOADL, Machine.falseRep);
      }
      return null;
   }

   private boolean isTrue(Object arg) {
      if (arg == null) {
         return false;
      }
      boolean isWrite = (boolean) arg;
      return isWrite;
   }

}

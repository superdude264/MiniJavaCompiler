package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.SyntacticAnalyzer.SourcePosition;

public interface BaseTypes {

   static final Type       VOID              = new BaseType(TypeKind.VOID, SourcePosition.DUMMY_POSITION);
   static final Type       INT               = new BaseType(TypeKind.INT, SourcePosition.DUMMY_POSITION);
   static final Type       BOOLEAN           = new BaseType(TypeKind.BOOLEAN, SourcePosition.DUMMY_POSITION);
   static final Type       ERROR             = new BaseType(TypeKind.ERROR, SourcePosition.DUMMY_POSITION);
   static final MethodDecl ARRAY_LENGTH_DECL = new MethodDecl(new FieldDecl(false, false, BaseTypes.INT, "length", SourcePosition.DUMMY_POSITION, -1),
                                                   new ParameterDeclList(), new StatementList(), null, SourcePosition.DUMMY_POSITION);
}

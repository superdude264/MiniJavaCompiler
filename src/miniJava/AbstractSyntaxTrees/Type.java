/**
 * miniJava Abstract Syntax Tree classes
 * 
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class Type extends AST {

   public TypeKind typeKind;

   public Type(TypeKind typ, SourcePosition posn) {
      super(posn);
      typeKind = typ;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof Type)) {
         return false;
      }

      Type other = (Type) obj;
      // 'Error' equals any type.
      if (typeKind == TypeKind.ERROR || other.typeKind == TypeKind.ERROR) {
         return true;
      }
      // 'Unsupported' is not equal to any type.
      if (typeKind == TypeKind.UNSUPPORTED || other.typeKind == TypeKind.UNSUPPORTED) {
         return false;
      }
      // Regular check.
      if (typeKind != other.typeKind) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return TypeKind.values()[typeKind.ordinal()].toString().toLowerCase();
   }

}

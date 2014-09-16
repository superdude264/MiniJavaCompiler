/**
 * miniJava Abstract Syntax Tree classes
 * 
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends Type {

   public Type eltType;

   public ArrayType(Type eltType, SourcePosition posn) {
      super(TypeKind.ARRAY, posn);
      this.eltType = eltType;
   }

   public <A, R> R visit(Visitor<A, R> v, A o) {
      return v.visitArrayType(this, o);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (!super.equals(obj)) {
         return false;
      }
      if (!(obj instanceof ArrayType)) {
         return false;
      }
      ArrayType other = (ArrayType) obj;
      if (eltType == null) {
         if (other.eltType != null) {
            return false;
         }
      }
      else if (!eltType.equals(other.eltType)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return eltType + "[]";
   }

}

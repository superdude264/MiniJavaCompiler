/**
 * miniJava Abstract Syntax Tree classes
 * 
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassType extends Type {

   public Identifier className;

   public ClassType(Identifier cn, SourcePosition posn) {
      super(TypeKind.CLASS, posn);
      className = cn;
   }

   public <A, R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassType(this, o);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (!super.equals(obj)) {
         return false;
      }
      if (!(obj instanceof ClassType)) {
         return false;
      }
      ClassType other = (ClassType) obj;
      if (className == null) {
         if (other.className != null) {
            return false;
         }
      }
      // Assume all class names have an existing spelling.
      else if (!className.spelling.equals(other.className.spelling)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return className.spelling;
   }

}

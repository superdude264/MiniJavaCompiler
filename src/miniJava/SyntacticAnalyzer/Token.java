package miniJava.SyntacticAnalyzer;

public class Token {

   public TokenKind      kind;
   public String         spelling;
   public SourcePosition position;

   protected Token(TokenKind kind, String value, SourcePosition position) {
      this.kind = kind;
      this.spelling = value;
      this.position = position;
   }

   public String errorString() {
      return String.format("%s ('%s') [line %s]", kind, spelling, position.start);
   }

   @Override
   public String toString() {
      return String.format("Token:[kind='%s', spelling='%s' (position='%s')]", kind, spelling, position);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((kind == null) ? 0 : kind.hashCode());
      result = prime * result + ((spelling == null) ? 0 : spelling.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      Token other = (Token) obj;
      if (kind != other.kind) {
         return false;
      }
      if (spelling == null) {
         if (other.spelling != null) {
            return false;
         }
      }
      else if (!spelling.equals(other.spelling)) {
         return false;
      }
      return true;
   }

}

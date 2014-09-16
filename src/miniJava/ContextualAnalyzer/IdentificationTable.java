package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MethodDecl;

public class IdentificationTable {

   @SuppressWarnings("unused")
   private final class ScopeLevel {

      private static final int ROOT         = 0;
      private static final int CLASS_NAMES  = 1;
      private static final int MEMBER_NAMES = 2;
      private static final int PARAM_NAMES  = 3;
      private static final int LOCALS       = 4;
   }

   private Stack<Map<String, Declaration>> scopeStack;
   private int                             currentLevelIndex;

   public IdentificationTable() {
      scopeStack = new Stack<Map<String, Declaration>>();
      Map<String, Declaration> level0 = new HashMap<String, Declaration>();
      scopeStack.push(level0);
      currentLevelIndex = 0;
   }

   public void openScope() {
      Map<String, Declaration> newLevel = new HashMap<String, Declaration>();
      scopeStack.push(newLevel);
      currentLevelIndex++;
   }

   public void closeScope() throws IdentifierError {
      if (currentLevelIndex <= ScopeLevel.ROOT) {
         throw new IdentifierError("Attempted to close scope at root scope level.");
      }
      scopeStack.pop();
      currentLevelIndex--;
   }

   public void put(String key, Declaration value) throws DuplicateIdentifierError {
      Map<String, Declaration> currentLevel = scopeStack.peek();
      // If already in current level.
      if (currentLevel.containsKey(key)) {
         throw new DuplicateIdentifierError("Identifier '" + key + "' is already in scope.");
      }

      // If illegal shadow (scope level of LOCALS or higher trying to shadow an id at PARAM_NAMES or higher).
      else if (currentLevelIndex >= ScopeLevel.LOCALS) {
         // Check from below the current level (size-2) through param names level.
         for (int i = scopeStack.size() - 2; i >= ScopeLevel.PARAM_NAMES; i--) {
            Map<String, Declaration> scopeLevel = scopeStack.elementAt(i);
            if (scopeLevel.containsKey(key)) {
               throw new DuplicateIdentifierError("Identifier '" + key + "' is illegally shadowing another identifier.");
            }
         }
      }

      currentLevel.put(key, value);
   }

   public Declaration get(String key) throws UndefinedIdentifierError {
      return get(key, false);
   }

   public Declaration get(String key, boolean skipMethods) throws UndefinedIdentifierError {
      for (int i = scopeStack.size() - 1; i >= 0; i--) {
         Map<String, Declaration> scopeLevel = scopeStack.elementAt(i);
         if (scopeLevel.containsKey(key)) {
            Declaration decl = scopeLevel.get(key);
            if (skipMethods && decl instanceof MethodDecl) {
               continue;
            }
            return scopeLevel.get(key);
         }
      }
      throw new UndefinedIdentifierError("Identifier '" + key + "' is undefined.");
   }

}

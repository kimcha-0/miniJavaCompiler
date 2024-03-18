/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class Identifier extends Terminal {

  private Declaration decl;

  public Identifier (Token t) {
    super (t);
  }

  public Identifier (Token t, Declaration decl) {
    super (t);
    this.decl = decl;
  }

  public Declaration getDecl() {
    return this.decl;
  }

  public void setDecl(Declaration newDecl) {
    this.decl = newDecl;
  }
  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIdentifier(this, o);
  }

}

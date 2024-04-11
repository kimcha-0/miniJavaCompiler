/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ClassDecl extends Declaration {

    public List<FieldDecl> toInitialize;

    public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, new ClassType(new Identifier(new Token(TokenType.CLASS, cn, null), null), null), posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
      toInitialize = new ArrayList<>();
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }


  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
}

/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.SyntacticAnalyzer.SourcePosition;

import java.util.ArrayList;

public class MethodDecl extends MemberDecl {

    public ClassDecl inClass;

    public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, SourcePosition posn){
    super(md,posn);
    parameterDeclList = pl;
    statementList = sl;
    patchList = new ArrayList<>();
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodDecl(this, o);
    }
	
	public ParameterDeclList parameterDeclList;
	public StatementList statementList;
    public ArrayList<Instruction> patchList;
}

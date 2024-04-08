/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class MemberDecl extends Declaration {

    public MemberDecl(boolean isPrivate, boolean isStatic, TypeDenoter mt, String name, SourcePosition posn) {
        super(name, mt, posn);
        this.isPrivate = isPrivate;
        this.isStatic = isStatic;
        this.classContext = null;
    }
    
    public MemberDecl(MemberDecl md, SourcePosition posn){
    	super(md.name, md.type, posn);
    	this.isPrivate = md.isPrivate;
    	this.isStatic = md.isStatic;
        this.classContext = md.classContext;
    }

    @Override
    public String toString() {
        if (classContext != null) {
            return this.name + " context: " + classContext.name;
        }
        return this.name;
    }

    public boolean isPrivate;
    public boolean isStatic;
    public ClassDecl classContext;
}

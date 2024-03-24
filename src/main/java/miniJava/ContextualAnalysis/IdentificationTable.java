package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class IdentificationTable {
    private Stack<Map<String, Declaration>> tables;
    public ErrorReporter reporter;

    public IdentificationTable(ErrorReporter reporter) {
        this.reporter = reporter;
        this.tables = new Stack<>();
        /// Predefined names. Yes, this is hacky.
        // String
        openScope();
        enter(new ClassDecl(
                "String",
                new FieldDeclList(),
                new MethodDeclList(),
                null));

        // _PrintStream
        enter(new ClassDecl(
                "_PrintStream",
                new FieldDeclList(),
                new MethodDeclList(
                        new MethodDecl(
                                new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null),
                                new ParameterDeclList(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null)),
                                new StatementList(),
                                null)
                ),
                null));
        Token pStreamToken = new Token(TokenType.IDENTIFIER, "_PrintStream", null);
        Identifier pStreamIden = new Identifier(pStreamToken);

        enter(new ClassDecl(
                "System",
                new FieldDeclList(
                        new FieldDecl(
                                false,
                                true, new ClassType(new Identifier(pStreamToken, retrieve(pStreamIden, null)), null),
                                "out",
                                null)
                ),
                new MethodDeclList(
                        new MethodDecl(
                                new FieldDecl(false, true, new BaseType(TypeKind.VOID, null), "exit", null),
                                new ParameterDeclList(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null)),
                                new StatementList(),
                                null)
                ),
                null));
    }

    public ErrorReporter getReporter() {
        return this.reporter;
    }

    public void enter(Declaration decl) {
        Declaration context;
        if (this.tables.peek().containsKey(decl.name)) {
            boolean isSame;
            boolean isConflict = false;
            context = tables.peek().get(decl.name);
            if (decl instanceof MethodDecl) {
                if (decl instanceof ConflictingMethodDecl) {
                    ConflictingMethodDecl conflictMD = (ConflictingMethodDecl) decl;
                    for (int i = 0; i < conflictMD.conflictingDeclarations.size(); i++) {
                        MethodDecl possibleConflict = conflictMD.conflictingDeclarations.get(i);
                        if (possibleConflict.parameterDeclList.size() == ((MethodDecl)decl).parameterDeclList.size()) {
                            isSame = true;
                            for (int j = 0; j < possibleConflict.parameterDeclList.size(); j++) {
                                if (!possibleConflict.parameterDeclList.get(j).type.equals(((MethodDecl)decl).parameterDeclList.get(j).type)) {
                                    isSame = false;
                                    break;
                                }
                            }
                            if (isSame) {
                                isConflict = true;
                                idError("Attempt to declare method " + decl.name + " which conflicts with the declaration of " + possibleConflict.name);
                                break;
                            }
                        }
                        if (!isConflict) {
                            ConflictingMethodDecl cd = new ConflictingMethodDecl(decl.name);
                            cd.addConflictingDecl((MethodDecl)context);
                            cd.addConflictingDecl((MethodDecl)decl);
                            this.tables.peek().put(cd.name, cd);
                        }
                    }
                } else if (decl instanceof MethodDecl) {
                    // have to check if paramter count, return type are the same
                    // if so, this is an idError

                    MethodDecl mdContext = (MethodDecl)context;
                    if (mdContext.parameterDeclList.size() == ((MethodDecl) decl).parameterDeclList.size()) {
                        isSame = true;
                        for (int i = 0; i < mdContext.parameterDeclList.size(); i++) {
                            if (!mdContext.parameterDeclList.get(i).type.equals(((MethodDecl)decl).parameterDeclList.get(i).type)) {
                                isSame = false;
                                break;
                            }
                        }
                        if (isSame) {
                            idError("Attempt to declare method " + decl.name + " which conflicts with the declaration of " + mdContext.name);
                            isConflict = true;
                        }
                    }
                    if (!isConflict) {
                        ConflictingMethodDecl cd = new ConflictingMethodDecl(decl.name);
                        cd.addConflictingDecl(mdContext);
                        cd.addConflictingDecl((MethodDecl)decl);
                        this.tables.peek().put(cd.name, cd);
                    }
                }
            } else {
                idError("Attempt to declare " + decl.name + " conflicts with the declaration of " + context.name);
            }
        } else {
            tables.peek().put(decl.name, decl);
        }
    }

    public void openScope() {
        this.tables.push(new HashMap<>());
    }

    public void closeScope() {
        this.tables.pop();
    }


    public Declaration retrieve(Identifier iden, MethodDecl methodDecl) {
        // return the attribute associated with the identifier
        // If there are both global and local entires for `iden`, return the local entry
        Declaration ret = null;
        for (int i = tables.size() - 1; i >= 0; i--) {
            Declaration candidate = tables.get(i).get(iden.spelling);
            if (candidate != null) {
                // System.out.println(candidate.name);
                ret = candidate;
            }
        }
        if (ret == null) {
            idError("Attempts to reference: " + iden.spelling + " which was not found!");
            return null;
        }
        return ret;
    }

    public void idError(String error) {
        this.reporter.reportError("Identification error: " + error);
    }
}

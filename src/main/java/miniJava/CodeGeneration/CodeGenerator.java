package miniJava.CodeGeneration;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

public class CodeGenerator implements Visitor<Object, Object> {
    private ErrorReporter _errors;
    private InstructionList _asm; // our list of instructions that are used to make the code section
    private int bssOffset;
    private boolean hasMainMethod;
    private int mainMethodAddr;

    public CodeGenerator(ErrorReporter errors, AST ast) {
        this._errors = errors;
        hasMainMethod = false;
        parse((Package) ast);
    }

    public void parse(Package prog) {
        _asm = new InstructionList();
        // If you haven't refactored the name "R" to something like "R",
        //  go ahead and do that now. You'll be needing that object a lot.
        // Here is some example code.

        // Simple operations:
        // _asm.add( new Push(0) ); // push the value zero onto the stack
        // _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX

        // Fancier operations:
        // _asm.add( new Cmp(new R(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
        // _asm.add( new Cmp(new R(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
        // _asm.add( new Add(new R(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx

        // Thus:
        // new R( ... ) where the "..." can be:
        //  RegRM, RegR						== rm, r
        //  RegRM, int, RegR				== [rm+int], r
        //  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
        // Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
        //
        // Note there are constructors for R where RegR is skipped.
        // This is usually used by instructions that only need one register operand, and often have an immediate
        //   So they actually will set RegR for us when we create the instruction. An example is:
        // _asm.add( new Mov_rmi(new R(Reg64.RDX,true), 3) ); // mov rdx,3
        //   In that last example, we had to pass in a "true" to indicate whether the passed register
        //    is the operand RM or R, in this case, true means RM
        //  Similarly:
        // _asm.add( new Push(new R(Reg64.RBP,16)) );
        //   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed

        // Patching example:
        // Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
        // _asm.add( someJump ); // populate listIdx and startAPddress for the instruction
        // ...
        // ... visit some code that probably uses _asm.add
        // ...
        // patch method 1: calculate the offset yourself
        //     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
        // -=-=-=-
        // patch method 2: let the jmp calculate the offset
        //  Note the false means that it is a 32-bit immediate for jumping (an int)
//             _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
        prog.visit(this, null);
//        _asm.add( new Push(90));
//        makePrintln();
        makeExit();
        if (!this.hasMainMethod)
            reportCodeGenError("main method not found for this program");

        // Output the file "a.out" if no errors
        if (!_errors.hasErrors())
            makeElf("a.out");
    }

    private void reportCodeGenError(String msg) {
        _errors.reportError("Code Generation Error: " + msg);
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        // TODO: visit relevant parts of our AST
        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        RuntimeEntity classRT = new RuntimeEntity();
        cd.fieldDeclList.forEach(fd -> fd.visit(this, classRT));
        cd.methodDeclList.forEach(md -> md.visit(this, classRT));
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        // TODO
        RuntimeEntity objRT = (RuntimeEntity) arg;
        RuntimeEntity fieldRT = new RuntimeEntity();
        TypeKind fdType = (TypeKind) fd.type.visit(this, null);
        switch (fdType) {
            case INT:
                fieldRT.size = 4;
                break;
            case BOOLEAN:
                fieldRT.size = 1;
                break;
            default:
                fieldRT.size = 8;
                break;
        }
        if (fd.isStatic) {
            // store at beginning of stack
            fieldRT.offset = this.bssOffset;
            this.bssOffset += fieldRT.size;
            _asm.add(new Push(0));
        } else {
            fieldRT.offset = fieldRT.size + objRT.size;
            objRT.size += fieldRT.size;
        }
        fd.runtimeEntity = fieldRT;
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        checkMainMethod(md);
        // TODO: manage methodRT
        RuntimeEntity classRT = (RuntimeEntity) arg;
        // base is first instruction of method
        int methodStartAddr = _asm.getSize();
        RuntimeEntity methodRT = new RuntimeEntity();
        methodRT.offset = methodStartAddr;
        md.runtimeEntity = methodRT;
        // TODO: patching method jump location
        md.patchList.forEach(patch -> {
            _asm.patch(patch.listIdx, new Jmp(methodStartAddr));
        });
        // allocate space for local variables
        // method prologue
        // save caller rbp
        _asm.add(new Push(Reg64.RBP));
        // set rsp to rbp
        _asm.add(new Mov_rmr(new R(Reg64.RBP, Reg64.RSP)));
        md.parameterDeclList.forEach(pd -> pd.visit(this, methodRT));
        md.statementList.forEach(stmt -> stmt.visit(this, methodRT));
        // return to caller
        // clean up stack space
        _asm.add(new Mov_rmr(new R(Reg64.RSP, Reg64.RBP)));
        // return rbp to caller
        _asm.add(new Pop(Reg64.RBP));
        if (!md.name.equals("main")) {
            _asm.add(new Ret());
        }
        return null;
    }

    private void checkMainMethod(MethodDecl md) {
        if (md.name.equals("main") && md.isStatic && md.parameterDeclList.size() == 1) {
            ParameterDecl mainParam = md.parameterDeclList.get(0);
            if (mainParam.name.equals("args") && mainParam.type.typeKind == TypeKind.ARRAY) {
                this.mainMethodAddr = _asm.getSize();
                this.hasMainMethod = true;
            }
        }
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        RuntimeEntity methodRT = (RuntimeEntity) arg;
        RuntimeEntity paramRT = new RuntimeEntity();
        paramRT.size = 8;
        paramRT.offset = paramRT.getSize() + methodRT.getSize();
        methodRT.size += paramRT.size;
        pd.runtimeEntity = paramRT;
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        RuntimeEntity methodRT = (RuntimeEntity) arg;
        // handle class Object case
        // base should be rbp of originating method
        RuntimeEntity varRT = new RuntimeEntity();
        TypeKind varType = (TypeKind) decl.type.visit(this, null);
        switch (varType) {
            case INT:
                varRT.size = 4;
                break;
            case BOOLEAN:
                varRT.size = 1;
                break;
            default:
                varRT.size = 8;
                break;
        }

        varRT.offset = -varRT.size - methodRT.size;
        methodRT.size += varRT.size;
        decl.runtimeEntity = varRT;
        // push space on stack
        _asm.add(new Push(0));
        return null;
    }

    @Override
    public TypeKind visitBaseType(BaseType type, Object arg) {

        return type.typeKind;
    }

    @Override
    public TypeKind visitClassType(ClassType type, Object arg) {
        return TypeKind.CLASS;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        return TypeKind.ARRAY;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        stmt.sl.forEach(s -> s.visit(this, arg));
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        // push space on stack and inits runtimeEntity
        stmt.varDecl.visit(this, arg);
        // push val
        stmt.initExp.visit(this, arg);
        // pop rax = val;
        _asm.add(new Pop(Reg64.RAX));
        // mov [rbp - offset], rax; store val in varDecl
        _asm.add(new Mov_rmr(new R(Reg64.RBP, stmt.varDecl.runtimeEntity.offset, Reg64.RAX)));
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        // push val
        stmt.val.visit(this, null);
        // pop val; rax := val
        _asm.add(new Pop(Reg64.RAX));
        // mov [rbp - offset], rax; stores val in ref
        _asm.add(new Mov_rmr(new R(Reg64.RBP, stmt.ref.decl.runtimeEntity.offset, Reg64.RAX)));
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        // arr[index] = val;
        // push refAddr
        stmt.ref.visit(this, null);
        int offset = stmt.ref.decl.runtimeEntity.offset;
        // push ix
        stmt.ix.visit(this, null);
        // push val
        stmt.exp.visit(this, null);
        // pop rdx; rdx = val
        _asm.add(new Pop(Reg64.RDX));
        // pop rcx; rcx = ix
        _asm.add(new Pop(Reg64.RCX));
        // rax := rbp - offset (address of arr)
        _asm.add(new Mov_rrm(new R(Reg64.RBP, offset, Reg64.RAX)));
        _asm.add(new Mov_rmr(new R(Reg64.RAX, Reg64.RCX, 4, 0, Reg64.RDX)));
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        for (int i = stmt.argList.size() - 1; i > -1; i--) {
            // push value of argument onto stack in reverse order
            Expression a = stmt.argList.get(i);
            a.visit(this, null);
        }
        if (stmt.methodRef.decl.name.equals("println")) {
            makePrintln();
        }
        // if method has not been visited yet, patch
        else if (stmt.methodRef.decl.runtimeEntity == null) {
            ((MethodDecl)stmt.methodRef.decl).patchList.add(new Call(0));
        } else {
            // call method
            _asm.add(new Call(stmt.methodRef.decl.runtimeEntity.offset) );
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        // push val
        stmt.returnExpr.visit(this, null);
        // pop rax
        _asm.add(new Pop(Reg64.RAX));
        // return
        _asm.add(new Ret());
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        // evaluate condition and push result
        stmt.cond.visit(this, null);
        // pop rax
        _asm.add(new Pop(Reg64.RAX));
        // cmp rax, 0
        _asm.add(new Cmp(new R(Reg64.RAX, 0)));
        // jz else
        Jmp elseJump = new Jmp(0, 0, false);
        _asm.add(elseJump);
        // visit then
        stmt.thenStmt.visit(this, null);
        // patch else jump
        _asm.patch(elseJump.listIdx, new Jmp(_asm.getSize(), elseJump.startAddress, false));
        // visit else
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        // push condition
        int startAddr = _asm.getSize();
        stmt.cond.visit(this, null);
        // pop rax
        _asm.add( new Pop(Reg64.RAX) );
        // cmp rax, 0
        _asm.add( new Cmp(new R(Reg64.RAX, 0)) );
        // jz end
        Jmp endJump = new Jmp(0, 0, false);
        _asm.add( endJump );
        // visit body
        stmt.body.visit(this, null);
        // patch end jump
        _asm.patch( endJump.listIdx, new Jmp(_asm.getSize(), endJump.startAddress, false) );
        // jmp start
        _asm.add( new Jmp(startAddr) );

        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        // -int
        // !boolean
        // push val
        expr.expr.visit(this, null);
        // value of expression stored at rsp
        if (expr.operator.spelling.equals("-")) {
            // pop rax
            _asm.add(new Pop(Reg64.RAX));
            // neg rax
            _asm.add(new Neg(new R(Reg64.RAX, false)));
            // push rax
            _asm.add(new Push(Reg64.RAX));
        } else {
            // pop rax
            _asm.add(new Pop(Reg64.RAX));
            // not rax
            _asm.add(new Not(new R(Reg64.RAX, false)));
            // push rax
            _asm.add(new Push(Reg64.RAX));
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        // push val
        expr.left.visit(this, null);
        // push val
        expr.right.visit(this, null);
        // pop right
        _asm.add(new Pop(Reg64.RCX));
        // pop left
        _asm.add(new Pop(Reg64.RAX));
        R rightLeftVal = new R(Reg64.RAX, Reg64.RCX);
        switch (expr.operator.spelling) {
            case "+":
                _asm.add(new Add(rightLeftVal));
                break;
            case "-":
                _asm.add(new Sub(rightLeftVal));
                break;
            case "*":
                _asm.add(new Imul(new R(Reg64.RCX, true)));
                break;
            case "/":
                _asm.add(new Xor(new R(Reg64.RDX, Reg64.RDX)));
                _asm.add(new Idiv(new R(Reg64.RCX, true)));
                break;
            case "&&":
                _asm.add(new Add(rightLeftVal));
                break;
            case "||":
                _asm.add(new Or(rightLeftVal));
                break;
            case "==":
                _asm.add(new Cmp(rightLeftVal));
                break;
        }
        _asm.add(new Push(Reg64.RAX));
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        RuntimeEntity refRT = (RuntimeEntity) expr.ref.visit(this, null);
        // push ref value
        _asm.add( new Push(new R(Reg64.RBP, expr.ref.decl.runtimeEntity.offset)) );
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        // ref[ix]
        // push refAddr
        expr.ref.visit(this, null);
        // push ix
        expr.ixExpr.visit(this, null);
        // pop ix
        _asm.add(new Pop(Reg64.RCX));
        // pop refAddr
        _asm.add(new Pop(Reg64.RAX));
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        // patch if method has not been visited yet
        // push all arguments in reverse order
        for (int i = expr.argList.size() - 1; i > -1; i--) {
            Expression param = expr.argList.get(i);
            param.visit(this, null);
        }
        if (expr.functionRef.decl.runtimeEntity == null) {
            ((MethodDecl)expr.functionRef.decl).patchList.add(new Call(0));
        } else {
            // call method
            _asm.add(new Call(expr.functionRef.decl.runtimeEntity.offset));
        }
        for (int i = 0; i < expr.argList.size(); i++) {
            // pop all arguments to clean up stack
            _asm.add(new Pop(Reg64.RAX));
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        // push literal value
        int literal = (int) expr.lit.visit(this, null);
        _asm.add(new Push(literal));
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        makeMalloc();
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        RuntimeEntity idRT = (RuntimeEntity) ref.id.visit(this, null);
        return idRT;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        // push refAddr
        RuntimeEntity refRT = ref.id.decl.runtimeEntity;
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return id.decl.runtimeEntity;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        String val = num.spelling;
        return Integer.parseInt(val);
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return bool.spelling.equals("true") ? 1 : 0;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nil, Object arg) {
        return 0;
    }

    public void makeElf(String fname) {
        ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
        elf.outputELF(fname, _asm.getBytes(), this.mainMethodAddr); // TODO: set the location of the main method
    }

    private int makeMalloc() {
        // void *mmap(int addr, size_t len, int prot, int flags, int fd, off_t offset);
        int idxStart = _asm.add(new Mov_rmi(new R(Reg64.RAX, true), 0x09)); // mmap

        _asm.add(new Xor(new R(Reg64.RDI, Reg64.RDI))); // addr=0
        _asm.add(new Mov_rmi(new R(Reg64.RSI, true), 0x1000)); // 4kb alloc
        _asm.add(new Mov_rmi(new R(Reg64.RDX, true), 0x03)); // prot read|write
        _asm.add(new Mov_rmi(new R(Reg64.R10, true), 0x22)); // flags= private, anonymous
        _asm.add(new Mov_rmi(new R(Reg64.R8, true), -1)); // fd= -1
        _asm.add(new Xor(new R(Reg64.R9, Reg64.R9))); // offset=0
        _asm.add(new Syscall());

        // pointer to newly allocated memory is in RAX

        // return the index of the first instruction in this method, if needed
        return idxStart;
    }

    private int makeExit() {
        // exit(0)
        int idxStart = _asm.add(new Mov_rmi(new R(Reg64.RAX, true), 0x3C));
        _asm.add(new Xor(new R(Reg64.RDI, Reg64.RDI))); // error_code = 0
        _asm.add(new Syscall());
        return idxStart;
    }

    private int makePrintln() {
        // TODO: how can we generate the assembly to println?
        // write(int fildes, const void *buf, size_t nbyte)
        int idxStart = _asm.add(new Mov_rmi(new R(Reg64.RAX, true), 0x1));
        // fidles = 1;
        _asm.add(new Mov_rmi(new R(Reg64.RDI, true), 0x1));
        // *buf = val;
        _asm.add(new Mov_rmr(new R(Reg64.RSI, Reg64.RSP)));
        _asm.add(new Pop(Reg64.RCX));
        // nbyte = 4;
        _asm.add(new Mov_rmi(new R(Reg64.RDX, true), 0x1));
        _asm.add(new Syscall());
        // return start index of first instruction in write() procedure
        return idxStart;
    }
}

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
    boolean booleanExpr;
    private boolean hasMainMethod;
    private int numMainMethods;
    private int mainMethodAddr;

    public CodeGenerator(ErrorReporter errors, AST ast) {
        this._errors = errors;
        hasMainMethod = false;
        parse((Package) ast);
        this.numMainMethods = 0;
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
        _asm.markOutputStart();
        prog.visit(this, null);
//        _asm.add( new Push(90));
//        makePrintln();
        makeExit();
        _asm.outputFromMark();
        if (!this.hasMainMethod || this.numMainMethods > 1)
            reportCodeGenError("main method error");

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
        prog.classDeclList.forEach(cd -> cd.visit(this, true));
        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        RuntimeEntity classRT = new RuntimeEntity();
        if (arg != null) {
            cd.fieldDeclList.forEach(fd -> fd.visit(this, classRT));
        } else {
            cd.methodDeclList.forEach(md -> md.visit(this, classRT));
        }
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
        methodRT.size = 0;
        methodRT.paramBase = 0;
        md.runtimeEntity = methodRT;
        // TODO: patching method jump location
        md.patchList.forEach(patch -> {
            _asm.patch(patch.listIdx, new Call(patch.startAddress, methodStartAddr));
        });
        // allocate space for local variables
        // method prologue
        // save caller rbp
        _asm.add(new Push(Reg64.RBP));
        // set rbp to rbp
        _asm.add(new Mov_rmr(new R(Reg64.RBP, Reg64.RSP)));
        md.parameterDeclList.forEach(pd -> pd.visit(this, methodRT));
        md.statementList.forEach(stmt -> stmt.visit(this, methodRT));
        // return rbp to caller
        _asm.add(new Mov_rmr(new R(Reg64.RSP, Reg64.RBP)));
        _asm.add(new Pop(Reg64.RBP));
        return null;
    }

    private void checkMainMethod(MethodDecl md) {
        if (md.name.equals("main") && md.isStatic && md.parameterDeclList.size() == 1) {
            ParameterDecl mainParam = md.parameterDeclList.get(0);
            if (mainParam.name.equals("args") && mainParam.type.typeKind == TypeKind.ARRAY
                    && !md.isPrivate && ((ClassType) ((ArrayType) mainParam.type).eltType).className.spelling.equals("String")) {
                this.mainMethodAddr = _asm.getSize();
                this.hasMainMethod = true;
                this.numMainMethods += 1;
            }
        }
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        RuntimeEntity methodRT = (RuntimeEntity) arg;
        RuntimeEntity paramRT = new RuntimeEntity();
        paramRT.size = 8;
        paramRT.offset = paramRT.size + methodRT.paramBase;
        methodRT.paramBase += paramRT.size;
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
//        switch (varType) {
//            case INT:
//                varRT.size = 4;
//                break;
//            case BOOLEAN:
//                varRT.size = 1;
//                break;
//            default:
//                varRT.size = 8;
//                break;
//        }
        varRT.size = 8;
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
        stmt.initExp.visit(this, null);
        _asm.add(new Pop(Reg64.RAX));
        // pop rax = val;
        // mov [rbp - offset], rax; store val in varDecl
        // pop offset
        _asm.add(new Mov_rmr(new R(Reg64.RBP, stmt.varDecl.runtimeEntity.offset, Reg64.RAX)));
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        // visit reference. This visit should allow us to access the memory location of the reference
        // pushes reference value or effective address
        stmt.ref.visit(this, true);
        // expression visit
        stmt.val.visit(this, null);
        // pop the value off the stack
        _asm.add(new Pop(Reg64.RAX));
        _asm.add(new Pop(Reg64.RDX));
        _asm.add(new Mov_rmr(new R(Reg64.RDX,0, Reg64.RAX)));
        // store value at memory address of reference
//        if (stmt.ref.decl instanceof FieldDecl) {
//        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        // arr[index] = val;
        // push refAddr

        int offset = stmt.ref.decl.runtimeEntity.offset;
        // push refAddr
        stmt.ref.visit(this, true);
        // push ix
        stmt.ix.visit(this, null);
        // push val
        stmt.exp.visit(this, null);
        // pop rdx; rdx = val
        _asm.add(new Pop(Reg64.RDX));
        // pop rcx; rcx = ix
        _asm.add(new Pop(Reg64.RCX));
        _asm.add(new Pop(Reg64.RAX));
        _asm.add( new Mov_rmr(new R(Reg64.RAX, Reg64.RCX, 4, 0, Reg64.RDX)));
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
            Call callInstruction = new Call(_asm.getSize(), 0);
            _asm.add(callInstruction);
            ((MethodDecl) stmt.methodRef.decl).patchList.add(callInstruction);
        } else {
            // call method
            _asm.add(new Call(_asm.getSize(), stmt.methodRef.decl.runtimeEntity.offset));
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
        // ZF flag set
        stmt.cond.visit(this, null);

        // pop rax
        _asm.add(new Pop(Reg64.RAX));
        _asm.add(new Cmp(new R(Reg64.RAX, true), 1));
        int elseAddr = _asm.getSize();
        CondJmp elseJump = new CondJmp(Condition.NE, elseAddr, 0, false);
        _asm.add(elseJump);

        // visit then
        stmt.thenStmt.visit(this, null);
        int endAddr = _asm.getSize();
        Jmp endJump = new Jmp(endAddr, 0, false);
        _asm.add(endJump);

        // once done with then, skip over else statement
        // patch else jump
        _asm.patch(elseJump.listIdx, new CondJmp(Condition.NE, elseAddr, _asm.getSize(), false));
        // visit else
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
        }
        _asm.patch(endJump.listIdx, new Jmp(endAddr, _asm.getSize(), false));
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        // push condition into RAX/AL register
        int startAddr = _asm.getSize();
        stmt.cond.visit(this, null);
        // pop rax -> 1 if true
        _asm.add(new Pop(Reg64.RAX));
        _asm.add(new Cmp(new R(Reg64.RAX, true), 1));
        // jz end
        int endAddr = _asm.getSize();
        CondJmp endJump = new CondJmp(Condition.NE, endAddr, 0, false);
        _asm.add(endJump);
        // visit body
        stmt.body.visit(this, null);
        // jmp start
        _asm.add(new Jmp(_asm.getSize(), startAddr, false));
        // patch end jump
        _asm.patch(endJump.listIdx, new CondJmp(Condition.NE, endAddr, _asm.getSize(), false));

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
            // pop rax val
            _asm.add(new Pop(Reg64.RAX));
            // neg rax: -val
            _asm.add(new Neg(new R(Reg64.RAX, true)));
            // push rax
            _asm.add(new Push(Reg64.RAX));
        } else {
            // pop rax
            _asm.add(new Pop(Reg64.RAX));
            // not rax
            _asm.add(new Not(new R(Reg64.RAX, true)));
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
                // e
                // set to 1 if equal
                _asm.add(new SetCond(Condition.E, Reg8.AL));
                break;
            case ">":
                // gt
                _asm.add(new Cmp(rightLeftVal));
                _asm.add(new SetCond(Condition.GT, Reg8.AL));
                break;
            case ">=":
                // zf + gt
                _asm.add(new Cmp(rightLeftVal));
                _asm.add(new SetCond(Condition.GTE, Reg8.AL));
                break;
            case "<":
                // lt
                _asm.add(new Cmp(rightLeftVal));
                _asm.add(new SetCond(Condition.LT, Reg8.AL));
                break;
            case "<=":
                // zf + lt
                _asm.add(new Cmp(rightLeftVal));
                _asm.add(new SetCond(Condition.LTE, Reg8.AL));
                break;
        }
        _asm.add(new Push(Reg64.RAX));
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        // retrieve runtime entity of reference
        // RuntimeEntity refRT = (RuntimeEntity) expr.ref.visit(this, null);
        expr.ref.visit(this, null);
        // push [rbp - offset]
//        _asm.add(new Push(new R(Reg64.RBP, expr.ref.decl.runtimeEntity.offset)));
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        // ref[ix]
        // push refAddr
        expr.ref.visit(this, true);
        // push ix
        expr.ixExpr.visit(this, null);
        // pop ix
        _asm.add(new Pop(Reg64.RCX));
        // pop refAddr
        _asm.add(new Pop(Reg64.RAX));
        _asm.add(new Mov_rrm(new R(Reg64.RAX, Reg64.RCX, 4, 0, Reg64.RAX)));
        _asm.add(new Push(Reg64.RAX));
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
        _asm.add(new Push(_asm.getSize()));
        if (expr.functionRef.decl.runtimeEntity == null) {
            Call callInstruction = new Call(_asm.getSize(), 0);
            _asm.add(callInstruction);
            ((MethodDecl) expr.functionRef.decl).patchList.add(callInstruction);
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
        expr.lit.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // new Class();
        makeMalloc();
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        makeMalloc();
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return ref.decl.runtimeEntity;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
//        RuntimeEntity idRT = (RuntimeEntity) ref.id.visit(this, null);
        if (arg != null) {
            if ((boolean)arg)
                // effective address
                _asm.add( new Lea(new R(Reg64.RBP, ref.id.decl.runtimeEntity.offset, Reg64.RAX)));
        } else
            _asm.add(new Mov_rrm(new R(Reg64.RBP, ref.id.decl.runtimeEntity.offset, Reg64.RAX)));
        _asm.add(new Push(Reg64.RAX));
        return ref.id.decl.runtimeEntity;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);
        //Get the field declaration of the current id
        FieldDecl decl = (FieldDecl)ref.id.decl;
        //Get the value of the previous register
        // base case pops ref pushed by refVisit
        _asm.add(new Pop(Reg64.RAX));
        //Add the current
        //_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RBX,true), decl.indexInClass*8));
        //_asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
        if(arg != null)
        {
            _asm.add(new Lea(new R(Reg64.RAX,decl.runtimeEntity.offset, Reg64.RAX)));
        }else{
            _asm.add(new Mov_rrm(new R(Reg64.RAX,decl.runtimeEntity.offset, Reg64.RAX)));
        }
        _asm.add(new Push(Reg64.RAX));
        return null;
//        boolean bool = arg != null;
//
//
//        // location of current class object
//        RuntimeEntity refRT;
//        // [rbp + refRT.offset] is the location of the class object
//        // [rbp + refRT.offset] + ref.id.decl.runtimeEntity.offset is the location of the field
//        if (bool) {
//            _asm.add(new Mov_rrm(new R(Reg64.RBP, ref.ref.decl.runtimeEntity.offset, Reg64.RSI)));
//            refRT = (RuntimeEntity) ref.ref.visit(this, null);
//            RuntimeEntity retRT = new RuntimeEntity();
////        System.out.println("QualRef ref offset " + ref.ref.decl.name + refRT.offset);
////        System.out.println("QualRef id offset " + ref.id.decl.runtimeEntity.offset);
//            // mov rsi, [rbp + refRT.offset]
//            // mov [rsi + ref.id.decl.runtimeEntity.offset], rax
////        _asm.add(new Push(new R(Reg64.RSI, ref.id.decl.runtimeEntity.offset)));
//            retRT.offset = ref.id.decl.runtimeEntity.offset;
//            // mov [rsi + offset], rsi
//            _asm.add(new Mov_rrm(new R(Reg64.RSI, refRT.offset, Reg64.RSI)));
//            // mov [rsi + offset], rsi
//            _asm.add(new Mov_rrm(new R(Reg64.RSI, retRT.offset, Reg64.RSI)));
//            if (bool) {
//                // retrieve address of reference
//                // load object pointer into rsi
//            } else {
//                // just push value on stack
//                _asm.add(new Push(new R(Reg64.RSI, retRT.offset)));
//            }
//            return retRT;
//        }
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
        _asm.add( new Push(Integer.parseInt(val)) );
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        _asm.add( new Push(bool.spelling.equals("true") ? 1 : 0) );
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nil, Object arg) {
        _asm.add( new Push(0));
        return null;
    }

    public void makeElf(String fname) {
        ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
        elf.outputELF(fname, _asm.getBytes(), this.mainMethodAddr); // TODO: set the location of the main method
    }

    private int makeMalloc() {
        System.out.println("malloc");
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
        _asm.add(new Push(Reg64.RAX));

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
        _asm.add(new Push(0));
        _asm.add(new Mov_rmr(new R(Reg64.RSI, Reg64.RSP)));
        _asm.add(new Pop(Reg64.RCX));
        _asm.add(new Syscall());
        // return start index of first instruction in write() procedure
        return idxStart;
    }
}
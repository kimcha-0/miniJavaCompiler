package miniJava.CodeGeneration;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

public class CodeGenerator implements Visitor<Object, Object> {
    private ErrorReporter _errors;
    private InstructionList _asm; // our list of instructions that are used to make the code section
    private boolean hasMainMethod;
    private int mainMethodAddr;

    public CodeGenerator(ErrorReporter errors, AST ast) {
        this._errors = errors;
        hasMainMethod = false;
        parse((Package)ast);
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
        _asm.add( new Mov_rmr(new R(Reg64.RBP, Reg64.RSP)));
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
        RuntimeEntity classRT = new RuntimeEntity(_asm.getSize());
        cd.fieldDeclList.forEach(fd -> fd.visit(this, classRT));
        cd.methodDeclList.forEach(md -> md.visit(this, classRT));
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        // push 0
        RuntimeEntity classRT = (RuntimeEntity)arg;
        RuntimeEntity fieldRT = new RuntimeEntity(classRT.getBase());
        fieldRT.setSize(8);
        // base + currSize + currSize
        classRT.setSize(classRT.getSize() + fieldRT.getSize());
        // offset = base + currSize - base;
        fieldRT.setOffset(classRT.getBase() - classRT.getSize());
        fd.runtimeEntity = fieldRT;
        // create stack space for value
        _asm.add( new Push(0) );
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        // callee
        // create stack space for method
        _asm.add( new Push(0) );
        if (md.name.equals("main") && md.isStatic && md.parameterDeclList.size() == 1) {
            System.out.println("main method gen");
            this.hasMainMethod = true;
            this.mainMethodAddr = _asm.getSize();
        }
        if (md.patchList != null) {
            for (Instruction instruction : md.patchList) {
                instruction.startAddress = _asm.getSize();
            }
        }
        RuntimeEntity classRT = (RuntimeEntity)arg;
        RuntimeEntity methodRT = new RuntimeEntity(_asm.getSize());
        methodRT.setSize(8);
        classRT.setSize(classRT.getSize() + methodRT.getSize());
        methodRT.setOffset(methodRT.getBase() - classRT.getSize());
        md.runtimeEntity = methodRT;
        md.parameterDeclList.forEach(pd -> pd.visit(this, methodRT));
        md.statementList.forEach(stmt -> stmt.visit(this, methodRT));
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        RuntimeEntity methodRT = (RuntimeEntity)arg;
        RuntimeEntity paramRT = new RuntimeEntity(methodRT.getBase());
        paramRT.setSize(8);
        methodRT.setSize(methodRT.getSize() + paramRT.getSize());
        paramRT.setOffset(paramRT.getBase() - methodRT.getSize());

        // create space for pointer to array or class object or primitive
        _asm.add( new Push(0) );
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        RuntimeEntity methodRT = (RuntimeEntity)arg;
        // handle class Object case
        // base should be rbp of originating method
        RuntimeEntity varRT = new RuntimeEntity(methodRT.getBase());
        varRT.setSize(8);
        methodRT.setSize(methodRT.getSize() + varRT.getSize());
        varRT.setOffset(varRT.getBase() - methodRT.getSize());
        decl.runtimeEntity = varRT;
        // push space on stack
        _asm.add( new Push(0) );
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        // visit varDecl -> allocates space for variable push 0
        RuntimeEntity methodRT = (RuntimeEntity)arg;
        stmt.varDecl.visit(this, methodRT);
        // visit expr -> push val
        if (stmt.varDecl.type.typeKind == TypeKind.CLASS) {
            // class type variable; if constructor, call malloc and store pointer to memory on stack
        }
        if (stmt.initExp != null) {
            stmt.initExp.visit(this, methodRT);
            // pop rax; rax := val
            _asm.add( new Pop(Reg64.RAX) );
            // mov rsp, rax; rsp := val
            _asm.add( new Mov_rmr(new R(Reg64.RSP, Reg64.RAX)));
        }
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        // push [ref]
        stmt.ref.visit(this, null);
        // push val
        stmt.val.visit(this, null);
        // pop rcx
        _asm.add( new Pop(Reg64.RCX) );
        // pop rax
        _asm.add( new Pop(Reg64.RAX));
        // mov [rax], rcx
        _asm.add( new Mov_rmr(new R(Reg64.RAX, 0, Reg64.RCX)));
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        // arr[index] = 2;
        // push [ref]
        stmt.ref.visit(this, null);
        // push ix
        stmt.ix.visit(this, null);
        // push val
        stmt.exp.visit(this, null);
        // pop rdx; rdx = val
        _asm.add( new Pop(new R(Reg64.RDX, false)));
        // pop rcx; rcx = ix
        _asm.add( new Pop(new R(Reg64.RCX, false)));
        // pop rax; rax = [ref]
        _asm.add( new Pop(new R(Reg64.RAX, false)));
        // [rax + rcx*4] = rdx
        _asm.add( new Mov_rmr(new R(Reg64.RAX, Reg64.RCX, 4, 0, Reg64.RDX)) );
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        // if method has not been visited yet, patch
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        // -int
        // !boolean
        expr.expr.visit(this, null);
        // value of expression stored at rsp
        if (expr.operator.spelling.equals("-")) {
            // pop rax
            // neg rax
            // push rax
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        // patch if method has not been visited yet
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        // push literal value
        int literal = (int) expr.lit.visit(this, null);
        _asm.add( new Push(literal) );
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
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
        _asm.add( new Push(idRT.getAddress()) );
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
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
        // push bool
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

    private int makePrintln(int val) {
        // TODO: how can we generate the assembly to println?
        // write(int fildes, const void *buf, size_t nbyte)
        int idxStart = _asm.add(new Mov_ri64(Reg64.RAX, 1));
        // fidles = 1;
        _asm.add(new Mov_rmi(new R(Reg64.RDI, true), 0x1));
        // *buf = val;
        _asm.add(new Mov_rmi(new R(Reg64.RSI, true), val));
        // nbyte = 4;
        _asm.add(new Mov_rmi(new R(Reg64.RDX, true), 0x4));
        _asm.add(new Syscall());
        // return start index of first instruction in write() procedure
        return idxStart;
    }
}

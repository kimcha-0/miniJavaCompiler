package miniJava.CodeGeneration;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

public class CodeGen implements Visitor<Object, Object> {
    int numMainMethods;
    int bssOffset;
    private ErrorReporter _errors;
    private InstructionList _asm; // our list of instructions that are used to make the code section
    private long mainMethodAddr;

    public CodeGen(ErrorReporter errors) {
        this.numMainMethods = 0;
        this._errors = errors;
        this.bssOffset = 0;
    }

    public void parse(Package prog) {
        _asm = new InstructionList();

        // If you haven't refactored the name "ModRMSIB" to something like "R",
        //  go ahead and do that now. You'll be needing that object a lot.
        // Here is some example code.

        // Simple operations:
        // _asm.add( new Push(0) ); // push the value zero onto the stack
        // _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX

        // Fancier operations:
        // _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
        // _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
        // _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx

        // Thus:
        // new ModRMSIB( ... ) where the "..." can be:
        //  RegRM, RegR						== rm, r
        //  RegRM, int, RegR				== [rm+int], r
        //  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
        // Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
        //
        // Note there are constructors for ModRMSIB where RegR is skipped.
        // This is usually used by instructions that only need one register operand, and often have an immediate
        //   So they actually will set RegR for us when we create the instruction. An example is:
        // _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
        //   In that last example, we had to pass in a "true" to indicate whether the passed register
        //    is the operand RM or R, in this case, true means RM
        //  Similarly:
        // _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
        //   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed

        // Patching example:
        // Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
        // _asm.add( someJump ); // populate listIdx and startAddress for the instruction
        // ...
        // ... visit some code that probably uses _asm.add
        // ...
        // patch method 1: calculate the offset yourself
        //     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
        // -=-=-=-
        // patch method 2: let the jmp calculate the offset
        //  Note the false means that it is a 32-bit immediate for jumping (an int)
        //     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );

        prog.visit(this,null);
        if (this.numMainMethods != 1)
            this._errors.reportError("too many or no main method found");

        // Output the file "a.out" if no errors
        if( !_errors.hasErrors() )
            makeElf("a.out");
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        // TODO: visit relevant parts of our AST
        // only visit fields in this stage of a class visit in order to initialize offsets
        prog.classDeclList.forEach(cd -> cd.visit(this, true));
        // visit methods
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
        return null;
    }

    private void checkMainMethod(MethodDecl md) {
        if (md.name.equals("main") && md.isStatic && md.parameterDeclList.size() == 1) {
            ParameterDecl mainParam = md.parameterDeclList.get(0);
            if (mainParam.name.equals("args") && mainParam.type.typeKind == TypeKind.ARRAY
                    && !md.isPrivate && ((ClassType) ((ArrayType) mainParam.type).eltType).className.spelling.equals("String")) {
                this.mainMethodAddr = _asm.getSize();
                this.numMainMethods += 1;
            }
        }
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
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
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
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
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
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
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nil, Object arg) {
        return null;
    }

    public void makeElf(String fname) {
        ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
        elf.outputELF(fname, _asm.getBytes(), this.mainMethodAddr); // TODO: set the location of the main method
    }

    private int makeMalloc() {
        int idxStart = _asm.add( new Mov_rmi(new R(Reg64.RAX,true),0x09) ); // mmap

        _asm.add( new Xor(		new R(Reg64.RDI,Reg64.RDI)) 	); // addr=0
        _asm.add( new Mov_rmi(	new R(Reg64.RSI,true),0x1000) ); // 4kb alloc
        _asm.add( new Mov_rmi(	new R(Reg64.RDX,true),0x03) 	); // prot read|write
        _asm.add( new Mov_rmi(	new R(Reg64.R10,true),0x22) 	); // flags= private, anonymous
        _asm.add( new Mov_rmi(	new R(Reg64.R8, true),-1) 	); // fd= -1
        _asm.add( new Xor(		new R(Reg64.R9,Reg64.R9)) 	); // offset=0
        _asm.add( new Syscall() );

        // pointer to newly allocated memory is in RAX
        // return the index of the first instruction in this method, if needed
        return idxStart;
    }

    private int makePrintln() {
        // TODO: how can we generate the assembly to println?
        return -1;
    }
}
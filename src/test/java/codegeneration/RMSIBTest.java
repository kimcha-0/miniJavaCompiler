package codegeneration;

import miniJava.CodeGeneration.x64.ISA.*;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.InstructionList;
import miniJava.CodeGeneration.x64.R;
import miniJava.CodeGeneration.x64.Reg64;
import org.junit.jupiter.api.Test;

import static miniJava.CodeGeneration.x64.Reg64.RegFromIdx;

public class RMSIBTest {

    @Test
    void instructions() {
        InstructionList instructionList = new InstructionList();
        Instruction push = new Push(500);
        Instruction pop = new Pop(Reg64.R9);
        Instruction movri64 = new Mov_ri64(Reg64.RAX, 0xFF0000000000000FL);
        Instruction ret = new Ret((short) 2);
        printHexString(push.getBytes());
        printHexString(ret.getBytes());
        printHexString(pop.getBytes());
        printHexString(movri64.getBytes());
    }
    @Test
    void makeRMR() {
        // rm, r
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Reg64 rm = (Reg64) RegFromIdx(j, false);
                Reg64 r = (Reg64) RegFromIdx(i, false);
                R reg = new R(r, rm);
                byte[] regEncoding = getEncoding(reg);
                System.out.println("r: " + r.toString() + ", rm: " + rm.toString() + " " + getHexString(regEncoding[0]));
            }
        }
    }

    @Test
    void makeRDisp() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 8; j++) {
                for (int k = 0; k < 8; k++) {
                    Reg64 r = (Reg64) RegFromIdx(j, false);
                    Reg64 rm = (Reg64) RegFromIdx(k, false);
                    int disp = i == 2 ? 1 << 8 : i == 1 ? i << 1 : 0;
                    R reg = new R(rm, disp, r);
                    byte[] regEncoding = getEncoding(reg);
                    System.out.println("r: " + r.toString() + ", rm: " + rm.toString() + ", disp: " + disp + " "
                            + getHexString(regEncoding[0]));
                }
            }
        }
    }

    @Test
    void mov() {
        // mov r11, [r10 + r8*8+2222]
        R r = new R(Reg64.R10, Reg64.R8, 8, 2222, Reg64.R11);
        Instruction rrmMov = new Mov_rrm(r);
        byte[] instructionEncoding = rrmMov.getBytes();
//        System.out.println(Arrays.toString(rrmMov.getBytes()));
        printHexString(instructionEncoding);

    }

    @Test
    void makeSIB() {
    }

    String getHexString(int n) {
        return String.format("0x%016x", n);
    }

    void printHexString(byte[] encoding) {
        System.out.print("0x");
        for (byte b : encoding) {
            System.out.print(String.format("%02x", b) + " ");
        }
        System.out.println();
    }

    byte[] getEncoding(R reg) {
        return reg.getBytes();
    }
}

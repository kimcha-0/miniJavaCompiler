package codegeneration;

import miniJava.CodeGeneration.x64.R;
import miniJava.CodeGeneration.x64.Reg64;
import org.junit.jupiter.api.Test;

import static miniJava.CodeGeneration.x64.Reg64.RegFromIdx;

public class RMSIBTest {
    @Test
    void makeRMR() {
        // rm, r
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Reg64 rm = (Reg64) RegFromIdx(j, false);
                Reg64 r = (Reg64) RegFromIdx(i, false);
                R reg = new R(rm, r);
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
    void makeRDispMult() {
        for (int i = 0; i < 3; i++) {
            for (int n = 0; n < 4; n++) {
                for (int j = 0; j < 8; j++) {
                    for (int k = 0; k < 8; k++) {
                        Reg64 r = (Reg64) RegFromIdx(j, false);
                        Reg64 rm = (Reg64) RegFromIdx(k, false);
                        if (k == 4)
                            // ridx is rsp
                            continue;
                        int disp = i == 2 ? 1 << 8 : i == 1 ? i << 1 : 0;
                        int mult = (int) Math.pow(2, n);
                        R reg = new R(Reg64.RSP, rm, mult, disp, r);
                        byte[] regEncoding = getEncoding(reg);
                        System.out.println("r: " + r.toString() + ", rm: " + rm.toString() + ", disp: " + disp +
                                ", mult: " + mult + " "
                                + getHexString(regEncoding[0]));
                    }
                }
            }
        }
    }

    @Test
    void makeSIB() {
    }

    String getHexString(int n) {
        return String.format("0x%016x", n);
    }

    byte[] getEncoding(R reg) {
        return reg.getBytes();
    }
}

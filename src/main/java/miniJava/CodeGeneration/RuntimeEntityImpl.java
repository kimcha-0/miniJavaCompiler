package miniJava.CodeGeneration;

public class RuntimeEntityImpl implements RuntimeEntity {
    private int base;
    private int offset;

    public RuntimeEntityImpl(int base, int offset) {
        this.base = base;
        this.offset = offset;
    }

    @Override
    public int getAddress() {
        return base + offset;
    }

    public int getBase() {
        return this.base;
    }

    public int getOffset() {
        return this.offset;
    }
}

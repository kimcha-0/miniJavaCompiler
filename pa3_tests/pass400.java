class A {
     C c;
     int x;
    public static void main(String[] args) {
        int[] x = new int[1];
        x.length();

    }

    public B seeB() {
        return b;
    }
}
class B {
    int x;
    A a;

    public int seeX() {
        return x;
    }
}

class C {
    B b;
}
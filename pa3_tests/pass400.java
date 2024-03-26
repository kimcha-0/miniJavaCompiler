class A {
    B b;
    int x;
}

class B {
    C c;
    int x;
    void main() {
        c.fun();
    }
}

class C {
    int x;
    void fun() {
        int c = 4;
        int x = 5;
        B b = new B();
        A a = new A();
        a.b.c.x = 6;
        b.c.x = 2;
    }
}

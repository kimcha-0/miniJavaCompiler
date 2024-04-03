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
    A a;
    void fun() {
        a.b.c.x = 2;
    }
}

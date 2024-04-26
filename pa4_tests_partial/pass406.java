/**
 * COMP 520
 * Object creation and update
 */
class MainClass {
    public static void main(String[] args) {

        FirstClass f = new FirstClass();
        f.s = new SecondClass();

        // write and then read;
        f.n = 5;
        f.s.n = 48 + 1;

//      int tstvar = f.n + f.s.n;
//      int x = f.n;

//      System.out.println(tstvar);
        System.out.println(f.n);
    }
}

class FirstClass {
    int n;
    SecondClass s;

}

class SecondClass {
    int n;
    FirstClass f;

}




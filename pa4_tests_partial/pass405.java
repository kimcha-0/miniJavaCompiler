/**
 * COMP 520
 * Object creation and field reference
 */
class MainClass {
   public static void main (String [] args) {

       FirstClass f = new FirstClass ();
       f.n = 48;
       int tstvar = 5;

       System.out.println(tstvar + f.n);
   }
}

class FirstClass
{
   int n;

}




/**
 * COMP 520
 * Array creation
 */
class MainClass {
   public static void main (String [] args) {

       int test = 7;
       int [] aa = new int [test];
       test = 8;
       aa[0] = 100;
       aa[1] = aa[0] - 3;
       aa[2] = aa[0];
       System.out.println(48 + test);
       System.out.println(aa[1]);
   }
}

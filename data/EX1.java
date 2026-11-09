package TP1;

import java.util.Scanner;

public class EX1 {
	
	public static boolean estpremier(int n) {
		if (n<=1) return false ;
		if(n<=3 ) return true;
		if(n % 2==0 || n % 3==0) return false;
		
		for(int i=5;i*i<n;i+=6) {
			if(n % i==0 || n % (i+2)==0 ) {
				return false ;
			}
		}
		return false ;
	}



    public static void main(String[] args) {
    	Scanner e=new Scanner(System.in);
    	while(true) {
    		System.out.print("Entre le nombre : ");
    		int n=e.nextInt();
    		  if (estpremier(n)) {
                  System.out.println(" est premier");
              } else {
                  System.out.println(" n'est pas premier");
              }
    	}
    
    }
}
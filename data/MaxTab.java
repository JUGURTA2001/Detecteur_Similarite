package TP1;

import java.util.Scanner;

public class MaxTab {

    public static void afficherDeuxiemeMax(int[] tab) {
        if (tab == null || tab.length < 2) {
            System.out.println("Il faut au moins 2 éléments dans le tableau.");
            return;
        }

        int max = tab[0];
        int max2 = tab[1];
        int comparaisons = 1; 

        if (max < max2) {
            int tmp = max;
            max = max2;
            max2 = tmp;
        }

        for (int j = 2; j < tab.length; j++) {
            comparaisons+=2; 

            if (tab[j] > max) {
                max2 = max;
                max = tab[j];
            } else {
                comparaisons++; 

                if (tab[j] > max2) {
                    max2 = tab[j];
                }
            }
        }

        System.out.println("Le 2e max du tableau est : " + max2);
        System.out.println("Nombre de comparaisons : " + comparaisons);
    }

    public static void main(String[] args) {
        Scanner e = new Scanner(System.in);

        System.out.print("Entrer la taille du tableau : ");
        int n = e.nextInt();

        if (n < 2) {
            System.out.println("La taille doit être au moins 2.");
            e.close();
            return;
        }

        int[] tab = new int[n];

        for (int k = 0; k < n; k++) {
            System.out.print("Entrer l'élément " + k + " : ");
            tab[k] = e.nextInt();
        }

        afficherDeuxiemeMax(tab);

        e.close();
    }
}
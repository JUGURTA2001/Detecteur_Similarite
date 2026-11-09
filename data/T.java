package TP1;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class T {

   
    public static int methodeIterative(int[] tab, int[] compteur) {
        int n = tab.length;
        
        int[] courant = new int[n];
        for (int i = 0; i < n; i++) {
            courant[i] = i;
        }

        List<Integer>[] beaten = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            beaten[i] = new ArrayList<>();
        }

       
        while (courant.length > 1) {
            int nextSize = (courant.length + 1) / 2;
            int[] suivant = new int[nextSize];
            int idxSuivant = 0;

            for (int i = 0; i < courant.length; i += 2) {
                if (i + 1 == courant.length) {
                    suivant[idxSuivant++] = courant[i];
                } else {
                    int idx1 = courant[i];
                    int idx2 = courant[i + 1];

                    compteur[0]++; 
                    if (tab[idx1] > tab[idx2]) {
                        suivant[idxSuivant++] = idx1;
                        beaten[idx1].add(idx2);
                    } else {
                        suivant[idxSuivant++] = idx2;
                        beaten[idx2].add(idx1);
                    }
                }
            }
            courant = suivant;
        }

        
        int indexMax = courant[0];
        int deuxiemeMax = Integer.MIN_VALUE;
        
        for (int indexAdv : beaten[indexMax]) {
            compteur[0]++; 
            if (tab[indexAdv] > deuxiemeMax) {
                deuxiemeMax = tab[indexAdv];
            }
        }
        return deuxiemeMax;
    }

 
    static class ResultatTournoi {
        int indexGagnant;
        List<Integer> adversairesBattus;

        ResultatTournoi(int indexGagnant) {
            this.indexGagnant = indexGagnant;
            this.adversairesBattus = new ArrayList<>();
        }
    }

   
    public static ResultatTournoi tournoiRecursif(int[] tab, int debut, int fin, int[] compteur) {
       
        if (debut == fin) {
            return new ResultatTournoi(debut);
        }

       
        int milieu = debut + (fin - debut) / 2;
        
     
        ResultatTournoi gauche = tournoiRecursif(tab, debut, milieu, compteur);
        ResultatTournoi droite = tournoiRecursif(tab, milieu + 1, fin, compteur);

        
        compteur[0]++; 
        if (tab[gauche.indexGagnant] > tab[droite.indexGagnant]) {
            gauche.adversairesBattus.add(droite.indexGagnant);
            return gauche;
        } else {
            droite.adversairesBattus.add(gauche.indexGagnant);
            return droite;
        }
    }

    
    public static int methodeRecursive(int[] tab, int[] compteur) {
        
        ResultatTournoi resultat = tournoiRecursif(tab, 0, tab.length - 1, compteur);

        
        int indexMax = resultat.indexGagnant;
        int deuxiemeMax = Integer.MIN_VALUE;
        
        for (int indexAdv : resultat.adversairesBattus) {
            compteur[0]++; // Comparaison
            if (tab[indexAdv] > deuxiemeMax) {
                deuxiemeMax = tab[indexAdv];
            }
        }
        return deuxiemeMax;
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

       
        int[] compteurIteratif = {0};
        int resIteratif = methodeIterative(tab, compteurIteratif);

       
        int[] compteurRecursif = {0};
        int resRecursif = methodeRecursive(tab, compteurRecursif);


       
        System.out.println("           RÉSULTATS DU TOURNOI");
   
        System.out.println("2ème Maximum (Méthode Itérative) : " + resIteratif);
        System.out.println("2ème Maximum (Méthode Récursive) : " + resRecursif);
        
        
        System.out.println("          COMPARAISONS (COMPLEXITÉ)");
     
        System.out.println("Comparaisons Itératif : " + compteurIteratif[0]);
        System.out.println("Comparaisons Récursif : " + compteurRecursif[0]);
        
        int log2n = (int) (Math.log(n) / Math.log(2));
        int theorique = n + log2n - 2;
        System.out.println("Théorique (n + log2(n) - 2) : " + theorique);
     
        e.close();
    }
}
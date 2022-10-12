//pas besoin de stocker toute la matrice de distance 
//si la commande est coeur alor il n'y a pas de 4eme argument 
// prendre un sommet plus petit k  l'enlever et recommencer attention a pas parcourir plusieur fois le graphs adapter la structure de sommets pour pouvoir avoir facilement le plus petit sommet 
// pour cela utiliser la priorityqueue (pas obligatoire plus simple) (le prof utilise des liste de taille k)
//il a pas reussi a faire tourner sont algo sur les grand graph donc si on reussis on aura fait mieux que lui ....
// 

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Random;

import static java.lang.Integer.parseInt;

class Sommet {
    int degreOut; // son degré, = nombre de voisins
    int degreIn; // son degré, = nombre de voisins
    boolean visited; // dit si déjà visité par PL (ParcoursLargeur)
    boolean dead;
    int[] adjOut;
    int[] adjIn; // tableau d'adjacence. une case = un numero de voisin. sa longueur est degré
    int dist; // sa distance à D, utilisé dans le parcours en largeur
} // pas de constructeur on affectera tous les champs plus tard

class Graphe {
    int n; // nombre de sommets
    int m; // nombre d'arcs
    int dmax; // degre max d'un sommet
    Sommet[] V; // tableau des sommets. De taille n+1 normalement

    Graphe(String filename, int nblignes) {
        // construit à partir du fichier filename de (au moins) nblignes
        n = m = dmax = 0;
        // Passe 1 : lit le fichier et le transforme en int[nblignes][2]
        int orig[] = new int[nblignes], dest[] = new int[nblignes]; // l'origine et la destination de chaque arc
        // donc la ligne numero i est deux nombre qui vont remplit dest[i] et orig[i]
        try {
            BufferedReader read = new BufferedReader(new FileReader(filename));

            for (int l = 0; l < nblignes; l++) {
                String line = read.readLine();
                if (line == null) // fin de fichier
                    break;
                if (line.length() == 0 || line.charAt(0) == '#') // commentaire
                    continue;
                int a = 0;
                for (int pos = 0; pos < line.length(); pos++) {
                    char c = line.charAt(pos);
                    if (c == ' ' || c == '\t') {
                        if (a != 0)
                            orig[m] = a;
                        a = 0;
                        continue;
                    }
                    if (c < '0' || c > '9') {
                        System.out.println("ERREUR format ligne " + l + "c = " + c + " valeur " + (int) c);
                        System.exit(1);
                    }
                    a = 10 * a + c - '0';
                }
                dest[m] = a;
                n = Math.max(n, Math.max(orig[m], dest[m])); // au passage calcul du numéro de sommet max
                m++;
            }
            read.close();
        } catch (IOException e) {
            System.out.println("ERREUR entree/sortie sur " + filename);
            System.exit(1);
        }
        // a ce point n est le NUMERO DE SOMMETS MAX. On le change pour avoir le NOMBRE
        // DE SOMMETS, un de plus à cause du sommet 0
        n++;

        // deuxième boucle, sur les sommets : alloue les sommets
        V = new Sommet[n];
        for (int i = 0; i < n; i++)
            V[i] = new Sommet();

        // troisieme boucle, sur les arcs : calcule les degrés pour pas allouer trop de
        // mémoire
        for (int i = 0; i < m; i++) {
            (V[orig[i]].degreOut)++; // si arrete orig--dest augmente le degre de orig
            (V[dest[i]].degreIn)++; // si
        }

        // quatrieme passe : alloue les tableaux d'adjacance. Ici boucle sur les sommets
        for (int i = 0; i < n; i++) {
            if (V[i].degreOut > 0) // on n'alloue rien si pas de voisin
                V[i].adjOut = new int[V[i].degreOut];
            V[i].adjIn = new int[V[i].degreIn];
            dmax = Math.max(dmax, V[i].degreOut); // au passage calcul de degré max ici
            V[i].degreOut = 0; // on remet le degre a zero car degre pointe la première place libre où insérer
                               // un élément pour la cinquieme passe
            V[i].degreIn = 0; // on remet le degre a zero car degre pointe la première place libre où insérer
                              // un élément pour la cinquieme passe
        }

        // passe 5 (sur les arcs, encore) : enfin, remplit les listes d'adjacence
        for (int i = 0; i < m; i++) {
            V[orig[i]].adjOut[V[orig[i]].degreOut++] = dest[i]; // dest est mis dans la liste des voisins de orig en
                                                                // case numero degre
            V[dest[i]].adjIn[V[dest[i]].degreIn++] = orig[i]; // dest est mis dans la liste des voisins de orig en case
                                                              // numero degre
        }
    }

    public void coeur() {
        System.out.println(kmax(-1));
    }

    public int kmax(int node) {// Algo Cours 3 Page 48

        for (int v = 0; v < V.length; v++) {
            V[v].visited = false;
        }

        ArrayDeque<Integer> file = new ArrayDeque<Integer>(); // file du parcours. On aurait pu faire <Sommet> aussi.

        // Iterate until the max k-coeur is found or the max arc limit reached
        for (int k = 0; k < dmax; k++) {

            // IsGone is an array that registers if a node has been "deleted" from the graph
            // of not
            boolean cycleAgain = true;// Only cycle again when a node has been deleted on the last iteration

            // Purge every node from the graph where the cardinality(n) < k
            while (cycleAgain) {

                cycleAgain = false;

                for (int i = 0; i < n; i++) {// Iterate on the entire graph

                    // If the count is inferior to the current k-coeur, "delete" it and cycle again
                    if (V[i].degreOut < k && V[i].visited == false) {

                        if (i == node) {
                            return k - 1;
                        }
                        file.add(i);
                        V[i].visited = true;
                        cycleAgain = true;

                        // For each inbound edge, change origin node to a degreOut--
                        while (file.size() > 0) {
                            int updateNode = file.poll();

                            for (int adj = 0; adj < V[updateNode].degreIn; adj++) {
                                int originNode = V[updateNode].adjIn[adj];
                                V[originNode].degreOut--;

                                if (V[originNode].degreOut < k && V[originNode].visited == false) {
                                    V[originNode].visited = true;
                                    if (originNode == node) {
                                        return k - 1;
                                    }
                                    file.add(originNode);
                                }
                            }
                        }
                    }
                }
            }

            // Check if there is a remaining node that hasn't been deleted
            boolean isEmpty = true;
            for (int y = 0; y < n; y++) {
                if (V[y].visited == false) {
                    isEmpty = false;
                    break;
                }
            }

            // If there is not, we have found the max k
            if (isEmpty) {
                return k - 1;
            }
        }

        return dmax;
    }

    // For each node in V, write the distance from node into them
    public void nodeDist(int node) {
        // Retourne le nombre de sommets à distance dist de D

        for (int i = 0; i < n; i++) {
            V[i].visited = false;
            V[i].dist = 0;
        }

        ArrayDeque<Integer> file = new ArrayDeque<Integer>(); // file du parcours. On aurait pu faire <Sommet> aussi.

        file.add(node); // file = (D)
        V[node].dist = 0; // D a distance 0 de lui-meme
        V[node].visited = true;

        while (!file.isEmpty()) {

            int nodeEdgeStart = file.poll(); // extraire tete

            for (int i = 0; i < V[nodeEdgeStart].degreOut; i++) { // parcours des voisins

                int nodeEdgeEnd = V[nodeEdgeStart].adjOut[i];

                if (V[nodeEdgeEnd].visited == false) {
                    V[nodeEdgeEnd].visited = true; // marqué comme déjà visité, car dans la file
                    V[nodeEdgeEnd].dist = V[nodeEdgeStart].dist + 1;
                    file.add(nodeEdgeEnd);
                }
            }
        }
    }

    public void unSeul(int arg) {

        nodeDist(arg);

        int totalAccesibleNodes = 0;
        int excentricity = 0;
        double proximity = 0;

        // Compute some stuff
        for (int i = 0; i < n; i++) {
            if (V[i].visited == true) {
                totalAccesibleNodes++;
                proximity += V[i].dist;
                if (V[i].dist > excentricity) {
                    excentricity = V[i].dist;
                }
            }
        }

        proximity = proximity / totalAccesibleNodes;

        System.out.println(kmax(arg));
        System.out.println(totalAccesibleNodes);
        System.out.println(excentricity);
        System.out.println(proximity);
    }

    public void exact() {
        approx(n);
    }

    public void approx(int arg) {
        // Diameter
        int diameter = 0;
        double avgExc = 0;
        double avgProx = 0;
        double avgDist = 0;
        int nbNodeSeen = 0;

        int[] sampledNumbers = sampleRandomNumbersWithoutRepetition(0, n, arg);
        
        for (int i = 0; i < sampledNumbers.length; i++) {

            nodeDist(sampledNumbers[i]);

            int totalAccesibleNodes = 0;
            int excentricity = 0;
            double proximity = 0;

            // Compute some stuff
            for (int y = 0; y < n; y++) {
                if (V[y].visited == true) {
                    totalAccesibleNodes++;
                    proximity += V[y].dist;
                    if (V[y].dist > excentricity) {
                        excentricity = V[y].dist;
                        if (excentricity > diameter) {
                            diameter = excentricity;
                        }
                    }
                }
            }

            avgExc += excentricity;
            avgProx += proximity / totalAccesibleNodes;
            avgDist += proximity;
            nbNodeSeen += totalAccesibleNodes;
        }

        avgExc = (avgExc / n) * (n / sampledNumbers.length);
        avgProx = (avgProx / n) * (n / sampledNumbers.length);
        avgDist = avgDist / nbNodeSeen;

        System.out.println(diameter);
        System.out.println(avgExc);
        System.out.println(avgProx);
        System.out.println(avgDist);
    }

    // Code shamelessly stolen from stackoverflow
    public static int[] sampleRandomNumbersWithoutRepetition(int start, int end, int count) {
        Random rng = new Random();

        if(count > end){
            System.out.println("ERROR : Option invalide : "+count+" plus grand que le nombre de sommets : "+end);
            System.exit(0);
        }

        int[] result = new int[count];
        int cur = 0;
        int remaining = end - start;
        for (int i = start; i < end && count > 0; i++) {
            double probability = rng.nextDouble();
            if (probability < ((double) count) / (double) remaining) {
                count--;
                result[cur++] = i;
            }
            remaining--;
        }
        return result;
    }
}

public class TP2 {

    String file;
    int lines;

    public TP2(String f, int l) {
        file = f;
        lines = l;
    }

    public static void main(String[] args) {

        if (args.length != 3 && args.length != 4) {
            System.out.println("ERREUR Usage : java TP1 nomFichier.txt nblignes commande option");
            return;
        }

        String path = args[0];
        int lineCount = parseInt(args[1]);
        String command = args[2];

        Graphe computedGraph = new Graphe(path, lineCount);

        switch (command) {
            case "coeur":
                computedGraph.coeur();
                break;
            case "1seul":
                computedGraph.unSeul(parseInt(args[3]));
                break;
            case "exact":
                computedGraph.exact();
                break;
            case "approx":
                computedGraph.approx(parseInt(args[3]));
                break;
        }
    }
}
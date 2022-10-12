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
    int effectiveDegree; // son degré, = nombre de voisins
    int totalDegree; // Max degree at any given point
    boolean visited; // dit si déjà visité par PL (ParcoursLargeur)
    boolean mark;
    int[] adj;
    int dist; // sa distance à D, utilisé dans le parcours en largeur
} // pas de constructeur on affectera tous les champs plus tard

class Graphe {
    int n; // nombre de sommets
    int m; // nombre d'arcs
    int dmax; // degre max d'un sommet
    Sommet[] V; // tableau des sommets. De taille n+1 normalement

    int nonIsolatedNodes = 0;
    int edgeCount = 0;
    int ignoredLoops = 0;
    int ignoredDuplicates = 0;
    long triangleCount = 0;
    double clustGlobal = 0;
    double clustLocal = 0;
    long arreteIncidentes = 0;

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
            (V[dest[i]].effectiveDegree)++;
            (V[orig[i]].effectiveDegree)++; // si arrete orig--dest augmente le degree de orig
        }

        // quatrieme passe : alloue les tableaux d'adjacance. Ici boucle sur les sommets
        for (int i = 0; i < n; i++) {
            if (V[i].effectiveDegree > 0) // on n'alloue rien si pas de voisin
                V[i].adj = new int[V[i].effectiveDegree];
            V[i].effectiveDegree = 0; // on remet le degre a zero car degre pointe la première place libre où insérer
                            // un élément pour la cinquieme passe
        }

        // passe 5 (sur les arcs, encore) : enfin, remplit les listes d'adjacence
        for (int i = 0; i < m; i++) {
            
            boolean alreadyExists = false;

            //Check for loops
            if(orig[i] == dest[i]) {
                alreadyExists = true;
                ignoredLoops++;
            }

            //Check origin node
            if(!alreadyExists){
                for(int y = 0; y < V[orig[i]].effectiveDegree; y++){
                    if(V[orig[i]].adj[y] == dest[i]){
                        alreadyExists = true;
                        ignoredDuplicates++;
                        break;
                    }
                }
            }
            
            if(!alreadyExists){
                V[orig[i]].adj[V[orig[i]].effectiveDegree++] = dest[i];
                V[dest[i]].adj[V[dest[i]].effectiveDegree++] = orig[i];
                V[orig[i]].totalDegree++;
                V[dest[i]].totalDegree++;
                dmax = Math.max(dmax, V[orig[i]].effectiveDegree); // au passage calcul de degré max ici
                dmax = Math.max(dmax, V[dest[i]].effectiveDegree); // au passage calcul de degré max ici
            }
        }
    }

    public int kmax() {

        for (int v = 0; v < V.length; v++) {
            V[v].visited = false;
        }

        //int idxK [][]; 

        ArrayDeque<Sommet> file = new ArrayDeque<Sommet>(); // file du parcours. On aurait pu faire <Sommet> aussi.
        
        // Iterate until the max k-coeur is found or the max arc limit reached
        for (int k = 0; k < dmax; k++) {
            
            // IsGone is an array that registers if a node has been "deleted" from the graph
            // of not
            boolean cycleAgain = true;// Only cycle again when a node has been deleted on the last iteration

            // Purge every node from the graph where the cardinality(n) < k
            while (cycleAgain) {

                cycleAgain = false;

                for (int i = 0; i < n; i++) {// Iterate on the entire graph

                    Sommet current = V[i];

                    // If the count is inferior to the current k-coeur, "delete" it and cycle again
                    if (current.effectiveDegree < k && current.visited == false) {

                        //Add current for removal
                        file.add(current);
                        current.visited = true;
                        cycleAgain = true;

                        //Removal of any element in the list
                        while (file.size() > 0) {
                            Sommet A = file.poll();

                            //Update the degre of connected nodes and mark bad ones for removal
                            for (int y = 0; y < A.totalDegree; y++) {
                                Sommet B = V[A.adj[y]];
                                if(B.visited == false){
                                    B.effectiveDegree--;
                                    if (B.effectiveDegree < k) {
                                        B.visited = true;
                                        file.add(B);
                                    }
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

    public void computeStuff(){
        double avgCluster = 0;

        //For each node "orig" in the graph
        for(int i = 0; i < n; i++){
            Sommet orig = V[i];
            double local = 0;
            double potential = 0;
            
            if(orig.effectiveDegree > 0){

                edgeCount += orig.effectiveDegree;

                //Mark the neighbors of orig
                for(int y = 0; y < orig.effectiveDegree; y++){
                    Sommet dest1 = V[V[i].adj[y]];
                    dest1.mark = true;
                }

                //For each node "dest1" adjacent to the current node "orig" | orig - dest1
                for(int y = 0; y < orig.effectiveDegree; y++){
                    Sommet dest1 = V[V[i].adj[y]];

                    //Check if "dest1" has been reported as a non isolated node and mark it
                    if(dest1.visited == false){
                        nonIsolatedNodes++;
                        dest1.visited = true;
                    }

                    //For each node "dest1" adjacent to "orig", add the number of possible connections to the potential
                    potential += orig.effectiveDegree-1; 
                    
                    //For each node "link" adjacent to the secondary node "dest1" | orig - dest1 - link
                    for(int z2 = 0; z2 < dest1.effectiveDegree; z2++){ 
                        Sommet link = V[dest1.adj[z2]];

                        //Skip if we went back to the origin
                        if(link == orig)
                            continue;

                        if(link.mark){
                            triangleCount++;
                            local++;
                        }

                        //V edges counts
                        arreteIncidentes++;
                    }    
                }

                //Unmark the neighbors of orig
                for(int y = 0; y < orig.effectiveDegree; y++){
                    Sommet dest1 = V[V[i].adj[y]];
                    dest1.mark = false;
                }

                //Save the average fraction of possible connections made
                if(potential != 0)
                    avgCluster += local / potential;  

                //Check again for non isolated nodes
                if(V[i].visited == false){
                    V[i].visited = true;
                    nonIsolatedNodes++;
                } 
            }            
        }

        //Do some magic
        edgeCount /= 2;
        triangleCount = (triangleCount/6);
        arreteIncidentes /= 2;
        clustLocal = avgCluster / nonIsolatedNodes;
        
        //More magic
        if(arreteIncidentes == 0){
            clustGlobal = 0;
        } else {
            clustGlobal = (double)(3 * triangleCount) / arreteIncidentes; 
        }
    }
}

public class TP3 {

    String file;
    int lines;

    public TP3(String f, int l) {
        file = f;
        lines = l;
    }

    public static void main(String[] args) {

        String path = args[0];
        int lineCount = parseInt(args[1]);

        Graphe computedGraph = new Graphe(path, lineCount);
        computedGraph.computeStuff();

        System.out.println(computedGraph.nonIsolatedNodes);
        System.out.println(computedGraph.edgeCount);
        System.out.println(computedGraph.ignoredLoops);
        System.out.println(computedGraph.ignoredDuplicates);
        System.out.println(computedGraph.dmax);
        System.out.println(computedGraph.kmax());
        System.out.println(computedGraph.triangleCount);
        System.out.println(computedGraph.clustGlobal);
        System.out.println(computedGraph.clustLocal);
    }
}
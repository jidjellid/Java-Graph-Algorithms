import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.PriorityQueue;

import static java.lang.Integer.parseInt;

class Sommet {
    int effectiveDegree; // son degré, = nombre de voisins
    boolean visited; // dit si déjà visité par PL (ParcoursLargeur)
    int[] adj;
    int cluster;
} // pas de constructeur on affectera tous les champs plus tard

class Graphe {

    class Cluster{
        double modularity;
        int [] nodesIndexInCluster;
        boolean removed;
    
        public Cluster(int[] n){
            nodesIndexInCluster = n;
            modularity = computeClusterModularity();
        }
    
        public double computeClusterModularity(){
            double val = 0;
            int loops = 0;
            double degreSum = 0;
    
            int [] nodeIndexes = nodesIndexInCluster;
    
            for (int i = 0; i < nodeIndexes.length; i++) {// For each node inside this cluster
                
                Sommet currentNode = V[nodeIndexes[i]];
                currentNode.visited = true;
                degreSum += currentNode.effectiveDegree;
    
                // Count total loops for each adj in the currentNode
                if (currentNode.effectiveDegree > 0) {
                    for (int y = 0; y < currentNode.adj.length; y++) {
                        Sommet secondaryNode = V[currentNode.adj[y]];
    
                        if (!secondaryNode.visited && currentNode.cluster == secondaryNode.cluster) {
                            loops++;
                        }
                    }
                }
            }
    
            degreSum = Math.pow(degreSum, 2);
            val += (loops / (double) m) - (degreSum / (4 * Math.pow(m, 2)));
            return val;
        }
    }

    public class ClusterCouple implements Comparable<ClusterCouple>{
        int a;
        int b;
        Double coupleModularity;
        Double netGain;
        private int hashCode;

        public ClusterCouple(int a, int b, Double mod){
            this.a = a;
            this.b = b;
            this.coupleModularity = mod;
            this.netGain = mod - clusters[a].modularity - clusters[b].modularity;
            this.hashCode = Objects.hash(a, b);
        }

        //Depends only on account number
        @Override
        public int hashCode() {  
            return hashCode;
        }
        
        //Compare only account numbers
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
    
            if (obj == null || getClass() != obj.getClass())
                return false;
    
            ClusterCouple other = (ClusterCouple) obj;
            return (a == other.a && b == other.b);
        }
    
        @Override
        public int compareTo(ClusterCouple o) {

            if(netGain != null && o.netGain == null)
                return 1;
            else if(netGain == null && o.netGain != null)
                return -1;
            else if(netGain == null && o.netGain == null)
                return 0;

            if(netGain > o.netGain)
                return 1;
            else if(netGain < o.netGain)
                return -1;
            return 0;
        }

        public String toString(){
            return "("+a+", "+b+")";
        }
    }

    class ClusterCoupleStorage{
        HashMap<Integer,HashMap<Integer,ClusterCouple>> clusterCouplesHashMap;

        ClusterCoupleStorage(){
            clusterCouplesHashMap = new HashMap<>();
        }

        public void add(ClusterCouple c){
            if(clusterCouplesHashMap.get(c.a) == null){
                clusterCouplesHashMap.put(c.a,new HashMap<Integer, ClusterCouple>());
            }

            clusterCouplesHashMap.get(c.a).put(c.b,c);
        }

        public ClusterCouple get(int a, int b){
            if(clusterCouplesHashMap.get(a) != null)
                return clusterCouplesHashMap.get(a).get(b);
            return null;
        }

        public void remove(ClusterCouple c){
            if(clusterCouplesHashMap.get(c.a) != null){
                clusterCouplesHashMap.get(c.a).remove(c.b);
                
                if(clusterCouplesHashMap.get(c.a).size() == 0){
                    clusterCouplesHashMap.remove(c.a);
                }
            }
        }

        public void removeAny(int a){
            clusterCouplesHashMap.remove(a);
            for(int i=0; i < clusters.length; i++){
                ClusterCouple c = get(i,a);
                if(c != null){
                    remove(c);
                }
            }
        }
    }

    int n; // nombre de sommets
    int m; // nombre d'arcs
    int dmax; // degre max d'un sommet
    int clusterCount;
    Sommet[] V; // tableau des sommets. De taille n+1 normalement
    Cluster [] clusters;
    double bestMod;
    int bestIter;

    double timeSpentMerging = 0.0;
    double total2 = 0.0;
    
    ClusterCoupleStorage coupleStorage = new ClusterCoupleStorage();
    PriorityQueue<ClusterCouple> mergeStack = new PriorityQueue<>(Collections.reverseOrder());

    Graphe(String filename, int nblignes) {
        // construit à partir du fichier filename de (au moins) nblignes
        n = m = dmax = clusterCount = 0;
        bestMod = bestIter = -1;
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
        clusters = new Cluster[n];// cluster array
        
        for (int i = 0; i < n; i++){
            V[i] = new Sommet();
        }

        // troisieme boucle, sur les arcs : calcule les degrés pour pas allouer trop de
        // mémoire
        for (int i = 0; i < m; i++) {
            (V[dest[i]].effectiveDegree)++;
            (V[orig[i]].effectiveDegree)++; // si arrete orig--dest augmente le degree de orig
        }

        // quatrieme passe : alloue les tableaux d'adjacance. Ici boucle sur les sommets
        for (int i = 0; i < n; i++) {
            if (V[i].effectiveDegree > 0){ // on n'alloue rien si pas de voisin
                V[i].adj = new int[V[i].effectiveDegree];
                V[i].cluster = i; 
                clusters[i] = new Cluster(new int[]{i});  
                clusterCount++;
            }
            V[i].effectiveDegree = 0; // on remet le degre a zero car degre pointe la première place libre où insérer
        }

        // passe 5 (sur les arcs, encore) : enfin, remplit les listes d'adjacence
        for (int i = 0; i < m; i++) {
                    
            V[orig[i]].adj[V[orig[i]].effectiveDegree++] = dest[i];
            V[dest[i]].adj[V[dest[i]].effectiveDegree++] = orig[i];
            
            dmax = Math.max(dmax, V[orig[i]].effectiveDegree); // au passage calcul de degré max ici
            dmax = Math.max(dmax, V[dest[i]].effectiveDegree); // au passage calcul de degré max ici    
        }
    }

    //Returns the resulting array from the merge of arr1 and arr2
    public static int [] mergeArrays(int [] arr1, int [] arr2){
        int [] finalClust = new int[arr1.length + arr2.length]; 

        System.arraycopy(arr1, 0, finalClust, 0, arr1.length);
        System.arraycopy(arr2, 0, finalClust, arr1.length, arr2.length);

        return finalClust;
    }

    //Return true if the ClusterCouple given is the most recent one stored in coupleStorage
    public boolean isValid(ClusterCouple c){
        ClusterCouple stored = coupleStorage.get(c.a,c.b);
        if(stored != null && stored.coupleModularity == c.coupleModularity){
            return true;
        }
        return false;
    }

    //Merge the best cluster couple taken from the stack
    public void mergeBestPair(){

        ClusterCouple bestCouple = mergeStack.poll();

        //If the bestCouple polled isn't up to date, take the next one
        while(!isValid(bestCouple)){//Take the first couple that hasn't been replaced or already merged yet
            bestCouple = mergeStack.poll();
        } 
        
        //Get node indexes for both clusters
        int [] clusterA = clusters[bestCouple.a].nodesIndexInCluster;
        int [] clusterB = clusters[bestCouple.b].nodesIndexInCluster;

        int [] merged = mergeArrays(clusterA, clusterB);

        //Modify the cluster of the nodes of cluster A
        for(int i = 0; i < clusterA.length; i++){
            V[clusterA[i]].cluster = bestCouple.b;
        }

        //Remove any coupleCluster from storage that was related to one of the two clusters we are merging
        coupleStorage.removeAny(bestCouple.a);
        coupleStorage.removeAny(bestCouple.b);

        //Remove cluster A from existence
        clusters[bestCouple.a].removed = true;
        clusters[bestCouple.a].nodesIndexInCluster = null;//Array of the cluster A becomes empty
        
        //Set cluster B values
        clusters[bestCouple.b].nodesIndexInCluster = merged;//Array of the cluster B becomes A + B
        clusters[bestCouple.b].modularity = bestCouple.coupleModularity;

        clusterCount--;

        Integer bestA = null;
        Integer bestB = null;
        Double bestMod = null;
        Double bestGain = null;

        

        for(int i=0; i < clusters.length; i++){//For each cluster, compute the new merge score with the recently merged cluster
            if(i != bestCouple.b && clusters[i] != null && !clusters[i].removed){
     
                double mod = computeMergeModularity(bestCouple.b, i);
                double gain = mod - clusters[bestCouple.b].modularity - clusters[i].modularity;

                if(bestMod == null || bestGain < gain){
                    bestA = bestCouple.b;
                    bestB = i;
                    bestMod = mod;
                    bestGain = gain;
                }              
            }
        }

        if(bestMod != null){
            ClusterCouple newCouple = new ClusterCouple(bestA, bestB, bestMod);
            coupleStorage.add(newCouple);
            mergeStack.add(newCouple);
        }
    }

    //TODO : THIS TAKES FOREVER
    //Returns the modularity if cluster A and B were to be merged
    public double computeMergeModularity(int clusterAIndex, int clusterBIndex){

        double val = 0;
        int loops = 0;
        double degreSum = 0;

        int [] clusterANodes = clusters[clusterAIndex].nodesIndexInCluster;
        int [] clusterBNodes = clusters[clusterBIndex].nodesIndexInCluster;

        //Temporarily set the cluster value of every node in cluster A to cluster B + visited to false
        for(int i = 0; i < clusterANodes.length;i++){
            V[clusterANodes[i]].visited = false;
            V[clusterANodes[i]].cluster = clusterBIndex;
        }

        for(int i = 0; i < clusterBNodes.length;i++){
            V[clusterBNodes[i]].visited = false;
        }

        for (int i = 0; i < clusterANodes.length + clusterBNodes.length; i++) {// For each node inside this cluster

            //Check if we are currently parsing the first or second array
            Sommet currentNode;
            if(i < clusterANodes.length){
                currentNode = V[clusterANodes[i]];
            } else {
                currentNode = V[clusterBNodes[i-clusterANodes.length]];
            }
            
            currentNode.visited = true;   
            degreSum += currentNode.effectiveDegree;

            // Count total loops for each adj in the currentNode
            if (currentNode.effectiveDegree > 0) {
                for (int k = 0; k < currentNode.adj.length; k++) {
                    Sommet secondaryNode = V[currentNode.adj[k]];

                    if (!secondaryNode.visited && currentNode.cluster == secondaryNode.cluster) {
                        loops++;
                    }
                }
            }
        }
 
        //Return cluster values of A to its original value
        for(int i = 0; i < clusterANodes.length;i++){
            V[clusterANodes[i]].cluster = clusterAIndex;
        }

        degreSum = Math.pow(degreSum, 2);
        val += (loops / (double) m) - (degreSum / (4 * Math.pow(m, 2)));

        return val;
    }

    //Merge until one cluster remains and print the iteration with the best modularity
    public void mergeLoop(String tracePath){

        BufferedWriter writer = null;

        if(!tracePath.isEmpty()){
            try {
                writer = new BufferedWriter(new FileWriter(tracePath));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

        //Add initial merge couples for every cluster
        for(int i = 0; i < clusters.length; i++) {
            if(clusters[i] == null) {
                continue;
            }

            Integer bestA = null;
            Integer bestB = null;
            Double bestMod = null;
            Double bestGain = null;
            
            for(int y = i+1; y < clusters.length; y++){
                if(clusters[y] == null) {
                    continue;
                }

                double mod = computeMergeModularity(i, y);
                double gain = mod - clusters[i].modularity - clusters[y].modularity;

                if(bestMod == null || bestGain < gain){
                    bestA = i;
                    bestB = y;
                    bestMod = mod;
                    bestGain = gain;
                }  
            }

            if(bestMod != null){
                ClusterCouple newCouple = new ClusterCouple(bestA, bestB, bestMod);
                coupleStorage.add(newCouple);
                mergeStack.add(newCouple);
            }
        }

        //Loop until there is only one single cluster remaining 
        for(int i = 0; true; i++){
            
            //Find the graph modularity for this iteration
            double loopMod = 0;
            int clusterCount = 0;

            for(int y = 0; y < clusters.length; y++) {//For each cluster
                if(clusters[y] != null && !clusters[y].removed) {
                    loopMod += clusters[y].modularity;
                    clusterCount++;
                } 
            }
            
            //Save it if its better than our previous best
            if(bestMod < loopMod){
                bestMod = loopMod;
                bestIter = clusterCount;
            }

            //Write to file if needed
            if(writer != null){
                try{
                    writer.write((i+1)+"\n"+loopMod+"\n");

                    for(int y = 0; y < clusters.length; y++){
                        if(clusters[y] != null && !clusters[y].removed){
                            writer.write("[");
                            for(int k = 0; k < clusters[y].nodesIndexInCluster.length; k++){
                                writer.write(clusters[y].nodesIndexInCluster[k]+" ");
                            }
                            writer.write("] ");
                        }
                    }

                    writer.write("\n");

                } catch(Exception e){
                    e.printStackTrace();
                    System.exit(0);
                } 
            }

            //If one cluster remains, stop
            if(clusterCount > 1){
                mergeBestPair();//Else, merge the best pair
            }else{
                break;
            }
        }

        System.out.println(bestMod);
        System.out.println(bestIter);

        if(writer != null){
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}

public class TP4 {

    String file;
    int lines;

    public TP4(String f, int l) {
        file = f;
        lines = l;
    }

    public static void main(String[] args) {
        String path = args[0];
        int nbEdges = parseInt(args[1]);
        
        String tracePath = "";
        if(args.length > 2)
            tracePath = args[2];

            
        Graphe computedGraph = new Graphe(path, nbEdges);
        computedGraph.mergeLoop(tracePath);
    }
}
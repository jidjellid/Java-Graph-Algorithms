import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;

import static java.lang.Integer.parseInt;

public class TP1 {

    String file;
    int lines;

    public TP1(String f,int l){
        file = f;
        lines = l;
    }

    public class Graph{
        
        //Filled at first read
        int biggestNodeNumber = 0;
        int maxEdgeCount = 0;
        int totalEdges = 0;
        int nodeEdgesCount[];

        //Filled at second read
        int [][] nodeEdges;

        public Graph(){

            //Number of edges for each node
            nodeEdgesCount = new int [lines+1];
        
            //First file read, used to find maxEdgeCount, totalEdges, biggestNodeNumber and nodeEdgesCount
            readFileInitialization();

            //Creation of the first dimension of the edge list to store indexes of edges start
            nodeEdges = new int[biggestNodeNumber+1][];

            //Creation of the second dimension of the edge list to store indexes of edges end using a ragged array
            for(int i = 0; i < nodeEdges.length; i++){
                nodeEdges[i] = new int[nodeEdgesCount[i]];
                for(int y = 0; y < nodeEdges[i].length; y++){
                    nodeEdges[i][y] = -1;//Initializing the value to -1
                }
            }

            //Second file read, used to fill nodeEdges
            readFileStoreEdges();
        }

        public void readFileInitialization(){

            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
    
                for(int lineCount = 0; lineCount < lines; lineCount++){
                    
                    String t = reader.readLine();
                    int pos = 0;
     
                    //Skip to the next line
                    if(t.charAt(pos) == '#'){
                        continue;
                    }
    
                    //Compute the index of the edge start
                    int edgeStart = 0;
                    while(t.charAt(pos) != '	'){
                        edgeStart = edgeStart * 10 + Character.getNumericValue(t.charAt(pos));
                        pos++;
                    }
    
                    pos++;
    
                    //Compute the index of the edge end
                    int edgeEnd = 0;
                    while(pos < t.length()){
                        edgeEnd = edgeEnd * 10 + Character.getNumericValue(t.charAt(pos));
                        pos++;
                    }
                    
                    nodeEdgesCount[edgeStart] += 1;
                    totalEdges += 1;

                    if(nodeEdgesCount[edgeStart] > maxEdgeCount){
                        maxEdgeCount = nodeEdgesCount[edgeStart];
                    }
    
                    if(edgeStart > biggestNodeNumber){
                        biggestNodeNumber = edgeEnd;
                    }
    
                    if(edgeEnd > biggestNodeNumber){
                        biggestNodeNumber = edgeEnd;
                    }
                }
    
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void readFileStoreEdges(){
            //Second reading of the file, this time filling the edge list 
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
    
                for(int lineCount = 0; lineCount < lines; lineCount++){
                    
                    String t = reader.readLine();
                    int pos = 0;
     
                    //Skip to the next line
                    if(t.charAt(pos) == '#'){
                        continue;
                    }
    
                    //Compute the index of the edge start
                    int edgeStart = 0;
                    while(t.charAt(pos) != '	'){
                        edgeStart = edgeStart * 10 + Character.getNumericValue(t.charAt(pos));
                        pos++;
                    }
    
                    pos++;
    
                    //Compute the index of the edge end
                    int edgeEnd = 0;
                    while(pos < t.length()){
                        edgeEnd = edgeEnd * 10 + Character.getNumericValue(t.charAt(pos));
                        pos++;
                    }
             
                    //Find the next free slot at the index edgeStart
                    int posFreeSlot = 0;
                    while(nodeEdges[edgeStart][posFreeSlot] != -1){
                        posFreeSlot++;
                    }
                    
                    //Insert the edge end at the index edgeStart wherever there is a free slot
                    nodeEdges[edgeStart][posFreeSlot] = edgeEnd;
                }

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public int breadthFirstSearch(int source, int distance){

            int total = 0;

            int [] exploredDepth = new int[nodeEdges.length+1];
            for(int i=0; i < exploredDepth.length; i++)
                exploredDepth[i] = -1;
    
            ArrayDeque<Integer> queue = new ArrayDeque<>();
    
            queue.add(source);
            exploredDepth[source] = 0;
    
            while(queue.size() > 0){
    
                int current = queue.pop();
    
                for(int i = 0; i < nodeEdges[current].length; i++){
                    int arcEnding = nodeEdges[current][i];
                    if(arcEnding != -1 && exploredDepth[arcEnding] == -1){
                        if(exploredDepth[current] + 1 == distance){
                            total += 1;
                        }
                        exploredDepth[arcEnding] = exploredDepth[current] + 1;
                        if(exploredDepth[arcEnding] < distance){
                            queue.add(arcEnding);
                        }
                    }
                }
            }
            
            return total;
        }
    }
    
    public static void main(String[] args) {

        String path = args[0];
        int lineCount = parseInt(args[1]);
        int nodeCount = parseInt(args[2]);
        int distance = parseInt(args[3]);

        TP1 tpClass = new TP1(path,lineCount);

        Graph computedGraph = tpClass.new Graph();

        int nbNodesAtDepth = computedGraph.breadthFirstSearch(nodeCount, distance);

        System.out.println(computedGraph.biggestNodeNumber);
        System.out.println(computedGraph.totalEdges);
        System.out.println(computedGraph.maxEdgeCount);
        System.out.println(nbNodesAtDepth);
    }
}
package run;


import graph.GraphBuilder;
import graph.Node;
import indexing.MyStandardAnalyzer;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.Version;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import util.*;

import java.math.*;

/**
 *
 * @author isminilourentzou
 */
public class Main {//

    HashSet<String> sentences = new HashSet<String>();
    Analyzer analyze = new MyStandardAnalyzer(Version.LUCENE_42);

    /**
     * Generates the csv file for visualizing the graph
     *
     * @param fileName the file that contains the sentences or titles
     * @param distr the topical distributions file from CPLSA
     * @param outfile the filename for the new csv graph file
     * @param topicId the topic cluster we are interested in
     * @param removeEdges whether to remove edges with score lower than the
     * threshold (average) or not
     * @param topK the number of top scored pairs of nodes with their respective
     * edges to save to csv. If topK is set to -1 then we include the whole edge
     * set (independent to removing edges lower than a thershold)
     */
    public GraphBuilder doGenerateSummary(String fileName, String distr, String outfile, int topicId, boolean removeEdges, int topK) {
        PrintWriter csv = null;
        GraphBuilder builder = new GraphBuilder();
        SimpleDirectedWeightedGraph g;

        try {
            csv = new PrintWriter(new File(outfile));
            //Create a new empty graph
            //Read the topic model distributions
            ReadDistributions.readFile(distr);
            //Get the topic of interest
            TreeMap<String, Double> topics = ReadDistributions.commonModel.get(topicId);
            HashMap wordNodeMap = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                String str;
                int sentenceid = 0;
                while ((str = reader.readLine()) != null) {
                    sentenceid++;
                    str = StringPreprocessing.analyseString(analyze, str);
                    //unfortunatelly the lucene analyzer messes up punctuaction, will be solved later on ...
                    sentences.add(str.toLowerCase() + " ._.");
                    wordNodeMap = builder.growGraph(str, 1, sentenceid, topics);
                }
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
            if (topK != -1) {
                builder.saveRankedEdgeListToCSV(csv, topK);
            } else if (removeEdges) {
                double median = builder.medianGraph();
                builder.saveGraphToCSV(csv, median);
            } else if (topK == -1) {
                builder.saveGraphToCSV(csv);
            }

           // testing .getSortedVertex()
            g = builder.getGraph();
            builder.getSortedVertexSet(true);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            csv.close();
        }

        g = builder.getGraph();
        //builder.printGraph();//
        return builder;
    }
    
    public static void main(String[] args) {
        Main main = new Main();
        String choice = "test/allafrica/titles_allafrica.txt";
        String distr = "test/allafrica/allafrica_ctm_results_titles_4topics.txt";
        String outputFileA = "test/allafrica/forceA.csv";
        String outputFileB = "test/allafrica/forceB.csv";
        int numberOfTopicA = 2;
        int numberOfTopicB = 4;
        
        //System.out.println("Yay! First graph ----------------------------------------------");
        //SimpleDirectedWeightedGraph a = main.doGenerateSummary(choice, distr, outputFileA, numberOfTopicA, true, 100);
        //System.out.println("Yay! Second graph ----------------------------------------------");
        //SimpleDirectedWeightedGraph b = main.doGenerateSummary(choice, distr, outputFileB, numberOfTopicB, true, 100);
        
        GraphBuilder gb = main.doGenerateSummary(choice, distr, outputFileA, numberOfTopicA, true, 100);
        
        System.out.println();
        //traverse(a);
        extract(gb);
        
//pass parameters, set edges cooccurances, prob|topic, median-
    }
    
    public static void extract(GraphBuilder graphBuilder){
    	//double threshold = 25;
    	//double accumulate = 0;
    	//while (accumulate < threshold)
    	//will also loop through Harshay's list most likely
    	
    	SimpleDirectedWeightedGraph graph = graphBuilder.getGraph();
    	List<Object> sorted = graphBuilder.getSortedVertexSet(false);
    	
    	Set vertexSet = graph.vertexSet();
    	double bestVertexProb = 0;
    	Node bestVertex = (Node) vertexSet.iterator().next();
    	for (Iterator vIter = vertexSet.iterator(); vIter.hasNext();){
    		Node v = (Node) vIter.next();
    		//System.out.println(graph.getEdgeWeight(v.getNodeProb()));
    		double curr = v.getNodeProb();
    		if (curr > bestVertexProb){
    			bestVertexProb = curr;
    			bestVertex = v;
    		}
    	}
		System.out.println(bestVertex.getNodeName() 
				+ "\t" + bestVertexProb 
				+ "\t" + Math.log(bestVertexProb));
		
    	//Set edges = graph.edgesOf(bestVertex);
		Set inEdges = graph.incomingEdgesOf(bestVertex);//all edges soon
    	double bestIncomingCount = 0;
    	DefaultWeightedEdge bestInEdge = (DefaultWeightedEdge) inEdges.iterator().next();//hopefully not empty
    	for (Iterator eIter = inEdges.iterator(); eIter.hasNext();) {//every edge of that node
    		DefaultWeightedEdge e = (DefaultWeightedEdge) eIter.next();
    		//System.out.println(graph.getEdgeWeight(e));
    		double curr = graph.getEdgeWeight(e); //why are edge weights doubles? 
    		if (curr > bestIncomingCount){
    			bestIncomingCount = curr;
    			bestInEdge = e;
    		}
    	}
    	Node inNode = (Node) graph.getEdgeSource(bestInEdge);
    	System.out.println(inNode.getNodeName() 
    			+ "\t" + bestIncomingCount
    			+ "\t" + inNode.getNodeProb()
    			+ "\t" + Math.log(inNode.getNodeProb()));
		
    	//Set edges = graph.edgesOf(bestVertex);
		Set outEdges = graph.outgoingEdgesOf(bestVertex);//all edges soon
    	double bestOutgoingCount = 0;
    	DefaultWeightedEdge bestOutEdge = (DefaultWeightedEdge) outEdges.iterator().next();//hopefully not empty
    	for (Iterator eIter = outEdges.iterator(); eIter.hasNext();) {//every edge of that node
    		DefaultWeightedEdge e = (DefaultWeightedEdge) eIter.next();
    		//System.out.println(graph.getEdgeWeight(e));
    		double curr = graph.getEdgeWeight(e); //why are edge weights doubles? 
    		if (curr > bestOutgoingCount){
    			bestOutgoingCount = curr;
    			bestOutEdge = e;
    		}
    	}
    	Node outNode = (Node) graph.getEdgeTarget(bestOutEdge);
    	System.out.println(outNode.getNodeName() 
    			+ "\t" + bestOutgoingCount
    			+ "\t" + outNode.getNodeProb()
    			+ "\t" + Math.log(outNode.getNodeProb()));
    	
    	System.out.println();
    	System.out.println(inNode.getNodeName() 
    			+ " " + bestVertex.getNodeName() 
    			+ " " + outNode.getNodeName());
    }
    
    public static void traverse(SimpleDirectedWeightedGraph graph){//not a real method, I just made this for practice
    	Set vertexSet = graph.vertexSet();
    	Iterator vIter = vertexSet.iterator();
    	while (vIter.hasNext()) {//every node
    		Node v = (Node) vIter.next();
        	Set edges = graph.edgesOf(v);
        	for (Iterator eIter = edges.iterator(); eIter.hasNext();) {//every edge of that node
        		DefaultWeightedEdge e = (DefaultWeightedEdge) eIter.next();
                String target = ((Node) graph.getEdgeTarget(e)).getNodeName();
                String source = ((Node) graph.getEdgeSource(e)).getNodeName();
                if (!target.equals(v.getNodeName()) && ((Node) graph.getEdgeTarget(e)).getStartNode()) {
	                System.out.println((new StringBuilder(
	                		String.valueOf(v.getNodeName())))
	                		.append("\t")
	                		.append(v.getNodeProb())
	                		.append("\t")
	                		.append(v.getStartNode())
	                		.append("->")
	                		.append(target)
	                		.append("\t")
	                		.append(((Node) graph.getEdgeTarget(e)).getNodeProb())
	                		.append("\t")
	                		.append(((Node) graph.getEdgeTarget(e)).getStartNode())
	                		.append("; ")
	                		.append(graph.getEdgeWeight(e)).toString()
	                		);
                }
        	}
    	}
//    	while (vIter.hasNext()) {
//    		Node v = (Node) vIter.next();
//          Set edges = graph.edgesOf(v);
//    	}
    	/*
    	for (Iterator eIter = edges.iterator(); eIter.hasNext();) {
            DefaultWeightedEdge e = (DefaultWeightedEdge) eIter.next();
            String target = ((Node) graph.getEdgeTarget(e)).getNodeName();
            String source = ((Node) graph.getEdgeSource(e)).getNodeName();
            if (!target.equals(v.getNodeName())) {
                System.out.println((new StringBuilder(
                		String.valueOf(v.getNodeName())))
//                		.append(":")
//                		.append(v.getNodeProb())
//                		.append(":")
//                		.append(v.getStartNode())
//                		.append("->")
//                		.append(target)
//                		.append(":")
//                		.append(((Node) graph.getEdgeTarget(e)).getNodeProb())
//                		.append(":")
//                		.append(((Node) graph.getEdgeTarget(e)).getStartNode())
//                		.append(";")
//                		.append(graph.getEdgeWeight(e)).toString()
                		);
                //System.out.println(graph.getEdgeWeight(e));
            }
        }
        */
    }
}

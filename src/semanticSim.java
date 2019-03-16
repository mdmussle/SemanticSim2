/*
Author: Michael Mussler
Title: Semantic Similarity
Reason: Find the information content of genes
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.chainsaw.Main;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class semanticSim {

    public static void main(String[] args) {

        String prefix = "http://purl.obolibrary.org/obo/";

        String ontology = "go.owl";

        String geneF = "Assignment3-Corpus.txt";

        String output = "info.txt";

        String gene1 = "CREB3L1";
        String gene2 = "RPS11";

        //Write information to the file
        printToFile(ontology, geneF, prefix, gene1, gene2, output);
    }



    //Obtain the super class. returning all of the super classes of the gene
    public static Set<String> obtainSuperClassSetForGene
        (String ontologyF, String gene, String geneF, String prefix) {

        boolean Array = false;
        boolean Set = true;

        //create the set and hashset
        Set<OWLClass> classSet = new HashSet<>();
        Set<String> returnSet = new HashSet<>();
        String[] geneInQuestion = getTerms(geneF, gene);

        if (Array) {
            for (String str : geneInQuestion) {
                System.out.println(str);
            }
        }
        // Load ontology from the file.
        OWLOntology ontology = loadOntology(ontologyF);

        // create the manager and factory
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        // Set up the ELK reasoner.
        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        // Classify the type of the ontology.
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        // Add the prefix to the gene in question.
        PrefixManager prefixManager = new DefaultPrefixManager(prefix);

        // Extract the super classes.
        for (String goTerm : geneInQuestion) {
            // Add the current gene we are looking for
            returnSet.add(goTerm);
            OWLClass goTermClass = dataFactory.getOWLClass(goTerm, prefixManager);
            classSet.addAll(reasoner.getSuperClasses(goTermClass, false).getFlattened());
        }

        // Dispose of the reasoner once the reasoning has been completed.
        reasoner.dispose();

        // fix so we get the GO_TERM we want
        for (OWLClass OWL : classSet) {
            String Str = " ";
            Str = OWL.toString().replace("<" + prefix, " ");
            Str = Str.replace(">", " ");
            returnSet.add(Str);
        }

        return returnSet;
    }
    //get the term we want from the file
    public static String[] getTerms(String filePath, String gene) {
        //set up scanner
        File geneF = new File(filePath);
        Scanner s = null;

        try {
            s = new Scanner(geneF);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        String[] line = null;

        while (s.hasNext()) {
            String geneLine = s.nextLine();

            if (geneLine.contains(gene)) {
                geneLine = geneLine.replace(gene + ",", "");
                return line = geneLine.split(",");
            }
        }

        // Close the scanner once finshed
        s.close();

        return null;
    }
public static OWLOntology loadOntology(String filePath) {
        File file = new File(filePath);

        // Creating the input and output managers.
        OWLOntologyManager inputManage = OWLManager.createOWLOntologyManager();
        OWLDataFactory data = inputManage.getOWLDataFactory();

        OWLOntology ontology = null;

        try {
            // Load the ontology from the passed in file path.
            ontology = inputManage.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException ex) {
            System.err.println("Ontology failed");
        }

        return ontology;
    }
    //calculate the information content
    public static Map<String, Double> getInfo(Set<String>... Sets) {
        double NumSets = Sets.length;

        // A map that counts the number of genes that are in the list.
        Map<String, Double> occurrenceMap = new HashMap<>();

        /* A map that holds the individual values for the information content
         * associated with each GO Term.
         */
        Map<String, Double> info = new HashMap<>();

        // show how many times the go terms appear in each set.
        for (Set<String> Set : Sets) {
            for (String str : Set) {
                if (occurrenceMap.containsKey(str)) {
                    occurrenceMap.put(str, occurrenceMap.get(str) + 1.0);
                } else {
                    occurrenceMap.put(str, 1.0);
                }
            }
        }

        // Calculate the information content and store it.
        for (Map.Entry<String, Double> entry : occurrenceMap.entrySet()) {
            //divide by 2 because have 2 genes
            info.put(entry.getKey(), (Math.log10((double) (entry.getValue() / 2.0)))*-1.0);
        }

        return info;
    }
//process two genes to get content and then writen to file
    public static void printToFile
        (String ontology, String geneF, String prefix, String gene1, String gene2, String output) {
        Set<String> gene1Terms = obtainSuperClassSetForGene(ontology, gene1, geneF, prefix);
        Set<String> gene2Terms = obtainSuperClassSetForGene(ontology, gene2, geneF, prefix);

        writeToFile(getInfo(gene1Terms, gene2Terms), output);
    }

    //write to the file
    public static void writeToFile(Map<String, Double> map, String File) {
        PrintWriter writer = null;

        // Try and write to the file based on the given file path.
        try {
            writer = new PrintWriter(File, "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Write to the file
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            writer.println(entry.getKey() + "\t\t" + entry.getValue());
        }

        // Closes the path to write.
        writer.close();
    }
}

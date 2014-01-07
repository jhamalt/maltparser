package org.maltparser.examples.old;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;

/**
 * This example shows how to parse a sentence with MaltParser by using an URL to a parser model in a jar-file. Otherwise this example is the same as ParseSentence3
 * 
 * To run this example requires that you have ran TrainingExperiment that creates model0.mco and created a jar-file with model0.mco in it:
 *
 * jar cf mco_nested_in_jar.jar model0.mco
 * 
 * @author Johan Hall
 */
public class ParseSentence4 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MaltParserService service =  new MaltParserService();
			// Inititalize the parser model 'model0' and sets the working directory to '.' and sets the logging file to 'parser.log'
			service.initializeParserModel("-u jar:file:mco_nested_in_jar.jar!/model0.mco -m parse -w . -lfi parser.log -v debug");

			// Creates an array of tokens, which contains the Swedish sentence 'Grundavdraget upphör alltså vid en taxerad inkomst på 52500 kr.'
			// in the CoNLL data format.
			String[] tokens = new String[11];
			tokens[0] = "1\tGrundavdraget\t_\tN\tNN\tDD|SS";
			tokens[1] = "2\tupphör\t_\tV\tVV\tPS|SM";
			tokens[2] = "3\talltså\t_\tAB\tAB\tKS";
			tokens[3] = "4\tvid\t_\tPR\tPR\t_";
			tokens[4] = "5\ten\t_\tN\tEN\t_";
			tokens[5] = "6\ttaxerad\t_\tP\tTP\tPA";
			tokens[6] = "7\tinkomst\t_\tN\tNN\t_";
			tokens[7] = "8\tpå\t_\tPR\tPR\t_";
			tokens[8] = "9\t52500\t_\tR\tRO\t_";
			tokens[9] = "10\tkr\t_\tN\tNN\t_";
			tokens[10] = "11\t.\t_\tP\tIP\t_";
			// Parses the Swedish sentence above
			String[] outputTokens = service.parseTokens(tokens);
			// Outputs the with the head index and dependency type information
			for (int i = 0; i < outputTokens.length; i++) {
				System.out.println(outputTokens[i]);
			}
			// Terminates the parser model
			service.terminateParserModel();
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

}

package org.maltparser.parser.algorithm.stack;

import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.Oracle;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.action.GuideUserAction;
/**
 * @author Johan Hall
 *
 */
public class ProjectiveOracle  extends Oracle {
	private SymbolTable cpostagTable;
	public ProjectiveOracle(DependencyParserConfig manager, GuideUserHistory history) throws MaltChainedException {
		super(manager, history);
		setGuideName("projective");
	}
	
	public GuideUserAction predict(DependencyStructure gold, ParserConfiguration configuration) throws MaltChainedException {
		StackConfig config = (StackConfig)configuration;
		Stack<DependencyNode> stack = config.getStack();
		if (cpostagTable == null) {
			cpostagTable = gold.getSymbolTables().getSymbolTable("CPOSTAG");
		} 
		if (stack.size() < 1) {
			return updateActionContainers(Projective.SHIFT, null, null);
		} else {
			DependencyNode n = stack.get(stack.size()-1);
			if (!n.isRoot() && !n.getLabelSet().containsKey(cpostagTable)) {
//				System.out.println("Hej " + gold.getTokenNode(n.getIndex()).getLabelSymbol(cpostagTable));
				LabelSet ls = new LabelSet(1);
				ls.put(cpostagTable, gold.getTokenNode(n.getIndex()).getLabelCode(cpostagTable));
//				System.out.println("ORACLE:" + cpostagTable.getSymbolCodeToString(ls.get(cpostagTable)));
				return updateActionContainers(Projective.CPOSTAG, null, ls);
			} else if (stack.size() < 2) {
				return updateActionContainers(Projective.SHIFT, null, null);
			} else {
				DependencyNode left = stack.get(stack.size()-2);
				int leftIndex = left.getIndex();
				int rightIndex = stack.get(stack.size()-1).getIndex();
				if (!left.isRoot() && gold.getTokenNode(leftIndex).getHead().getIndex() == rightIndex) {
					return updateActionContainers(Projective.LEFTARC, gold.getTokenNode(leftIndex).getHeadEdge().getLabelSet(), null);
				} else if (gold.getTokenNode(rightIndex).getHead().getIndex() == leftIndex && checkRightDependent(gold, config.getDependencyGraph(), rightIndex)) {
					return updateActionContainers(Projective.RIGHTARC, gold.getTokenNode(rightIndex).getHeadEdge().getLabelSet(), null);
				} else {
					return updateActionContainers(Projective.SHIFT, null, null);
				} // Solve the problem with non-projective input.
		}
		}
	}
	
	private boolean checkRightDependent(DependencyStructure gold, DependencyStructure parseDependencyGraph, int index) throws MaltChainedException {
		if (gold.getTokenNode(index).getRightmostDependent() == null) {
			return true;
		} else if (parseDependencyGraph.getTokenNode(index).getRightmostDependent() != null) {
			if (gold.getTokenNode(index).getRightmostDependent().getIndex() == parseDependencyGraph.getTokenNode(index).getRightmostDependent().getIndex()) {
				return true;
			}
		}
		return false;
	}
	
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		
	}
	
	public void terminate() throws MaltChainedException {
		
	}
}

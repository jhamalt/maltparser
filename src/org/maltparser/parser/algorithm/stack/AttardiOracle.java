package org.maltparser.parser.algorithm.stack;

import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.Oracle;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.action.GuideUserAction;

public class AttardiOracle  extends Oracle {
	public AttardiOracle(DependencyParserConfig manager, GuideUserHistory history) throws MaltChainedException {
		super(manager, history);
		setGuideName("attardi");
	}
	
	public GuideUserAction predict(DependencyStructure gold, ParserConfiguration configuration) throws MaltChainedException {
		StackConfig config = (StackConfig)configuration;
		Stack<DependencyNode> stack = config.getStack();

		if (stack.size() < 2) {
			return updateActionContainers(Projective.SHIFT, null);
		} else {
			int rightIndex = stack.get(stack.size()-1).getIndex();
			DependencyNode left = stack.get(stack.size()-2);
			int leftIndex = left.getIndex();
			if (!left.isRoot() && gold.getTokenNode(leftIndex).getHead().getIndex() == rightIndex && isComplete(gold, config.getDependencyGraph(), leftIndex)) {
				return updateActionContainers(Projective.LEFTARC, gold.getTokenNode(leftIndex).getHeadEdge().getLabelSet());
			} else if (gold.getTokenNode(rightIndex).getHead().getIndex() == leftIndex && isComplete(gold, config.getDependencyGraph(), rightIndex)) {
				return updateActionContainers(Projective.RIGHTARC, gold.getTokenNode(rightIndex).getHeadEdge().getLabelSet());
			} else {
				if (stack.size() >= 3) {
					DependencyNode left2 = stack.get(stack.size()-3);
					int left2Index = left2.getIndex();
					if (!left2.isRoot() && gold.getTokenNode(left2Index).getHead().getIndex() == rightIndex && isComplete(gold, config.getDependencyGraph(), left2Index)) {
						return updateActionContainers(Attardi.LEFTARC2, gold.getTokenNode(left2Index).getHeadEdge().getLabelSet());
					} else if (gold.getTokenNode(rightIndex).getHead().getIndex() == left2Index && isComplete(gold, config.getDependencyGraph(), rightIndex)) {
						return updateActionContainers(Attardi.RIGHTARC2, gold.getTokenNode(rightIndex).getHeadEdge().getLabelSet());
					}
				}
				return updateActionContainers(Projective.SHIFT, null);
			} // Solve the problem with non-projective input.
		}
	}
	
	private boolean isComplete(DependencyStructure gold, DependencyStructure parseDependencyGraph, int index) throws MaltChainedException {
		final int m = gold.getTokenNode(index).getLeftDependentCount() + gold.getTokenNode(index).getRightDependentCount();
		final int n = parseDependencyGraph.getTokenNode(index).getLeftDependentCount() + parseDependencyGraph.getTokenNode(index).getRightDependentCount();
		return m == n;
	}
	
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		
	}
	
	public void terminate() throws MaltChainedException {
		
	}
}

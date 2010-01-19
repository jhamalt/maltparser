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
/**
 * @author Johan Hall
 *
 */
public class IncrementalOracle  extends Oracle {
	public IncrementalOracle(DependencyParserConfig manager, GuideUserHistory history) throws MaltChainedException {
		super(manager, history);
		setGuideName("incremental");
	}
	
	public GuideUserAction predict(DependencyStructure gold, ParserConfiguration configuration) throws MaltChainedException {
		StackConfig config = (StackConfig)configuration;
		Stack<DependencyNode> stack = config.getStack();
		Stack<DependencyNode> input = config.getInput();

		if (stack.size() < 2) {
			return updateActionContainers(Incremental.SHIFT, null);
		} else { // Add 4 (simple else)
			DependencyNode left = stack.get(stack.size()-2);
			DependencyNode right = stack.get(stack.size()-1);

			int leftIndex = left.getIndex();
			int rightIndex = right.getIndex();
			if (!left.isRoot() && gold.getTokenNode(leftIndex).getHead().getIndex() == rightIndex) {
				return updateActionContainers(Incremental.LEFTARC, gold.getTokenNode(leftIndex).getHeadEdge().getLabelSet());
			} else if (gold.getTokenNode(rightIndex).getHead().getIndex() == leftIndex && !right.hasHead()) {
				return updateActionContainers(Incremental.RIGHTARC, gold.getTokenNode(rightIndex).getHeadEdge().getLabelSet());
			/* } else if (input.isEmpty()) { // && right.hasHead()
			    return updateActionContainers(Incremental.REDUCE, null);
			} else if (gold.getTokenNode(rightIndex).getHead().getIndex() != input.peek().getIndex() && gold.getTokenNode(input.peek().getIndex()).hasLeftDependent() &&
				   gold.getTokenNode(input.peek().getIndex()).getLeftmostDependent().getIndex() < rightIndex) {
			    return updateActionContainers(Incremental.REDUCE, null);
			} else if (gold.getTokenNode(rightIndex).getHead().getIndex() != input.peek().getIndex() && gold.getTokenNode(input.peek().getIndex()).getHead().getIndex() < rightIndex) {
			return updateActionContainers(Incremental.REDUCE, null);*/
			} else if (right.hasHead() && checkRightDependent(gold, config.getDependencyGraph(), rightIndex)) {
			    return updateActionContainers(Incremental.REDUCE, null);
			} else {
			    return updateActionContainers(Incremental.SHIFT, null);
			} // Solve the problem with non-projective input.
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

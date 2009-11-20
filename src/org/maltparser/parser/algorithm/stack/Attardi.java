package org.maltparser.parser.algorithm.stack;

import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.TransitionSystem;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.transition.TransitionTable;


public class Attardi extends TransitionSystem {
	protected static final int SHIFT = 1;
	protected static final int RIGHTARC = 2;
	protected static final int LEFTARC = 3;
	protected static final int RIGHTARC2 = 4;
	protected static final int LEFTARC2 = 5;

	public Attardi() throws MaltChainedException {
		super();
	}
	
	public void apply(GuideUserAction currentAction, ParserConfiguration configuration) throws MaltChainedException {
		StackConfig config = (StackConfig)configuration;
		Stack<DependencyNode> stack = config.getStack();
		Stack<DependencyNode> input = config.getInput();
		currentAction.getAction(actionContainers);
		Edge e = null;
		DependencyNode head = null;
		DependencyNode dep = null;
		DependencyNode tmp = null;
		switch (transActionContainer.getActionCode()) {
		case LEFTARC:
			head = stack.pop(); 
			dep = stack.pop();
			e = config.getDependencyStructure().addDependencyEdge(head.getIndex(), dep.getIndex());
			addEdgeLabels(e);
			stack.push(head);
			break;
		case RIGHTARC:
			dep = stack.pop(); 
			e = config.getDependencyStructure().addDependencyEdge(stack.peek().getIndex(), dep.getIndex());
			addEdgeLabels(e);
			break;
		case LEFTARC2:
			head = stack.pop();
			tmp = stack.pop();
			dep = stack.pop();
			e = config.getDependencyStructure().addDependencyEdge(head.getIndex(), dep.getIndex());
			addEdgeLabels(e);
			stack.push(tmp);
			stack.push(head);
			break;
		case RIGHTARC2:
			dep = stack.pop();
			tmp = stack.pop();
			e = config.getDependencyStructure().addDependencyEdge(stack.peek().getIndex(), dep.getIndex());
			addEdgeLabels(e);
//			stack.push(tmp);
			input.push(tmp);
			break;
		default:
			if (input.isEmpty()) {
				stack.pop();
			} else {
				stack.push(input.pop()); // SHIFT
			}
			((StackConfig)configuration).lookaheadDecrement();
			break;
		}
	}
	
	public boolean permissible(GuideUserAction currentAction, ParserConfiguration configuration) throws MaltChainedException {
		StackConfig config = (StackConfig)configuration;
		currentAction.getAction(actionContainers);
		int trans = transActionContainer.getActionCode();
		if ((trans == LEFTARC || trans == RIGHTARC || trans == LEFTARC2 || trans == RIGHTARC2) && !isActionContainersLabeled()) {
			return false;
		}
		Stack<DependencyNode> stack = config.getStack();
		Stack<DependencyNode> input = config.getInput();
		if ((trans == LEFTARC || trans == RIGHTARC) && stack.size() < 2) {
			return false;
		}
		if ((trans == LEFTARC2 || trans == RIGHTARC2) && stack.size() < 3) {
			return false;
		}
		if (trans == LEFTARC && stack.get(stack.size()-2).isRoot()) { 
			return false;
		}
		if (trans == LEFTARC2 && stack.get(stack.size()-3).isRoot()) {
			return false;
		}
		if (trans == SHIFT && input.isEmpty()) { 
			return false;
		}
		return true;
	}
	
	public GuideUserAction getDeterministicAction(GuideUserHistory history, ParserConfiguration config) throws MaltChainedException {
		return null;
	}
	
	protected void addAvailableTransitionToTable(TransitionTable ttable) throws MaltChainedException {
		ttable.addTransition(SHIFT, "SH", false, null);
		ttable.addTransition(RIGHTARC, "RA", true, null);
		ttable.addTransition(LEFTARC, "LA", true, null);
		ttable.addTransition(RIGHTARC2, "RA2", true, null);
		ttable.addTransition(LEFTARC2, "LA2", true, null);
	}
	
	protected void initWithDefaultTransitions(GuideUserHistory history) throws MaltChainedException {
		GuideUserAction currentAction = new ComplexDecisionAction((History)history);
		
		transActionContainer.setAction(SHIFT);
		for (int i = 0; i < arcLabelActionContainers.length; i++) {
			arcLabelActionContainers[i].setAction(-1);
		}
		currentAction.addAction(actionContainers);
	}
	
	public String getName() {
		return "attardi";
	}
	
	public GuideUserAction defaultAction(GuideUserHistory history, ParserConfiguration configuration) throws MaltChainedException {
		if (((StackConfig)configuration).getInput().isEmpty()) {
			return updateActionContainers(history, RIGHTARC, ((StackConfig)configuration).getDependencyGraph().getDefaultRootEdgeLabels());
		}
		
		return updateActionContainers(history, SHIFT, null);
	}
}

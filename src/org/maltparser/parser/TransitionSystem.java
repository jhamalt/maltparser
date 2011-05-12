package org.maltparser.parser;


import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.TableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;
import org.maltparser.parser.transition.TransitionTable;
import org.maltparser.parser.transition.TransitionTableHandler;
/**
 * @author Johan Hall
 *
 */
public abstract class TransitionSystem {
	protected HashMap<String, TableHandler> tableHandlers;
	protected TransitionTableHandler transitionTableHandler;
	protected ActionContainer[] actionContainers;
	protected ActionContainer transActionContainer;
	protected ActionContainer[] arcLabelActionContainers;
	
	protected ActionContainer[] nodeLabelActionContainers;
	
	protected PropagationManager propagationManager = null;
	
	public TransitionSystem() throws MaltChainedException {	}
	
	public abstract void apply(GuideUserAction currentAction, ParserConfiguration config) throws MaltChainedException;
	public abstract boolean permissible(GuideUserAction currentAction, ParserConfiguration config) throws MaltChainedException;
	public abstract GuideUserAction getDeterministicAction(GuideUserHistory history, ParserConfiguration config) throws MaltChainedException;
	protected abstract void addAvailableTransitionToTable(TransitionTable ttable) throws MaltChainedException;
	protected abstract void initWithDefaultTransitions(GuideUserHistory history) throws MaltChainedException;
	public abstract String getName();
	public abstract GuideUserAction defaultAction(GuideUserHistory history, ParserConfiguration configuration) throws MaltChainedException;
	
	protected GuideUserAction updateActionContainers(GuideUserHistory history, int transition, LabelSet arcLabels, LabelSet nodeLabels) throws MaltChainedException {	
		transActionContainer.setAction(transition);

		if (arcLabels == null) {
			for (int i = 0; i < arcLabelActionContainers.length; i++) {
				arcLabelActionContainers[i].setAction(-1);	
			}
		} 
		if (nodeLabels == null) {
			for (int i = 0; i < nodeLabelActionContainers.length; i++) {
				nodeLabelActionContainers[i].setAction(-1);	
			}
		} 
		if (arcLabels != null) {
			for (int i = 0; i < arcLabelActionContainers.length; i++) {
				if (arcLabelActionContainers[i] == null) {
					throw new MaltChainedException("arcLabelActionContainer " + i + " is null when doing transition " + transition);
				}
				
				Integer code = arcLabels.get(arcLabelActionContainers[i].getTable());
				if (code != null) {
					arcLabelActionContainers[i].setAction(code.shortValue());
				} else {
					arcLabelActionContainers[i].setAction(-1);
				}
			}
		}

		if (nodeLabels != null) {
			for (int i = 0; i < nodeLabelActionContainers.length; i++) {
				if (nodeLabelActionContainers[i] == null) {
					throw new MaltChainedException("nodeLabelActionContainers " + i + " is null when doing transition " + transition);
				}
				
				Integer code = nodeLabels.get(nodeLabelActionContainers[i].getTable());
				if (code != null) {
					nodeLabelActionContainers[i].setAction(code.shortValue());
				} else {
					nodeLabelActionContainers[i].setAction(-1);
				}
			}
		}
		
		GuideUserAction oracleAction = history.getEmptyGuideUserAction();
		oracleAction.addAction(actionContainers);
		return oracleAction;
	}
	
	protected boolean isActionContainersLabeled() {
		boolean arcIsLabeled = true;
		boolean nodeIsLabeled = true;
		
		for (int i = 0; i < arcLabelActionContainers.length; i++) {
			if (arcLabelActionContainers[i].getActionCode() < 0) {
				arcIsLabeled = false;
				break;
			}
		}
		if (nodeLabelActionContainers != null) {
			for (int i = 0; i < nodeLabelActionContainers.length; i++) {
				if (nodeLabelActionContainers[i].getActionCode() < 0) {
					nodeIsLabeled = false;
					break;
				}
			}
		}
		return arcIsLabeled || nodeIsLabeled;
	}
	
	protected void addEdgeLabels(Edge e) throws MaltChainedException {
		if (e != null) { 
			for (int i = 0; i < arcLabelActionContainers.length; i++) {
				if (arcLabelActionContainers[i].getActionCode() != -1) {
					e.addLabel((SymbolTable)arcLabelActionContainers[i].getTable(), arcLabelActionContainers[i].getActionCode());
				} else {
					e.addLabel((SymbolTable)arcLabelActionContainers[i].getTable(), ((DependencyGraph)e.getBelongsToGraph()).getDefaultRootEdgeLabelCode((SymbolTable)arcLabelActionContainers[i].getTable()));
				}
			}
			if (propagationManager != null) {
				propagationManager.propagate(e);
			}
		}
	}
	
	protected void addNodeLabels(DependencyNode n) throws MaltChainedException {
		if (n != null) {
			for (int i = 0; i < nodeLabelActionContainers.length; i++) {
				n.addLabel((SymbolTable)nodeLabelActionContainers[0].getTable(), nodeLabelActionContainers[0].getActionCode());
			}
		}
	}
	
	public void initTransitionSystem(GuideUserHistory history) throws MaltChainedException {
		this.actionContainers = history.getActionContainerArray();
		if (actionContainers.length < 1) {
			throw new ParsingException("Problem when initialize the history (sequence of actions). There are no action containers. ");
		}
		int nArcLabels = 0;
		int nNodeLabels = 0;
		for (int i = 0; i < actionContainers.length; i++) {
			if (actionContainers[i].getTableContainerName().startsWith("A.")) {
				nArcLabels++;
			}
			if (actionContainers[i].getTableContainerName().startsWith("N.")) {
				nNodeLabels++;
			}
		}
		int j = 0;
		int k = 0;
		for (int i = 0; i < actionContainers.length; i++) {
			if (actionContainers[i].getTableContainerName().equals("T.TRANS")) {
				transActionContainer = actionContainers[i];
			} else if (actionContainers[i].getTableContainerName().startsWith("A.")) {
				if (arcLabelActionContainers == null) {
					arcLabelActionContainers = new ActionContainer[nArcLabels];
				}
				arcLabelActionContainers[j++] = actionContainers[i];
			} else if (actionContainers[i].getTableContainerName().startsWith("N.")) {
				if (nodeLabelActionContainers == null) {
					nodeLabelActionContainers = new ActionContainer[nNodeLabels];
				}
				nodeLabelActionContainers[k++] = actionContainers[i];
			}
		}
		initWithDefaultTransitions(history);
	}
	
	public void initTableHandlers(String decisionSettings, SymbolTableHandler symbolTableHandler) throws MaltChainedException {
		transitionTableHandler = new TransitionTableHandler();
		tableHandlers = new HashMap<String, TableHandler>();
		
		final String[] decisionElements =  decisionSettings.split(",|#|;|\\+|\\||%");
		int nTrans = 0;
		for (int i = 0; i < decisionElements.length; i++) {
			int index = decisionElements[i].indexOf('.');
			if (index == -1) {
				throw new ParsingException("Decision settings '"+decisionSettings+"' contain an item '"+decisionElements[i]+"' that does not follow the format {TableHandler}.{Table}. ");
			}
			if (decisionElements[i].substring(0,index).equals("T")) {
				if (!getTableHandlers().containsKey("T")) {
					getTableHandlers().put("T", getTransitionTableHandler());
				}
				if (decisionElements[i].substring(index+1).equals("TRANS")) {
					if (nTrans == 0) {
						TransitionTable ttable = (TransitionTable)getTransitionTableHandler().addSymbolTable("TRANS");
						addAvailableTransitionToTable(ttable);
					} else {
						throw new ParsingException("Illegal decision settings '"+decisionSettings+"'");
					}
					nTrans++;
				}  
			} else if (decisionElements[i].substring(0,index).equals("A")) {
				if (!getTableHandlers().containsKey("A")) {
					getTableHandlers().put("A", symbolTableHandler);
				}
			} else if (decisionElements[i].substring(0,index).equals("N")) {
				if (!getTableHandlers().containsKey("N")) {
					getTableHandlers().put("N", symbolTableHandler);
				}
			} else {
				throw new ParsingException("The decision settings '"+decisionSettings+"' contains an unknown table handler '"+decisionElements[i].substring(0,index)+"'. " +
						"Only T (Transition table handler), A (ArcLabel table handler) and N (NodeLabel table handler) is allowed. ");
			}
		}
	}
	
	public void copyAction(GuideUserAction source, GuideUserAction target) throws MaltChainedException {
		source.getAction(actionContainers);
		target.addAction(actionContainers);
	}
	
	public HashMap<String, TableHandler> getTableHandlers() {
		return tableHandlers;
	}

	public TransitionTableHandler getTransitionTableHandler() {
		return transitionTableHandler;
	}
	
	public PropagationManager getPropagationManager() {
		return propagationManager;
	}

	
	public void setPropagationManager(PropagationManager propagationManager) {
		this.propagationManager = propagationManager;
	}

	public String getActionString(GuideUserAction action) throws MaltChainedException {
		StringBuilder sb = new StringBuilder();
		action.getAction(actionContainers);
		TransitionTable ttable = (TransitionTable)getTransitionTableHandler().getSymbolTable("TRANS");
		sb.append(ttable.getSymbolCodeToString(transActionContainer.getActionCode()));
		for (int i = 0; i < arcLabelActionContainers.length; i++) {
			if (arcLabelActionContainers[i].getActionCode() != -1) {
				sb.append("+");
				sb.append(arcLabelActionContainers[i].getTable().getSymbolCodeToString(arcLabelActionContainers[i].getActionCode()));
			}
		}
		for (int i = 0; i < nodeLabelActionContainers.length; i++) {
			if (nodeLabelActionContainers[i].getActionCode() != -1) {
				sb.append("+");
				sb.append(nodeLabelActionContainers[i].getTable().getSymbolCodeToString(nodeLabelActionContainers[i].getActionCode()));
			}
		}
		return sb.toString();
	}
}

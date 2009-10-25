package org.maltparser.core.syntaxgraph;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.flow.FlowChartInstance;
import org.maltparser.core.flow.item.ChartItem;
import org.maltparser.core.flow.spec.ChartItemSpecification;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
/**
*
*
* @author Johan Hall
*/
public class CopyChartItem extends ChartItem {
	private String targetName;
	private String sourceName;
	private String taskName;
	
	private TokenStructure cachedSource = null;
	private TokenStructure cachedTarget = null;
	
	public CopyChartItem() {}
	
	public void initialize(FlowChartInstance flowChartinstance, ChartItemSpecification chartItemSpecification) throws MaltChainedException {
		super.initialize(flowChartinstance, chartItemSpecification);
		for (String key : chartItemSpecification.getChartItemAttributes().keySet()) {
			if (key.equals("target")) {
				targetName = chartItemSpecification.getChartItemAttributes().get(key);
			} else if (key.equals("source")) {
				sourceName = chartItemSpecification.getChartItemAttributes().get(key);
			}  else if (key.equals("task")) {
				taskName = chartItemSpecification.getChartItemAttributes().get(key);
			}
		}
		if (targetName == null) {
			targetName = getChartElement("copy").getAttributes().get("target").getDefaultValue();
		} else if (sourceName == null) {
			sourceName = getChartElement("copy").getAttributes().get("source").getDefaultValue();
		} else if (taskName == null) {
			taskName = getChartElement("copy").getAttributes().get("task").getDefaultValue();
		}
	}
	
	public int preprocess(int signal) throws MaltChainedException {
		return signal;
	}
	
	public int process(int signal) throws MaltChainedException {
		if (taskName.equals("terminals")) {
			if (cachedSource == null) {
				cachedSource = (TokenStructure)flowChartinstance.getFlowChartRegistry(org.maltparser.core.syntaxgraph.TokenStructure.class, sourceName);
			}
			if (cachedTarget == null) {
				cachedTarget = (TokenStructure)flowChartinstance.getFlowChartRegistry(org.maltparser.core.syntaxgraph.TokenStructure.class, targetName);
			}
			copyTerminalStructure(cachedSource, cachedTarget);
		}
		return signal;
	}
	
	public int postprocess(int signal) throws MaltChainedException {
		return signal;
	}

	
	public void terminate() throws MaltChainedException {
		cachedSource = null;
		cachedTarget = null;
	}
	
	public void copyTerminalStructure(TokenStructure sourceGraph, TokenStructure targetGraph) throws MaltChainedException {
		targetGraph.clear();
		for (int index : sourceGraph.getTokenIndices()) {
			DependencyNode gnode = sourceGraph.getTokenNode(index);
			DependencyNode pnode = targetGraph.addTokenNode(gnode.getIndex());
			for (SymbolTable table : gnode.getLabelTypes()) {
				pnode.addLabel(table, gnode.getLabelSymbol(table));
			}
		}
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return obj.toString().equals(this.toString());
	}
	
	public int hashCode() {
		return 217 + (null == toString() ? 0 : toString().hashCode());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("    copy ");
		sb.append("task:");
		sb.append(taskName);
		sb.append(' ');
		sb.append("source:");
		sb.append(sourceName);
		sb.append(' ');
		sb.append("target:");
		sb.append(targetName);
		return sb.toString();
	}
}

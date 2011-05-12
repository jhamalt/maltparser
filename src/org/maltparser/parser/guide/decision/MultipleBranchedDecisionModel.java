package org.maltparser.parser.guide.decision;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.GuideException;
import org.maltparser.parser.guide.instance.AtomicModel;
import org.maltparser.parser.guide.instance.FeatureDivideModel;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.GuideDecision;
import org.maltparser.parser.history.action.MultipleDecision;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.container.TableContainer.RelationToNextDecision;
/**
*
* @author Johan Hall
* @since 1.1
**/
public class MultipleBranchedDecisionModel implements DecisionModel {
	private ClassifierGuide guide;
	private String modelName;
	private FeatureModel featureModel;
	private InstanceModel instanceModel;
	private int decisionIndex;
	private DecisionModel parentDecisionModel;
	private HashMap<Integer,DecisionModel> children;
	private String branchedDecisionSymbols;
	
	public MultipleBranchedDecisionModel(ClassifierGuide guide, FeatureModel featureModel) throws MaltChainedException {
		this.branchedDecisionSymbols = "";
		setGuide(guide);
		setFeatureModel(featureModel);
		setDecisionIndex(0);
		setModelName("bdm"+decisionIndex);
		setParentDecisionModel(null);
		System.out.println("MN:"+getModelName());
	}
	
	public MultipleBranchedDecisionModel(ClassifierGuide guide, DecisionModel parentDecisionModel, String branchedDecisionSymbol, Integer decisionIndex) throws MaltChainedException {
		if (branchedDecisionSymbol != null && branchedDecisionSymbol.length() > 0) {
			this.branchedDecisionSymbols = branchedDecisionSymbol;
		} else {
			this.branchedDecisionSymbols = "";
		}
		setGuide(guide);
		setParentDecisionModel(parentDecisionModel);
		setDecisionIndex(decisionIndex);
		setFeatureModel(parentDecisionModel.getFeatureModel());
		if (branchedDecisionSymbols != null && branchedDecisionSymbols.length() > 0) {
			setModelName("bdm"+decisionIndex+branchedDecisionSymbols);
		} else {
			setModelName("bdm"+decisionIndex);
		}
		this.parentDecisionModel = parentDecisionModel;
	}
	
	public void updateFeatureModel() throws MaltChainedException {
		featureModel.update();
	}
	

	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.finalizeSentence(dependencyGraph);
		}
		if (children != null) {
			for (DecisionModel child : children.values()) {
				child.finalizeSentence(dependencyGraph);
			}
		}
	}
	
	public void noMoreInstances() throws MaltChainedException {
		if (guide.getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			throw new GuideException("The decision model could not create it's model. ");
		}
		if (instanceModel != null) {
			instanceModel.noMoreInstances();
			instanceModel.train();
		}
		if (children != null) {
			for (DecisionModel child : children.values()) {
				child.noMoreInstances();
			}
		}
	}

	public void terminate() throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.terminate();
			instanceModel = null;
		}
		if (children != null) {
			for (DecisionModel child : children.values()) {
				child.terminate();
			}
		}
	}
	
	public void addInstance(GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		updateFeatureModel();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(singleDecision.getTableContainer().getTableContainerName());
		}
		
		instanceModel.addInstance(singleDecision);

//		System.out.println("MB "+singleDecision.getDecisionSymbol());
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision() && singleDecision.getRelationToNextDecision() == RelationToNextDecision.MULTIPLE_BRANCHED) {
				if (children == null) {
					children = new HashMap<Integer,DecisionModel>();
				}
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child == null) {
					SingleDecision sd = null;
					int index = -1;
					if (singleDecision.getDecisionSymbol().equals("LA") || singleDecision.getDecisionSymbol().equals("RA")) {
//						System.out.println(decisionIndex+1);
						index = decisionIndex+1;
						sd = ((MultipleDecision)decision).getSingleDecision(decisionIndex+1);
					} else {
//						System.out.println(decisionIndex+2);
						index = decisionIndex+2;
						sd = ((MultipleDecision)decision).getSingleDecision(decisionIndex+2);
					}
//					System.out.println(sd.getDecisionSymbol());
					child = initChildDecisionModel(decision, index,
							branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
					children.put(singleDecision.getDecisionCode(), child);
				}
				child.addInstance(decision);
			}
		}
	}
	
	public boolean predict(GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		updateFeatureModel();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(singleDecision.getTableContainer().getTableContainerName());
		}
		instanceModel.predict(singleDecision);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision() && singleDecision.getRelationToNextDecision() == RelationToNextDecision.MULTIPLE_BRANCHED) {
				if (children == null) {
					children = new HashMap<Integer,DecisionModel>();
				}
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child == null) {
					int index = -1;
					if (singleDecision.getDecisionSymbol().equals("LA") || singleDecision.getDecisionSymbol().equals("RA")) {
						index = decisionIndex+1;
					} else {
						index = decisionIndex+2;
					}
					child = initChildDecisionModel(decision, index,
							branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
					children.put(singleDecision.getDecisionCode(), child);
				}
				child.predict(decision);
			}
		}

		return true;
	}
	
	public FeatureVector predictExtract(GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		updateFeatureModel();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(singleDecision.getTableContainer().getTableContainerName());
		}
		FeatureVector fv = instanceModel.predictExtract(singleDecision);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision() && singleDecision.getRelationToNextDecision() == RelationToNextDecision.MULTIPLE_BRANCHED) {
				if (children == null) {
					children = new HashMap<Integer,DecisionModel>();
				}
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child == null) {
					int index = -1;
					if (singleDecision.getDecisionSymbol().equals("LA") || singleDecision.getDecisionSymbol().equals("RA")) {
						index = decisionIndex+1;
					} else {
						index = decisionIndex+2;
					}
					child = initChildDecisionModel(decision, index,
							branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
					children.put(singleDecision.getDecisionCode(), child);
				}
				child.predictExtract(decision);
			}
		}

		return fv;
	}
	
	public FeatureVector extract() throws MaltChainedException {
		updateFeatureModel();
		return instanceModel.extract(); // TODO handle many feature vectors
	}
	
	public boolean predictFromKBestList(GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		
		boolean success = false;
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision() && singleDecision.getRelationToNextDecision() == RelationToNextDecision.MULTIPLE_BRANCHED) {
				if (children == null) {
					children = new HashMap<Integer,DecisionModel>();
				}
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child != null) {
					success = child.predictFromKBestList(decision);
				}
				
			}
		}
		if (!success) {
			success = singleDecision.updateFromKBestList();
			if (decisionIndex+1 < decision.numberOfDecisions()) {
				if (singleDecision.continueWithNextDecision()) {
					if (children == null) {
						children = new HashMap<Integer,DecisionModel>();
					}
					DecisionModel child = children.get(singleDecision.getDecisionCode());
					if (child == null) {
						int index = -1;
						if (singleDecision.getDecisionSymbol().equals("LA") || singleDecision.getDecisionSymbol().equals("RA")) {
							index = decisionIndex+1;
						} else {
							index = decisionIndex+2;
						}
						child = initChildDecisionModel(decision, index,
								branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
						children.put(singleDecision.getDecisionCode(), child);
					}
					child.predict(decision);
				}
			}
		}
		return success;
	}
	

	public ClassifierGuide getGuide() {
		return guide;
	}

	public String getModelName() {
		return modelName;
	}
	
	public FeatureModel getFeatureModel() {
		return featureModel;
	}

	public int getDecisionIndex() {
		return decisionIndex;
	}

	public DecisionModel getParentDecisionModel() {
		return parentDecisionModel;
	}

	private void setFeatureModel(FeatureModel featureModel) {
		this.featureModel = featureModel;
	}
	
	private void setDecisionIndex(int decisionIndex) {
		this.decisionIndex = decisionIndex;
	}
	
	private void setParentDecisionModel(DecisionModel parentDecisionModel) {
		this.parentDecisionModel = parentDecisionModel;
	}

	private void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	private void setGuide(ClassifierGuide guide) {
		this.guide = guide;
	}
	
	
	private DecisionModel initChildDecisionModel(GuideDecision decision, int index, String branchedDecisionSymbol) throws MaltChainedException {
		Class<?> decisionModelClass = null;
		SingleDecision nextDecision = ((MultipleDecision)decision).getSingleDecision(index);
		if (nextDecision.getRelationToNextDecision() == RelationToNextDecision.MULTIPLE_BRANCHED_ITEM) {
			decisionModelClass = org.maltparser.parser.guide.decision.MultipleBranchedDecisionModel.class;
		}

		if (decisionModelClass == null) {
			throw new GuideException("Could not find an appropriate decision model for the relation to the next decision"); 
		}
		
		try {
			Class<?>[] argTypes = { org.maltparser.parser.guide.ClassifierGuide.class, org.maltparser.parser.guide.decision.DecisionModel.class, 
						java.lang.String.class, java.lang.Integer.class};
			Object[] arguments = new Object[4];
			arguments[0] = getGuide();
			arguments[1] = this;
			arguments[2] = branchedDecisionSymbol;
			arguments[3] = index;
			Constructor<?> constructor = decisionModelClass.getConstructor(argTypes);
			return (DecisionModel)constructor.newInstance(arguments);
		} catch (NoSuchMethodException e) {
			throw new GuideException("The decision model class '"+decisionModelClass.getName()+"' cannot be initialized. ", e);
		} catch (InstantiationException e) {
			throw new GuideException("The decision model class '"+decisionModelClass.getName()+"' cannot be initialized. ", e);
		} catch (IllegalAccessException e) {
			throw new GuideException("The decision model class '"+decisionModelClass.getName()+"' cannot be initialized. ", e);
		} catch (InvocationTargetException e) {
			throw new GuideException("The decision model class '"+decisionModelClass.getName()+"' cannot be initialized. ", e);
		}
	}
	
	private void initInstanceModel(String subModelName) throws MaltChainedException {
		FeatureVector fv = featureModel.getFeatureVector(branchedDecisionSymbols+"."+subModelName);
		if (fv == null) {
			fv = featureModel.getFeatureVector(subModelName);
		}
		if (fv == null) {
			fv = featureModel.getMainFeatureVector();
		}
		
		DependencyParserConfig c = guide.getConfiguration();
	
		if (c.getOptionValue("guide", "data_split_column").toString().length() == 0) {
			instanceModel = new AtomicModel(-1, fv, this);
		} else {
			instanceModel = new FeatureDivideModel(fv, this);
		}
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(modelName + ", ");
		for (DecisionModel model : children.values()) {
			sb.append(model.toString() + ", ");
		}
		return sb.toString();
	}
}
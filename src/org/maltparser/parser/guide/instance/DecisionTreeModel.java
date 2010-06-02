package org.maltparser.parser.guide.instance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.function.Modifiable;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.GuideException;
import org.maltparser.parser.guide.Model;
import org.maltparser.parser.history.action.SingleDecision;

/**
 * 
 * @author Kjell Winblad
 */
public class DecisionTreeModel implements InstanceModel {

	
	private Map<Integer, Integer> divideFeatureIdToCountMap = new HashMap<Integer, Integer>();
	/*
	 * The leaf nodes needs a int index that is unique among all leaf nodes because they have an AtomicModel
	 * which need such an index.
	 */
	//private static Map<String, Integer> modelNameToIndexMap = null;
	//This is increased by one for every sub DecisionTreeModel that is created
	private static int leafModelIndexConter = 0;
	
	
	private final static int OTHER_BRANCH_ID = 999;//Integer.MAX_VALUE;
	
	//The number of division used when doing cross validation test
	private int numberOfCrossValidationSplits = 10;
	/*
	 * Cross validation accuracy is calculated for every node during training
	 * This should be calculated for every node and is set to -1.0 if it isn't calculated yet
	 */
	private final static double CROSS_VALIDATION_ACCURACY_NOT_SET_VALUE = -1.0;
	private double crossValidationAccuracy = CROSS_VALIDATION_ACCURACY_NOT_SET_VALUE ;
	//The parent model
	private Model parent = null;
	//An ordered list of features to divide on 
	private LinkedList<FeatureFunction> divideFeatures = null;
	/*
	 * The branches of the tree
	 * Is set to null if this is a leaf node
	 */
	private SortedMap<Integer,DecisionTreeModel> branches = null;
	
	/*
	 * This model is used if this is a leaf node
	 * Is set to null if this is a branch node
	 */
	private AtomicModel leafModel = null;
	//Number of training instances added
	private int frequency = 0;
	/*
	 *min number of instances for a node to exist
	 *All sub nodes with less instances will be concatenated to one sub node 
	 */
	private int divideThreshold = 0;
	//The feature vector for this problem
	private FeatureVector featureVector;
	
	
	private FeatureVector subFeatureVector = null;

	//Used to indicate that the modelIndex field is not set
	private static final int MODEL_INDEX_NOT_SET = Integer.MIN_VALUE;
	/*
	 * Model index is the identifer used to distinguish this model from other models at
	 * the same level. This should not be used in the root model and has the value
	 * MODEL_INDEX_NOT_SET in it.
	 */
	private int modelIndex =  MODEL_INDEX_NOT_SET;
	private ArrayList<Integer> divideFeatureIndexVector;
	
	


	/**
	 * Constructs a feature divide model.
	 * 
	 * @param features the feature vector used by the atomic model.
	 * @param parent the parent guide model.
	 * @throws MaltChainedException
	 */
	public DecisionTreeModel(FeatureVector featureVector, Model parent) throws MaltChainedException {
		
		this.featureVector = featureVector;
		this.divideFeatures = new LinkedList<FeatureFunction>();
		setParent(parent);
		setFrequency(0);
		initDecisionTreeParam();
		
		
		
		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
			/*
			modelNameToIndexMap = new TreeMap<String, Integer>();
			
			modelNameToIndexMap.put(getModelName(), leafModelIndexConter);
			*/
			//Prepare for training
			branches = new TreeMap<Integer,DecisionTreeModel>();
			leafModel = new AtomicModel(-1, featureVector, this);
			//masterModel = new AtomicModel(-1, masterFeatureVector, this);
		} else if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			load();
		}
	}
	
	
	/*
	 * This constructor is used from the class when the parameters are already set
	 * 
	 * @param features the feature vector used by the atomic model.
	 * @param parent the parent guide model.
	 * @throws MaltChainedException
	 */
	private DecisionTreeModel(int modelIndex, FeatureVector featureVector,
			Model parent, LinkedList<FeatureFunction> divideFeatures,
			int divideThreshold) throws MaltChainedException {

		this.featureVector = featureVector;

		setParent(parent);
		setFrequency(0);

		this.modelIndex = modelIndex;
		this.divideFeatures = divideFeatures;
		this.divideThreshold = divideThreshold;

		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.BATCH) {

			if (divideFeatures.size() > 0) {

				divideFeatureIndexVector = new ArrayList<Integer>();
				for (int i = 0; i < featureVector.size(); i++) {
					if (featureVector.get(i).equals(divideFeatures.get(0))) {
						divideFeatureIndexVector.add(i);
					}
				}

			}
			leafModelIndexConter++;
			//modelNameToIndexMap.put(getModelName(), leafModelIndexConter);

			// Prepare for training
			branches = new TreeMap<Integer, DecisionTreeModel>();
			leafModel = new AtomicModel(-1, featureVector, this);

			// masterModel = new AtomicModel(-1, masterFeatureVector, this);
		} else if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			load();
		}
	}
	
	
	/**
	 * Loads the feature divide model settings .fsm file.
	 * 
	 * @throws MaltChainedException
	 */
	protected void load() throws MaltChainedException {
		
		
		ConfigurationDir configDir = getGuide().getConfiguration().getConfigurationDir();
		
		/*if(modelNameToIndexMap==null){
			
			modelNameToIndexMap = new TreeMap<String, Integer>();
			
			//Load the modelNameToIndexMap from file
			try {
				final BufferedReader in = new BufferedReader(configDir.getInputStreamReaderFromConfigFile(getModelName()+".nmf"));
				final Pattern tabPattern = Pattern.compile("\t");
				while(true) {
					String line = in.readLine();
					if(line == null) break;
					String[] cols = tabPattern.split(line);
					if (cols.length != 2) { 
						throw new GuideException("");
					}
					String name = null;
					int index = 0;
					try {
						name = cols[0];
						index = Integer.parseInt(cols[1]);
					} catch (NumberFormatException e) {
						throw new GuideException("Could not convert a string value into an integer value when loading the feature divide model settings (.nmf). ", e);
					}
					
					//modelNameToIndexMap.put(name, index);
					
				}
				in.close();
			} catch (IOException e) {
				throw new GuideException("Could not read from the guide model settings file '"+getModelName()+".dsm"+"', when " +
						"loading the guide model settings. ", e);
			}
			
		}*/
		


		//load the dsm file
			
			try{
				
			final BufferedReader in = new BufferedReader(configDir.getInputStreamReaderFromConfigFile(getModelName()+".dsm"));
			final Pattern tabPattern = Pattern.compile("\t");
			
			boolean first = true;
			while(true) {
				String line = in.readLine();
				if(line == null) break;
				String[] cols = tabPattern.split(line);
				if (cols.length != 2) { 
					throw new GuideException("");
				}
				int code = -1;
				int freq = 0;
				try {
					code = Integer.parseInt(cols[0]);
					freq = Integer.parseInt(cols[1]);
				} catch (NumberFormatException e) {
					throw new GuideException("Could not convert a string value into an integer value when loading the feature divide model settings (.fsm). ", e);
				}

				if(code==MODEL_INDEX_NOT_SET){
					if(!first)
						throw new GuideException("Error in config file '"+getModelName()+".dsm"+"'. If the index in the .dsm file is MODEL_INDEX_NOT_SET it should be the first.");
						
					first = false;
					//It is a leaf node
					//Create atomic model for the leaf node
					leafModel = new AtomicModel(-1, featureVector, this);
					
					//setIsLeafNode();
					
				}else{
					if(first){
						//Create the branches holder
						
						branches = new TreeMap<Integer, DecisionTreeModel>();
						
						//setIsBranchNode();
						
						first = false;
					}
					
					if(branches==null)
						throw new GuideException("Error in config file '"+getModelName()+".dsm"+"'. If MODEL_INDEX_NOT_SET is the first model index in the .dsm file it should be the only.");
					
					if(code==OTHER_BRANCH_ID)
						branches.put(code, new DecisionTreeModel(code, featureVector, this, new LinkedList<FeatureFunction>(), divideThreshold));
					else
						branches.put(code, new DecisionTreeModel(code, getSubFeatureVector(), this, createNextLevelDivideFeatures(), divideThreshold));
					
					branches.get(code).setFrequency(freq);

					setFrequency(getFrequency()+freq);
						
					
				}
				
					
			}
			in.close();
			
			


			} catch (IOException e) {
				throw new GuideException("Could not read from the guide model settings file '"+getModelName()+".dsm"+"', when " +
						"loading the guide model settings. ", e);
			}
					

	}


	private void initDecisionTreeParam() throws MaltChainedException{
		String treeSplitColumns = getGuide().getConfiguration().getOptionValue("guide", "tree_split_columns").toString();
		String treeSplitStructures = getGuide().getConfiguration().getOptionValue("guide", "tree_split_structures").toString();
		if (treeSplitColumns == null 
				|| treeSplitColumns.length() == 0) {
			throw new GuideException("The option '--guide-tree_split_columns' cannot be found, when initializing the decision tree model. ");
		}

		if (treeSplitStructures == null 
				|| treeSplitStructures.length() == 0) {
			throw new GuideException("The option '--guide-tree_split_structures' cannot be found, when initializing the decision tree model. ");
		}
		
		
		String[] treeSplitColumnsArray = treeSplitColumns.split("@");
		String[] treeSplitStructuresArray = treeSplitStructures.split("@");
		
		if(treeSplitColumnsArray.length != treeSplitStructuresArray.length)
			throw new GuideException("The option '--guide-tree_split_structures' and '--guide-tree_split_columns' must be followed by a ; separated lists of the same length");			
		
		try {
		
			
			for(int n =0; n < treeSplitColumnsArray.length ; n++){
				
				
				final String spec = "InputColumn(" + treeSplitColumnsArray[n].trim()+
				", "+treeSplitStructuresArray[n].trim() +")";
				
				divideFeatures.addLast(featureVector.getFeatureModel().identifyFeature(spec));
			}
			
		
		} catch (FeatureException e) {
			throw new GuideException("The data split feature 'InputColumn("+getGuide().getConfiguration().getOptionValue("guide", "data_split_column").toString()+", "+getGuide().getConfiguration().getOptionValue("guide", "data_split_structure").toString()+") cannot be initialized. ", e);
		}
		
		for(FeatureFunction divideFeature:divideFeatures){
			if (!(divideFeature instanceof Modifiable)) {
				throw new GuideException("The data split feature 'InputColumn("+getGuide().getConfiguration().getOptionValue("guide", "data_split_column").toString()+", "+getGuide().getConfiguration().getOptionValue("guide", "data_split_structure").toString()+") does not implement Modifiable interface. ");
			}
		}
		

		divideFeatureIndexVector = new ArrayList<Integer>();
		for (int i = 0; i < featureVector.size(); i++) {

			if (featureVector.get(i).equals(divideFeatures.get(0))) {

				divideFeatureIndexVector.add(i);
			}
		}
		
		if(divideFeatureIndexVector.size()==0){
			throw new GuideException("Could not match the given divide features to any of the available features.");
		}

// Remember this		
//		divideFeatureIndexVector = new ArrayList<Integer>();
//		for (int i = 0; i < featureVector.size(); i++) {
//			if (featureVector.get(i).equals(divideFeature.get)) {
//				divideFeatureIndexVector.add(i);
//			}
//		}
//		
//			masterFeatureVector = featureVector;
//			divideFeatureVector = (FeatureVector)featureVector.clone();
//			for (Integer i : divideFeatureIndexVector) {
//				divideFeatureVector.remove(divideFeatureVector.get(i));
//			}
//		}
		try {
			
			String treeSplitTreshold = getGuide().getConfiguration().getOptionValue("guide", "tree_split_threshold").toString();
			
			if (treeSplitTreshold != null && treeSplitTreshold.length() > 0) {
				
				divideThreshold = Integer.parseInt(treeSplitTreshold);
			
			} else {
				divideThreshold = 0;
			}
		} catch (NumberFormatException e) {
			throw new GuideException("The --guide-tree_split_threshold option is not an integer value. ", e);
		}
		
		try {
			
			String treeNumberOfCrossValidationDivisions = getGuide().getConfiguration().getOptionValue("guide", "tree_number_of_cross_validation_divisions").toString();
			
			if (treeNumberOfCrossValidationDivisions != null && treeNumberOfCrossValidationDivisions.length() > 0) {
				
				numberOfCrossValidationSplits = Integer.parseInt(treeNumberOfCrossValidationDivisions);
			
			} else {
				divideThreshold = 0;
			}
		} catch (NumberFormatException e) {
			throw new GuideException("The --guide-tree_number_of_cross_validation_divisions option is not an integer value. ", e);
		}
		
	}



	@Override
	public void addInstance(SingleDecision decision)
			throws MaltChainedException {

		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			throw new GuideException("Can only add instance during learning. ");
		} else if (divideFeatures.size() > 0) {
			FeatureFunction divideFeature = divideFeatures.getFirst();

			if (!(divideFeature.getFeatureValue() instanceof SingleFeatureValue)) {
				throw new GuideException(
						"The divide feature does not have a single value. ");
			}

			divideFeature.update();

			

			int divideFeatureCode = ((SingleFeatureValue) divideFeature
					.getFeatureValue()).getCode();

			
			if (!divideFeatureIdToCountMap.containsKey(divideFeatureCode)) {

				divideFeatureIdToCountMap.put(divideFeatureCode, 0);

				

			}

			int previousCount = divideFeatureIdToCountMap.get(divideFeatureCode);
			
			divideFeatureIdToCountMap.put(divideFeatureCode, previousCount + 1);
			
			leafModel.addInstance(decision);




		} else {
			// Model has already been decided. It is a leaf node
			if(branches!=null)
				setIsLeafNode();
			
			leafModel.addInstance(decision);

		}

	}

	
	@SuppressWarnings("unchecked")
	private LinkedList<FeatureFunction> createNextLevelDivideFeatures() {
		


		LinkedList<FeatureFunction> nextLevelDivideFeatures = (LinkedList<FeatureFunction>) divideFeatures
		.clone();

		nextLevelDivideFeatures.removeFirst();
		
		return nextLevelDivideFeatures;
	}


	/*
	 * Removes the current divide feature from the feature vector so it is not
	 * present in the sub node
	 */
	private FeatureVector getSubFeatureVector() {

		if(subFeatureVector!=null)
			return subFeatureVector;
		
		FeatureFunction divideFeature = divideFeatures.getFirst();

		ArrayList<Integer> divideFeatureIndexVector = new ArrayList<Integer>();
		for (int i = 0; i < featureVector.size(); i++) {
			if (featureVector.get(i).equals(divideFeature)) {
				divideFeatureIndexVector.add(i);
			}
		}

		FeatureVector divideFeatureVector = (FeatureVector) featureVector
				.clone();
		
		for (Integer i : divideFeatureIndexVector) {
			divideFeatureVector.remove(divideFeatureVector.get(i));
		}

		subFeatureVector = divideFeatureVector;
		
		return divideFeatureVector;
	}


	@Override
	public FeatureVector extract() throws MaltChainedException {
		
		return getCurrentAtomicModel().extract();
	
	}
	
	/*
	 * Returns the atomic model that is effected by this parsing step
	 */
	private AtomicModel getCurrentAtomicModel() throws MaltChainedException {
		
		
		
		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
			throw new GuideException("Can only predict during parsing. ");
		}
		
		if(branches==null && leafModel!=null)
			return leafModel;
		
		
		FeatureFunction divideFeature = divideFeatures.getFirst();
		
		if (!(divideFeature.getFeatureValue() instanceof SingleFeatureValue)) {
			throw new GuideException("The divide feature does not have a single value. ");
		}
		
		if (branches != null && branches.containsKey(((SingleFeatureValue)divideFeature.getFeatureValue()).getCode())) {
			return branches.get(((SingleFeatureValue)divideFeature.getFeatureValue()).getCode()).getCurrentAtomicModel();
		} else if (branches.containsKey(OTHER_BRANCH_ID)  && branches.get(OTHER_BRANCH_ID).getFrequency() > 0) {
			return branches.get(OTHER_BRANCH_ID).getCurrentAtomicModel();
		} else {
			getGuide().getConfiguration().getConfigLogger().info("Could not predict the next parser decision because there is " +
					"no divide or master model that covers the divide value '"+((SingleFeatureValue)divideFeature.getFeatureValue()).getCode()+"', as default" +
							" class code '1' is used. ");
		}
		return null;
	}

	/**
	 * Increase the frequency by 1
	 */
	public void increaseFrequency() {
		//if (parent instanceof InstanceModel) {
		//	((InstanceModel)parent).increaseFrequency();
		//}
		frequency++;
	}
	
	public void decreaseFrequency() {
		//if (parent instanceof InstanceModel) {
		//	((InstanceModel)parent).decreaseFrequency();
		//}
		frequency--;
	}

	@Override
	public boolean predict(SingleDecision decision) throws MaltChainedException {
		

		
		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
			throw new GuideException("Can only predict during parsing. ");
		} else if (divideFeatures.size()>0 && !(divideFeatures.getFirst().getFeatureValue() instanceof SingleFeatureValue)) {
			throw new GuideException("The divide feature does not have a single value. ");
		} 
		
		//divideFeature.update();
		if (branches != null && branches.containsKey(((SingleFeatureValue)divideFeatures.getFirst().getFeatureValue()).getCode())) {
			//System.out.print("BRANCH PREDICT " + getModelName() + " ");
			return branches.get(((SingleFeatureValue)divideFeatures.getFirst().getFeatureValue()).getCode()).predict(decision);
		}else if (branches != null && branches.containsKey(OTHER_BRANCH_ID)) {
			//System.out.print("BRANCH PREDICT OTHER "  + getModelName() + " ");
			return branches.get(OTHER_BRANCH_ID).predict(decision);		
		}else if (leafModel != null /*&& leafModel.getFrequency() > 0*/) {
			//System.out.print("LEAF PREDICT "  + getModelName() + " ");
			return leafModel.predict(decision);
		} else {
			
			for(Entry<Integer, DecisionTreeModel>  b:branches.entrySet()){
				System.out.print(b.getKey());

				System.out.print( " | ");
			}
			
			System.out.println( getModelName());
			System.out.println("OTHER BRANCH ID " + OTHER_BRANCH_ID);
			
			getGuide().getConfiguration().getConfigLogger().info("Could not predict the next parser decision because there is " +
					"no divide or master model that covers the divide value '"+((SingleFeatureValue)divideFeatures.getFirst().getFeatureValue()).getCode()+"', as default" +
							" class code '1' is used. ");
			
			decision.addDecision(1); // default prediction
			//classCodeTable.getEmptyKBestList().addKBestItem(1); 
		}
		return true;
	}

	@Override
	public FeatureVector predictExtract(SingleDecision decision)
			throws MaltChainedException {
		return getCurrentAtomicModel().predictExtract(decision);
	}


	/*
	 * Decides if this is a branch or leaf node by doing cross validation and returns the cross validation score for this node
	 */
	private double decideNodeType() throws MaltChainedException {

		//We don't want to do this twice test
		if(crossValidationAccuracy!=CROSS_VALIDATION_ACCURACY_NOT_SET_VALUE)
			return crossValidationAccuracy;
		 
		if(modelIndex==MODEL_INDEX_NOT_SET )
			if (getGuide().getConfiguration().getConfigLogger().isInfoEnabled()) {
				getGuide().getConfiguration().getConfigLogger().info("Starting deph first pruning of the decision tree\n");
			}
		
		long start = System.currentTimeMillis();

		double leafModelCrossValidationAccuracy = leafModel.getMethod().crossValidate(featureVector, numberOfCrossValidationSplits);
		
		long stop = System.currentTimeMillis();

		if (getGuide().getConfiguration().getConfigLogger().isInfoEnabled()) {
			getGuide().getConfiguration().getConfigLogger().info("Cross Validation Time: " + (stop - start) + " ms"  + " for model " + getModelName() + "\n");
		}
		
		if (getGuide().getConfiguration().getConfigLogger().isInfoEnabled()) {
			getGuide().getConfiguration().getConfigLogger().info("Cross Validation Accuracy as leaf node = " + leafModelCrossValidationAccuracy + " for model " + getModelName() + "\n");
		}
		
		
		if(branches == null && leafModel != null){//If it is already decided that this is a leaf node
			
			crossValidationAccuracy = leafModelCrossValidationAccuracy;
			
			return crossValidationAccuracy;
		
		}
		
		
		int totalFrequency = 0;
		double totalAccuracyCount = 0.0;
		//Calculate crossValidationAccuracy for  branch nodes
		for(DecisionTreeModel b: branches.values()){
			
			double bAccuracy = b.decideNodeType();
			
			totalFrequency = totalFrequency + b.getFrequency();
			
			totalAccuracyCount = totalAccuracyCount + bAccuracy * b.getFrequency();
			
		}
		
		double branchModelCrossValidationAccuracy = totalAccuracyCount/totalFrequency;
		
		if (getGuide().getConfiguration().getConfigLogger().isInfoEnabled()) {
			getGuide().getConfiguration().getConfigLogger().info("Total Cross Validation Accuracy for branches = " + branchModelCrossValidationAccuracy + " for model " + getModelName() + "\n");
		}
		
		//Finally decide which model to use
		if(branchModelCrossValidationAccuracy > leafModelCrossValidationAccuracy){
			
			setIsBranchNode();
			
			crossValidationAccuracy = branchModelCrossValidationAccuracy;
			
			return crossValidationAccuracy;
			
		}else{
			
			setIsLeafNode();
			
			crossValidationAccuracy = leafModelCrossValidationAccuracy;
			
			return crossValidationAccuracy;
			
			
		}
		
		
		
		

		
	}
	
	@Override
	public void train() throws MaltChainedException {
		
		//Decide node type
		//This operation is more expensive than the training itself
		decideNodeType();
		
		//Do the training depending on which type of node this is
		if(branches == null && leafModel != null){
			
			//If it is a leaf node
			
			leafModel.train();
			
			save();
			
			leafModel.terminate();
		
		}else{
			//It is a branch node

			for (DecisionTreeModel b : branches.values()) 
				b.train();
			
			save();
			
			for (DecisionTreeModel b : branches.values()) 
				b.terminate();
			
				
		}
		terminate();

		
	}

	/**
	 * Saves the feature divide model settings .fsm file, as well as the name mapping file .nmf if it is the root node.
	 * 
	 * @throws MaltChainedException
	 */
	private void save() throws MaltChainedException {
		try {
			
			//System.out.println("CALLING SAVE IN " + getModelName());
			
			final BufferedWriter out = new BufferedWriter(getGuide().getConfiguration().getConfigurationDir().getOutputStreamWriter(getModelName()+".dsm"));
			
			if (branches != null) {
				for (DecisionTreeModel b : branches.values()) {
					out.write(b.getModelIndex() + "\t" + b.getFrequency() + "\n");
	        	}
			}else{
				out.write(MODEL_INDEX_NOT_SET + "\t" + getFrequency() + "\n");
			}

			out.close();
			
			//if (branches != null)
			//	for (DecisionTreeModel b : branches.values()) {
			//		b.save();
			//	}
			
			/*if(modelIndex==MODEL_INDEX_NOT_SET){
				//This is the root branch
				//Save the name to leaf node mapping to a file
				final BufferedWriter mappingFileOut = new BufferedWriter(getGuide().getConfiguration().getConfigurationDir().getOutputStreamWriter(getModelName()+".nmf"));
				
				if (branches != null) {
					for (Entry<String, Integer> e : modelNameToIndexMap.entrySet()) {
						mappingFileOut.write(e.getKey() + "\t" + e.getValue() + "\n");
		        	}
				}

				mappingFileOut.close();
				
				
			}*/
			
		} catch (IOException e) {
			throw new GuideException("Could not write to the guide model settings file '"+getModelName()+".dsm"+"' or the name mapping file '"+getModelName()+".nmf"+"', when " +
					"saving the guide model settings to files. ", e);
		}
	}


	@Override
	public void finalizeSentence(DependencyStructure dependencyGraph)
			throws MaltChainedException {
		
		
		if (branches != null) { 
			
			for (DecisionTreeModel b : branches.values()) {
				b.finalizeSentence(dependencyGraph);
			}
			
		} else if(leafModel != null){
			
			leafModel.finalizeSentence(dependencyGraph);
		
		} else {
			
			throw new GuideException("The feature divide models cannot be found. ");
		
		}
		
	}

	@Override
	public ClassifierGuide getGuide() {
		return parent.getGuide();
	}

	@Override
	public String getModelName() throws MaltChainedException {
		try {
			
			
			return parent.getModelName() + (modelIndex==MODEL_INDEX_NOT_SET ? "" : ("_" + modelIndex)) ;
		} catch (NullPointerException e) {
			throw new GuideException("The parent guide model cannot be found. ", e);
		}
	}
	
	private void setIsLeafNode() throws MaltChainedException{
		
		if(branches==null && leafModel!=null)
			return;
		
		if(branches!=null && leafModel!=null){
			
			for(DecisionTreeModel t : branches.values())
				t.terminate();
			
			branches = null;
			
		}else
			throw new MaltChainedException("Can't set a node that have aleready been set to a leaf node.");
			
		
	}
	
	private void setIsBranchNode() throws MaltChainedException{
		if(branches!=null && leafModel!=null){
			
			leafModel.terminate();
			
			leafModel = null;
			
		}else
			throw new MaltChainedException("Can't set a node that have aleready been set to a branch node.");
			
		
	}
	/*
	private AtomicModel getLeafAtomicModel() throws MaltChainedException{
		if(leafModel==null)
			throw new MaltChainedException("Can't get leaf atomic model from a node that has already been set.");
		
		return leafModel;
	}*/

	@Override
	public void noMoreInstances() throws MaltChainedException {
		// Use later when changed for new division stratedgy
		// if (branches == null)
		// throw new GuideException(
		// "The feature divide models cannot be found. ");
		// DecisionTreeModel newBranch = new
		// DecisionTreeModel(divideFeatureCode,
		// getSubFeatureVector(), this, createNextLevelDivideFeatures(),
		// divideThreshold);
		// branches.put(divideFeatureCode, newBranch);
		// for (DecisionTreeModel t : branches.values()) {
		// t.noMoreInstances();
		// }
		// final TreeSet<Integer> removeSet = new TreeSet<Integer>();
		//
		// DecisionTreeModel newOtherBrach = new
		// DecisionTreeModel(OTHER_BRANCH_ID, featureVector, this,
		// divideFeatures, divideThreshold);
		//		
		//		
		// for (Integer index : branches.keySet()) {
		// if (branches.get(index).getFrequency() <= divideThreshold) {
		//				
		// //Create a new branch for those branches that don't get to the limit
		//				
		//
		// ArrayList<Integer> divideFeatureIndexVector = new
		// ArrayList<Integer>();
		// for (int i = 0; i < featureVector.size(); i++) {
		// if (featureVector.get(i).equals(divideFeature)) {
		// divideFeatureIndexVector.add(i);
		// }
		// }
		//				
		//				
		//				
		// branches.get(index).getLeafAtomicModel().moveAllInstances(newOtherBrach.getLeafAtomicModel(),
		// divideFeature, divideFeatureIndexVector);
		//				
		//				
		//				
		// removeSet.add(index);
		//			
		// }
		// }
		//		
		// if(newOtherBrach.getFrequency()>0)
		// branches.put(OTHER_BRANCH_ID, newOtherBrach);
		//		
		// for (Integer index : removeSet) {
		// branches.get(index).terminate();
		// branches.remove(index);
		// }

		if (leafModel == null)
			throw new GuideException(
					"The model in tree node is null in a state where it is not allowed");

		leafModel.noMoreInstances();
		// System.out.println("NO MORE INSTANCES IN " + getModelName() + " " +
		// branches + " " + divideFeatures.size());
		if (divideFeatures.size() == 0)
			setIsLeafNode();

		if (branches != null) {

			FeatureFunction divideFeature = divideFeatures.getFirst();

			// ArrayList<Integer> divideFeatureIndexVector = new
			// ArrayList<Integer>();
			// for (int i = 0; i < featureVector.size(); i++) {
			// if (featureVector.get(i).equals(divideFeature)) {
			// divideFeatureIndexVector.add(i);
			// }
			// }

			divideFeature.updateCardinality();

			leafModel.noMoreInstances();

			if (divideFeatureIdToCountMap.size() == 0)
				divideFeatureIdToCountMap = leafModel.getMethod()
						.createFeatureIdToCountMap(divideFeatureIndexVector);

			int totalInOther = 0;

			Set<Integer> featureIdsToCreateSeparateBranchesForSet = new HashSet<Integer>();

			for (Entry<Integer, Integer> entry : divideFeatureIdToCountMap
					.entrySet())
				if (entry.getValue() >= divideThreshold) {
					featureIdsToCreateSeparateBranchesForSet
							.add(entry.getKey());
				} else {
					totalInOther = totalInOther + entry.getValue();
				}

			boolean otherExists = false;

			if (totalInOther > 0)
				otherExists = true;

			if ((totalInOther < divideThreshold && featureIdsToCreateSeparateBranchesForSet
					.size() <= 1)
					|| featureIdsToCreateSeparateBranchesForSet.size() == 0) {
				// Node enough instances, make this a leaf node
				setIsLeafNode();
			} else {

				// If total in other is less then divideThreshold then add the
				// smallest of the other parts to other
				if (otherExists && totalInOther < divideThreshold) {
					int smallestSoFar = Integer.MAX_VALUE;
					int smallestSoFarId = Integer.MAX_VALUE;
					for (Entry<Integer, Integer> entry : divideFeatureIdToCountMap
							.entrySet()) {
						if (entry.getValue() < smallestSoFar) {
							smallestSoFar = entry.getValue();
							smallestSoFarId = entry.getKey();
						}
					}
					if (getModelName().equals("odm0"))
						System.out.println("REMOVE ID " + smallestSoFarId + " otherExists " + otherExists + " divide treachhold " +  divideThreshold);

					featureIdsToCreateSeparateBranchesForSet
							.remove(smallestSoFarId);
				}

				// Create new files for all feature ids with count value greater
				// than divideThreshold and one for the
				// other branch
				leafModel.getMethod().divideByFeatureSet(
						featureIdsToCreateSeparateBranchesForSet,
						divideFeatureIndexVector, "" + OTHER_BRANCH_ID);

				for (int id : featureIdsToCreateSeparateBranchesForSet) {
					DecisionTreeModel newBranch = new DecisionTreeModel(id,
							getSubFeatureVector(), this,
							createNextLevelDivideFeatures(), divideThreshold);
					branches.put(id, newBranch);

				}
				if (otherExists) {
					DecisionTreeModel newBranch = new DecisionTreeModel(
							OTHER_BRANCH_ID, featureVector, this,
							new LinkedList<FeatureFunction>(), divideThreshold);
					branches.put(OTHER_BRANCH_ID, newBranch);

					if ("odm0".equals(getModelName()))
						System.out.println("OTHER EXISTS odm0");
				}

				for (DecisionTreeModel b : branches.values())
					b.noMoreInstances();

			}

		}

	}

	@Override
	public void terminate() throws MaltChainedException {
		if (branches != null) {
			for (DecisionTreeModel branch : branches.values()) {	
				branch.terminate();
			}
			branches=null;
		}
		if (leafModel != null) {
			leafModel.terminate();
			leafModel=null;
		}
		
	}


	public void setParent(Model parent) {
		this.parent = parent;
	}


	public Model getParent() {
		return parent;
	}


	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}


	public int getFrequency() {
		return frequency;
	}


	public int getModelIndex() {
		return modelIndex;
	}
	/*
	private int getLeafModelIndex() throws MaltChainedException{
		return modelNameToIndexMap.get(getModelName());
	}
	 */

}

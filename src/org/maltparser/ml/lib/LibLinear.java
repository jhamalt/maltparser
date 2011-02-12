package org.maltparser.ml.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.TreeSet;

import liblinear.FeatureNode;
import liblinear.Linear;
import liblinear.Model;
import liblinear.Parameter;
import liblinear.Problem;
import liblinear.SolverType;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.kbest.KBestList;
import org.maltparser.parser.history.kbest.ScoredKBestList;

public class LibLinear extends Lib {
	/**
	 * Liblinear model object, only used during classification.
	 */
	private Model model = null;
	
	public LibLinear(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		super(owner, learnerMode, "liblinear");
	}
	
	protected boolean prediction(TreeSet<XNode> featureSet, SingleDecision decision) throws MaltChainedException {
		if (model == null) {
			try {
				model = Linear.loadModel(new BufferedReader(getInstanceInputStreamReaderFromConfigFile(".mod")));
			} catch (IOException e) {
				throw new LibException("The model cannot be loaded. ", e);
			}
		}
		if (model == null) { 
			throw new LibException("The Liblinear learner cannot predict the next class, because the learning model cannot be found. ");
		}
		FeatureNode[] xarray = new FeatureNode[featureSet.size()];
		int k = 0;
		for (XNode x : featureSet) {
			xarray[k++] = new FeatureNode(x.getIndex(), x.getValue());
		}
		if (decision.getKBestList().getK() == 1) {
			decision.getKBestList().add(Linear.predict(model, xarray));
		} else {
			liblinear_predict_with_kbestlist(model, xarray, decision.getKBestList());
		}
		return true;
	}
	
	protected void trainInternal(FeatureVector featureVector) throws MaltChainedException {
		try {
			Problem problem = readProblem(getInstanceInputStreamReader(".ins"), featureSet);

			if (owner.getGuide().getConfiguration().getConfigLogger().isInfoEnabled()) {
				owner.getGuide().getConfiguration().getConfigLogger().info("Creating Liblinear model "+getFile(".mod").getName()+"\n");
			}
			final PrintStream out = System.out;
			final PrintStream err = System.err;
			System.setOut(NoPrintStream.NO_PRINTSTREAM);
			System.setErr(NoPrintStream.NO_PRINTSTREAM);
			Linear.saveModel(new File(getFile(".mod").getAbsolutePath()), Linear.train(problem, getLiblinearParameters()));
			System.setOut(err);
			System.setOut(out);
			if (!saveInstanceFiles) {
				getFile(".ins").delete();
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The Liblinear learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The Liblinear learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The Liblinear learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		}
	}
	
	public void terminate() throws MaltChainedException { 
		super.terminate();
		model = null;
	}

	public void initLibOptions() {
		libOptions = new LinkedHashMap<String, String>();
		libOptions.put("s", "4"); // type = SolverType.L2LOSS_SVM_DUAL (default)
		libOptions.put("c", "0.1"); // cost = 1 (default)
		libOptions.put("e", "0.1"); // epsilon = 0.1 (default)
		libOptions.put("B", "-1"); // bias = -1 (default)
	}
	
	public void initAllowedLibOptionFlags() {
		allowedLibOptionFlags = "sceB";
	}
	
	private Problem readProblem(InputStreamReader isr, TreeSet<XNode> featureSet) throws MaltChainedException {
		Problem problem = new Problem();
		
		try {
			final BufferedReader fp = new BufferedReader(isr);
			
			problem.bias = -1;
			problem.l = getNumberOfInstances();
			problem.x = new FeatureNode[problem.l][];
			problem.y = new int[problem.l];
			int i = 0;

			while(true) {
				String line = fp.readLine();
				if(line == null) break;
				int y = binariesInstance(line, featureSet);
				if (y == -1) {
					continue;
				}
				try {
					problem.y[i] = y;
					problem.x[i] = new FeatureNode[featureSet.size()];
					int p = 0;
					for (XNode x : featureSet) {
						problem.x[i][p++] = new FeatureNode(x.getIndex(), x.getValue());
					}
					featureSet.clear();
					i++;
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new LibException("Couldn't read liblinear problem from the instance file. ", e);
				}
			}
			fp.close();	
			featureSet = null;
			problem.n = featureMap.size();
		} catch (IOException e) {
			throw new LibException("Cannot read from the instance file. ", e);
		}
		return problem;
	}
	
	private Parameter getLiblinearParameters() throws MaltChainedException {
		Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
		String type = libOptions.get("s");
		
		if (type.equals("0")) {
			param.setSolverType(SolverType.L2R_LR);
		} else if (type.equals("1")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC_DUAL);
		} else if (type.equals("2")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC);
		} else if (type.equals("3")) {
			param.setSolverType(SolverType.L2R_L1LOSS_SVC_DUAL);
		} else if (type.equals("4")) {
			param.setSolverType(SolverType.MCSVM_CS);
		} else if (type.equals("5")) {
			param.setSolverType(SolverType.L1R_L2LOSS_SVC);	
		} else if (type.equals("6")) {
			param.setSolverType(SolverType.L1R_LR);	
		} else {
			throw new LibException("The liblinear type (-s) is not an integer value between 0 and 4. ");
		}
		try {
			param.setC(Double.valueOf(libOptions.get("c")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear cost (-c) value is not numerical value. ", e);
		}
		try {
			param.setEps(Double.valueOf(libOptions.get("e")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear epsilon (-e) value is not numerical value. ", e);
		}
		return param;
	}
	
	private void liblinear_predict_with_kbestlist(Model model, FeatureNode[] x, KBestList kBestList) throws MaltChainedException {
		int i;
		final int nr_class = model.getNrClass();
		final double[] dec_values = new double[nr_class];

		Linear.predictValues(model, x, dec_values);
		final int[] labels = model.getLabels();
		int[] predictionList = new int[nr_class];
		for(i=0;i<nr_class;i++) {
			predictionList[i] = labels[i];
		}

		double tmpDec;
		int tmpObj;
		int lagest;
		for (i=0;i<nr_class-1;i++) {
			lagest = i;
			for (int j=i;j<nr_class;j++) {
				if (dec_values[j] > dec_values[lagest]) {
					lagest = j;
				}
			}
			tmpDec = dec_values[lagest];
			dec_values[lagest] = dec_values[i];
			dec_values[i] = tmpDec;
			tmpObj = predictionList[lagest];
			predictionList[lagest] = predictionList[i];
			predictionList[i] = tmpObj;
		}
		
		int k = nr_class-1;
		if (kBestList.getK() != -1) {
			k = kBestList.getK() - 1;
		}
		
		for (i=0; i<nr_class && k >= 0; i++, k--) {
			if (kBestList instanceof ScoredKBestList) {
				((ScoredKBestList)kBestList).add(predictionList[i], (float)dec_values[i]);
			} else {
				kBestList.add(predictionList[i]);
			}
		}
	}
}

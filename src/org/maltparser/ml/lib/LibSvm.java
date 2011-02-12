package org.maltparser.ml.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.util.LinkedHashMap;
import java.util.TreeSet;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.ml.lib.XNode;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.kbest.KBestList;
import org.maltparser.parser.history.kbest.ScoredKBestList;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class LibSvm extends Lib {
	private svm_model model = null;
	
	public LibSvm(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		super(owner, learnerMode, "libsvm");
	}
	
	protected boolean prediction(TreeSet<XNode> featureSet, SingleDecision decision) throws MaltChainedException {
		if (model == null) {
			try {
				model = svm.svm_load_model(new BufferedReader(getInstanceInputStreamReaderFromConfigFile(".mod")));
			} catch (IOException e) {
				throw new LibException("The model cannot be loaded. ", e);
			}
		}
		svm_node[] xarray = new svm_node[featureSet.size()];
		int k = 0;
		for (XNode x : featureSet) {
			xarray[k] = new svm_node();
			xarray[k].index = x.getIndex();
			xarray[k].value = x.getValue();
			k++;
		}
		try {
			if (decision.getKBestList().getK() == 1 || svm.svm_get_svm_type(model) == svm_parameter.ONE_CLASS ||
					svm.svm_get_svm_type(model) == svm_parameter.EPSILON_SVR ||
					svm.svm_get_svm_type(model) == svm_parameter.NU_SVR) { 
				decision.getKBestList().add((int)svm.svm_predict(model, xarray));
			} else {
				svm_predict_with_kbestlist(model, xarray, decision.getKBestList());
			}
		} catch (OutOfMemoryError e) {
				throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
		return true;
	}
	
	protected void trainInternal(FeatureVector featureVector) throws MaltChainedException {
		try {
			final svm_problem prob = readProblem(getInstanceInputStreamReader(".ins"), featureSet);
			final svm_parameter param = getLibSvmParameters();
			if(svm.svm_check_parameter(prob, param) != null) {
				throw new LibException(svm.svm_check_parameter(prob, param));
			}
			owner.getGuide().getConfiguration().getConfigLogger().info("Creating LIBSVM model "+getFile(".mod").getName()+"\n");
			final PrintStream out = System.out;
			final PrintStream err = System.err;
			System.setOut(NoPrintStream.NO_PRINTSTREAM);
			System.setErr(NoPrintStream.NO_PRINTSTREAM);
			svm.svm_save_model(getFile(".mod").getAbsolutePath(), svm.svm_train(prob, param));
			System.setOut(err);
			System.setOut(out);
			if (!saveInstanceFiles) {
				getFile(".ins").delete();
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The LIBSVM learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The LIBSVM learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The LIBSVM learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		}
	}
	
	public void terminate() throws MaltChainedException { 
		super.terminate();
		model = null;
	}
	
	public void initLibOptions() {
		libOptions = new LinkedHashMap<String, String>();
		libOptions.put("s", Integer.toString(svm_parameter.C_SVC));
		libOptions.put("t", Integer.toString(svm_parameter.POLY));
		libOptions.put("d", Integer.toString(2));
		libOptions.put("g", Double.toString(0.2));
		libOptions.put("r", Double.toString(0));
		libOptions.put("n", Double.toString(0.5));
		libOptions.put("m", Integer.toString(100));
		libOptions.put("c", Double.toString(1));
		libOptions.put("e", Double.toString(1.0));
		libOptions.put("p", Double.toString(0.1));
		libOptions.put("h", Integer.toString(1));
		libOptions.put("b", Integer.toString(0));
	}
	
	public void initAllowedLibOptionFlags() {
		allowedLibOptionFlags = "stdgrnmcepb";
	}
	
	private svm_parameter getLibSvmParameters() throws MaltChainedException {
		svm_parameter param = new svm_parameter();
	
		param.svm_type = Integer.parseInt(libOptions.get("s"));
		param.kernel_type = Integer.parseInt(libOptions.get("t"));
		param.degree = Integer.parseInt(libOptions.get("d"));
		param.gamma = Double.valueOf(libOptions.get("g")).doubleValue();
		param.coef0 = Double.valueOf(libOptions.get("r")).doubleValue();
		param.nu = Double.valueOf(libOptions.get("n")).doubleValue();
		param.cache_size = Double.valueOf(libOptions.get("m")).doubleValue();
		param.C = Double.valueOf(libOptions.get("c")).doubleValue();
		param.eps = Double.valueOf(libOptions.get("e")).doubleValue();
		param.p = Double.valueOf(libOptions.get("p")).doubleValue();
		param.shrinking = Integer.parseInt(libOptions.get("h"));
		param.probability = Integer.parseInt(libOptions.get("b"));
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		return param;
	}
	
	private svm_problem readProblem(InputStreamReader isr, TreeSet<XNode> featureSet) throws MaltChainedException {
		final svm_problem problem = new svm_problem();
		final svm_parameter param = getLibSvmParameters();
		try {
			final BufferedReader fp = new BufferedReader(isr);
			
			problem.l = getNumberOfInstances();
			problem.x = new svm_node[problem.l][];
			problem.y = new double[problem.l];
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
					problem.x[i] = new svm_node[featureSet.size()];
					int p = 0;
					for (XNode x : featureSet) {
						problem.x[i][p] = new svm_node();
						problem.x[i][p].value = x.getValue();
						problem.x[i][p].index = x.getIndex();          
						p++;
					}
					featureSet.clear();
					i++;
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new LibException("Couldn't read libsvm problem from the instance file. ", e);
				}
			}
			fp.close();	
			featureSet = null;
			if (param.gamma == 0) {
				param.gamma = 1.0/featureCounter;
			}
		} catch (IOException e) {
			throw new LibException("Couldn't read libsvm problem from the instance file. ", e);
		}
		return problem;
	}
	
	public void svm_predict_with_kbestlist(svm_model model, svm_node[] x, KBestList kBestList) throws MaltChainedException {
		int i;
		final int nr_class = svm.svm_get_nr_class(model);
		final double[] dec_values = new double[nr_class*(nr_class-1)/2];
		svm.svm_predict_values(model, x, dec_values);

		final int[] vote = new int[nr_class];
		final double[] score = new double[nr_class];
		final int[] voteindex = new int[nr_class];
		for(i=0;i<nr_class;i++) {
			vote[i] = 0;
			score[i] = 0.0;
			voteindex[i] = i;
		}
		int pos=0;
		for(i=0;i<nr_class;i++) {
			for(int j=i+1;j<nr_class;j++) {
				if(dec_values[pos] > 0) {
					vote[i]++;
				} else {
					vote[j]++;
				}
				score[i] += dec_values[pos];
				score[j] += dec_values[pos];
				pos++;
			}
		}
		for(i=0;i<nr_class;i++) {
			score[i] = score[i]/nr_class;
		}
		int lagest, tmpint;
		double tmpdouble;
		for (i=0;i<nr_class-1;i++) {
			lagest = i;
			for (int j=i;j<nr_class;j++) {
				if (vote[j] > vote[lagest]) {
					lagest = j;
				}
			}
			tmpint = vote[lagest];
			vote[lagest] = vote[i];
			vote[i] = tmpint;
			tmpdouble = score[lagest];
			score[lagest] = score[i];
			score[i] = tmpdouble;
			tmpint = voteindex[lagest];
			voteindex[lagest] = voteindex[i];
			voteindex[i] = tmpint;
		}
		final int[] labels = new int[nr_class];
		svm.svm_get_labels(model, labels);
		int k = nr_class-1;
		if (kBestList.getK() != -1) {
			k = kBestList.getK() - 1;
		}
		
		for (i=0; i<nr_class && k >= 0; i++, k--) {
			if (vote[i] > 0 || i == 0) {
				if (kBestList instanceof ScoredKBestList) {
					((ScoredKBestList)kBestList).add(labels[voteindex[i]], (float)vote[i]/(float)(nr_class*(nr_class-1)/2));
				} else {
					kBestList.add(labels[voteindex[i]]);
				}
			}
		}
	}
}

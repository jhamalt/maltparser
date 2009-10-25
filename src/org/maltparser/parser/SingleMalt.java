package org.maltparser.parser;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.config.ConfigurationException;
import org.maltparser.core.config.ConfigurationRegistry;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.SystemLogger;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;

/**
 * @author Johan Hall
 *
 */
public class SingleMalt implements DependencyParserConfig {
	public static final int LEARN = 0;
	public static final int PARSE = 1;
	protected ConfigurationDir configDir;
	protected Logger configLogger;
	protected int optionContainerIndex;
	protected Algorithm parsingAlgorithm = null;
	protected int mode;
	protected ConfigurationRegistry registry;
	protected SymbolTableHandler symbolTableHandler;
	protected long startTime;
	protected long endTime;
	protected int nIterations = 0;
	
	public void initialize(int containerIndex, DataFormatInstance dataFormatInstance, ConfigurationDir configDir, int mode) throws MaltChainedException {

		this.optionContainerIndex = containerIndex;
		this.mode = mode;
		setConfigurationDir(configDir);
		startTime = System.currentTimeMillis();
		configLogger = initConfigLogger(getOptionValue("config", "logfile").toString(), getOptionValue("config", "logging").toString());
		registry = new ConfigurationRegistry();
		symbolTableHandler = dataFormatInstance.getSymbolTables();

		if (mode == SingleMalt.LEARN) {
			checkOptionDependency();
		}
		registry.put(org.maltparser.core.symbol.SymbolTableHandler.class, getSymbolTables());
		registry.put(org.maltparser.core.io.dataformat.DataFormatInstance.class, dataFormatInstance);
//		registry.put(org.maltparser.parser.DependencyParserConfig.class, this);
		initParsingAlgorithm(); 

	}
	
	
	/**
	 * Initialize the parsing algorithm
	 * 
	 * @throws MaltChainedException
	 */
	protected void initParsingAlgorithm() throws MaltChainedException {
		if (mode == LEARN) {
			parsingAlgorithm = new BatchTrainer(this);
		} else if (mode == PARSE) {
			parsingAlgorithm = new DeterministicParser(this);
		}
	}
	
	public void addRegistry(Class<?> clazz, Object o) {
		registry.put(clazz, o);
	}
	
	public void process(Object[] arguments) throws MaltChainedException {
		if (mode == LEARN) {
			if (arguments.length < 2 || !(arguments[0] instanceof DependencyStructure) || !(arguments[1] instanceof DependencyStructure)) {
				throw new MaltChainedException("The single malt learn task must be supplied with at least two dependency structures. ");
			}
			DependencyStructure systemGraph = (DependencyStructure)arguments[0];
			DependencyStructure goldGraph = (DependencyStructure)arguments[1];
			if (systemGraph.hasTokens() && getGuide() != null) {
				getGuide().finalizeSentence(((Trainer)getAlgorithm()).parse(goldGraph, systemGraph));
			}
		} else if (mode == PARSE) {
			if (arguments.length < 1 || !(arguments[0] instanceof DependencyStructure)) {
				throw new MaltChainedException("The single malt parse task must be supplied with at least one input terminal structure and one output dependency structure. ");
			}
			DependencyStructure processGraph = (DependencyStructure)arguments[0];
			if (processGraph.hasTokens()) {
				((Parser)getAlgorithm()).parse(processGraph);
			}
		}
	}
	
	public void parse(DependencyStructure graph) throws MaltChainedException {
		if (graph.hasTokens()) {
			((Parser)getAlgorithm()).parse(graph);
		}
	}
	
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException {
		if (oracleGraph.hasTokens()) {
			if (getGuide() != null) {
				getGuide().finalizeSentence(((Trainer)getAlgorithm()).parse(goldGraph, oracleGraph));
			} else {
				((Trainer)getAlgorithm()).parse(goldGraph, oracleGraph);
			}
		}
	}
	
	public void train() throws MaltChainedException {
		if (getGuide() == null) {
			((Trainer)getAlgorithm()).train();
		}
	}
	
	public void terminate(Object[] arguments) throws MaltChainedException {
//		if (getAlgorithm() instanceof Trainer) {
//			((Trainer)getAlgorithm()).terminate();
//		}
		getAlgorithm().terminate();
		if (getGuide() != null) {
			getGuide().terminate();
		}
		if (mode == LEARN) {
			endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			if (configLogger.isInfoEnabled()) {
				configLogger.info("Learning time: " +new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)\n");
			}
		} else if (mode == PARSE) {
			endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			if (configLogger.isInfoEnabled()) {
				configLogger.info("Parsing time: " +new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)\n");
			}
		}
		if (SystemLogger.logger() != configLogger && configLogger != null) {
			configLogger.removeAllAppenders();
		}
	}
	
	/**
	 * Initialize the configuration logger
	 * 
	 * @return the configuration logger
	 * @throws MaltChainedException
	 */
	public Logger initConfigLogger(String logfile, String level) throws MaltChainedException {
		if (logfile != null && logfile.length() > 0 && !logfile.equalsIgnoreCase("stdout") && configDir != null) {
			configLogger = Logger.getLogger(logfile);
			FileAppender fileAppender = null;
			try {
				fileAppender = new FileAppender(new PatternLayout("%m"),configDir.getWorkingDirectory().getPath()+File.separator+logfile, true);
			} catch(IOException e) {
				throw new ConfigurationException("It is not possible to create a configuration log file. ", e);
			}
			fileAppender.setThreshold(Level.toLevel(level, Level.INFO));
			configLogger.addAppender(fileAppender);
			configLogger.setLevel(Level.toLevel(level, Level.INFO));	
		} else {
			configLogger = SystemLogger.logger();
		}

		return configLogger;
	}
	
	public Logger getConfigLogger() {
		return configLogger;
	}

	public void setConfigLogger(Logger logger) {
		configLogger = logger;
	}
	
	public ConfigurationDir getConfigurationDir() {
		return configDir;
	}
	
	public void setConfigurationDir(ConfigurationDir configDir) {
		this.configDir = configDir;
	}
	
	public int getMode() {
		return mode;
	}
	
	public ConfigurationRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(ConfigurationRegistry registry) {
		this.registry = registry;
	}

	public Object getOptionValue(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValue(optionContainerIndex, optiongroup, optionname);
	}
	
	public String getOptionValueString(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValueString(optionContainerIndex, optiongroup, optionname);
	}
	
	public OptionManager getOptionManager() throws MaltChainedException {
		return OptionManager.instance();
	}
	/******************************** MaltParserConfiguration specific  ********************************/
	
	/**
	 * Returns the list of symbol tables
	 * 
	 * @return the list of symbol tables
	 */
	public SymbolTableHandler getSymbolTables() {
		return symbolTableHandler;
	}
	
	public Algorithm getAlgorithm() {
		return parsingAlgorithm;
	}
	/**
	 * Returns the guide
	 * 
	 * @return the guide
	 */
	public ClassifierGuide getGuide() {
		return parsingAlgorithm.getGuide();
	}
	
	public void checkOptionDependency() throws MaltChainedException {
		try {
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().write("\nDEPENDENCIES\n");
			}
//			if ((Boolean)getOptionValue("malt0.4", "behavior") == true) {
//				if (!getOptionValueString("singlemalt", "null_value").equals("rootlabel")) {
//					OptionManager.instance().overloadOptionValue(optionContainerIndex, "singlemalt", "null_value", "rootlabel");
//					if (configDir.getInfoFileWriter() != null) {
//						configDir.getInfoFileWriter().write("--singlemalt-null_value (-nv)     rootlabel\n");
//					}
//					configLogger.warn("Option --malt0.4-behavior = true and --singlemalt-null_value != 'rootlabel'. Option --singlemalt-null_value is overloaded with value 'rootlabel'\n");
//				}
//				if (getOptionValue("malt0.4", "depset").toString().equals("")) {				
//					configLogger.warn("Option --malt0.4-behavior = true and option --malt0.4-depset has no value. These combination will probably not reproduce the behavior of MaltParser 0.4 (C-impl)\n");
//				}
//				if (getOptionValue("malt0.4", "posset").toString().equals("")) {				
//					configLogger.warn("Option --malt0.4-behavior = true and option --malt0.4-posset has no value. These combination will probably not reproduce the behavior of MaltParser 0.4 (C-impl)\n");
//				}
//				if (getOptionValue("malt0.4", "cposset").toString().equals("")) {				
//					configLogger.warn("Option --malt0.4-behavior = true and option --malt0.4-cposset has no value. These combination will probably not reproduce the behavior of MaltParser 0.4 (C-impl)\n");
//				}
//				if (!getOptionValue("guide", "kbest").toString().equals("1")) {
//					OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "kbest", "1");
//					if (configDir.getInfoFileWriter() != null) {
//						configDir.getInfoFileWriter().write("--guide-kbest (  -k)                    1\n");
//					}
//					configLogger.warn("Option --malt0.4-behavior = true and --guide-kbest != '1'. Option --guide-kbest is overloaded with value '1'\n");
//				}
//			}
			if (getOptionValue("guide", "features").toString().equals("")) {
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "features", getOptionValueString("singlemalt", "parsing_algorithm"));
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-features (  -F)                 "+getOptionValue("guide", "features").toString()+"\n");
				}
			} else {
				configDir.copyToConfig(getOptionValue("guide", "features").toString());
			}
			if (getOptionValue("guide", "data_split_column").toString().equals("") && !getOptionValue("guide", "data_split_structure").toString().equals("")) {
				configLogger.warn("Option --guide-data_split_column = '' and --guide-data_split_structure != ''. Option --guide-data_split_structure is overloaded with '', this will cause the parser to induce a single model.\n ");
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "data_split_structure", "");
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-data_split_structure (  -s)\n");
				}
			}
			if (!getOptionValue("guide", "data_split_column").toString().equals("") && getOptionValue("guide", "data_split_structure").toString().equals("")) {
				configLogger.warn("Option --guide-data_split_column != '' and --guide-data_split_structure = ''. Option --guide-data_split_column is overloaded with '', this will cause the parser to induce a single model.\n");
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "data_split_column", "");
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-data_split_column (  -d)\n");
				}
			}
			
			String decisionSettings = getOptionValue("guide", "decision_settings").toString().trim();
			String markingStrategy = getOptionValue("pproj", "marking_strategy").toString().trim();
			String coveredRoot = getOptionValue("pproj", "covered_root").toString().trim();
			StringBuilder newDecisionSettings = new StringBuilder();
//			if ((Boolean)getOptionValue("malt0.4", "behavior") == true) {
//				decisionSettings = "T.TRANS+A.DEPREL";
//			}
			if (decisionSettings == null || decisionSettings.length() < 1 || decisionSettings.equals("default")) {
				decisionSettings = "T.TRANS+A.DEPREL";
			} else {
				decisionSettings = decisionSettings.toUpperCase();
			}
			
			if (markingStrategy.equalsIgnoreCase("head") || markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				if (!Pattern.matches(".*A\\.PPLIFTED.*", decisionSettings)) {
					newDecisionSettings.append("+A.PPLIFTED");
				}
			}
			if (markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				if (!Pattern.matches(".*A\\.PPPATH.*", decisionSettings)) {
					newDecisionSettings.append("+A.PPPATH");
				}
			}
			if (!coveredRoot.equalsIgnoreCase("none") && !Pattern.matches(".*A\\.PPCOVERED.*", decisionSettings)) {
				newDecisionSettings.append("+A.PPCOVERED");
			}
			if (!getOptionValue("guide", "decision_settings").toString().equals(decisionSettings) || newDecisionSettings.length() > 0) {
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "decision_settings", decisionSettings+newDecisionSettings.toString());
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-decision_settings (  -gds)                 "+getOptionValue("guide", "decision_settings").toString()+"\n");
				}
			}
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().flush();
			}
		} catch (IOException e) {
			throw new ConfigurationException("Could not write to the configuration information file. ", e);
		}
	}
}

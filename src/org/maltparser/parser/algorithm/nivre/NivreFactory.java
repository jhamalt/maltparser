package org.maltparser.parser.algorithm.nivre;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureRegistry;
import org.maltparser.core.feature.function.Function;
import org.maltparser.parser.AbstractParserFactory;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.ParserRegistry;
/**
 * @author Johan Hall
 *
 */
public abstract class NivreFactory implements AbstractParserFactory {
	protected final DependencyParserConfig manager;
	
	public NivreFactory(DependencyParserConfig _manager) {
		this.manager = _manager;
	}
	
	public ParserConfiguration makeParserConfiguration() throws MaltChainedException {
		boolean allowRoot = (Boolean)manager.getOptionValue("nivre", "allow_root");
		boolean allowReduce = (Boolean)manager.getOptionValue("nivre", "allow_reduce");
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Parser configuration : Nivre with with allow_root="+allowRoot+" and allow_reduce="+allowReduce+"\n");
		}
		return new NivreConfig(allowRoot, allowReduce);
	}
	
	public Function makeFunction(String subFunctionName, FeatureRegistry registry) throws MaltChainedException {
		AlgoritmInterface algorithm = ((ParserRegistry)registry).getAlgorithm();
		return new NivreAddressFunction(subFunctionName, algorithm);
	}
}

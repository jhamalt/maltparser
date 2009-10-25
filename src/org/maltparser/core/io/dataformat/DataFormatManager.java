package org.maltparser.core.io.dataformat;

import java.util.HashMap;
import java.util.HashSet;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.io.dataformat.DataFormatSpecification.Dependency;

public class DataFormatManager {
	private DataFormatSpecification inputDataFormatSpec;
	private DataFormatSpecification outputDataFormatSpec;
	private final HashMap<String, DataFormatSpecification> fileNameDataFormatSpecs;
	private final HashMap<String, DataFormatSpecification> nameDataFormatSpecs;
	
	public DataFormatManager(String inputFormatName, String outputFormatName) throws MaltChainedException {
		fileNameDataFormatSpecs = new HashMap<String, DataFormatSpecification>();
		nameDataFormatSpecs = new HashMap<String, DataFormatSpecification>();
		inputDataFormatSpec = loadDataFormat(inputFormatName);
		outputDataFormatSpec = loadDataFormat(outputFormatName);
	}

	public DataFormatSpecification loadDataFormat(String dataFormatName) throws MaltChainedException {
		if (dataFormatName == null || dataFormatName.length() == 0 ) {
			return null;
		}
		DataFormatSpecification dataFormat = fileNameDataFormatSpecs.get(dataFormatName);
		if (dataFormat == null) {
			dataFormat = new DataFormatSpecification();
			dataFormat.parseDataFormatXMLfile(dataFormatName);
			fileNameDataFormatSpecs.put(dataFormatName, dataFormat);
			nameDataFormatSpecs.put(dataFormat.getDataFormatName(), dataFormat);
			HashSet<Dependency> dependencies = dataFormat.getDependencies();
			for (Dependency dep : dependencies) {
				loadDataFormat(dep.urlString);
			}
		}
		return dataFormat;
	}

	public DataFormatSpecification getInputDataFormatSpec() {
		return inputDataFormatSpec;
	}

	public DataFormatSpecification getOutputDataFormatSpec() {
		return outputDataFormatSpec;
	}
	
	public DataFormatSpecification getDataFormatSpec(String name) {
		return nameDataFormatSpecs.get(name);
	}
}

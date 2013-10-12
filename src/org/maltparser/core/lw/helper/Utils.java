package org.maltparser.core.lw.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.node.TokenNode;

/**
 * @author Johan Hall
 *
 */
public final class Utils {
	public static JarFile getConfigJarfile(URL url)  {
		JarFile mcoFile = null;
		try {
			JarURLConnection conn = (JarURLConnection)new URL("jar:" + url.toString() + "!/").openConnection();
			mcoFile = conn.getJarFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mcoFile;
	}
	
	public static JarEntry getConfigFileEntry(JarFile mcoJarFile, String mcoName, String fileName)  {

		JarEntry entry = mcoJarFile.getJarEntry(mcoName+'/'+fileName);
		if (entry == null) {
			entry = mcoJarFile.getJarEntry(mcoName+'\\'+fileName);
		}
		return entry;
	}
	
	public static InputStream getInputStreamFromConfigFileEntry(URL mcoURL, String mcoName, String fileName) {
		JarFile mcoJarFile = getConfigJarfile(mcoURL);
		JarEntry entry = getConfigFileEntry(mcoJarFile, mcoName, fileName);
		
		try {
          if (entry == null) {
        	  throw new FileNotFoundException();
		  }
		  return mcoJarFile.getInputStream(entry);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
		return null;
	}
	
	public static InputStreamReader getInputStreamReaderFromConfigFileEntry(URL mcoURL, String mcoName, String fileName, String charSet)  {
		try {
			return new InputStreamReader(getInputStreamFromConfigFileEntry(mcoURL, mcoName, fileName),  charSet);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();	
		}
		return null;
	}
	
	public static URL getConfigFileEntryURL(URL mcoURL, String mcoName, String fileName)  {
		try {
			URL url = new URL("jar:"+mcoURL.toString()+"!/"+mcoName+'/'+fileName + "\n");
			try { 
				InputStream is = url.openStream();
				is.close();
			} catch (IOException e) {
				url = new URL("jar:"+mcoURL.toString()+"!/"+mcoName+'\\'+fileName + "\n");
			}
			return url;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getInternalParserModelName(URL mcoUrl) {
		String internalParserModelName = null;
		try {
			JarEntry je;
			JarInputStream jis = new JarInputStream(mcoUrl.openConnection().getInputStream());
	
			while ((je = jis.getNextJarEntry()) != null) {
				String fileName = je.getName();
				jis.closeEntry();
				int index = fileName.indexOf('/');
				if (index == -1) {
					index = fileName.indexOf('\\');
				}
				if (internalParserModelName == null) {
					internalParserModelName = fileName.substring(0, index);
					break;
				}
			}
			jis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return internalParserModelName;
	}
	
    public static String[] readSentences(BufferedReader reader) throws IOException {
    	ArrayList<String> tokens = new ArrayList<String>();
    	String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().length() == 0) {
				break;
			} else {
				tokens.add(line.trim());
			}

		}
    	return tokens.toArray(new String[tokens.size()]);
    }
    
    public static void writeSentences(String[] inTokens, BufferedWriter writer) throws IOException {
    	for (int i = 0; i < inTokens.length; i++) {
    		writer.write(inTokens[i]);
    		writer.newLine();
    	}
    	writer.newLine();
    	writer.flush();
    }
    
    public static String[] stripGold(String[] inTokens) {
    	String[] outTokens = new String[inTokens.length];
    	
    	for (int i = 0; i < inTokens.length; i++) {
    		int tabCounter = 0;
    		int j = inTokens[i].length()-1;
    		for (; j >= 0; j--) {
    			if (inTokens[i].charAt(j) == '\t') {
    				tabCounter++;
    			}
    			if (tabCounter == 2) {
    				outTokens[i] = inTokens[i].substring(0, j);
    				break;
    			}
    		}
    	}
    	return outTokens;
    }
    
    public static void printTokens(String[] inTokens) {
    	for (int i = 0; i < inTokens.length; i++) {
    		System.out.println(inTokens[i]);
    	}
    }
    
    public static boolean diff(String[] goldTokens, String[] outputTokens) {
    	if (goldTokens.length != outputTokens.length) {
    		return true;
    	}
    	for (int i = 0; i < goldTokens.length; i++) {
    		if (!	goldTokens[i].equals(outputTokens[i])) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static String[] toStringArray(DependencyGraph graph, DataFormatInstance dataFormatInstance, SymbolTableHandler symbolTables) throws MaltChainedException {
        String[] tokens = new String[graph.nTokenNode()];
	    StringBuilder sb = new StringBuilder();
	    Iterator<ColumnDescription> columns = dataFormatInstance.iterator();
	    for (Integer index : graph.getTokenIndices()) {
	        sb.setLength(0);
	        if (index <= tokens.length) {
	            ColumnDescription column = null;
	            TokenNode node = graph.getTokenNode(index);
	            while (columns.hasNext()) {
	                column = columns.next();
	                if (column.getCategory() == ColumnDescription.INPUT) {
	                    if (!column.getName().equals("ID")) {                 	
	                        if (node.hasLabel(symbolTables.getSymbolTable(column.getName())) && node.getLabelSymbol(symbolTables.getSymbolTable(column.getName())).length() > 0) {
	                            sb.append(node.getLabelSymbol(symbolTables.getSymbolTable(column.getName())));
	                        } else {
	                            sb.append('_');
	                        }
	                    } else {
	                        sb.append(index.toString());
	                    }
	                    sb.append('\t');
	                }
	            }
	            sb.setLength(sb.length()-1);
	            tokens[index-1] = sb.toString();
	            columns = dataFormatInstance.iterator();
	        }
	    }
	    return tokens;
	}
}

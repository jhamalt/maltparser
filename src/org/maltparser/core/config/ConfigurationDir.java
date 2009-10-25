package org.maltparser.core.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.SystemInfo;
import org.maltparser.core.helper.SystemLogger;
import org.maltparser.core.helper.Util;
import org.maltparser.core.options.OptionManager;

/**
* This class contains methods for handle the configuration directory.
*
* @author Johan Hall
*/
public class ConfigurationDir  {
	protected static final int BUFFER = 4096;
	protected File configDirectory;
	protected String name;
	protected String type;
	protected File workingDirectory;
	protected URL url = null;
	protected int containerIndex;
	protected BufferedWriter infoFile = null;
	protected String createdByMaltParserVersion;

	
	/**
	 * Creates a configuration directory from a mco-file specified by an URL.
	 * 
	 * @param url	an URL to a mco-file
	 * @throws MaltChainedException
	 */
	public ConfigurationDir(URL url) throws MaltChainedException {
		initWorkingDirectory();
		setUrl(url);
		initNameNTypeFromInfoFile(url);
	}
	
	/**
	 * Creates a new configuration directory or a configuration directory from a mco-file
	 * 
	 * @param name	the name of the configuration
	 * @param type	the type of configuration
	 * @param containerIndex	the container index
	 * @throws MaltChainedException
	 */
	public ConfigurationDir(String name, String type, int containerIndex) throws MaltChainedException {
		setContainerIndex(containerIndex);
		initWorkingDirectory();
		if (name != null && name.length() > 0 && type != null && type.length() > 0) {
			setName(name);
			setType(type);
		} else {
			throw new ConfigurationException("The configuration name is not specified. ");
		}
		setConfigDirectory(new File(workingDirectory.getPath()+File.separator+getName()));
	}
	
	/**
	 * Creates an output stream writer, where the corresponding file will be included in the configuration directory
	 * 
	 * @param fileName	a file name
	 * @param charSet	a char set
	 * @return	an output stream writer for writing to a file within the configuration directory
	 * @throws MaltChainedException
	 */
	public OutputStreamWriter getOutputStreamWriter(String fileName, String charSet) throws MaltChainedException {
		try {
			return new OutputStreamWriter(new FileOutputStream(configDirectory.getPath()+File.separator+fileName), charSet);
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The file '"+fileName+"' cannot be created. ", e);
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException("The char set '"+charSet+"' is not supported. ", e);
		}
	}
	
	/**
	 * Creates an output stream writer, where the corresponding file will be included in the 
	 * configuration directory. Uses UTF-8 for character encoding.
	 * 
	 * @param fileName	a file name
	 * @return an output stream writer for writing to a file within the configuration directory
	 * @throws MaltChainedException
	 */
	public OutputStreamWriter getOutputStreamWriter(String fileName) throws MaltChainedException {
		try {
			return new OutputStreamWriter(new FileOutputStream(configDirectory.getPath()+File.separator+fileName), "UTF-8");
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The file '"+fileName+"' cannot be created. ", e);
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException("The char set 'UTF-8' is not supported. ", e);
		}
	}
	
	/**
	 * Creates an input stream reader for reading a file within the configuration directory
	 * 
	 * @param fileName	a file name
	 * @param charSet	a char set
	 * @return an input stream reader for reading a file within the configuration directory
	 * @throws MaltChainedException
	 */
	public InputStreamReader getInputStreamReader(String fileName, String charSet) throws MaltChainedException {
		try {
			return new InputStreamReader(new FileInputStream(configDirectory.getPath()+File.separator+fileName), charSet);
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The file '"+fileName+"' cannot be found. ", e);
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException("The char set '"+charSet+"' is not supported. ", e);
		}
	}
	
	/**
	 * Creates an input stream reader for reading a file within the configuration directory.
	 * Uses UTF-8 for character encoding.
	 * 
	 * @param fileName	a file name
	 * @return	an input stream reader for reading a file within the configuration directory
	 * @throws MaltChainedException
	 */
	public InputStreamReader getInputStreamReader(String fileName) throws MaltChainedException {
		try {
			return new InputStreamReader(new FileInputStream(configDirectory.getPath()+File.separator+fileName), "UTF-8");
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The file '"+fileName+"' cannot be found. ", e);
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException("The char set 'UTF-8' is not supported. ", e);
		}
	}
	
	/**
	 * Returns a file handler object of a file within the configuration directory
	 * 
	 * @param fileName	a file name
	 * @return	a file handler object of a file within the configuration directory
	 * @throws MaltChainedException
	 */
	public File getFile(String fileName) throws MaltChainedException {
		return new File(configDirectory.getPath()+File.separator+fileName);
	}
	
    /**
     * Copies a file into the configuration directory.
     * 
     * @param source	a path to file 
     * @throws MaltChainedException
     */
    public void copyToConfig(File source) throws MaltChainedException {
    	byte[] readBuffer = new byte[BUFFER];
    	String destination = configDirectory.getPath()+File.separator+source.getName();
    	try {
	    	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(source));
	        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destination), BUFFER);
	    
	        int n = 0;
		    while ((n = bis.read(readBuffer, 0, BUFFER)) != -1) {
		    	bos.write(readBuffer, 0, n);
		    }
	        bos.flush();
	        bos.close();
	        bis.close();
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The source file '"+source+"' cannot be found or the destination file '"+destination+"' cannot be created when coping the file. ", e);
		} catch (IOException e) {
			throw new ConfigurationException("The source file '"+source+"' cannot be copied to destination '"+destination+"'. ", e);
		}
    }
    
    /**
     * Copies a file into the configuration directory.
     * 
     * @param fileUrl 	an URL to the file
     * @throws MaltChainedException
     */
    public void copyToConfig(String fileUrl) throws MaltChainedException {
    	URL url = Util.findURL(fileUrl);
    	if (url == null) {
    		throw new ConfigurationException("The file or URL '"+fileUrl+"' could not be found. ");
    	}
    	byte[] readBuffer = new byte[BUFFER];
    	String destFileName = url.getFile();
    	int indexSlash = destFileName.lastIndexOf('/');
    	int indexQuery = destFileName.lastIndexOf('?');
    	
    	if (indexSlash != -1 || indexQuery != -1) {
    		if (indexSlash == -1) {
    			indexSlash = 0;
    		}
    		if (indexQuery == -1) {
    			indexQuery = destFileName.length();
    		}
    		destFileName = destFileName.substring(indexSlash, indexQuery);
    	}
    	
    	String destination = configDirectory.getPath()+File.separator+destFileName;
    	try {
	    	BufferedInputStream bis = new BufferedInputStream(url.openStream());
	        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destination), BUFFER);
	    
	        int n = 0;
		    while ((n = bis.read(readBuffer, 0, BUFFER)) != -1) {
		    	bos.write(readBuffer, 0, n);
		    }
	        bos.flush();
	        bos.close();
	        bis.close();
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The destination file '"+destination+"' cannot be created when coping the file. ", e);
		} catch (IOException e) {
			throw new ConfigurationException("The URL '"+url+"' cannot be copied to destination '"+destination+"'. ", e);
		}
    }
    
	/**
	 * Removes the configuration directory, if it exists and it contains a .info file. 
	 * 
	 * @throws MaltChainedException
	 */
	public void deleteConfigDirectory() throws MaltChainedException {
		if (!configDirectory.exists()) {
			return;
		}
		File infoFile = new File(configDirectory.getPath()+File.separator+getName()+"_"+getType()+".info");
		if (infoFile.exists()) {
			deleteConfigDirectory(configDirectory);
		} else {
			throw new ConfigurationException("There exists a directory that is not a MaltParser configuration directory. ");
		}
	}
	
	private void deleteConfigDirectory(File directory) throws MaltChainedException {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteConfigDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		} else {
			throw new ConfigurationException("The directory '"+directory.getPath()+ "' cannot be found. ");
		}
		directory.delete();
	}
	
	/**
	 * Returns a file handler object for the configuration directory
	 * 
	 * @return a file handler object for the configuration directory
	 */
	public File getConfigDirectory() {
		return configDirectory;
	}

	protected void setConfigDirectory(File dir) {
		this.configDirectory = dir;
	}

	/**
	 * Creates the configuration directory
	 * 
	 * @throws MaltChainedException
	 */
	public void createConfigDirectory() throws MaltChainedException {
		checkConfigDirectory();
		configDirectory.mkdir();
		createInfoFile();
	}
	
	protected void checkConfigDirectory()  throws MaltChainedException {
		if (configDirectory.exists() && !configDirectory.isDirectory()) {
			throw new ConfigurationException("The configuration directory name already exists and is not a directory. ");
		}
		
		if (configDirectory.exists()) {
			deleteConfigDirectory();
		} 
	}
	
	protected void createInfoFile() throws MaltChainedException {
		infoFile = new BufferedWriter(getOutputStreamWriter(getName()+"_"+getType()+".info"));
		try {
			infoFile.write("CONFIGURATION\n");
			infoFile.write("Configuration name:   "+getName()+"\n");
			infoFile.write("Configuration type:   "+getType()+"\n");
			infoFile.write("Created:              "+new Date(System.currentTimeMillis())+"\n");
			
			infoFile.write("\nSYSTEM\n");
			infoFile.write("Operating system architecture: "+System.getProperty("os.arch")+"\n");
			infoFile.write("Operating system name:         "+System.getProperty("os.name")+"\n");
			infoFile.write("JRE vendor name:               "+System.getProperty("java.vendor")+"\n");
			infoFile.write("JRE version number:            "+System.getProperty("java.version")+"\n");
			
			infoFile.write("\nMALTPARSER\n");
			infoFile.write("Version:                       "+SystemInfo.getVersion()+"\n");
			infoFile.write("Build date:                    "+SystemInfo.getBuildDate()+"\n");
			HashSet<String> excludeGroups = new HashSet<String>();
			excludeGroups.add("system");
			infoFile.write("\nSETTINGS\n");
			infoFile.write(OptionManager.instance().toStringPrettyValues(containerIndex, excludeGroups));
			infoFile.flush();
		} catch (IOException e) {
			throw new ConfigurationException("Could not create the maltparser info file. ");
		}
	}
	
	/**
	 * Returns a writer to the configuration information file
	 * 
	 * @return	a writer to the configuration information file
	 * @throws MaltChainedException
	 */
	public BufferedWriter getInfoFileWriter() throws MaltChainedException {
		return infoFile;
	}
	
	/**
	 * Creates the malt configuration file (.mco). This file is compressed.   
	 * 
	 * @throws MaltChainedException
	 */
	public void createConfigFile() throws MaltChainedException {
		try {
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(workingDirectory.getPath()+File.separator+getName()+".mco"));
//			configLogger.info("Creates configuration file '"+workingDirectory.getPath()+File.separator+getName()+".mco' ...\n");
			createConfigFile(configDirectory.getPath(), jos);
			jos.close();
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The maltparser configurtation file '"+workingDirectory.getPath()+File.separator+getName()+".mco"+"' cannot be found. ", e);
		} catch (IOException e) {
			throw new ConfigurationException("The maltparser configurtation file '"+workingDirectory.getPath()+File.separator+getName()+".mco"+"' cannot be created. ", e);
		} 
	}
	
	private void createConfigFile(String directory, JarOutputStream jos) throws MaltChainedException {
    	byte[] readBuffer = new byte[BUFFER];
		try {
			File zipDir = new File(directory);
			String[] dirList = zipDir.list();
			
			int bytesIn = 0;
	
			for (int i = 0; i < dirList.length; i++) {
				File f = new File(zipDir, dirList[i]);
				if (f.isDirectory()) {
					String filePath = f.getPath();
					createConfigFile(filePath, jos);
					continue;
				}
	
				FileInputStream fis = new FileInputStream(f);
				
				
				JarEntry entry = new JarEntry(f.getPath().substring(workingDirectory.getPath().length()+1));
				jos.putNextEntry(entry);
	
				while ((bytesIn = fis.read(readBuffer)) != -1) {
					jos.write(readBuffer, 0, bytesIn);
				}
	
				fis.close();
			}
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("The directory '"+directory+"' cannot be found. ", e);
		} catch (IOException e) {
			throw new ConfigurationException("The directory '"+directory+"' cannot be compressed into a mco file. ", e);
		} 
	}
    
    protected void initNameNTypeFromInfoFile(URL url) throws MaltChainedException {
		if (url == null) {
			throw new ConfigurationException("The URL cannot be found. ");
		}  	
		try {
			JarEntry je;
			JarInputStream jis = new JarInputStream(url.openConnection().getInputStream());
			while ((je = jis.getNextJarEntry()) != null) {
				String entryName = je.getName();
				if (entryName.endsWith(".info")) {
					int indexUnderScore = entryName.lastIndexOf('_');
					int indexSeparator = entryName.lastIndexOf(File.separator);
					if (indexSeparator == -1) {
						indexSeparator = entryName.lastIndexOf('/');
					}
					if (indexSeparator == -1) {
						indexSeparator = entryName.lastIndexOf('\\');
					}
					int indexDot = entryName.lastIndexOf('.');
					if (indexUnderScore == -1 || indexDot == -1) {
						throw new ConfigurationException("Could not find the configuration name and type from the URL '"+url.toString()+"'. ");
					}
					setName(entryName.substring(indexSeparator+1, indexUnderScore));
					setType(entryName.substring(indexUnderScore+1, indexDot));
					setConfigDirectory(new File(workingDirectory.getPath()+File.separator+getName()));
					jis.close();
					return;
				}
			}
			
		} catch (IOException e) {
			throw new ConfigurationException("Could not find the configuration name and type from the URL '"+url.toString()+"'. ", e);
		}
    }
    
    /**
     * Prints the content of the configuration information file to the system logger
     * 
     * @throws MaltChainedException
     */
    public void echoInfoFile() throws MaltChainedException {
    	checkConfigDirectory();
    	JarInputStream jis;
    	try {
	    	if (url == null) {
	    		jis = new JarInputStream(new FileInputStream(workingDirectory.getPath()+File.separator+getName()+".mco"));
	    	} else {
	    		jis = new JarInputStream(url.openConnection().getInputStream());
	    	}
			JarEntry je;

			while ((je = jis.getNextJarEntry()) != null) {
		        String entryName = je.getName();

		        if (entryName.endsWith(getName()+"_"+getType()+".info")) {
		        	int c;
				    while ((c = jis.read()) != -1) {
				    	SystemLogger.logger().info((char)c);
				    }	
		        }
			}
	    	jis.close();
    	} catch (FileNotFoundException e) {
    		throw new ConfigurationException("Could not print configuration information file. The configuration file '"+workingDirectory.getPath()+File.separator+getName()+".mco"+"' cannot be found. ", e);
    	} catch (IOException e) {
			throw new ConfigurationException("Could not print configuration information file. ", e);
		}

    }
    
    /**
     * Unpacks the malt configuration file (.mco).
     * 
     * @throws MaltChainedException
     */
    public void unpackConfigFile() throws MaltChainedException {
    	checkConfigDirectory();
    	JarInputStream jis;
    	try {
	    	if (url == null) {
	    		jis = new JarInputStream(new FileInputStream(workingDirectory.getPath()+File.separator+getName()+".mco"));
	    	} else {
	    		jis = new JarInputStream(url.openConnection().getInputStream());
	    	}
	    	unpackConfigFile(jis);
	    	jis.close();
    	} catch (FileNotFoundException e) {
    		throw new ConfigurationException("Could not unpack configuration. The configuration file '"+workingDirectory.getPath()+File.separator+getName()+".mco"+"' cannot be found. ", e);
    	} catch (IOException e) {
    		if (configDirectory.exists()) {
    			deleteConfigDirectory();
    		}
			throw new ConfigurationException("Could not unpack configuration. ", e);
		}
    	initCreatedByMaltParserVersionFromInfoFile();
    }

    protected void unpackConfigFile(JarInputStream jis) throws MaltChainedException {
		try {
			JarEntry je;
			byte[] readBuffer = new byte[BUFFER];
	    	SortedSet<String> directoryCache  = new TreeSet<String>();
			while ((je = jis.getNextJarEntry()) != null) {
		        String entryName = je.getName();

		        if (entryName.startsWith("/")) {
		        	entryName = entryName.substring(1);
		        }
		        if (entryName.endsWith(File.separator)) {
		            return;
		        }
		        int index = -1;
	        	if (File.separator.equals("\\")) {
	        		entryName = entryName.replace('/', '\\');
	        		index = entryName.lastIndexOf("\\");
	        	} else if (File.separator.equals("/")) {
	        		entryName = entryName.replace('\\', '/');
	        		index = entryName.lastIndexOf("/");
	        	}
		        if (index > 0) {
		            String dirName = entryName.substring(0, index);
		            if (!directoryCache.contains(dirName)) {
		                File directory = new File(workingDirectory.getPath()+File.separator+dirName);
		                if (!(directory.exists() && directory.isDirectory())) {
		                    if (!directory.mkdirs()) {
		                    	throw new ConfigurationException("Unable to make directory '" + dirName +"'. ");
		                    }
		                    directoryCache.add(dirName);
		                }
		            }
		        }
		       
		        if (new File(workingDirectory.getPath()+File.separator+entryName).isDirectory() && new File(workingDirectory.getPath()+File.separator+entryName).exists()) {
		        	continue;
		        }
		        BufferedOutputStream bos;
		        try {
		        	bos = new BufferedOutputStream(new FileOutputStream(workingDirectory.getPath()+File.separator+entryName), BUFFER);
		    	} catch (FileNotFoundException e) {
					throw new ConfigurationException("Could not unpack configuration. The file '"+workingDirectory.getPath()+File.separator+entryName+"' cannot be unpacked. ", e);
		    	}
				int n = 0;
			    while ((n = jis.read(readBuffer, 0, BUFFER)) != -1) {
			    	bos.write(readBuffer, 0, n);
			    }
			    bos.flush();
			    bos.close();
			}
		} catch (IOException e) {
			throw new ConfigurationException("Could not unpack configuration. ", e);
		}
    }
	    
	/**
	 * Returns the name of the configuration directory
	 * 
	 * @return the name of the configuration directory
	 */
	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the type of the configuration directory
	 * 
	 * @return the type of the configuration directory
	 */
	public String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Returns a file handler object for the working directory
	 * 
	 * @return a file handler object for the working directory
	 */
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * Initialize the working directory
	 * 
	 * @throws MaltChainedException
	 */
	public void initWorkingDirectory() throws MaltChainedException {
		try {
			initWorkingDirectory(OptionManager.instance().getOptionValue(0, "config", "workingdir").toString());
		} catch (NullPointerException e) {
			throw new ConfigurationException("The configuration cannot be found.", e);
		}
	}

	/**
	 * Initialize the working directory according to the path. If the path is equals to "user.dir" or current directory, then the current directory
	 *  will be the working directory.
	 * 
	 * @param pathPrefixString	the path to the working directory
	 * @throws MaltChainedException
	 */
	public void initWorkingDirectory(String pathPrefixString) throws MaltChainedException {
		if (pathPrefixString == null || pathPrefixString.equalsIgnoreCase("user.dir") || pathPrefixString.equalsIgnoreCase(".")) {
			workingDirectory = new File(System.getProperty("user.dir"));
		} else {
			workingDirectory = new File(pathPrefixString);
		}

		if (workingDirectory == null || !workingDirectory.isDirectory()) {
			new ConfigurationException("The specified working directory '"+pathPrefixString+"' is not a directory. ");
		}
	}
	
	/**
	 * Returns the URL to the malt configuration file (.mco) 
	 * 
	 * @return the URL to the malt configuration file (.mco)
	 */
	public URL getUrl() {
		return url;
	}

	protected void setUrl(URL url) {
		this.url = url;
	}

	/**
	 * Returns a reference to the configuration 
	 * 
	 * @return a reference to the configuration 
	 */
//	public Configuration getConfiguration() {
//		return configuration;
//	}

	/**
	 * Sets the reference to the configuration
	 * 
	 * @param configuration a reference to a configuration
	 */
//	public void setConfiguration(Configuration configuration) {
//		this.configuration = configuration;
//	}
	
	/**
	 * Returns the option container index
	 * 
	 * @return the option container index
	 */
	public int getContainerIndex() {
		return containerIndex;
	}

	/**
	 * Sets the option container index
	 * 
	 * @param containerIndex a option container index
	 */
	public void setContainerIndex(int containerIndex) {
		this.containerIndex = containerIndex;
	}

	/**
	 * Returns the version number of MaltParser which created the malt configuration file (.mco)
	 * 
	 * @return the version number of MaltParser which created the malt configuration file (.mco)
	 */
	public String getCreatedByMaltParserVersion() {
		return createdByMaltParserVersion;
	}

	/**
	 * Sets the version number of MaltParser which created the malt configuration file (.mco)
	 * 
	 * @param createdByMaltParserVersion a version number of MaltParser
	 */
	public void setCreatedByMaltParserVersion(String createdByMaltParserVersion) {
		this.createdByMaltParserVersion = createdByMaltParserVersion;
	}

	protected void initCreatedByMaltParserVersionFromInfoFile() throws MaltChainedException {
		File info = new File(configDirectory.getPath()+File.separator+getName()+"_"+getType()+".info");
		if (!info.exists()) {
			throw new ConfigurationException("Could not retrieve the version number of the MaltParser configuration.");
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(info));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("Version:                       ")) {
					setCreatedByMaltParserVersion(line.substring(31));
					break;
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			throw new ConfigurationException("Could not retrieve the version number of the MaltParser configuration.", e);
		} catch (IOException e) {
			throw new ConfigurationException("Could not retrieve the version number of the MaltParser configuration.", e);
		}
		checkNConvertConfigVersion();
	}
	
	protected void checkNConvertConfigVersion() throws MaltChainedException {
		if (createdByMaltParserVersion.startsWith("1.0")) {
			SystemLogger.logger().info("  Converts the MaltParser configuration ");
			SystemLogger.logger().info("1.0");
			SystemLogger.logger().info(" to ");
			SystemLogger.logger().info(SystemInfo.getVersion());
			SystemLogger.logger().info("\n");
			File[] configFiles = configDirectory.listFiles();
			for (int i = 0, n = configFiles.length; i < n; i++) {
				if (configFiles[i].getName().endsWith(".mod")) {
					configFiles[i].renameTo(new File(configDirectory.getPath()+File.separator+"odm0."+configFiles[i].getName()));
				}
				if (configFiles[i].getName().endsWith(getName()+".dsm")) {
					configFiles[i].renameTo(new File(configDirectory.getPath()+File.separator+"odm0.dsm"));
				}
				if (configFiles[i].getName().equals("savedoptions.sop")) {
					configFiles[i].renameTo(new File(configDirectory.getPath()+File.separator+"savedoptions.sop.old"));
				}
				if (configFiles[i].getName().equals("symboltables.sym")) {
					configFiles[i].renameTo(new File(configDirectory.getPath()+File.separator+"symboltables.sym.old"));
				}
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(configDirectory.getPath()+File.separator+"savedoptions.sop.old"));
				BufferedWriter bw = new BufferedWriter(new FileWriter(configDirectory.getPath()+File.separator+"savedoptions.sop"));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("0\tguide\tprediction_strategy")) {
						bw.write("0\tguide\tdecision_settings\tT.TRANS+A.DEPREL\n");
					} else {
						bw.write(line);
						bw.write('\n');
					}
				}
				br.close();
				bw.flush();
				bw.close();
				new File(configDirectory.getPath()+File.separator+"savedoptions.sop.old").delete();
			} catch (FileNotFoundException e) {
				throw new ConfigurationException("Could convert savedoptions.sop version 1.0.4 to version 1.1. ", e);
			}  catch (IOException e) {
				throw new ConfigurationException("Could convert savedoptions.sop version 1.0.4 to version 1.1. ", e);
			}		
			try {
				BufferedReader br = new BufferedReader(new FileReader(configDirectory.getPath()+File.separator+"symboltables.sym.old"));
				BufferedWriter bw = new BufferedWriter(new FileWriter(configDirectory.getPath()+File.separator+"symboltables.sym"));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("AllCombinedClassTable")) {
						bw.write("T.TRANS+A.DEPREL\n");
					} else {
						bw.write(line);
						bw.write('\n');
					}
				}
				br.close();
				bw.flush();
				bw.close();
				new File(configDirectory.getPath()+File.separator+"symboltables.sym.old").delete();
			} catch (FileNotFoundException e) {
				throw new ConfigurationException("Could convert symboltables.sym version 1.0.4 to version 1.1. ", e);
			}  catch (IOException e) {
				throw new ConfigurationException("Could convert symboltables.sym version 1.0.4 to version 1.1. ", e);
			}
		}
		if (!createdByMaltParserVersion.startsWith("1.3")) {
			SystemLogger.logger().info("  Converts the MaltParser configuration ");
			SystemLogger.logger().info(createdByMaltParserVersion);
			SystemLogger.logger().info(" to ");
			SystemLogger.logger().info(SystemInfo.getVersion());
			SystemLogger.logger().info("\n");
			

			new File(configDirectory.getPath()+File.separator+"savedoptions.sop").renameTo(new File(configDirectory.getPath()+File.separator+"savedoptions.sop.old"));
			try {
				BufferedReader br = new BufferedReader(new FileReader(configDirectory.getPath()+File.separator+"savedoptions.sop.old"));
				BufferedWriter bw = new BufferedWriter(new FileWriter(configDirectory.getPath()+File.separator+"savedoptions.sop"));
				String line;
				while ((line = br.readLine()) != null) {
					int index = line.indexOf('\t');
					int container = 0;
					if (index > -1) {
						container = Integer.parseInt(line.substring(0,index));
					}
					
					if (line.startsWith(container+"\tnivre\tpost_processing")) {
					} else if (line.startsWith(container+"\tmalt0.4\tbehavior")) {
						if (line.endsWith("true")) {
							SystemLogger.logger().info("MaltParser 1.3 doesn't support MaltParser 0.4 emulation.");
							br.close();
							bw.flush();
							bw.close();
							deleteConfigDirectory();
							System.exit(0);
						}
					} else if (line.startsWith(container+"\tsinglemalt\tparsing_algorithm")) {
						bw.write(container);
						bw.write("\tsinglemalt\tparsing_algorithm\t");
						if (line.endsWith("NivreStandard")) {
							bw.write("class org.maltparser.parser.algorithm.nivre.NivreArcStandardFactory");	
						} else if (line.endsWith("NivreEager")) {
							bw.write("class org.maltparser.parser.algorithm.nivre.NivreArcEagerFactory");
						} else if (line.endsWith("CovingtonNonProjective")) {
							bw.write("class org.maltparser.parser.algorithm.covington.CovingtonNonProjFactory");
						} else if (line.endsWith("CovingtonProjective")) {
							bw.write("class org.maltparser.parser.algorithm.covington.CovingtonProjFactory");
						}
						bw.write('\n');
					} else {
						bw.write(line);
						bw.write('\n');
					}
				}
				br.close();
				bw.flush();
				bw.close();
				new File(configDirectory.getPath()+File.separator+"savedoptions.sop.old").delete();
			} catch (FileNotFoundException e) {
				throw new ConfigurationException("Could convert savedoptions.sop version 1.0.4 to version 1.1. ", e);
			}  catch (IOException e) {
				throw new ConfigurationException("Could convert savedoptions.sop version 1.0.4 to version 1.1. ", e);
			}
		}
	}
	
	/**
	 * Terminates the configuration directory
	 * 
	 * @throws MaltChainedException
	 */
	public void terminate() throws MaltChainedException {
		if (infoFile != null) {
			try {
				infoFile.flush();
				infoFile.close();
			} catch (IOException e) {
				throw new ConfigurationException("Could not close configuration information file. ", e);
			}
		}
//		configuration = null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		try {
			if (infoFile != null) {
				infoFile.flush();
				infoFile.close();
			}
		} finally {
			super.finalize();
		}
	}
}

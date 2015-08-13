package com.paymentech.orbital.sdk.configurator;

import com.paymentech.orbital.sdk.logger.LoggerIF;
import com.paymentech.orbital.sdk.util.exceptions.InitializationException;
import com.paymentech.orbital.sdk.util.filehandling.ClassPathTemplateLoader;
import com.paymentech.orbital.sdk.util.filehandling.TemplateLoaderIF;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <p><b>Title:</b> Configurator.java</p> <p>(C)opyright 2007, Chase Paymentech Solutions, LLC. All rights reserved
 * <p/>
 * The copyright notice above does not evidence any actual or intended
 * publication of such source code.
 * Paymentech. The copyright notice above does not evidence any actual or intended publication of such source code.</p>
 * <p><b>Author:</b> Scott Monahan</p><p><b>Description:</b><br><br> Global configurations for the Orbital Java SDK </p>
 */
public class Configurator implements ConfiguratorIF {

  // default configuration file (linehandler.properties)
  private static final String DEFAULT_CONFIGURATION_FILE = "config/linehandler.properties";
  // search string to find the xml templates in the configuration file
  private static final String SEARCH_BY_XML_TEMPLATES = "XMLTemplates.Request.";
  // search string used to find complex element properties
  private static final String COMPLEX_ROOT_TEMPLATE_BASE = "XMLTemplates.Request.ComplexRoot.";
  // search string to find the security providers
  private static final String SEARCH_BY_SECURITY = "security.provider";
  // default security providers
  private static final String DEFAULT_SUN_SECURITY_PROVIDER = "sun.security.provider.Sun";
  private static final String DEFAULT_SECURITY_PROVIDER = "com.sun.net.ssl.internal.ssl.Provider";
  // just incase we need to load log4J
  private static final String SEARCH_BY_LOG4J = "log4j";
  // singleton instance
  private static Configurator instance = null;
  // reference to the template loader implementation
  private static TemplateLoaderIF templateLoader = null;
  // default template loader
  private static String DEFAULT_LOG_FILE = "config/log4j-config.xml";
  // configuration file (default is linehandler.properties)
  private static String configFile = null;
  // application loggers - these will use whatever logging is being used (Java, log4J, or anything supported
  // Apache Common Logging implementation
  private static Logger eCommerceLogger;
  private static Logger engineLogger;
  // old versions of the LoggerIF (just a wrapper for the Apache-Commons Log class
  private static LoggerIF eCommerceLoggerIF;
  private static LoggerIF engineLoggerIF;
  private static boolean logInitializedBySDK = false;
  // Map which contains all the linehandler.properties
  private Map configurations = new HashMap();
  // Map which contains - Key as the transaction type and value as the xml template (as string)
  private Map xmlTemplates = new HashMap();
  private Properties propsFile = null;
  private String configHome = "";
  private boolean isUniqueConfig = false;

  /**
   * Configurator is a Singleton
   */
  private Configurator() {
  }

  public static synchronized Configurator getInstance(Logger engine, Logger eCommerce) throws InitializationException {
    if (engine == null || eCommerce == null) {
      throw new ConfigurationException("Exception while initilizing the configurator - Invalid logger object provided");
    }
    eCommerceLogger = engine;
    engineLogger = eCommerce;
    return getInstance();
  }

  public static synchronized Configurator getInstance(String theConfigFile, Logger engine, Logger eCommerce) throws InitializationException {
    if (engine == null || eCommerce == null) {
      throw new ConfigurationException("Exception while initilizing the configurator - Invalid logger object provided");
    }
    eCommerceLogger = engine;
    engineLogger = eCommerce;
    return getInstance(theConfigFile);
  }

  /**
   * Get the Configurator object as a Singleton
   *
   * @return Configurator - which is a singleton
   * @throws InitializationException
   */
  public static synchronized Configurator getInstance() throws InitializationException {
    if (instance == null) {
      try {
        instance = new Configurator();
        instance.load();
      } catch (InitializationException iex) {
        instance = null;
        Configurator.configFile = null;
        throw iex;
      }
    }
    return instance;
  }

  /**
   * Set the configuration file and return the Configurator object as a Singleton
   *
   * @param theConfigFile
   * @return Configurator - which is a singleton
   * @throws InitializationException
   */
  public static synchronized Configurator getInstance(String theConfigFile) throws InitializationException {
    if (theConfigFile == null || theConfigFile.trim().length() <= 0) {
      throw new ConfigurationException("Exception while initilizing the configurator - Invalid config file name provided");
    }
    if (theConfigFile != null) {
      if (Configurator.configFile != null && instance != null) {
        File temp = new File(theConfigFile);
        if (!temp.getPath().toUpperCase().equals(Configurator.configFile.toUpperCase())) {
          throw new ConfigurationException("A singleton configurator is already initialized with the following  [" + Configurator.configFile + "]");
        }
      }
      setConfigFileName(theConfigFile);
    }
    if (instance == null) {
      try {
        instance = new Configurator();
        instance.load();
      } catch (InitializationException iex) {
        instance = null;
        Configurator.configFile = null;
        throw iex;
      }
    }
    return instance;
  }

  /**
   * Used for JUnit test cases only
   */
  public static void reload() {
    instance = null;
  }

  public String getConfigFileName() {
    return Configurator.configFile;
  }

  public static void setConfigFileName(String theConfigFile) {
    Configurator.configFile = theConfigFile;
  }

  public Map getConfigurations() {
    return this.configurations;
  }

  public void setConfigurations(Map configurations) {
    this.configurations = configurations;
  }

  /**
   * Get the xmlTemplates - key as the transaction type and value as the xml template as string
   *
   * @return Map
   */
  public Map getXmlTemplates() {
    return this.xmlTemplates;
  }

  /**
   * Set the xmlTemplates - key as the transaction type and value as the xml template (as string)
   *
   * @param xmlTemplates - map
   */
  public void setXmlTemplates(Map xmlTemplates) {
    this.xmlTemplates = xmlTemplates;

    // this is to allow changing of template loaders
    // testing purposes
    if (xmlTemplates == null) {
      this.xmlTemplates = new HashMap();
      templateLoader = null;
    }
  }

  /**
   * Get the eCommerceLogger
   *
   * @return Log
   * @deprecated
   */
  public LoggerIF getEcommerceLogger() {
    return eCommerceLoggerIF;
  }

  /**
   * Get the engineLogger
   *
   * @return Log
   * @deprecated
   */
  public LoggerIF getEngineLogger() {
    return engineLoggerIF;
  }

  /**
   * Get the log4J properties
   *
   * @return Log
   * @deprecated
   */
  public Map getLog4JConfigurations() {
    return new HashMap();
  }

  /**
   * Get the Apache-Commons Logger which logs to engine.log
   *
   * @return
   */
  public Logger getCommonEngineLogger() {
    return engineLogger;
  }

  /**
   * Get the Apache-Commons Logger which logs to eCommerce.log
   *
   * @return
   */
  public Logger getCommonEcommerceLogger() {
    return eCommerceLogger;
  }

  public TemplateLoaderIF getTemplateLoader() {
    return templateLoader;
  }

  /**
   * This method loads the XML templates from the Configurations as defined in the linehandler.properties, if any
   *
   * @throws InitializationException
   */
  public void loadXmlTemplates() throws InitializationException {
    try {

      createTemplateLoader();

      if (configurations.size() > 0) {
        Set set = configurations.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
          Object line = iterator.next();
          String keyStr = line.toString();
          loadXmlTemplates(keyStr);
        }
      }
    } catch (InitializationException ie) {
      engineLogger.error("InitializationException:- " + ie.getMessage());
      throw ie;
    }
  }

  /**
   * This method loads an XML template file as a String into the Map
   *
   * @param templateFilePath - location of the xml template file
   * @throws InitializationException
   */
  public void loadXmlTemplates(String templateFilePath) throws InitializationException {
    StringTokenizer token = null;

    try {

      // Search by this string "XMLTemplates.Request." - xml template
      if (templateFilePath.startsWith(SEARCH_BY_XML_TEMPLATES)
          && !templateFilePath.startsWith(COMPLEX_ROOT_TEMPLATE_BASE)) {

        // search for "="
        token = new StringTokenizer(templateFilePath, "=");

        // copy the value on the left hand side of the "=" as key
        String key = token.nextElement().toString();

        // extract the string w/o "XMLTemplates.Request." in it
        String subKey = key.substring(SEARCH_BY_XML_TEMPLATES.length(), key.length());

        // copy the value on the right hand side of the "=" as value
        String xmlFile = token.nextToken().toString();

        // get the xml file as a String
        String value = templateLoader.loadTemplate(xmlFile);

        // put the xml string (value) and and its name (subKey) into the Map
        xmlTemplates.put(subKey, value);
      }

    } catch (InitializationException ie) {
      engineLogger.error("InitializationException:- " + ie.getMessage());
      throw ie;
    } catch (Exception e) {
      engineLogger.error("Exception:- " + e.getMessage());
      throw new InitializationException(e.getMessage());
    }

  }

  public void loadSecurityProviders() throws InitializationException {
    List providersList = new ArrayList();
    Set set = null;
    Iterator iterator = null;
    Class providerClass = null;
    String provider = null;
    String key = null;

    try {

      if (configurations.size() > 0) {

        set = configurations.entrySet();
        iterator = set.iterator();

        while (iterator.hasNext()) {

          String keyStr = iterator.next().toString().trim();

          if (keyStr.startsWith(SEARCH_BY_SECURITY)) {

            if (keyStr.indexOf("=") > -1) {

              key = keyStr.substring(0, keyStr.indexOf("="));
              provider = keyStr.substring(keyStr.indexOf("=") + 1);
              engineLogger.debug("Loading Security Provider: " + provider);
              providersList.add(provider);

            } else {
              engineLogger.debug("Failed to load Security Provider: " + keyStr);
            }
          }
        }

        // if (providersList.size() == 0) {
        //   //add default providers
        //     providersList.add(DEFAULT_SUN_SECURITY_PROVIDER);
        //     providersList.add(DEFAULT_SECURITY_PROVIDER);
        // }

        //add the security providers
        for (int i = 0; i < providersList.size(); i++) {
          providerClass = Class.forName((String) providersList.get(i));
          java.security.Security.addProvider((java.security.Provider) providerClass.newInstance());
        }

      } else {
        throw new InitializationException();
      }

    } catch (IllegalAccessException iae) {
      engineLogger.error("IllegalAccessException:- " + iae.getMessage());
      throw new InitializationException();
    } catch (ClassNotFoundException cnfe) {
      engineLogger.error("ClassNotFoundException:- " + cnfe.getMessage());
      throw new InitializationException();
    } catch (InstantiationException ine) {
      engineLogger.error("InstantiationException:- " + ine.getMessage());
      throw new InitializationException();
    }

  }

  public void load() throws InitializationException {
    try {

      // initialize the logging
      // since it is done with Apache commons-logging
      // we really don't have to do much here
      // log4J will pick up by default and is already part of the
      // linehandler.properties file

      if (eCommerceLogger == null && engineLogger == null) {
        logInitializedBySDK = true;
        eCommerceLogger = Logger.getLogger("eCommerceLogger");
        engineLogger = Logger.getLogger("engineLogger");
      }

      // set the LoggerIF instances for older users
      //   eCommerceLoggerIF = new LoggerIF (eCommerceLogger);
      // engineLoggerIF  = new LoggerIF (engineLogger);

      loadConfigurations();

      // make sure the logging is configured
      // or we will do it ourselves
      if (!logInitializedBySDK) {
        unSetHTTPCLientLog();
      }

      engineLogger.info("************ New Configurator created *************");
      engineLogger.info("Configurator configuration file = " + configFile);

      loadSecurityProviders();

      engineLogger.info("************ Security Providers Loaded *************");

      loadXmlTemplates();

      engineLogger.info("************ XML Templates Loaded *************");

      // dump the properties file
      engineLogger.info(instance.toString());

    } catch (InitializationException iex) {
      throw iex;
    }
  }

  private void loadConfigurations() throws ConfigurationException {
    // if the config file is not set; use default
    configFile = DEFAULT_CONFIGURATION_FILE;
    try {

      // Get the configurations that the linehandler will use
      configurations = readPropertiesFile(configFile);
      if (configurations.isEmpty()) {
        throw new ConfigurationException("Exception while initilizing the configurator - Invalid linehandler.properties file provided");
      }
    } catch (ConfigurationException ce) {
      throw ce;
    } catch (Exception e) {
      System.err.println("Exception - " + e.getMessage());
      throw new ConfigurationException(e.getMessage());
    }
  }

  /**
   * Gets the whole Configurator Object details (for logging)
   *
   * @return String
   */
  public String toString() {
    StringBuffer content = new StringBuffer();
    // Configurations Map
    content.append("\n\n************* Start Of Configuration Properties *************\n");
    if (!configurations.isEmpty()) {
      Set set = configurations.entrySet();
      Iterator iterator = set.iterator();
      while (iterator.hasNext()) {
        Object line = iterator.next();
        Map.Entry<String, String> entry = (Map.Entry<String, String>) line;
        if (entry.getKey().equals("OrbitalConnectionPassword")) {
          content.append(entry.getKey() + "=########\n");
        } else {
          content.append(line.toString());
          content.append("\n");
        }
      }
    }
    content.append("************* End Of Configuration Properties *************");
    content.append("\n\n************* Start of XML Templates *************\n");
    // XML Templates Map
    if (!xmlTemplates.isEmpty()) {
      Set set = xmlTemplates.entrySet();
      Iterator iterator = set.iterator();
      while (iterator.hasNext()) {
        Object line = iterator.next();
        content.append(line.toString());
        content.append("\n");
      }
    }
    content.append("************* End of XML Templates *************\n");
    return content.toString();
  }

  /**
   * Put the linehandler.properties (default name) into a Map
   *
   * @param propsFile - properties file
   * @return Map - key/value pairs
   */
  private Map buildPropertiesMap(Properties propsFile) {
    Map propertiesMap = new HashMap();
    Enumeration e = propsFile.keys();
    while (e.hasMoreElements()) {
      String nextKey = (String) e.nextElement();
      String nextValue = propsFile.getProperty(nextKey);
      propertiesMap.put(nextKey, nextValue);
    }
    return propertiesMap;
  }

  /**
   * Load the linehandler.properties (default name) into the Properties object
   *
   * @param configfile
   * @return Map
   */
  private Map readPropertiesFile(String configfile) throws ConfigurationException {
    propsFile = new Properties();
    Map propertiesMap = new HashMap();
    InputStream configStream = null;
    try {
      // If the file is set to an relative path
      configStream = getClass().getClassLoader().getResourceAsStream(configfile);
      // If the file is set to an absolute path
      // load the properties file
      propsFile.load(configStream);
      propertiesMap = buildPropertiesMap(propsFile);
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("ConfigurationException =  " + e.getMessage());
      throw new ConfigurationException("Configuration file (linehandler.properties) is not found!");
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Exception: " + e.getMessage());
      throw new ConfigurationException("Configuration file (linehandler.properties) is not found!");
    } finally {
      try {
        if (configStream != null) configStream.close();
      } catch (Exception ignored) {
      }
    }
    return propertiesMap;
  }

  private void unSetHTTPCLientLog() {
    String httpClientLogLevel = (String) configurations.get("HTTPClientLogLevel");
    Logger log1 = Logger.getLogger("org.apache.commons.httpclient");
    Logger log2 = Logger.getLogger("httpclient");
    // Turn off the log level
    if (httpClientLogLevel == null) {
      log1.setLevel(Level.OFF);
      log2.setLevel(Level.OFF);
    }
    // set with the level from config.xml, if it is not a valid one set to "Off"
    if (httpClientLogLevel != null) {
      log1.setLevel(Level.toLevel(httpClientLogLevel, Level.OFF));
      log2.setLevel(Level.toLevel(httpClientLogLevel, Level.OFF));
    }
  }

  /**
   * This method loads the log4j properties from the Configurations as defined in the linehandler.properties
   *
   * @throws InitializationException
   */
  public Properties loadLog4jProperties() throws InitializationException {
    StringTokenizer token = null;
    Properties log4JConfigurations = new Properties();

    try {

      if (configurations.size() > 0) {

        Set set = configurations.entrySet();
        Iterator iterator = set.iterator();

        while (iterator.hasNext()) {

          Object key = iterator.next();
          String keyStr = key.toString();

          if (keyStr.startsWith(SEARCH_BY_LOG4J)) {
            // search for "="
            token = new StringTokenizer(keyStr, "=");

            // copy the value on the left hand side of the "=" as key
            String keyName = token.nextElement().toString();
            String value = token.nextToken().toString();
            log4JConfigurations.put(keyName, value);
          }
        }
      } else {
        throw new InitializationException();
      }

    } catch (InitializationException ie) {
      engineLogger.error(ie.getMessage());
      throw ie;
    }

    return log4JConfigurations;
  }

  /*
   * @deprecated
   */
  public Properties loadLog4jProperties(String log4jPropertyString) {
    return new Properties();
  }

  private String getenv(String variable) {
    String retval = null;

    if (System.getProperty("java.version").startsWith("1.4")) {
      retval = System.getProperty(variable);
    } else {
      retval = System.getenv(variable);

      if (retval == null) {
        retval = System.getProperty(variable);
      }
    }
    return retval;
  }

  private void createTemplateLoader() throws InitializationException {
    if (templateLoader == null) {
      templateLoader = new ClassPathTemplateLoader();
    }
  }

}



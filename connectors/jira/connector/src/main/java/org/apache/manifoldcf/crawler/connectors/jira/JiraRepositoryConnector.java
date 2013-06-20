/* $Id: JiraRepositoryConnector.java 1490585 2013-06-07 11:13:35Z kwright $ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.manifoldcf.crawler.connectors.jira;

import java.io.ByteArrayInputStream;
import org.apache.manifoldcf.core.common.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import java.util.Map.Entry;
import java.security.GeneralSecurityException;
import org.json.simple.JSONObject;
/**
 *
 * @author andrew
 */
public class JiraRepositoryConnector extends BaseRepositoryConnector {

  protected final static String ACTIVITY_READ = "read document";
  public final static String ACTIVITY_FETCH = "fetch";
  protected static final String RELATIONSHIP_CHILD = "child";
  
  /** Deny access token for default authority */
  private final static String defaultAuthorityDenyToken = "DEAD_AUTHORITY";

  // Nodes
  private static final String JOB_STARTPOINT_NODE_TYPE = "startpoint";
  private static final String JOB_QUERY_ATTRIBUTE = "query";
  private static final String JOB_ACCESS_NODE_TYPE = "access";
  private static final String JOB_TOKEN_ATTRIBUTE = "token";

  // Configuration tabs
  private static final String JIRA_SERVER_TAB_PROPERTY = "JiraRepositoryConnector.Server";
  
  // Specification tabs
  private static final String JIRA_QUERY_TAB_PROPERTY = "JiraRepositoryConnector.JiraQuery";
  private static final String JIRA_SECURITY_TAB_PROPERTY = "JiraRepositoryConnector.Security";
  
  // Template names for configuration
  /**
   * Forward to the javascript to check the configuration parameters
   */
  private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration_jira_server.js";
  /**
   * Server tab template
   */
  private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_jira_server.html";
  
  /**
   * Forward to the HTML template to view the configuration parameters
   */
  private static final String VIEW_CONFIG_FORWARD = "viewConfiguration_jira.html";
   
  // Template names for specification
  /**
   * Forward to the javascript to check the specification parameters for the job
   */
  private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification_jira.js";
  /**
   * Forward to the template to edit the query for the job
   */
  private static final String EDIT_SPEC_FORWARD_JIRAQUERY = "editSpecification_jiraQuery.html";
  /**
   * Forward to the template to edit the security parameters for the job
   */
  private static final String EDIT_SPEC_FORWARD_SECURITY = "editSpecification_jiraSecurity.html";
  
  /**
   * Forward to the template to view the specification parameters for the job
   */
  private static final String VIEW_SPEC_FORWARD = "viewSpecification_jira.html";
  
  /**
   * Endpoint server name
   */
  protected String server = "jira";
  protected JiraSession session = null;
  protected long lastSessionFetch = -1L;
  protected static final long timeToRelease = 300000L;
  protected String clientid = null;
  protected String clientsecret = null;
  protected String jiraurl = null;

  public JiraRepositoryConnector() {
    super();
  }

  /**
   * Return the list of activities that this connector supports (i.e. writes
   * into the log).
   *
   * @return the list.
   */
  @Override
  public String[] getActivitiesList() {
    return new String[]{ACTIVITY_FETCH, ACTIVITY_READ};
  }

  /**
   * Get the bin name strings for a document identifier. The bin name
   * describes the queue to which the document will be assigned for throttling
   * purposes. Throttling controls the rate at which items in a given queue
   * are fetched; it does not say anything about the overall fetch rate, which
   * may operate on multiple queues or bins. For example, if you implement a
   * web crawler, a good choice of bin name would be the server name, since
   * that is likely to correspond to a real resource that will need real
   * throttle protection.
   *
   * @param documentIdentifier is the document identifier.
   * @return the set of bin names. If an empty array is returned, it is
   * equivalent to there being no request rate throttling available for this
   * identifier.
   */
  @Override
  public String[] getBinNames(String documentIdentifier) {
    return new String[]{server};
  }

  /**
   * Close the connection. Call this before discarding the connection.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    if (session != null) {
      session.close();
      session = null;
      lastSessionFetch = -1L;
    }

    clientid = null;
    clientsecret = null;
    jiraurl = null;
  }

  /**
   * This method create a new JIRA session for a JIRA
   * repository, if the repositoryId is not provided in the configuration, the
   * connector will retrieve all the repositories exposed for this endpoint
   * the it will start to use the first one.
   *
   * @param configParameters is the set of configuration parameters, which in
   * this case describe the target appliance, basic auth configuration, etc.
   * (This formerly came out of the ini file.)
   */
  @Override
  public void connect(ConfigParams configParams) {
    super.connect(configParams);

    clientid = params.getParameter(JiraConfig.CLIENT_ID_PARAM);
    clientsecret = params.getObfuscatedParameter(JiraConfig.CLIENT_SECRET_PARAM);
    jiraurl = params.getParameter(JiraConfig.JIRAURL_TOKEN_PARAM);
  }

  /**
   * Test the connection. Returns a string describing the connection
   * integrity.
   *
   * @return the connection's status as a displayable string.
   */
  @Override
  public String check() throws ManifoldCFException {
    try {
      checkConnection();
      return super.check();
    } catch (ServiceInterruption e) {
      return "Connection temporarily failed: " + e.getMessage();
    } catch (ManifoldCFException e) {
      return "Connection failed: " + e.getMessage();
    }
  }

  protected class CheckConnectionThread extends Thread {

    protected Throwable exception = null;

    public CheckConnectionThread() {
      super();
      setDaemon(true);
    }

    public void run() {
      try {
        session.getRepositoryInfo();
      } catch (Throwable e) {
        this.exception = e;
      }
    }

    public Throwable getException() {
      return exception;
    }
  }

  protected void checkConnection() throws ManifoldCFException, ServiceInterruption {
    getSession();
    CheckConnectionThread t = new CheckConnectionThread();
    try {
      t.start();
      t.join();
      Throwable thr = t.getException();
      if (thr != null) {
        if (thr instanceof IOException) {
          throw (IOException) thr;
        } else if (thr instanceof RuntimeException) {
          throw (RuntimeException) thr;
        } else {
          throw (Error) thr;
        }
      }
      return;
    } catch (InterruptedException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (java.net.SocketTimeoutException e) {
      Logging.connectors.warn("JIRA: Socket timeout: " + e.getMessage(), e);
      handleIOException(e);
    } catch (InterruptedIOException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (IOException e) {
      Logging.connectors.warn("JIRA: Error checking repository: " + e.getMessage(), e);
      handleIOException(e);
    }
  }

  /**
   * Set up a session
   */
  protected void getSession() throws ManifoldCFException, ServiceInterruption {
    if (session == null) {
      // Check for parameter validity

      if (StringUtils.isEmpty(clientid)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.CLIENT_ID_PARAM
            + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: Clientid = '" + clientid + "'");
      }

      if (StringUtils.isEmpty(clientsecret)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.CLIENT_SECRET_PARAM
            + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: Clientsecret = '" + clientsecret + "'");
      }

      if (StringUtils.isEmpty(jiraurl)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRAURL_TOKEN_PARAM
            + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: jiraurl = '" + jiraurl + "'");
      }

      session = new JiraSession(clientid, clientsecret, jiraurl);

    }
    lastSessionFetch = System.currentTimeMillis();
  }

  @Override
  public void poll() throws ManifoldCFException {
    if (lastSessionFetch == -1L) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime >= lastSessionFetch + timeToRelease) {
      session.close();
      session = null;
      lastSessionFetch = -1L;
    }
  }

  /**
   * Get the maximum number of documents to amalgamate together into one
   * batch, for this connector.
   *
   * @return the maximum number. 0 indicates "unlimited".
   */
  @Override
  public int getMaxDocumentRequest() {
    return 1;
  }

  /**
   * Return the list of relationship types that this connector recognizes.
   *
   * @return the list.
   */
  @Override
  public String[] getRelationshipTypes() {
    return new String[]{RELATIONSHIP_CHILD};
  }

  /**
   * Fill in a Server tab configuration parameter map for calling a Velocity
   * template.
   *
   * @param newMap is the map to fill in
   * @param parameters is the current set of configuration parameters
   */
  private static void fillInServerConfigurationMap(Map<String, Object> newMap, ConfigParams parameters) {
    String clientid = parameters.getParameter(JiraConfig.CLIENT_ID_PARAM);
    String clientsecret = parameters.getObfuscatedParameter(JiraConfig.CLIENT_SECRET_PARAM);
    String jiraurl = parameters.getParameter(JiraConfig.JIRAURL_TOKEN_PARAM);

    if (clientid == null) {
      clientid = StringUtils.EMPTY;
    }
    
    if (clientsecret == null) {
      clientsecret = StringUtils.EMPTY;
    }

    if (jiraurl == null) {
      jiraurl = StringUtils.EMPTY;
    }

    newMap.put("CLIENTID", clientid);
    newMap.put("CLIENTSECRET", clientsecret);
    newMap.put("JIRAURL", jiraurl);
  }

  /**
   * View configuration. This method is called in the body section of the
   * connector's view configuration page. Its purpose is to present the
   * connection information to the user. The coder can presume that the HTML
   * that is output from this configuration will be within appropriate <html>
   * and <body> tags.
   *
   * @param threadContext is the local thread context.
   * @param out is the output to which any HTML should be sent.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
      Locale locale, ConfigParams parameters) throws ManifoldCFException, IOException {
    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Fill in map from each tab
    fillInServerConfigurationMap(paramMap, parameters);

    Messages.outputResourceWithVelocity(out,locale,VIEW_CONFIG_FORWARD,paramMap);
  }

  /**
   *
   * Output the configuration header section. This method is called in the
   * head section of the connector's configuration page. Its purpose is to add
   * the required tabs to the list, and to output any javascript methods that
   * might be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out is the output to which any HTML should be sent.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   * @param tabsArray is an array of tab names. Add to this array any tab
   * names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters, List<String> tabsArray)
      throws ManifoldCFException, IOException {
    // Add the Server tab
    tabsArray.add(Messages.getString(locale, JIRA_SERVER_TAB_PROPERTY));
    // Map the parameters
    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Fill in the parameters from each tab
    fillInServerConfigurationMap(paramMap, parameters);

    // Output the Javascript - only one Velocity template for all tabs
    Messages.outputResourceWithVelocity(out,locale,EDIT_CONFIG_HEADER_FORWARD,paramMap);
  }

  @Override
  public void outputConfigurationBody(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
      throws ManifoldCFException, IOException {


    // Call the Velocity templates for each tab
    Map<String, Object> paramMap = new HashMap<String, Object>();
    // Set the tab name
    paramMap.put("TabName", tabName);

    // Server tab
    // Fill in the parameters
    fillInServerConfigurationMap(paramMap, parameters);
    Messages.outputResourceWithVelocity(out,locale,EDIT_CONFIG_FORWARD_SERVER,paramMap);
  }

  /**
   * Process a configuration post. This method is called at the start of the
   * connector's configuration page, whenever there is a possibility that form
   * data for a connection has been posted. Its purpose is to gather form
   * information and modify the configuration parameters accordingly. The name
   * of the posted form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param variableContext is the set of variables available from the post,
   * including binary file post information.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an
   * error that should prevent saving of the connection (and cause a
   * redirection to an error page).
   *
   */
  @Override
  public String processConfigurationPost(IThreadContext threadContext,
      IPostParameters variableContext, ConfigParams parameters)
      throws ManifoldCFException {

    String clientid = variableContext.getParameter(JiraConfig.CLIENT_ID_PARAM);
    if (clientid != null) {
      parameters.setParameter(JiraConfig.CLIENT_ID_PARAM, clientid);
    }

    String clientsecret = variableContext.getParameter(JiraConfig.CLIENT_SECRET_PARAM);
    if (clientsecret != null) {
      parameters.setObfuscatedParameter(JiraConfig.CLIENT_SECRET_PARAM, clientsecret);
    }

    String jiraurl = variableContext.getParameter(JiraConfig.JIRAURL_TOKEN_PARAM);
    if (jiraurl != null) {
      parameters.setParameter(JiraConfig.JIRAURL_TOKEN_PARAM, jiraurl);
    }

    return null;
  }

  /**
   * Fill in specification Velocity parameter map for JIRAQuery tab.
   */
  private static void fillInJIRAQuerySpecificationMap(Map<String, Object> newMap, DocumentSpecification ds) {
    String JiraQuery = JiraConfig.JIRA_QUERY_DEFAULT;
    for (int i = 0; i < ds.getChildCount(); i++) {
      SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
        JiraQuery = sn.getAttributeValue(JOB_QUERY_ATTRIBUTE);
      }
    }
    newMap.put("JIRAQUERY", JiraQuery);
  }

  /**
   * Fill in specification Velocity parameter map for JIRASecurity tab.
   */
  private static void fillInJIRASecuritySpecificationMap(Map<String, Object> newMap, DocumentSpecification ds) {
    List<Map<String,String>> accessTokenList = new ArrayList<Map<String,String>>();
    for (int i = 0; i < ds.getChildCount(); i++) {
      SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(JOB_ACCESS_NODE_TYPE)) {
        String token = sn.getAttributeValue(JOB_TOKEN_ATTRIBUTE);
        Map<String,String> accessMap = new HashMap<String,String>();
        accessMap.put("TOKEN",token);
        accessTokenList.add(accessMap);
      }
    }
    newMap.put("ACCESSTOKENS", accessTokenList);
  }

  /**
   * View specification. This method is called in the body section of a job's
   * view page. Its purpose is to present the document specification
   * information to the user. The coder can presume that the HTML that is
   * output from this configuration will be within appropriate <html> and
   * <body> tags.
   *
   * @param out is the output to which any HTML should be sent.
   * @param ds is the current document specification for this job.
   */
  @Override
  public void viewSpecification(IHTTPOutput out, Locale locale, DocumentSpecification ds)
      throws ManifoldCFException, IOException {

    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Fill in the map with data from all tabs
    fillInJIRAQuerySpecificationMap(paramMap, ds);
    fillInJIRASecuritySpecificationMap(paramMap, ds);

    Messages.outputResourceWithVelocity(out,locale,VIEW_SPEC_FORWARD,paramMap);
  }

  /**
   * Process a specification post. This method is called at the start of job's
   * edit or view page, whenever there is a possibility that form data for a
   * connection has been posted. Its purpose is to gather form information and
   * modify the document specification accordingly. The name of the posted
   * form is "editjob".
   *
   * @param variableContext contains the post data, including binary
   * file-upload information.
   * @param ds is the current document specification for this job.
   * @return null if all is well, or a string error message if there is an
   * error that should prevent saving of the job (and cause a redirection to
   * an error page).
   */
  @Override
  public String processSpecificationPost(IPostParameters variableContext,
      DocumentSpecification ds) throws ManifoldCFException {

    String jiraDriveQuery = variableContext.getParameter("jiraquery");
    if (jiraDriveQuery != null) {
      int i = 0;
      while (i < ds.getChildCount()) {
        SpecificationNode oldNode = ds.getChild(i);
        if (oldNode.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
          ds.removeChild(i);
          break;
        }
        i++;
      }
      SpecificationNode node = new SpecificationNode(JOB_STARTPOINT_NODE_TYPE);
      node.setAttribute(JOB_QUERY_ATTRIBUTE, jiraDriveQuery);
      ds.addChild(ds.getChildCount(), node);
    }
    
    String xc = variableContext.getParameter("tokencount");
    if (xc != null) {
      // Delete all tokens first
      int i = 0;
      while (i < ds.getChildCount()) {
        SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(JOB_ACCESS_NODE_TYPE))
          ds.removeChild(i);
        else
          i++;
      }

      int accessCount = Integer.parseInt(xc);
      i = 0;
      while (i < accessCount) {
        String accessDescription = "_"+Integer.toString(i);
        String accessOpName = "accessop"+accessDescription;
        xc = variableContext.getParameter(accessOpName);
        if (xc != null && xc.equals("Delete")) {
          // Next row
          i++;
          continue;
        }
        // Get the stuff we need
        String accessSpec = variableContext.getParameter("spectoken"+accessDescription);
        SpecificationNode node = new SpecificationNode(JOB_ACCESS_NODE_TYPE);
        node.setAttribute(JOB_TOKEN_ATTRIBUTE,accessSpec);
        ds.addChild(ds.getChildCount(),node);
        i++;
      }

      String op = variableContext.getParameter("accessop");
      if (op != null && op.equals("Add"))
      {
        String accessspec = variableContext.getParameter("spectoken");
        SpecificationNode node = new SpecificationNode(JOB_ACCESS_NODE_TYPE);
        node.setAttribute(JOB_TOKEN_ATTRIBUTE,accessspec);
        ds.addChild(ds.getChildCount(),node);
      }
    }

    return null;
  }

  /**
   * Output the specification body section. This method is called in the body
   * section of a job page which has selected a repository connection of the
   * current type. Its purpose is to present the required form elements for
   * editing. The coder can presume that the HTML that is output from this
   * configuration will be within appropriate <html>, <body>, and <form> tags.
   * The name of the form is "editjob".
   *
   * @param out is the output to which any HTML should be sent.
   * @param ds is the current document specification for this job.
   * @param tabName is the current tab name.
   */
  @Override
  public void outputSpecificationBody(IHTTPOutput out,
      Locale locale, DocumentSpecification ds, String tabName) throws ManifoldCFException,
      IOException {

    // Output JIRAQuery tab
    Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("TabName", tabName);
    fillInJIRAQuerySpecificationMap(paramMap, ds);
    fillInJIRASecuritySpecificationMap(paramMap, ds);
    Messages.outputResourceWithVelocity(out,locale,EDIT_SPEC_FORWARD_JIRAQUERY,paramMap);
    Messages.outputResourceWithVelocity(out,locale,EDIT_SPEC_FORWARD_SECURITY,paramMap);
  }

  /**
   * Output the specification header section. This method is called in the
   * head section of a job page which has selected a repository connection of
   * the current type. Its purpose is to add the required tabs to the list,
   * and to output any javascript methods that might be needed by the job
   * editing HTML.
   *
   * @param out is the output to which any HTML should be sent.
   * @param ds is the current document specification for this job.
   * @param tabsArray is an array of tab names. Add to this array any tab
   * names that are specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(IHTTPOutput out,
      Locale locale, DocumentSpecification ds, List<String> tabsArray)
      throws ManifoldCFException, IOException {

    tabsArray.add(Messages.getString(locale, JIRA_QUERY_TAB_PROPERTY));
    tabsArray.add(Messages.getString(locale, JIRA_SECURITY_TAB_PROPERTY));

    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Fill in the specification header map, using data from all tabs.
    fillInJIRAQuerySpecificationMap(paramMap, ds);
    fillInJIRASecuritySpecificationMap(paramMap, ds);

    Messages.outputResourceWithVelocity(out,locale,EDIT_SPEC_HEADER_FORWARD,paramMap);
  }

  /**
   * Queue "seed" documents. Seed documents are the starting places for
   * crawling activity. Documents are seeded when this method calls
   * appropriate methods in the passed in ISeedingActivity object.
   *
   * This method can choose to find repository changes that happen only during
   * the specified time interval. The seeds recorded by this method will be
   * viewed by the framework based on what the getConnectorModel() method
   * returns.
   *
   * It is not a big problem if the connector chooses to create more seeds
   * than are strictly necessary; it is merely a question of overall work
   * required.
   *
   * The times passed to this method may be interpreted for greatest
   * efficiency. The time ranges any given job uses with this connector will
   * not overlap, but will proceed starting at 0 and going to the "current
   * time", each time the job is run. For continuous crawling jobs, this
   * method will be called once, when the job starts, and at various periodic
   * intervals as the job executes.
   *
   * When a job's specification is changed, the framework automatically resets
   * the seeding start time to 0. The seeding start time may also be set to 0
   * on each job run, depending on the connector model returned by
   * getConnectorModel().
   *
   * Note that it is always ok to send MORE documents rather than less to this
   * method.
   *
   * @param activities is the interface this method should use to perform
   * whatever framework actions are desired.
   * @param spec is a document specification (that comes from the job).
   * @param startTime is the beginning of the time range to consider,
   * inclusive.
   * @param endTime is the end of the time range to consider, exclusive.
   * @param jobMode is an integer describing how the job is being run, whether
   * continuous or once-only.
   */
  @Override
  public void addSeedDocuments(ISeedingActivity activities,
      DocumentSpecification spec, long startTime, long endTime, int jobMode)
      throws ManifoldCFException, ServiceInterruption {

    String jiraDriveQuery = JiraConfig.JIRA_QUERY_DEFAULT;
    int i = 0;
    while (i < spec.getChildCount()) {
      SpecificationNode sn = spec.getChild(i);
      if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
        jiraDriveQuery = sn.getAttributeValue(JOB_QUERY_ATTRIBUTE);
        break;
      }
      i++;
    }

    getSession();
    GetSeedsThread t = new GetSeedsThread(jiraDriveQuery);
    try {
      t.start();
      boolean wasInterrupted = false;
      try {
        XThreadStringBuffer seedBuffer = t.getBuffer();
        // Pick up the paths, and add them to the activities, before we join with the child thread.
        while (true) {
          // The only kind of exceptions this can throw are going to shut the process down.
          String docPath = seedBuffer.fetch();
          if (docPath ==  null)
            break;
          // Add the pageID to the queue
          activities.addSeedDocument(docPath);
        }
      } catch (InterruptedException e) {
        wasInterrupted = true;
        throw e;
      } catch (ManifoldCFException e) {
        if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
          wasInterrupted = true;
        throw e;
      } finally {
        if (!wasInterrupted)
          t.finishUp();
      }
    } catch (InterruptedException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (java.net.SocketTimeoutException e) {
      Logging.connectors.warn("JIRA: Socket timeout adding seed documents: " + e.getMessage(), e);
      handleIOException(e);
    } catch (InterruptedIOException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (IOException e) {
      Logging.connectors.warn("JIRA: Error adding seed documents: " + e.getMessage(), e);
      handleIOException(e);
    }
  }
  
  protected class GetSeedsThread extends Thread {

    protected Throwable exception = null;
    protected final String jiraDriveQuery;
    protected final XThreadStringBuffer seedBuffer;
    
    public GetSeedsThread(String jiraDriveQuery) {
      super();
      this.jiraDriveQuery = jiraDriveQuery;
      this.seedBuffer = new XThreadStringBuffer();
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        session.getSeeds(seedBuffer, jiraDriveQuery);
        seedBuffer.signalDone();
      } catch (Throwable e) {
        this.exception = e;
      }
    }

    public XThreadStringBuffer getBuffer() {
      return seedBuffer;
    }
    
    public void finishUp()
      throws InterruptedException, IOException {
      seedBuffer.abandon();
      join();
      Throwable thr = exception;
      if (thr != null) {
        if (thr instanceof IOException)
          throw (IOException) thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException) thr;
        else if (thr instanceof Error)
          throw (Error) thr;
        else
          throw new RuntimeException("Unhandled exception of type: "+thr.getClass().getName(),thr);
      }
    }
  }

  protected JSONObject getObject(String nodeId)
    throws ManifoldCFException, ServiceInterruption {
    getSession();
    GetObjectThread t = new GetObjectThread(nodeId);
    try {
      t.start();
      t.join();
      Throwable thr = t.getException();
      if (thr != null) {
        if (thr instanceof IOException) {
          throw (IOException) thr;
        } else if (thr instanceof RuntimeException) {
          throw (RuntimeException) thr;
        } else {
          throw (Error) thr;
        }
      }
    } catch (InterruptedException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (java.net.SocketTimeoutException e) {
      Logging.connectors.warn("JIRA: Socket timeout getting object: " + e.getMessage(), e);
      handleIOException(e);
    } catch (InterruptedIOException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (IOException e) {
      Logging.connectors.warn("JIRA: Error getting object: " + e.getMessage(), e);
      handleIOException(e);
    }
    return t.getResponse();
  }
  
  protected class GetObjectThread extends Thread {

    protected final String nodeId;
    protected Throwable exception = null;
    protected JSONObject response = null;

    public GetObjectThread(String nodeId) {
      super();
      setDaemon(true);
      this.nodeId = nodeId;
    }

    public void run() {
      try {
        response = session.getObject(nodeId);
      } catch (Throwable e) {
        this.exception = e;
      }
    }

    public JSONObject getResponse() {
      return response;
    }
    
    public Throwable getException() {
      return exception;
    }
  }
  
  /**
   * Process a set of documents. This is the method that should cause each
   * document to be fetched, processed, and the results either added to the
   * queue of documents for the current job, and/or entered into the
   * incremental ingestion manager. The document specification allows this
   * class to filter what is done based on the job.
   *
   * @param documentIdentifiers is the set of document identifiers to process.
   * @param versions is the corresponding document versions to process, as
   * returned by getDocumentVersions() above. The implementation may choose to
   * ignore this parameter and always process the current version.
   * @param activities is the interface this method should use to queue up new
   * document references and ingest documents.
   * @param spec is the document specification.
   * @param scanOnly is an array corresponding to the document identifiers. It
   * is set to true to indicate when the processing should only find other
   * references, and should not actually call the ingestion methods.
   * @param jobMode is an integer describing how the job is being run, whether
   * continuous or once-only.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void processDocuments(String[] documentIdentifiers, String[] versions,
      IProcessActivity activities, DocumentSpecification spec,
      boolean[] scanOnly) throws ManifoldCFException, ServiceInterruption {

    Logging.connectors.debug("JIRA: Inside processDocuments");

    for (int i = 0; i < documentIdentifiers.length; i++) {
      // MHL for access tokens
      long startTime = System.currentTimeMillis();
      String errorCode = "FAILED";
      String errorDesc = StringUtils.EMPTY;
      Long fileSize = null;
      boolean doLog = false;
      String nodeId = documentIdentifiers[i];
      String version = versions[i];
      
      try {
        if (Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug("JIRA: Processing document identifier '"
              + nodeId + "'");
        }

        JSONObject jiraFile = getObject(nodeId);
        
        if (Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug("JIRA: have this file:\t" + jiraFile.get("key"));
        }

          // its a file
          if (!scanOnly[i]) {
            doLog = true;

            

            // Unpack the version string
            ArrayList acls = new ArrayList();
            StringBuilder denyAclBuffer = new StringBuilder();
            int index = unpackList(acls,version,0,'+');
            if (index < version.length() && version.charAt(index++) == '+') {
              index = unpack(denyAclBuffer,version,index,'+');
            }

            //otherwise process
            RepositoryDocument rd = new RepositoryDocument();
            
            
            // Turn into acls and add into description
            String[] aclArray = new String[acls.size()];
            for (int j = 0; j < aclArray.length; j++) {
              aclArray[j] = (String)acls.get(j);
            }
            rd.setACL(aclArray);
            if (denyAclBuffer.length() > 0) {
              String[] denyAclArray = new String[]{denyAclBuffer.toString()};
              rd.setDenyACL(denyAclArray);
            }

            // Now do standard stuff
            
            String mimeType = "Jira Ticket";
            String createdDate = ((JSONObject)jiraFile.get("fields")).get("created").toString();
            String modifiedDate = ((JSONObject)jiraFile.get("fields")).get("updated").toString();
            String title = ((JSONObject)jiraFile.get("fields")).get("summary").toString();
            
            // Get general document metadata
            HashMap<String,String> metadataMap=new HashMap<String, String>();
            
            metadataMap = JiraSession.addMetaDataToMap("", jiraFile, metadataMap);
            
            for (Entry<String, String> entry : metadataMap.entrySet()) {
              rd.addField(entry.getKey(), entry.getValue());
            }
            String documentURI = jiraFile.get("self").toString();
            // Fire up the document reading thread
            BackgroundStreamThread t = new BackgroundStreamThread(jiraFile);
            try {
              t.start();
              boolean wasInterrupted = false;
              try {
                InputStream is = t.getSafeInputStream();
                try {
                    rd.setBinary(is, session.getJiraBody(jiraFile).length());
                    activities.ingestDocument(nodeId, version, documentURI, rd);
                } finally {
                  is.close();
                }
              } catch (ManifoldCFException e) {
                if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
                  wasInterrupted = true;
                throw e;
              } catch (java.net.SocketTimeoutException e) {
                throw e;
              } catch (InterruptedIOException e) {
                wasInterrupted = true;
                throw e;
              } finally {
                if (!wasInterrupted)
                  t.finishUp();
              }

              // No errors.  Record the fact that we made it.
              errorCode = "OK";
            } catch (InterruptedException e) {
              t.interrupt();
              throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
                ManifoldCFException.INTERRUPTED);
            } catch (java.net.SocketTimeoutException e) {
              Logging.connectors.warn("JIRA: Socket timeout reading document: " + e.getMessage(), e);
              handleIOException(e);
            } catch (InterruptedIOException e) {
              t.interrupt();
              throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
                ManifoldCFException.INTERRUPTED);
            } catch (IOException e) {
              errorCode = "IO ERROR";
              errorDesc = e.getMessage();
              Logging.connectors.warn("JIRA: Error reading document: " + e.getMessage(), e);
              handleIOException(e);
            }
          }
        }
       finally {
        if (doLog)
          activities.recordActivity(new Long(startTime), ACTIVITY_READ,
            fileSize, nodeId, errorCode, errorDesc, null);
      }
    }
  }

protected class BackgroundStreamThread extends Thread
  {
    protected final JSONObject jiraissue;
    
    protected boolean abortThread = false;
    protected Throwable responseException = null;
    protected InputStream sourceStream = null;
    protected XThreadInputStream threadStream = null;
    
    public BackgroundStreamThread(JSONObject jiraissue)
    {
      super();
      setDaemon(true);
      this.jiraissue = jiraissue;
    }

    public void run()
    {
      try {
        try {
          synchronized (this) {
            if (!abortThread) {
              sourceStream = session.getJiraInputStream(jiraissue);
              threadStream = new XThreadInputStream(sourceStream);
              this.notifyAll();
            }
          }
          
          if (threadStream != null)
          {
            // Stuff the content until we are done
            threadStream.stuffQueue();
          }
        } finally {
          if (sourceStream != null)
            sourceStream.close();
        }
      } catch (Throwable e) {
        responseException = e;
      }
    }

    public InputStream getSafeInputStream()
      throws InterruptedException, IOException
    {
      // Must wait until stream is created, or until we note an exception was thrown.
      while (true)
      {
        synchronized (this)
        {
          if (responseException != null)
            throw new IllegalStateException("Check for response before getting stream");
          checkException(responseException);
          if (threadStream != null)
            return threadStream;
          wait();
        }
      }
    }
    
    public void finishUp()
      throws InterruptedException, IOException
    {
      // This will be called during the finally
      // block in the case where all is well (and
      // the stream completed) and in the case where
      // there were exceptions.
      synchronized (this) {
        if (threadStream != null) {
          threadStream.abort();
        }
        abortThread = true;
      }

      join();

      checkException(responseException);
    }
    
    protected synchronized void checkException(Throwable exception)
      throws IOException
    {
      if (exception != null)
      {
        Throwable e = exception;
        if (e instanceof IOException)
          throw (IOException)e;
        else if (e instanceof RuntimeException)
          throw (RuntimeException)e;
        else if (e instanceof Error)
          throw (Error)e;
        else
          throw new RuntimeException("Unhandled exception of type: "+e.getClass().getName(),e);
      }
    }
  }

  /**
   * The short version of getDocumentVersions. Get document versions given an
   * array of document identifiers. This method is called for EVERY document
   * that is considered. It is therefore important to perform as little work
   * as possible here.
   *
   * @param documentIdentifiers is the array of local document identifiers, as
   * understood by this connector.
   * @param spec is the current document specification for the current job. If
   * there is a dependency on this specification, then the version string
   * should include the pertinent data, so that reingestion will occur when
   * the specification changes. This is primarily useful for metadata.
   * @return the corresponding version strings, with null in the places where
   * the document no longer exists. Empty version strings indicate that there
   * is no versioning ability for the corresponding document, and the document
   * will always be processed.
   */
  @Override
  public String[] getDocumentVersions(String[] documentIdentifiers,
      DocumentSpecification spec) throws ManifoldCFException,
      ServiceInterruption {

    // Forced acls
    String[] acls = getAcls(spec);
    // Sort it,
    java.util.Arrays.sort(acls);

    String[] rval = new String[documentIdentifiers.length];
    for (int i = 0; i < rval.length; i++) {
      JSONObject jiraFile = getObject(documentIdentifiers[i]);
        String rev = ((JSONObject)jiraFile.get("fields")).get("updated").toString();
        if (StringUtils.isNotEmpty(rev)) {
          StringBuilder sb = new StringBuilder();

          // Acls
          packList(sb,acls,'+');
          if (acls.length > 0) {
            sb.append('+');
            pack(sb,defaultAuthorityDenyToken,'+');
          }
          else
            sb.append('-');
            sb.append(rev);
          rval[i] = sb.toString();
        } else {
          //a jira document that doesn't contain versioning information will NEVER be processed.
          // I don't know what this means, and whether it can ever occur.
          rval[i] = null;
        }
    }
    return rval;
  }

  /** Grab forced acl out of document specification.
  *@param spec is the document specification.
  *@return the acls.
  */
  protected static String[] getAcls(DocumentSpecification spec) {
    Set<String> map = new HashSet<String>();
    for (int i = 0; i < spec.getChildCount(); i++) {
      SpecificationNode sn = spec.getChild(i);
      if (sn.getType().equals(JOB_ACCESS_NODE_TYPE)) {
        String token = sn.getAttributeValue(JOB_TOKEN_ATTRIBUTE);
        map.add(token);
      }
    }

    String[] rval = new String[map.size()];
    Iterator<String> iter = map.iterator();
    int i = 0;
    while (iter.hasNext()) {
      rval[i++] = (String)iter.next();
    }
    return rval;
  }

  private static void handleIOException(IOException e)
    throws ManifoldCFException, ServiceInterruption {
    if (!(e instanceof java.net.SocketTimeoutException) && (e instanceof InterruptedIOException)) {
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    }
    long currentTime = System.currentTimeMillis();
    throw new ServiceInterruption("IO exception: "+e.getMessage(), e, currentTime + 300000L,
      currentTime + 3 * 60 * 60000L,-1,false);
  }
  
}


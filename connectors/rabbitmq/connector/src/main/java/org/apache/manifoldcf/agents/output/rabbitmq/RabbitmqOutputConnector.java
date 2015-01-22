/* $Id: RabbitmqOutputConnector.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package org.apache.manifoldcf.agents.output.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.AMQP;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.agents.output.BaseOutputConnector;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ConfigurationNode;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.interfaces.VersionContext;
import org.json.JSONException;

import java.util.*;
import java.io.*;

/** This is a RabbitMQ output connector.  It eats all output and simply logs the events.
 */
public class RabbitmqOutputConnector extends BaseOutputConnector {
    public static final String _rcsid = "@(#)$Id: RabbitmqOutputConnector.java 988245 2010-08-23 18:39:35Z kwright $";

    RabbitmqConfig rabbitconfig;

    // Activities we log

    /** Ingestion activity */
    public final static String INGEST_ACTIVITY = "document ingest";
    /** Document removal activity */
    public final static String REMOVE_ACTIVITY = "document deletion";
    /** Job notify activity */
    public final static String JOB_COMPLETE_ACTIVITY = "output notification";
    //Expiration interval:
    //protected final static long EXPIRATION_INTERVAL = 100000L;


    private ConnectionFactory factory = new ConnectionFactory();
    private Connection connection = null;
    private Channel channel = null;
    //private long expirationTime = -1L;

    /** Constructor.
     */
    public RabbitmqOutputConnector()
    {
    }

    /** Return the list of activities that this connector supports (i.e. writes into the log).
     *@return the list.
     */
    @Override
    public String[] getActivitiesList()
    {
        return new String[]{INGEST_ACTIVITY,REMOVE_ACTIVITY,JOB_COMPLETE_ACTIVITY};
    }

    /** Connect.
     *@param configParameters is the set of configuration parameters, which
     * in this case describe the target appliance, basic auth configuration, etc.  (This formerly came
     * out of the ini file.)
     */
    @Override
    public void connect(ConfigParams configParameters)
    {
        super.connect(configParameters);
    }

    /** Close the connection.  Call this before discarding the connection.
     */
    @Override
    public void disconnect()
            throws ManifoldCFException
    {
        try {
            channel.close();
            connection.close();
        }
        catch (IOException e){
            Logging.ingest.warn("Disconnection error: " + e.getMessage(), e);
            throw new ManifoldCFException("Disconnection error: " + e.getMessage(), e);
        }
        super.disconnect();
    }

    /** Set up a session */
    protected Channel getSession()
            throws ManifoldCFException, ServiceInterruption
    {
        if (channel == null){
            try{
                //First check if rabbitconfig is empty, if it is, we fill it with the configuration parameters:
                if (rabbitconfig == null) {
                    ConfigParams configParams = getConfiguration();
                    this.rabbitconfig = new RabbitmqConfig(configParams);
                }
                //Create a channel:
                factory.setHost(rabbitconfig.getHost());
                connection = factory.newConnection();
                channel = connection.createChannel();
            }
            catch (IOException e){
                Logging.ingest.warn("Session set up error: "+e.getMessage(), e);
                throw new ManifoldCFException("Session set up error: " + e.getMessage(), e);
            }
        }
        //Reset time:
        //expirationTime = System.currentTimeMillis() + EXPIRATION_INTERVAL;
        return channel;
    }

    /** Test the connection.  Returns a string describing the connection integrity.
     *@return the connection's status as a displayable string.
     */
    @Override
    public String check()
            throws ManifoldCFException
    {
        try
        {
            Channel auxchan = getSession();
            auxchan.queueDeclare("OK_QUEUE",false,true,true,null);
            String message = "OK";
            auxchan.basicPublish("", "OK_QUEUE", null, message.getBytes());
            return super.check();
        }
        catch (IOException e){
            Logging.ingest.warn("Error checking channel: "+e.getMessage(),e);
            return "Error: "+e.getMessage();
        }
        catch (ServiceInterruption e)
        {
            return "Transient error: "+e.getMessage();
        }
    }

    /** Get an output version string, given an output specification.  The output version string is used to uniquely describe the pertinent details of
     * the output specification and the configuration, to allow the Connector Framework to determine whether a document will need to be output again.
     * Note that the contents of the document cannot be considered by this method, and that a different version string (defined in IRepositoryConnector)
     * is used to describe the version of the actual document.
     *
     * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be
     * necessary.
     *@param spec is the current output specification for the job that is doing the crawling.
     *@return a string, of unlimited length, which uniquely describes output configuration and specification in such a way that if two such strings are equal,
     * the document will not need to be sent again to the output data store.
     */
    @Override
    public VersionContext getPipelineDescription(Specification spec)
            throws ManifoldCFException, ServiceInterruption
    {
        return new VersionContext("",params,spec);
    }

//----------------------------------------------------------------------------------------------------------------------

    @Override
    public void outputConfigurationHeader(IThreadContext threadContext,
                                          IHTTPOutput out, Locale locale, ConfigParams parameters,
                                          List<String> tabsArray) throws ManifoldCFException, IOException
    {
        super.outputConfigurationHeader(threadContext, out, locale, parameters,
                tabsArray);
        tabsArray.add("Parameters");
        Messages.outputResourceWithVelocity(out, locale, "Header.html", null);
    }

    @Override
    public void outputConfigurationBody(IThreadContext threadContext,
                                        IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
            throws ManifoldCFException, IOException
    {
        super.outputConfigurationBody(threadContext, out, locale, parameters,
                tabName);
        Map<String, String> velocityContext = new HashMap<String, String>();
        RabbitmqConfig config = new RabbitmqConfig(parameters);
        configToContext(velocityContext, config);
        velocityContext.put("TabName", tabName);

        Messages.outputResourceWithVelocity(out, locale, "ConfigurationBody.html", velocityContext, false);
    }

    @Override
    public String processConfigurationPost(IThreadContext threadContext,
                                           IPostParameters variableContext, ConfigParams parameters)
        throws ManifoldCFException{
        RabbitmqConfig.contextToConfig(variableContext, parameters);
        return null;
    }

    // Fills a hashmap with configuration information.
    private static void configToContext (Map<String, String> velocityContext, RabbitmqConfig rabbitmqConfig){
        String queuename = rabbitmqConfig.getQueueName();
        velocityContext.put("queue", queuename);
        String host = rabbitmqConfig.getHost();
        velocityContext.put("host", host);
        Boolean durable = rabbitmqConfig.isDurable();
        velocityContext.put("durable", durable.toString());
        Boolean exclusive = rabbitmqConfig.isExclusive();
        velocityContext.put("exclusive", exclusive.toString());
        Boolean autodelete = rabbitmqConfig.isAutoDelete();
        velocityContext.put("autodelete", autodelete.toString());
        Boolean usetransactions = rabbitmqConfig.isUseTransactions();
        velocityContext.put("usetransactions", usetransactions.toString());
        //Terminar función, llamarla en configurationBody y viewConfiguration. Quizás también en configurationHeader, ver esto.

    }

    @Override
    public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
                                  Locale locale, ConfigParams parameters) throws ManifoldCFException,
            IOException{
        Map<String, String> velocityContext = new HashMap<String, String>();
        RabbitmqConfig config = new RabbitmqConfig(parameters);
        configToContext(velocityContext, config);
        Messages.outputResourceWithVelocity(out, locale, "ViewConfig.html", velocityContext, false);
    }
//----------------------------------------------------------------------------------------------------------------------

    /** Add (or replace) a document in the output data store using the connector.
     * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be
     * necessary.
     * The OutputSpecification is *not* provided to this method, because the goal is consistency, and if output is done it must be consistent with the
     * output description, since that was what was partly used to determine if output should be taking place.  So it may be necessary for this method to decode
     * an output description string in order to determine what should be done.
     *@param documentURI is the URI of the document.  The URI is presumed to be the unique identifier which the output data store will use to process
     * and serve the document.  This URI is constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
     *@param outputDescription is the description string that was constructed for this document by the getOutputDescription() method.
     *@param document is the document data to be processed (handed to the output data store).
     *@param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in with the repository document.  May be null.
     *@param activities is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
     *@return the document status (accepted or permanently rejected).
     */
    @Override
    public int addOrReplaceDocumentWithException(String documentURI, VersionContext outputDescription, RepositoryDocument document, String authorityNameString, IOutputAddActivity activities)
            throws ManifoldCFException, ServiceInterruption, IOException
    {
        String QUEUE_NAME = rabbitconfig.getQueueName();
        String result = "OK";
        long startTime = System.currentTimeMillis();

        // Check ACL's.


        // Fill an OutboundDocument instance.
        OutboundDocument outboundDocument = new OutboundDocument(document);
        outboundDocument.setDocumentURI(documentURI);
        Writer writer = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {

            }

            @Override
            public void flush() throws IOException {

            }

            @Override
            public void close() throws IOException {

            }
        };
        try {
            String outString = outboundDocument.writeTo(writer);

            // Establish a session:
            Channel channel = getSession();

            //Declare a queue for publish:
            Boolean durable = rabbitconfig.isDurable();
            channel.queueDeclare(QUEUE_NAME, durable, true, true, null);

            //Publish the content of the document:
            channel.basicPublish("",QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, outString.getBytes());

            //For debugging purposes:
            System.out.println("Sent:   " + outString);

            result= "Document accepted.";
        }
        catch(JSONException e){
            Logging.ingest.warn("JSON exception: "+e.getMessage(), e);
            result = "Document rejected.";
            return DOCUMENTSTATUS_REJECTED;
        }
        finally {
            activities.recordActivity(startTime, INGEST_ACTIVITY, new Long(document.getBinaryLength()), documentURI, result, null);
            return DOCUMENTSTATUS_ACCEPTED;
        }
    }

    /** Remove a document using the connector.
     * Note that the last outputDescription is included, since it may be necessary for the connector to use such information to know how to properly remove the document.
     *@param documentURI is the URI of the document.  The URI is presumed to be the unique identifier which the output data store will use to process
     * and serve the document.  This URI is constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
     *@param outputDescription is the last description string that was constructed for this document by the getOutputDescription() method above.
     *@param activities is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
     */
    @Override
    public void removeDocument(String documentURI, String outputDescription, IOutputRemoveActivity activities)
            throws ManifoldCFException, ServiceInterruption
    {
        // Establish a session
        Channel channel = getSession();
        activities.recordActivity(null,REMOVE_ACTIVITY,null,documentURI,"OK",null);
    }

    /** Notify the connector of a completed job.
     * This is meant to allow the connector to flush any internal data structures it has been keeping around, or to tell the output repository that this
     * is a good time to synchronize things.  It is called whenever a job is either completed or aborted.
     *@param activities is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
     */
    @Override
    public void noteJobComplete(IOutputNotifyActivity activities)
            throws ManifoldCFException, ServiceInterruption
    {
        activities.recordActivity(null,JOB_COMPLETE_ACTIVITY,null,"","OK",null);
    }

}

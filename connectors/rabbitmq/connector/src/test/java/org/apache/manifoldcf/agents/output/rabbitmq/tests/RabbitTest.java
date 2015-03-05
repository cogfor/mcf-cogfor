package org.apache.manifoldcf.agents.output.rabbitmq.tests;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.junit.*;

import org.apache.manifoldcf.agents.output.rabbitmq.RabbitmqConfig;

import java.io.IOException;

/**
 * Created by mariana on 2/6/15.
 */
public class RabbitTest extends BaseITHSQLDB {
    protected ConnectionFactory connectionFactory= new ConnectionFactory();
    protected Connection connection;
    protected Channel channel;

    public RabbitTest(){
        try {
            this.connection = connectionFactory.newConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setMockRabbitConnector(){
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        try {
            this.channel = connection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRabbitCrawler() throws Exception
    {
        IThreadContext threadContext = ThreadContextFactory.make();

        // Create a basic file system connection, and save it.
        IRepositoryConnectionManager mgr = RepositoryConnectionManagerFactory.make(threadContext);
        IRepositoryConnection conn = mgr.create();
        conn.setName("Test Connection");
        conn.setDescription("Test Connection");
        conn.setClassName("org.apache.manifoldcf.crawler.tests.TestingRepositoryConnector");
        conn.setMaxConnections(100);
        mgr.save(conn);

        // Create a basic null output connection, and save it.
        IOutputConnectionManager outputMgr = OutputConnectionManagerFactory.make(threadContext);
        IOutputConnection outputConn = outputMgr.create();
        outputConn.setName("Rabbitmq Connection");
        outputConn.setDescription("Rabbitmq Connection");
        outputConn.setClassName("org.apache.manifoldcf.agents.output.rabbitmq.RabbitmqOutputConnector");
        outputConn.setMaxConnections(10);

        // Set the connection parameters
        ConfigParams configParams = outputConn.getConfigParams();
        RabbitmqConfig rabbitmqConfig = new RabbitmqConfig(configParams);
        configParams.setParameter("host", "localhost");
        configParams.setParameter("queue", "Queue_3");
        configParams.setParameter("port", "5672");
        configParams.setParameter("durable", "false");
        configParams.setParameter("autodelete", "true");
        configParams.setParameter("exclusive", "true");
        configParams.setParameter("transaction", "false");
        outputMgr.save(outputConn);

        // Create a job.
        IJobManager jobManager = JobManagerFactory.make(threadContext);
        IJobDescription job = jobManager.createJob();
        job.setDescription("Test Job");
        job.setConnectionName("Test Connection");
        job.addPipelineStage(-1,true,"Rabbitmq Connection","");
        job.setType(job.TYPE_SPECIFIED);
        job.setStartMethod(job.START_DISABLE);
        job.setHopcountMode(job.HOPCOUNT_NEVERDELETE);
        jobManager.save(job);

        try {
            //Start job.
            long startTime = System.currentTimeMillis();
            jobManager.manualStart(job.getID());
            mcfInstance.waitJobInactiveNative(jobManager, job.getID(), 1000000L);
            System.err.println("Crawl activity required " + new Long(System.currentTimeMillis() - startTime).toString() + " milliseconds");

            //Check number of documents sent.
            JobStatus status = jobManager.getStatus(job.getID());
            if (status.getDocumentsProcessed() != 3)
               throw new ManifoldCFException("Wrong number of documents. Expected 3 but saw " + new Long(status.getDocumentsProcessed()).toString());
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    @After
    public void cleanMockRabbitConnector(){
        try {
            this.channel.close();
            this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

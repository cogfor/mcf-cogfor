package org.apache.manifoldcf.agents.output.rabbitmq.tests;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.junit.*;

import org.apache.manifoldcf.agents.output.rabbitmq.RabbitmqConfig;

/**
 * Created by mariana on 2/6/15.
 */
public class RabbitTest extends BaseITHSQLDB {

    @Before
    public void setRabbitConnector(){

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

        //Start job.

        //Check number of documents sent.


    }

    @After
    public void cleanRabbitConnector(){

    }
}

package org.apache.manifoldcf.agents.output.rabbitmq.tests;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.apache.manifoldcf.crawler.system.ManifoldCF;
import org.junit.*;

import org.apache.manifoldcf.agents.output.rabbitmq.RabbitmqConfig;

import java.io.IOException;

/**
 * Created by mariana on 2/6/15.
 */
public class RabbitTest extends BaseITHSQLDB{

    //protected org.apache.manifoldcf.crawler.tests.ManifoldCFInstance instance;

    //public RabbitTest(org.apache.manifoldcf.crawler.tests.ManifoldCFInstance instance)
    //{
      //  this.instance = instance;
    //}
    public RabbitTest(){}

    @Test
    public void executeTest() throws Exception
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
        configParams.setParameter("username", "guest");
        configParams.setParameter("password", "guest");
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

        Specification ds = job.getSpecification();
        SpecificationNode sn = new SpecificationNode("documentcount");
        sn.setAttribute("count","111");
        ds.addChild(ds.getChildCount(),sn);
        jobManager.save(job);

        try
        {
            // Now, start the job, and wait until it completes.
            long startTime = System.currentTimeMillis();
            jobManager.manualStart(job.getID());
            mcfInstance.waitJobInactiveNative(jobManager, job.getID(), 300000L);
            System.err.println("Crawl required "+new Long(System.currentTimeMillis()-startTime).toString()+" milliseconds");
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
        }

        JobStatus status = jobManager.getStatus(job.getID());
        if (status.getDocumentsProcessed() != 111)
            new ManifoldCFException("Wrong number of documents processed - expected 111, saw "+new Long(status.getDocumentsProcessed()).toString());

        jobManager.deleteJob(job.getID());
        mcfInstance.waitJobDeletedNative(jobManager, job.getID(), 300000L);

    }

}

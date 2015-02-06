package org.apache.manifoldcf.agents.output.rabbitmq.tests;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.junit.*;

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
    }

    @After
    public void cleanRabbitConnector(){
        
    }
}

/**
 * Created by mariana on 2/4/15.
 */
package org.apache.manifoldcf.agents.output.rabbitmq.tests;

public class BaseUIHSQLDB extends org.apache.manifoldcf.crawler.tests.ConnectorBaseUIHSQLDB
{
    protected String[] getConnectorNames()
    {
        return new String[]{"Test Connector"};
    }

    protected String[] getConnectorClasses()
    {
        return new String[]{"org.apache.manifoldcf.crawler.tests.TestingRepositoryConnector"};
    }

    protected String[] getOutputNames()
    {
        return new String[]{"Rabbitmq"};
    }

    protected String[] getOutputClasses()
    {
        return new String[]{"org.apache.manifoldcf.agents.output.rabbitmq.RabbitmqOutputConnector"};
    }

}

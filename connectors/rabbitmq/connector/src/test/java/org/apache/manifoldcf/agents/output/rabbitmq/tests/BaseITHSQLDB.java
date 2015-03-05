package org.apache.manifoldcf.agents.output.rabbitmq.tests;

/**
 * Created by mariana on 2/6/15.
 */
public class BaseITHSQLDB extends org.apache.manifoldcf.crawler.tests.BaseITHSQLDB{
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

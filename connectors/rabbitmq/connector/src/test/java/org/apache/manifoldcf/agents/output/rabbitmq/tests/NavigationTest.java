package org.apache.manifoldcf.agents.output.rabbitmq.tests;

import org.apache.manifoldcf.core.tests.HTMLTester;
import org.junit.Test;

import java.util.Locale;

/**
 * Created by mariana on 2/4/15.
 */
public class NavigationTest extends BaseUIHSQLDB {
    @Test
    public void createConnectionsAndJob()
            throws Exception
    {
        testerInstance.newTest(Locale.US);

        HTMLTester.Window window;
        HTMLTester.Link link;
        HTMLTester.Form form;
        HTMLTester.Textarea textarea;
        HTMLTester.Selectbox selectbox;
        HTMLTester.Button button;
        HTMLTester.Radiobutton radiobutton;
        HTMLTester.Loop loop;

        window = testerInstance.openMainWindow("http://localhost:8346/mcf-crawler-ui/index.jsp");

        // Login
        form = window.findForm(testerInstance.createStringDescription("loginform"));
        textarea = form.findTextarea(testerInstance.createStringDescription("userID"));
        textarea.setValue(testerInstance.createStringDescription("admin"));
        textarea = form.findTextarea(testerInstance.createStringDescription("password"));
        textarea.setValue(testerInstance.createStringDescription("admin"));
        button = window.findButton(testerInstance.createStringDescription("Login"));
        button.click();
        window = testerInstance.findWindow(null);

        // Define an output connection via the UI
        link = window.findLink(testerInstance.createStringDescription("List output connections"));
        link.click();
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Add an output connection"));
        link.click();
        // Fill in a name
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editconnection"));
        textarea = form.findTextarea(testerInstance.createStringDescription("connname"));
        textarea.setValue(testerInstance.createStringDescription("MyOutputConnection"));
        link = window.findLink(testerInstance.createStringDescription("Type tab"));
        link.click();
        // Select a type
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editconnection"));
        selectbox = form.findSelectbox(testerInstance.createStringDescription("classname"));
        selectbox.selectValue(testerInstance.createStringDescription("org.apache.manifoldcf.agents.output.rabbitmq.RabbitmqOutputConnector"));
        button = window.findButton(testerInstance.createStringDescription("Continue to next page"));
        button.click();
        // Visit the Throttling tab
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Throttling tab"));
        link.click();
        // Go back to the Name tab
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Name tab"));
        link.click();
        // Parameters tab
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Parameters tab"));
        link.click();
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editconnection"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Queue name:"));
        textarea.setValue(testerInstance.createStringDescription("Queue_3"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Host:"));
        textarea.setValue(testerInstance.createStringDescription("localhost"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Port:"));
        textarea.setValue(testerInstance.createStringDescription("5672"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Is queue durable:"));
        textarea.setValue(testerInstance.createStringDescription("false"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Exclusive parameter:"));
        textarea.setValue(testerInstance.createStringDescription("false"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Autodelete parameter:"));
        textarea.setValue(testerInstance.createStringDescription("true"));
        textarea = form.findTextarea(testerInstance.createStringDescription("Use Transactions:"));
        textarea.setValue(testerInstance.createStringDescription("false"));

        // Go back to the Name tab
        link = window.findLink(testerInstance.createStringDescription("Name tab"));
        link.click();
        // Now save the connection.
        window = testerInstance.findWindow(null);
        button = window.findButton(testerInstance.createStringDescription("Save this output connection"));
        button.click();

        // Define a repository connection via the UI
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("List repository connections"));
        link.click();
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Add a connection"));
        link.click();
        // Fill in a name
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editconnection"));
        textarea = form.findTextarea(testerInstance.createStringDescription("connname"));
        textarea.setValue(testerInstance.createStringDescription("MyRepositoryConnection"));
        link = window.findLink(testerInstance.createStringDescription("Type tab"));
        link.click();
        // Select a type
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editconnection"));
        selectbox = form.findSelectbox(testerInstance.createStringDescription("classname"));
        selectbox.selectValue(testerInstance.createStringDescription("org.apache.manifoldcf.crawler.tests.TestingRepositoryConnector"));
        button = window.findButton(testerInstance.createStringDescription("Continue to next page"));
        button.click();
        // Visit the Throttling tab
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Throttling tab"));
        link.click();
        window = testerInstance.findWindow(null);
        // Now save the connection.
        button = window.findButton(testerInstance.createStringDescription("Save this connection"));
        button.click();

        // Create a job
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("List jobs"));
        link.click();
        // Add a job
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Add a job"));
        link.click();
        // Fill in a name
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editjob"));
        textarea = form.findTextarea(testerInstance.createStringDescription("description"));
        textarea.setValue(testerInstance.createStringDescription("MyJob"));
        link = window.findLink(testerInstance.createStringDescription("Connection tab"));
        link.click();
        // Select the connections
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editjob"));
        selectbox = form.findSelectbox(testerInstance.createStringDescription("output_connectionname"));
        selectbox.selectValue(testerInstance.createStringDescription("MyOutputConnection"));
        selectbox = form.findSelectbox(testerInstance.createStringDescription("output_precedent"));
        selectbox.selectValue(testerInstance.createStringDescription("-1"));
        button = window.findButton(testerInstance.createStringDescription("Add an output"));
        button.click();
        window = testerInstance.findWindow(null);
        form = window.findForm(testerInstance.createStringDescription("editjob"));
        selectbox = form.findSelectbox(testerInstance.createStringDescription("connectionname"));
        selectbox.selectValue(testerInstance.createStringDescription("MyRepositoryConnection"));
        button = window.findButton(testerInstance.createStringDescription("Continue to next screen"));
        button.click();

        window = testerInstance.findWindow(null);
        // MHL
        // Save the job
        button = window.findButton(testerInstance.createStringDescription("Save this job"));
        button.click();

        // Delete the job
        window = testerInstance.findWindow(null);
        HTMLTester.StringDescription jobID = window.findMatch(testerInstance.createStringDescription("<!--jobid=(.*?)-->"),0);
        testerInstance.printValue(jobID);
        link = window.findLink(testerInstance.createStringDescription("Delete this job"));
        link.click();

        // Wait for the job to go away
        loop = testerInstance.beginLoop(120);
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Manage jobs"));
        link.click();
        window = testerInstance.findWindow(null);
        HTMLTester.StringDescription isJobNotPresent = window.isNotPresent(jobID);
        testerInstance.printValue(isJobNotPresent);
        loop.breakWhenTrue(isJobNotPresent);
        loop.endLoop();

        // Delete the repository connection
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("List repository connections"));
        link.click();
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Delete MyRepositoryConnection"));
        link.click();

        // Delete the output connection
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("List output connections"));
        link.click();
        window = testerInstance.findWindow(null);
        link = window.findLink(testerInstance.createStringDescription("Delete MyOutputConnection"));
        link.click();

        testerInstance.executeTest();
    }
}

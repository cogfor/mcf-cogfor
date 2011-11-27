/* $Id$ */

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
package org.apache.manifoldcf.core.tests;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.core.system.ManifoldCF;

import java.io.*;
import java.util.*;
import org.junit.*;
import java.lang.reflect.*;

/** This is a testing base class that is responsible for setting up/tearing down the core HSQLDB remote database. */
public class BaseHSQLDBext
{
  protected File currentPath = null;
  protected File configFile = null;
  protected File loggingFile = null;
  protected File logOutputFile = null;

  protected DatabaseThread databaseThread = null;
  
  protected void initialize()
    throws Exception
  {
    if (currentPath == null)
    {
      // Start "remote" hsqldb instance
      startDatabase();
      
      currentPath = new File(".").getCanonicalFile();

      // First, write a properties file and a logging file, in the current directory.
      configFile = new File("properties.xml").getCanonicalFile();
      loggingFile = new File("logging.ini").getCanonicalFile();
      logOutputFile = new File("manifoldcf.log").getCanonicalFile();

      // Set a system property that will point us to the proper place to find the properties file
      System.setProperty("org.apache.manifoldcf.configfile",configFile.getCanonicalFile().getAbsolutePath());
    }
  }
  
  protected boolean isInitialized()
  {
    return configFile.exists();
  }
  
  @Before
  public void setUp()
    throws Exception
  {
    try
    {
      localCleanUp();
    }
    catch (Exception e)
    {
      System.out.println("Warning: Preclean error: "+e.getMessage());
    }
    try
    {
      localSetUp();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw e;
    }
  }


  protected void localSetUp()
    throws Exception
  {
    // Initialize
    initialize();
    String currentPathString = currentPath.getAbsolutePath();
    writeFile(loggingFile,
      "log4j.appender.MAIN.File="+logOutputFile.getAbsolutePath().replaceAll("\\\\","/")+"\n" +
      "log4j.rootLogger=WARN, MAIN\n" +
      "log4j.appender.MAIN=org.apache.log4j.RollingFileAppender\n" +
      "log4j.appender.MAIN.layout=org.apache.log4j.PatternLayout\n" +
      "log4j.appender.MAIN.layout.ConversionPattern=%5p %d{ISO8601} (%t) - %m%n\n"
    );

    writeFile(configFile,
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<configuration>\n"+
      "  <property name=\"org.apache.manifoldcf.databaseimplementationclass\" value=\"org.apache.manifoldcf.core.database.DBInterfaceHSQLDB\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.hsqldbdatabaseprotocol\" value=\"hsql\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.hsqldbdatabaseserver\" value=\"localhost\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.hsqldbdatabaseinstance\" value=\"xdb\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.database.maxquerytime\" value=\"30\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.crawler.threads\" value=\"30\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.crawler.expirethreads\" value=\"10\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.crawler.cleanupthreads\" value=\"10\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.crawler.deletethreads\" value=\"10\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.database.maxhandles\" value=\"80\"/>\n" +
      "  <property name=\"org.apache.manifoldcf.logconfigfile\" value=\""+loggingFile.getAbsolutePath().replaceAll("\\\\","/")+"\"/>\n" +
      "</configuration>\n");

    ManifoldCF.initializeEnvironment();
    IThreadContext tc = ThreadContextFactory.make();
    
    // Create the database
    ManifoldCF.createSystemDatabase(tc,"sa","");

  }
  
  @After
  public void cleanUp()
    throws Exception
  {
    try
    {
      localCleanUp();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw e;
    }
  }

  protected void localCleanUp()
    throws Exception
  {
    initialize();
    if (isInitialized())
    {
      ManifoldCF.initializeEnvironment();
      IThreadContext tc = ThreadContextFactory.make();
      
      // Remove the database
      ManifoldCF.dropSystemDatabase(tc,"sa","");
      
      // Get rid of the property and logging files.
      logOutputFile.delete();
      configFile.delete();
      loggingFile.delete();
      
      ManifoldCF.resetEnvironment();
      stopDatabase();
    }
  }

  protected void startDatabase()
    throws Exception
  {
    databaseThread = new DatabaseThread();
    databaseThread.start();
  }
  
  protected void stopDatabase()
    throws Exception
  {
    while (true)
    {
      if (!databaseThread.isAlive())
        break;
      databaseThread.interrupt();
      Thread.yield();
    }
    databaseThread.join();
  }
  
  protected static void writeFile(File f, String fileContents)
    throws IOException
  {
    OutputStream os = new FileOutputStream(f);
    try
    {
      Writer w = new OutputStreamWriter(os,"utf-8");
      try
      {
        w.write(fileContents);
      }
      finally
      {
        w.close();
      }
    }
    finally
    {
      os.close();
    }
  }
  
  protected static class DatabaseThread extends Thread
  {
    public DatabaseThread()
    {
      setName("Database runner thread");
    }
    
    public void run()
    {
      // We need to do the equivalent of:
      // java -cp ../lib/hsqldb.jar org.hsqldb.Server -database.0 file:mydb -dbname.0 xdb
      try
      {
        Class x = Class.forName("org.hsqldb.Server");
        String[] args = new String[]{"-database.0","file:extdb;hsqldb.tx=mvcc;hsqldb.cache_file_scale=512","-dbname.0","xdb"};
        Method m = x.getMethod("main",String[].class);
        m.invoke(null,(Object)args);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    
  }
  
}
/* $Id: RabbitmqConfig.java 988245 2010-08-23 18:39:35Z kwright $ */

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

import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.crawler.system.Logging;


public class RabbitmqConfig extends RabbitParameters{

/*
    public static final String hostParameter = "host";
    public static final String queueParameter = "queue";
    public static final String durableParameter = "durable";
    public static final String autoDeleteParameter = "autodelete";
    public static final String exclusiveParameter = "exclusive";
    public static final String transactionParameter = "transaction";

    private String queueName = "manifoldcf";
    private String host = "localhost";

    private String defaultPort = "5672";

    private String defaultDurable = "true";
    private String defaultExclusive = "false";
    private String defaultAutoDelete = "false";
    private String defaultUseTransactions = "false";
*/
    private static Parameters[] PARAMETERSLIST = {Parameters.username, Parameters.password, Parameters.host, Parameters.queue, Parameters.durable, Parameters.autodelete,
                                                    Parameters.port, Parameters.exclusive, Parameters.transaction};

    public RabbitmqConfig(ConfigParams configParams) {
        super(Parameters.class);
        for (Parameters parameters : PARAMETERSLIST){
            String p = configParams.getParameter(parameters.name());
            put(parameters, p);
        }
    }

    public final static void contextToConfig(IPostParameters variableContext, ConfigParams parameters) {
        for (Parameters parameter : PARAMETERSLIST){
            String p = variableContext.getParameter(parameter.name());
            if (p != null){
                parameters.setParameter(parameter.name(), p);
            }
        }
    }



    public String getQueueName() {
        return get(Parameters.queue);
    }

    public String getHost() {
        return get(Parameters.host);
    }

    public Integer getPort() {
        String portParam = get(Parameters.port);
        if(portParam == null){
            portParam = "5672";
        }
        return Integer.parseInt(portParam);
    }

    public String getUserName() {return get(Parameters.username);}

    protected String getPassword() {return get(Parameters.password);}

    public boolean isDurable() {
        return Boolean.parseBoolean(get(Parameters.durable));
    }

    public boolean isExclusive() {
        return Boolean.parseBoolean(get(Parameters.exclusive));
    }

    public boolean isAutoDelete() {
        return Boolean.parseBoolean(get(Parameters.autodelete));
    }

    public boolean isUseTransactions() {
        return Boolean.parseBoolean(get(Parameters.transaction));
    }
}
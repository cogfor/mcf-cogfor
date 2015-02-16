package org.apache.manifoldcf.agents.output.rabbitmq;

/**
 * Created by mariana on 2/2/15.
 */
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

class RabbitParameters extends EnumMap <RabbitParameters.Parameters, String> {

    public RabbitParameters(Class<Parameters> keyType) {
        super(keyType);
        this.put(Parameters.username, "username");
        this.put(Parameters.password, "password");
        this.put(Parameters.host, "host");
        this.put(Parameters.queue, "queue");
        this.put(Parameters.port, "5672");
        this.put(Parameters.durable, "true");
        this.put(Parameters.autodelete, "false");
        this.put(Parameters.exclusive, "true");
        this.put(Parameters.transaction, "false");
    }

    public enum Parameters  {
        username, password, host, queue, port, durable, autodelete, exclusive, transaction
    }

}

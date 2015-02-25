package org.apache.manifoldcf.agents.output.rabbitmq;

/**
 * Created by mariana on 2/2/15.
 */
import java.util.EnumMap;

class RabbitParameters extends EnumMap <RabbitParameters.Parameters, String> {

    public RabbitParameters(Class<Parameters> keyType) {
        super(keyType);
        this.put(Parameters.username, "username");
        this.put(Parameters.password, "password");
        this.put(Parameters.host, "host");
        this.put(Parameters.queue, "queue");
        this.put(Parameters.port, "5672");
        this.put(Parameters.durable, "");
        this.put(Parameters.autodelete, "");
        this.put(Parameters.exclusive, "");
        this.put(Parameters.transaction, "");
    }

    public enum Parameters  {
        username, password, host, queue, port, durable, autodelete, exclusive, transaction
    }

}

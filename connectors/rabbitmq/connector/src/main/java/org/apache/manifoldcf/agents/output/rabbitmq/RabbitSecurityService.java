package org.apache.manifoldcf.agents.output.rabbitmq;

import org.apache.http.HttpEntity;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

/**
 * Created by mariana on 2/24/15.
 */
public class RabbitSecurityService {

    private class IndexRequestEntity extends HttpEntity{

    }

    protected static String[] convertACL(String[] acl,
                                         String authorityNameString, IOutputAddActivity activities)
            throws ManifoldCFException
    {
        if (acl != null)
        {
            String[] rval = new String[acl.length];
            int i = 0;
            while (i < rval.length)
            {
                rval[i] = activities.qualifyAccessToken(authorityNameString, acl[i]);
                i++;
            }
            return rval;
        }
        return new String[0];
    }
}

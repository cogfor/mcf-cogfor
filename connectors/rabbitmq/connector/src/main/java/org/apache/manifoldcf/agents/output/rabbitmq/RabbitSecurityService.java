package org.apache.manifoldcf.agents.output.rabbitmq;

import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mariana on 2/24/15.
 */
public class RabbitSecurityService {

    private Map<String, String[]> mapedAcls;
    private Map<String, String[]> mapedDenyAcls;
    private String RESULT = "OK";

    protected String getResult (){return RESULT;}

    protected void mapAcls (String securityType, String[] acls, String[] denyAcls){
        this.mapedAcls = new HashMap<String, String[]>();
        this.mapedDenyAcls = new HashMap<String, String[]>();
        this.mapedAcls.put(securityType, acls);
        this.mapedDenyAcls.put(securityType, denyAcls);
    }

    protected Map<String, String[]> getMapedAcls (){return mapedAcls;}
    protected Map<String, String[]> getMapedDenyAcls () {return mapedDenyAcls;}

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

    public RabbitSecurityService(RepositoryDocument repositoryDocument, String authorityNameString, IOutputAddActivity activities, String documentURI) throws ManifoldCFException{
        Iterator<String> a = repositoryDocument.securityTypesIterator();
        if (a != null) {
            while (a.hasNext()) {
                String securityType = a.next();
                String[] convertedAcls = convertACL(
                        repositoryDocument.getSecurityACL(securityType), authorityNameString,
                        activities);
                String[] convertedDenyAcls = convertACL(
                        repositoryDocument.getSecurityDenyACL(securityType), authorityNameString,
                        activities);
                mapAcls(securityType, convertedAcls, convertedDenyAcls);
                if (!securityType.equals(RepositoryDocument.SECURITY_TYPE_DOCUMENT) &&
                        !securityType.equals(RepositoryDocument.SECURITY_TYPE_PARENT) &&
                        !securityType.equals(RepositoryDocument.SECURITY_TYPE_SHARE)){
                    Logging.ingest.warn("Unhandled security type" + securityType);
                    this.RESULT = "Document rejected due to unhandled security type.";
                    activities.recordActivity(null, activities.UNKNOWN_SECURITY, new Long(repositoryDocument.getBinaryLength()), documentURI, RESULT, null);
                }
            }
        }

    }
}

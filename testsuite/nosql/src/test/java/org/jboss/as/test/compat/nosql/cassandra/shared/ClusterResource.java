package org.jboss.as.test.compat.nosql.cassandra.shared;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.wildfly.nosql.ClientProfile;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@ClientProfile(profile="cassandratestprofile")
@Path("/" + ClusterResource.CLUSTER_RESOURCE_PATH)
@Singleton
public class ClusterResource {

    public static final String CLUSTER_RESOURCE_PATH = "cluster";

    public static final String SESSION_RESOURCE_PATH = "session";

    @Inject
    private Cluster cluster;

    private Session session;

    @POST
    public void connect() {
        session = cluster.connect();
    }

    @GET
    public String isClusterOpen() {
        return String.valueOf(!cluster.isClosed());
    }

    @GET
    @Path(SESSION_RESOURCE_PATH)
    public String isSessionOpen() {
        if (session == null) {
            return "false";
        }

        return String.valueOf(!session.isClosed());
    }

    @DELETE
    public void closeCluster() {
        cluster.close();
    }

    @DELETE
    @Path(SESSION_RESOURCE_PATH)
    public void closeSession() {
        if (session == null) {
            throw new ClientErrorException(Response.Status.PRECONDITION_FAILED);
        }

        session.close();
    }

}

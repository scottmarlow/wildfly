package org.jboss.as.test.compat.nosql.cassandra.shared;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.compat.nosql.cassandra.jaxrs.WebXml;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URL;

import static org.jboss.as.test.compat.nosql.cassandra.shared.ClusterResource.CLUSTER_RESOURCE_PATH;
import static org.jboss.as.test.compat.nosql.cassandra.shared.ClusterResource.SESSION_RESOURCE_PATH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SharedClusterTestCase {

    private static final String FIRST_DEPLOYMENT_NAME = "first-deployment";

    private static final String SECOND_DEPLOYMENT_NAME = "second-deployment";

    @OperateOnDeployment(FIRST_DEPLOYMENT_NAME)
    @ArquillianResource
    private URL firstDeploymentUrl;

    @OperateOnDeployment(SECOND_DEPLOYMENT_NAME)
    @ArquillianResource
    private URL secondDeploymentUrl;

    private String firstClusterResourcePath;

    private String secondClusterResourcePath;

    private String firstSessionResourcePath;

    private String secondSessionResourcePath;

    private Client client;

    @Deployment(name = FIRST_DEPLOYMENT_NAME, order = 1)
    public static WebArchive createFirstDeployment() {
        return getDeployment(FIRST_DEPLOYMENT_NAME);
    }

    @Deployment(name = SECOND_DEPLOYMENT_NAME, order = 1)
    public static WebArchive createSecondDeployment() {
        return getDeployment(SECOND_DEPLOYMENT_NAME);
    }

    private static final WebArchive getDeployment(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war").addClass(ClusterResource.class)
                .addAsWebInfResource(WebXml.get("<servlet-mapping>\n<servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                        + "<url-pattern>/*</url-pattern>\n</servlet-mapping>\n"), "web.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() {
        client = ClientBuilder.newClient();
        firstClusterResourcePath = firstDeploymentUrl.toExternalForm() + CLUSTER_RESOURCE_PATH;
        secondClusterResourcePath = secondDeploymentUrl.toExternalForm() + CLUSTER_RESOURCE_PATH;
        firstSessionResourcePath = firstClusterResourcePath + "/" + SESSION_RESOURCE_PATH;
        secondSessionResourcePath = secondClusterResourcePath + "/" + SESSION_RESOURCE_PATH;
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testCreatingAndClosingSessionsAndClusters() {
        connect(firstClusterResourcePath);
        assertTrue(isOpen(firstClusterResourcePath));
        assertTrue(isOpen(secondClusterResourcePath));
        assertTrue(isOpen(firstSessionResourcePath));
        assertFalse(isOpen(secondSessionResourcePath));

        connect(secondClusterResourcePath);
        assertTrue(isOpen(firstClusterResourcePath));
        assertTrue(isOpen(secondClusterResourcePath));
        assertTrue(isOpen(firstSessionResourcePath));
        assertTrue(isOpen(secondSessionResourcePath));

        close(firstSessionResourcePath);
        assertTrue(isOpen(firstClusterResourcePath));
        assertTrue(isOpen(secondClusterResourcePath));
        assertFalse(isOpen(firstSessionResourcePath));
        assertTrue(isOpen(secondSessionResourcePath));

        // The following step is unsafe. Closing cluster on the first deployment also closes it for the second deployment.
        close(firstClusterResourcePath);
        assertFalse(isOpen(firstClusterResourcePath));
        assertFalse(isOpen(secondClusterResourcePath));
        assertFalse(isOpen(firstSessionResourcePath));
        assertFalse(isOpen(secondSessionResourcePath));
    }

    private void connect(String url) {
        client.target(url).request().post(null);
    }

    private void close(String url) {
        client.target(url).request().delete();
    }

    private boolean isOpen(String url) {
        return Boolean.valueOf(client.target(url).request().get(String.class));
    }
}

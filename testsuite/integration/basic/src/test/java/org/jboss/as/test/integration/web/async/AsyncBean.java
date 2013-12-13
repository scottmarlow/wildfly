package org.jboss.as.test.integration.web.async;

import java.io.IOException;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.servlet.AsyncContext;

/**
 * AsyncBean
 *
 * @author Scott Marlow
 */
@Stateless
@Asynchronous
// @LocalBean
// @Remote
public class AsyncBean {

    public void doSomething(AsyncContext context) {
        try {
            context.getResponse().getWriter().println("AsyncBean output");
            context.getResponse().getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();    // print to console and ignore
        }
        context.complete();
    }
}

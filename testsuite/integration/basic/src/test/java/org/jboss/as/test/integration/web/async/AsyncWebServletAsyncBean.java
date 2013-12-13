package org.jboss.as.test.integration.web.async;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * AsyncWebServletAsyncBean test case for WFLY-2651
 *
 * @author Scott Marlow
 */

@WebServlet(name="async", urlPatterns = AsyncWebServletAsyncBean.URL_PATTERN, asyncSupported = true )
public class AsyncWebServletAsyncBean extends HttpServlet {
    static final String URL_PATTERN = "/async";

    @Inject
    AsyncBean asyncBean;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AsyncContext ctx = request.startAsync(request, response);
        asyncBean.doSomething(ctx);
    }

}

package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */

public class StatefulInterceptor implements Serializable {

    private final AtomicInteger count = new AtomicInteger(0);
    
    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        Object result = context.proceed();
        if (result instanceof Integer) {
            return ((Integer)result ) + count.addAndGet(100);
        }
        return result;
    }
}

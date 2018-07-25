package org.jboss.as.hibernate;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * TransformerLogger
 *
 * @author Scott Marlow
 */
@MessageLogger(projectCode = "ORMTRFMR")
public interface TransformerLogger extends BasicLogger {
    TransformerLogger LOGGER = Logger.getMessageLogger(TransformerLogger.class, "org.jboss.as.hibernate.transformer");
}

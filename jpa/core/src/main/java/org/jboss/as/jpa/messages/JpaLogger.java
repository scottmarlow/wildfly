/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.messages;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 07.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface JpaLogger extends BasicLogger {
    /**
     * Default root level logger with the package name for he category.
     */
    JpaLogger ROOT_LOGGER = Logger.getMessageLogger(JpaLogger.class, JpaLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.jpa}.
     */
    JpaLogger JPA_LOGGER = Logger.getMessageLogger(JpaLogger.class, "org.jboss.as.jpa");

    /**
     * Logs a warning message indicating duplicate persistence.xml files were found.
     *
     * @param puName    the persistence XML file.
     * @param ogPuName  the original persistence.xml file.
     * @param dupPuName the duplicate persistence.xml file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11400, value = "Duplicate Persistence Unit definition for %s " +
        "in application.  One of the duplicate persistence.xml should be removed from the application." +
        " Application deployment will continue with the persistence.xml definitions from %s used.  " +
        "The persistence.xml definitions from %s will be ignored.")
    void duplicatePersistenceUnitDefinition(String puName, String ogPuName, String dupPuName);

    /**
     * Logs an informational message indicating the persistence.xml file is being read.
     *
     * @param puUnitName the persistence unit name.
     */
    @LogMessage(level = INFO)
    @Message(id = 11401, value = "Read persistence.xml for %s")
    void readingPersistenceXml(String puUnitName);

    /**
     * Logs an informational message indicating the service, represented by the {@code serviceName} parameter, is
     * starting.
     *
     * @param serviceName the name of the service.
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 11402, value = "Starting %s Service '%s'")
    void startingService(String serviceName, String name);

    /**
     * Logs an informational message indicating the service, represented by the {@code serviceName} parameter, is
     * stopping.
     *
     * @param serviceName the name of the service.
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 11403, value = "Stopping %s Service '%s'")
    void stoppingService(String serviceName, String name);

    /**
     * Logs an error message indicating an exception occurred while preloading the default persistence provider adapter module.
     * Initialization continues after logging the error.
     *
     * @param cause the cause of the error.
     */
    //@LogMessage(level = ERROR)
    //@Message(id = 11404, value = "Could not load default persistence provider adaptor module.  Management attributes will not be registered for the adaptor")
    //void errorPreloadingDefaultProviderAdaptor(@Cause Throwable cause);

    /**
     * Logs an error message indicating an exception occurred while preloading the default persistence provider module.
     * Initialization continues after logging the error.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11405, value = "Could not load default persistence provider module.  ")
    void errorPreloadingDefaultProvider(@Cause Throwable cause);

    /**
     * Logs an error message indicating the persistence unit was not stopped
     *
     * @param cause       the cause of the error.
     * @param name        name of the persistence unit
     */
    @LogMessage(level = ERROR)
    @Message(id = 11406, value = "Failed to stop persistence unit service %s")
    void failedToStopPUService(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating a failure to get the module for the deployment unit represented by the
     * {@code deploymentUnit} parameter.
     *
     * @param deploymentUnit the deployment unit that failed.
     * @return a {@link org.jboss.as.server.deployment.DeploymentUnitProcessingException} for the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 11407, value = "Failed to get module attachment for %s")
    void failedToGetModuleAttachment(DeploymentUnit deploymentUnit);

    /**
     * warn that the entity class could not be loaded with the
     * {@link javax.persistence.spi.PersistenceUnitInfo#getClassLoader()}.
     *
     * @param cause     the cause of the error.
     * @param className the entity class name.
     */
    //@LogMessage(level = WARN)
    //@Message(id = 11408, value = "Could not load entity class '%s', ignoring this error and continuing with application deployment")
    //void cannotLoadEntityClass(@Cause Throwable cause, String className);

    /**
     * Logs an informational message indicating the persistence unit service is starting phase n of 2.
     *
     * @param phase       is the phase number (1 or 2)
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 11409, value = "Starting Persistence Unit (phase %d of 2) Service '%s'")
    void startingPersistenceUnitService(int phase, String name);

    /**
     * Logs an informational message indicating the service is stopping.
     *
     * @param phase       is the phase number (1 or 2)
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 11410, value = "Stopping Persistence Unit (phase %d of 2) Service '%s'")
    void stoppingPersistenceUnitService(int phase, String name);

    /**
     * Don't add any message ids higher than 11419 (will need a new chunk).
     */

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.datasourcedefinition;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.inject.Singleton;

/**
 * DataSourceConfig
 *
 * Ebuzer Taha Kanat
 * https://groups.google.com/d/msgid/wildfly/f6dcb86b-bccd-40b3-a4c6-2161b48da0e9n%40googlegroups.com?utm_medium=email&utm_source=footer
 */
@Singleton

@DataSourceDefinition(

    name = "java:app/deneme2DS",
    className = "org.h2.jdbcx.JdbcDataSource",
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "sa"
)
public class DataSourceConfig {

    // This class can be empty, as it only serves to define the DataSource

    // The DataSource will be automatically registered when this class is loaded

}


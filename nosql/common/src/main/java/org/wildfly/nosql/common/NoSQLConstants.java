package org.wildfly.nosql.common;

/**
 * NoSQLConstants common shared NoSQL constants
 *
 * @author Scott Marlow
 */
public class NoSQLConstants {

    // Cassandra constants
    public static final String CASSANDRACDIEXTENSIONMODULE = "org.wildfly.extension.nosql.cassandra";
    public static final String CASSANDRACLUSTERCLASS = "com.datastax.driver.core.Cluster";


    // Neo4j constants
    public static final String NEO4JCDIEXTENSIONMODULE = "org.wildfly.extension.nosql.neo4j";
    public static final String NEO4JDRIVERCLASS = "org.neo4j.driver.v1.Driver";

    // OrientDB constants
    public static final String ORIENTDBCDIEXTENSIONMODULE = "org.wildfly.extension.nosql.orientdb";
    public static final String ORIENTDBPARTIONEDDBPOOLCLASS = "com.orientechnologies.orient.core.db.OPartitionedDatabasePool";

    // MongoDB constants
    public static String MONGOCLIENTCLASS = "com.mongodb.MongoClient";
    public static final String MONGOCLIENTOPTIONSCLASS = "com.mongodb.MongoClientOptions";
    public static final String MONGODATABASECLASS = "com.mongodb.client.MongoDatabase";
    public static final String MONGOBUILDERCLASS = "com.mongodb.MongoClientOptions$Builder";
    public static final String MONGOWRITECONCERNCLASS = "com.mongodb.WriteConcern";
    public static final String MONGOSERVERADDRESSCLASS = "com.mongodb.ServerAddress";
    // MongoDB related constants
    public static final String MONGOCDIEXTENSIONMODULE = "org.wildfly.extension.nosql.mongodb";
    public static final String MONGOCDIEXTENSIONCLASS = "org.wildfly.extension.nosql.cdi.MongoExtension";
}

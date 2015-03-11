package org.jboss.as.test.compat.nosql.mongodb;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.json.Json;
import javax.json.JsonObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

/**
 * StatefulTestBean
 *
 * @author Scott Marlow
 */
@Stateful
public class StatefulTestBean {

    // @Resource(lookup = "java:jboss/mongodb/test")
    private MongoClient mongoClient;

    public void addData() {
        MongoClient mongoClient = getConnection();
        DB database = mongoClient.getDB("test");
        DBCollection collection = null;
        DBObject query = null;
        final String id = "m933";
        try {

            List<Integer> books = Arrays.asList(27464, 747854);
            DBObject person = new BasicDBObject("_id", id)
                    .append("name", "Melanie")
                    .append("address", new BasicDBObject("street", "123 Main Street")
                            .append("city", "Fastville")
                            .append("state", "MA")
                            .append("zip", 18180))
                    .append("books", books);
            collection = database.getCollection("people");

            collection.insert(person);
            query = new BasicDBObject("_id", id);
            DBCursor cursor = collection.find(query);
            DBObject melanie = cursor.next();
            System.out.println("DBObject.toString() = " + melanie.toString());
        } finally {
            collection.remove(query);
            mongoClient.close();
        }
    }

    private MongoClient getConnection() {
        try {
            if(this.mongoClient != null) {
                return this.mongoClient;
            }
            System.out.println("resource injection of mongoclient did not occur.");
            return new MongoClient("localhost");
        } catch (java.net.UnknownHostException u) {
            throw new RuntimeException(u);
        }
    }

    public void addDataViaJson() {
        MongoClient mongoClient = getConnection();
        DB database = mongoClient.getDB("test");
        DBCollection collection = null;
        final String id = "x559";
        DBObject query = null;
        try {
            collection = database.getCollection("company");

            JsonObject object = Json.createObjectBuilder()
                    .add("companyName", "Acme products")
                    .add("street", "999 Flow Lane")
                    .add("city", "Indiville")
                    .add("_id", id)
                    .build();

            collection.insert((DBObject) JSON.parse(object.toString()));
            query = new BasicDBObject("_id", id);
            DBCursor cursor = collection.find(query);
            DBObject x555 = cursor.next();
            System.out.println("DBObject.toString() = " + x555.toString());
        } finally {
            collection.remove(query);
            mongoClient.close();
        }

    }

}

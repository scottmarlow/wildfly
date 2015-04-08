package org.jboss.as.test.compat.nosql.mongodb;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.json.Json;
import javax.json.JsonObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * StatefulTestBean for the MongoDB document database
 *
 * @author Scott Marlow
 */
@Stateful
public class StatefulTestBean {

    @Resource(lookup = "java:jboss/mongodb/test")
    DB database;

    public void addUserComment() {
        DBCollection collection = null;
        DBObject query = null;
        try {
            // add a comment from user Melanie
            String who = "Melanie";
            DBObject comment = new BasicDBObject("_id", who)
                    .append("name", who)
                    .append("address", new BasicDBObject("street", "123 Main Street")
                            .append("city", "Fastville")
                            .append("state", "MA")
                            .append("zip", 18180))
                    .append("comment","I really love your new website but I have a lot of questions about using NoSQL versus a traditional RDBMS.  "+
                        "I would like to sign up for your 'Mongo DB Is Web Scale' training session.");
            // save the comment
            collection = database.getCollection("comments");
            collection.insert(comment);

            // look up the comment from Melanie
            query = new BasicDBObject("_id", who);
            DBCursor cursor = collection.find(query);
            DBObject userComment = cursor.next();
            System.out.println("DBObject.toString() = " + userComment.toString());
        } finally {
            collection.remove(query);
        }
    }

    public void addProduct() {
        DBCollection collection = null;
        database.getMongo();
        DBObject query = null;
        try {
            collection = database.getCollection("company");
            String companyName = "Acme products";
            JsonObject object = Json.createObjectBuilder()
                    .add("companyName", companyName)
                    .add("street", "999 Flow Lane")
                    .add("city", "Indiville")
                    .add("_id", companyName)
                    .build();

            collection.insert((DBObject) JSON.parse(object.toString()));
            query = new BasicDBObject("_id",companyName);
            DBCursor cursor = collection.find(query);
            DBObject dbObject = cursor.next();
            System.out.println("DBObject.toString() = " + dbObject.toString());
        } finally {
            if (query != null) {
                collection.remove(query);
            }
        }
    }


}

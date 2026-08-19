package com.mongodb;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private final MongoTemplate mongoTemplate;

    public PersonRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String add(PersonEntity person) {
        return mongoTemplate.insert(person).getId().toHexString();
    }

    public List<PersonEntity> getPersons() {
        return mongoTemplate.findAll(PersonEntity.class);
    }

    public long anniversaryPerson(String id) {
        return mongoTemplate.updateFirst(
                query(where("_id").is(new ObjectId(id))),
                new Update().inc("age", 1),
                PersonEntity.class).getModifiedCount();
    }

    public long deletePerson(String id) {
        return mongoTemplate.remove(
                query(where("_id").is(new ObjectId(id))),
                PersonEntity.class).getDeletedCount();
    }
}

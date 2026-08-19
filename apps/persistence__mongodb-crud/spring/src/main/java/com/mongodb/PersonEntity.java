package com.mongodb;

import java.util.Objects;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Document(collection = "persons")
public class PersonEntity {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    public ObjectId id;

    public String name;

    public Integer age;

    public PersonEntity() {
    }

    public PersonEntity(ObjectId id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PersonEntity other)) {
            return false;
        }
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(age, other.age);
    }
}

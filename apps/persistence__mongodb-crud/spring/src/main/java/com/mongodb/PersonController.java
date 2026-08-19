package com.mongodb;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class PersonController {

    private final PersonRepository personRepository;

    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @PostMapping(path = "/person", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String createPerson(@RequestBody PersonEntity person) {
        return personRepository.add(person);
    }

    @GetMapping("/persons")
    public List<PersonEntity> getPersons() {
        return personRepository.getPersons();
    }

    @PutMapping("/person/{id}")
    public long anniversaryPerson(@PathVariable("id") String id) {
        return personRepository.anniversaryPerson(id);
    }

    @DeleteMapping("/person/{id}")
    public long deletePerson(@PathVariable("id") String id) {
        return personRepository.deletePerson(id);
    }
}

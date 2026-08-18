package pl.piomin.spring.person.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.piomin.spring.person.model.Person;
import pl.piomin.spring.person.repository.PersonRepository;

@RestController
@RequestMapping("/persons")
public class PersonController {

    private final PersonRepository repository;

    public PersonController(PersonRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Person add(@RequestBody Person person) {
        return repository.save(person);
    }

    @GetMapping
    public List<Person> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Person findById(@PathVariable("id") Long id) {
        return repository.findById(id).orElse(null);
    }

    @GetMapping("/name/{name}")
    public List<Person> findByName(@PathVariable("name") String name) {
        return repository.findByName(name);
    }

    @GetMapping("/age-greater-than/{age}")
    public List<Person> findByAgeGreaterThan(@PathVariable("age") int age) {
        return repository.findByAgeGreaterThan(age);
    }
}

package pl.piomin.spring.person.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.piomin.spring.person.model.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByName(String name);

    List<Person> findByAgeGreaterThan(int age);
}

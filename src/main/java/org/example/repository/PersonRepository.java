package org.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.example.model.Person;
import java.util.List;

@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {
    @Transactional
    public Person getPersonById(long personId){
        return findById(personId);
    }

    @Transactional
    public List<Person> findAllOwners() {
        return list("role.roleName", "Owner"); //or (role.roleId, 2)
    }

}

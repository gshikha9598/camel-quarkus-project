package org.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.example.model.Role;

@ApplicationScoped
public class RoleRepository implements PanacheRepository<Role> {
}

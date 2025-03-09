package org.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.example.model.Tailor;

import java.util.List;

@ApplicationScoped
public class TailorRepository implements PanacheRepository<Tailor> {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void updateTailor(Tailor tailor){
        entityManager.merge(tailor);
    }

    @Transactional
    public List<Tailor> getAvailableTailor(){
        return  list("orderId IS NULL");
    }
}

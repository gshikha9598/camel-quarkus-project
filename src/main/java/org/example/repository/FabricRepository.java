package org.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.example.model.Fabric;


@ApplicationScoped
public class FabricRepository implements PanacheRepository<Fabric> {
    @Transactional
    public long getFabricByName(String fabricName){
        return find("fabricName=?1", fabricName).count();
    }
}

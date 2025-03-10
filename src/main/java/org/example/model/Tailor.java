package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
public class Tailor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long tailorId;
    private String tailorName;
    @ManyToMany
    private List<Fabric> fabrics;
    private String orderId;
    @OneToOne
    private Person manager;
}

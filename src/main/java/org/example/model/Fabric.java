package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class Fabric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long fabricId;
    private String fabricName;
}


package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "Orders")
@Getter
@Setter
@ToString
public class Order {
    @Id
    private String orderId;
    @OneToOne
    private Person user;
    private String fabric; //fabric Name
    private String stage;
    @ManyToOne
    @JoinColumn(name = "tailor_tailorid", nullable = false)
    private Tailor tailor;
    private boolean isCompleted;
    private LocalDateTime orderAcceptTime;
    private LocalDateTime orderCompleteTime;
    private LocalDateTime stageInTime;
}

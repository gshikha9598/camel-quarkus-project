package org.example.dto;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.model.Person;
import org.example.model.Tailor;

import java.time.LocalDateTime;

@ToString
@Getter
@Setter
public class OrderResponseDto {
    private String orderId;
    private String fabric; //fabric Name
    private String stage;
}

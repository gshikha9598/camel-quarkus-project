package org.example.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class OrderResponseDto {
    private String orderId;
    private String fabric; //fabric Name
    private String stage;
}

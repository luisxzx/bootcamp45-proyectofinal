package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendPaymentDTO {

    private String senderPhoneNumber;
    private String recipientPhoneNumber;
    private Double amount;

}
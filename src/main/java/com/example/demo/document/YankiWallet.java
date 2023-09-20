package com.example.demo.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "yankiWallets")
public class YankiWallet {

    @Id
    private String id;

    private String documentType;
    private String documentNumber;
    private String phoneNumber;
    private String imei;
    private String email;
    private Double balance;  // Saldo actual del monedero
    private String associatedDebitAccount;  // Cuenta de débito asociada

}
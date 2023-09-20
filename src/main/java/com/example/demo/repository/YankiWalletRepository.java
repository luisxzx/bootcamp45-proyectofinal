package com.example.demo.repository;

import com.example.demo.document.YankiWallet;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface YankiWalletRepository extends ReactiveMongoRepository<YankiWallet, String> {
    Mono<YankiWallet> findByPhoneNumber(String phoneNumber);
    Mono<YankiWallet> findByDocumentNumber(String documentNumber);
    Mono<YankiWallet> findByImei(String imei);
    Mono<YankiWallet> findByEmail(String email);
}

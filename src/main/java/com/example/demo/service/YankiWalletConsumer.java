package com.example.demo.service;

import com.example.demo.document.YankiWallet;
import com.example.demo.mapper.MessageMapper;
import com.example.demo.repository.YankiWalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class YankiWalletConsumer {

    private static final Logger logger = LoggerFactory.getLogger(YankiWalletConsumer.class);

    @Autowired
    private YankiWalletRepository repository;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private MessageMapper messageMapper;

    @KafkaListener(topics = "yanki-topic", groupId = "yanki-group")
    public void consume(String message) {
        logger.info("Recibido mensaje de Kafka: {}", message);

        Mono.just(message)
                .map(this::convertToYankiWallet)
                .doOnNext(wallet -> {
                    wallet.setBalance(0.0);
                    wallet.setAssociatedDebitAccount("cuenta de débito no asociada :( ");
                })
                .flatMap(wallet -> checkForDuplicates(wallet)
                        .then(repository.save(wallet))
                        .doOnNext(savedWallet -> logger.info("Guardando YankiWallet: {}", savedWallet))
                )
                .onErrorContinue((throwable, o) -> {
                    logger.error("Error al procesar el mensaje: ", throwable);
                })
                .subscribe();
    }

    public Mono<Void> checkForDuplicates(YankiWallet wallet) {
        return repository.findByDocumentNumber(wallet.getDocumentNumber())
                .flatMap(existingWallet -> Mono.<Void>error(new RuntimeException("El documento ya existe: " + existingWallet.getDocumentNumber())))
                .switchIfEmpty(repository.findByPhoneNumber(wallet.getPhoneNumber())
                        .flatMap(existingWallet -> Mono.<Void>error(new RuntimeException("El número de teléfono ya existe: " + existingWallet.getPhoneNumber())))
                )
                .switchIfEmpty(repository.findByImei(wallet.getImei())
                        .flatMap(existingWallet -> Mono.<Void>error(new RuntimeException("El IMEI ya existe: " + existingWallet.getImei())))
                )
                .switchIfEmpty(repository.findByEmail(wallet.getEmail())
                        .flatMap(existingWallet -> Mono.<Void>error(new RuntimeException("El email ya existe: " + existingWallet.getEmail())))
                );
    }




    public YankiWallet convertToYankiWallet(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(message, YankiWallet.class);
        } catch (Exception e) {
            logger.error("Error al convertir el mensaje a YankiWallet: ", e);
            throw new RuntimeException("Error al convertir el mensaje a YankiWallet", e);
        }
    }

    @KafkaListener(topics = "depositotowallet-topic", groupId = "yanki-group")
    public void consumeDeposit(String message) {
        logger.info("Recibido mensaje de depósito de Kafka: {}", message);

        Mono.just(message)
                .map(messageMapper::toDepositDTO)
                .flatMap(deposit -> repository.findByPhoneNumber(deposit.getPhoneNumber())
                        .flatMap(wallet -> {
                            wallet.setBalance(wallet.getBalance() + deposit.getAmount());
                            return repository.save(wallet);
                        }))
                .onErrorContinue((throwable, o) -> {
                    logger.error("Error al procesar el mensaje de depósito: ", throwable);
                })
                .subscribe();
    }

    @KafkaListener(topics = "sendpayment-topic", groupId = "yanki-group")
    public void consumeSendPayment(String message) {
        logger.info("Recibido mensaje de envío de pagos de Kafka: {}", message);

        Mono.just(message)
                .map(messageMapper::toSendPaymentDTO)
                .flatMap(payment -> repository.findByPhoneNumber(payment.getSenderPhoneNumber())
                        .flatMap(senderWallet -> {
                            if(senderWallet == null) {
                                logger.error("Número de teléfono del remitente no encontrado: {}", payment.getSenderPhoneNumber());
                                return Mono.empty();
                            }
                            if(senderWallet.getBalance() < payment.getAmount()) {
                                logger.error("Saldo insuficiente para el número de teléfono: {}", payment.getSenderPhoneNumber());
                                return Mono.empty();
                            }

                            return repository.findByPhoneNumber(payment.getRecipientPhoneNumber())
                                    .flatMap(recipientWallet -> {
                                        if(recipientWallet == null) {
                                            logger.error("Número de teléfono del destinatario no encontrado: {}", payment.getRecipientPhoneNumber());
                                            return Mono.empty();
                                        }

                                        senderWallet.setBalance(senderWallet.getBalance() - payment.getAmount());
                                        recipientWallet.setBalance(recipientWallet.getBalance() + payment.getAmount());

                                        return repository.save(senderWallet)
                                                .then(repository.save(recipientWallet));
                                    });
                        }))
                .onErrorContinue((throwable, o) -> {
                    logger.error("Error al procesar el mensaje de envío de pagos: ", throwable);
                })
                .subscribe();
    }

    @KafkaListener(topics = "consultaDni-topic", groupId = "yanki-group")
    public void getDniListener(String id) {
        getDniById(id)
                .doOnNext(dni -> logger.info("DNI obtenido desde redis: {}", dni))
                .doOnError(error -> logger.error("Error al obtener el DNI para el ID {}: {}", id, error.getMessage()))
                .subscribe();
    }

    public Mono<String> getDniById(String id) {
        String cacheKey = "wallet:" + id;

        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                        repository.findById(id)
                                .doOnNext(wallet -> {
                                    logger.info("Se está obteniendo el DNI desde MongoDB: {}", wallet.getDocumentNumber());
                                    redisTemplate.opsForValue().set(cacheKey, wallet.getDocumentNumber()).subscribe();
                                })
                                .switchIfEmpty(Mono.fromRunnable(() -> {
                                    logger.warn("No se encontró un YankiWallet con ID: {}", id);
                                }))
                                .map(YankiWallet::getDocumentNumber)
                );
    }





}
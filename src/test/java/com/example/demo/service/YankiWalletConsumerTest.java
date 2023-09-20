package com.example.demo.service;

import com.example.demo.DTO.DepositDTO;
import com.example.demo.DTO.SendPaymentDTO;
import com.example.demo.document.YankiWallet;
import com.example.demo.mapper.MessageMapper;
import com.example.demo.repository.YankiWalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YankiWalletConsumerTest {

    @Mock
    private YankiWalletRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private YankiWalletConsumer yankiWalletConsumer;


    @Mock
    private MessageMapper messageMapper;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void consume_validMessage_savesWallet() throws Exception {
        // Prepare data
        String message = "{\n" +
                "    \"id\": \"1234\",\n" +
                "    \"documentType\": \"DNI\",\n" +
                "    \"documentNumber\": \"12345678\",\n" +
                "    \"phoneNumber\": \"987654321\",\n" +
                "    \"imei\": \"35297306526358\",\n" +
                "    \"email\": \"luis@gmail.com\",\n" +
                "    \"balance\": 1000.50,\n" +
                "    \"associatedDebitAccount\": \"no hay aun\"\n" +
                "}";
        YankiWallet yankiWallet = objectMapper.readValue(message, YankiWallet.class);
        when(repository.findByDocumentNumber(anyString())).thenReturn(Mono.empty());
        when(repository.findByPhoneNumber(anyString())).thenReturn(Mono.empty());
        when(repository.findByImei(anyString())).thenReturn(Mono.empty());
        when(repository.findByEmail(anyString())).thenReturn(Mono.empty());
        when(repository.save(any(YankiWallet.class))).thenReturn(Mono.just(yankiWallet));
        yankiWalletConsumer.consume(message);
        verify(repository).save(any(YankiWallet.class));
    }

    @Test
    public void testConsumeDeposit() throws Exception {
        // Datos de entrada
        String inputMessage = "{ \"phoneNumber\": \"987654321\", \"amount\": 100.0 }";

        DepositDTO depositDTO = new DepositDTO("987654321", 100.0);

        YankiWallet existingWallet = new YankiWallet();
        existingWallet.setPhoneNumber("987654321");
        existingWallet.setBalance(500.0);
        when(messageMapper.toDepositDTO(inputMessage)).thenReturn(depositDTO);
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(existingWallet));
        when(repository.save(any(YankiWallet.class))).thenReturn(Mono.just(existingWallet));

        yankiWalletConsumer.consumeDeposit(inputMessage);

        verify(messageMapper).toDepositDTO(inputMessage);
        verify(repository).findByPhoneNumber("987654321");

        ArgumentCaptor<YankiWallet> walletCaptor = ArgumentCaptor.forClass(YankiWallet.class);
        verify(repository).save(walletCaptor.capture());

        YankiWallet updatedWallet = walletCaptor.getValue();
        assertEquals(Double.valueOf(600.0), updatedWallet.getBalance());
    }
    @Test
    public void testConsumeSendPayment() throws Exception {

        String inputMessage = "{ \"senderPhoneNumber\": \"987654321\", \"recipientPhoneNumber\": \"123456789\", \"amount\": 100.0 }";

        SendPaymentDTO paymentDTO = new SendPaymentDTO("987654321", "123456789", 100.0);

        YankiWallet senderWallet = new YankiWallet();
        senderWallet.setPhoneNumber("987654321");
        senderWallet.setBalance(500.0);

        YankiWallet recipientWallet = new YankiWallet();
        recipientWallet.setPhoneNumber("123456789");
        recipientWallet.setBalance(300.0);

        when(messageMapper.toSendPaymentDTO(inputMessage)).thenReturn(paymentDTO);
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(senderWallet));
        when(repository.findByPhoneNumber("123456789")).thenReturn(Mono.just(recipientWallet));
        when(repository.save(any(YankiWallet.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        yankiWalletConsumer.consumeSendPayment(inputMessage);

        // Verification steps
        verify(messageMapper).toSendPaymentDTO(inputMessage);
        verify(repository).findByPhoneNumber("987654321");
        verify(repository).findByPhoneNumber("123456789");

        ArgumentCaptor<YankiWallet> walletCaptor = ArgumentCaptor.forClass(YankiWallet.class);
        verify(repository, times(2)).save(walletCaptor.capture());

        List<YankiWallet> savedWallets = walletCaptor.getAllValues();

        YankiWallet updatedSenderWallet = savedWallets.get(0);
        YankiWallet updatedRecipientWallet = savedWallets.get(1);

        assertEquals(Double.valueOf(400.0), updatedSenderWallet.getBalance());
        assertEquals(Double.valueOf(400.0), updatedRecipientWallet.getBalance());
    }




}

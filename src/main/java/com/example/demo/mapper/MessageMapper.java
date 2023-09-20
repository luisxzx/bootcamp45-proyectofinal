package com.example.demo.mapper;

import com.example.demo.DTO.DepositDTO;
import com.example.demo.DTO.SendPaymentDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {


    private static final Logger logger = LoggerFactory.getLogger(MessageMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DepositDTO toDepositDTO(String message) {
        try {
            return objectMapper.readValue(message, DepositDTO.class);
        } catch (Exception e) {
            logger.error("Error al convertir el mensaje a DepositDTO: ", e);
            throw new RuntimeException("Error al convertir el mensaje a DepositDTO", e);
        }
    }

    public SendPaymentDTO toSendPaymentDTO(String message) {
        try {
            return objectMapper.readValue(message, SendPaymentDTO.class);
        } catch (Exception e) {
            logger.error("Error al convertir el mensaje a SendPaymentDTO: ", e);
            throw new RuntimeException("Error al convertir el mensaje a SendPaymentDTO", e);
        }
    }
}

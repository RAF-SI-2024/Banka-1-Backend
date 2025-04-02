package com.banka1.notification.listener;


import com.banka1.notification.listener.helper.MessageHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageHelperTest {

    @Mock
    private Validator validator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TextMessage message;

    @InjectMocks
    private MessageHelper messageHelper;

    private static class TestMessage {
        public String field;
    }

    @BeforeEach
    void setUp() {
        messageHelper = new MessageHelper(validator, objectMapper);
    }

    @Test
    void getMessage_ShouldReturnValidObject_WhenNoValidationErrors() throws Exception {
        String json = "{\"field\":\"value\"}";
        TestMessage testMessage = new TestMessage();
        testMessage.field = "value";

        when(message.getText()).thenReturn(json);
        when(objectMapper.readValue(json, TestMessage.class)).thenReturn(testMessage);
        when(validator.validate(testMessage)).thenReturn(Collections.emptySet());

        TestMessage result = messageHelper.getMessage(message, TestMessage.class);

        assertNotNull(result);
        assertEquals("value", result.field);
    }

    @Test
    void getMessage_ShouldThrowException_WhenValidationFails() throws Exception {
        String json = "{\"field\":\"value\"}";
        TestMessage testMessage = new TestMessage();
        testMessage.field = "value";

        @SuppressWarnings("unchecked")
        ConstraintViolation<TestMessage> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Field is invalid");
        Set<ConstraintViolation<TestMessage>> violations = Set.of(violation);

        when(message.getText()).thenReturn(json);
        when(objectMapper.readValue(json, TestMessage.class)).thenReturn(testMessage);
        when(validator.validate(testMessage)).thenReturn(violations);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                messageHelper.getMessage(message, TestMessage.class));
        assertEquals("Field is invalid", exception.getMessage());
    }

    @Test
    void getMessage_ShouldThrowException_WhenMessageParsingFails() throws Exception {
        String invalidJson = "invalid_json";

        when(message.getText()).thenReturn(invalidJson);
        when(objectMapper.readValue(invalidJson, TestMessage.class)).thenThrow(new RuntimeException("Parsing error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                messageHelper.getMessage(message, TestMessage.class));
        assertEquals("Parsing error", exception.getMessage());
    }
}
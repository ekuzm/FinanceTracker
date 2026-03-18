package com.finance.tracker.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.WARN);

        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @Test
    void handleApiExceptionShouldLogClientErrors() {
        var response = handler.handleApiException(new ResourceNotFoundException("User not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("API exception [404]: User not found", event.getFormattedMessage());
        assertEquals(null, event.getThrowableProxy());
    }

    @Test
    void handleApiExceptionShouldLogServerErrorsWithoutStackTrace() {
        var response = handler.handleApiException(new LoggingException("Transfer failed"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("API exception [500]: Transfer failed", event.getFormattedMessage());
        assertEquals(null, event.getThrowableProxy());
    }

    @Test
    void handleMethodArgumentNotValidExceptionShouldLogValidationErrors() throws NoSuchMethodException {
        Method method = SampleRequest.class.getDeclaredMethod("setName", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("Validation failed"));
        assertTrue(event.getFormattedMessage().contains("name=must not be blank"));
    }

    @Test
    void handleMethodArgumentTypeMismatchExceptionShouldLogBadParameter() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, new IllegalArgumentException("bad id"));

        var response = handler.handleMethodArgumentTypeMismatchException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("Method argument type mismatch: Invalid value 'abc' for parameter 'id'",
                event.getFormattedMessage());
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldLogMalformedPayload() {
        InvalidFormatException invalidFormatException = InvalidFormatException.from(
                null,
                "Invalid enum value",
                "oops",
                TransactionTypeStub.class);
        invalidFormatException.prependPath(new JsonMappingException.Reference(SampleRequest.class, "type"));
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON request",
                invalidFormatException,
                null);

        var response = handler.handleHttpMessageNotReadableException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("Malformed JSON request: Invalid value for field 'type'", event.getFormattedMessage());
    }

    @Test
    void handleExceptionShouldLogUnhandledErrors() {
        var response = handler.handleException(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Unhandled exception", event.getFormattedMessage());
    }

    private static final class SampleRequest {
        @SuppressWarnings("unused")
        void setName(String name) {
        }
    }

    private enum TransactionTypeStub {
        INCOME
    }
}

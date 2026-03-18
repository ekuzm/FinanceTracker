package com.finance.tracker.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.exception.ResourceNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ServiceLoggingAspectTest {

    private final ServiceLoggingAspect aspect = new ServiceLoggingAspect();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ServiceLoggingAspect.class);
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
    void logExecutionTimeShouldLogFailureTimeAndRethrowApiException() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("findTransaction");
        ResourceNotFoundException exception = new ResourceNotFoundException("Transaction not found");
        when(joinPoint.proceed()).thenThrow(exception);

        ResourceNotFoundException thrown = assertThrows(
                ResourceNotFoundException.class,
                () -> aspect.logExecutionTime(joinPoint));

        assertSame(exception, thrown);
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().startsWith("Method DummyService.findTransaction failed in "));
        assertTrue(event.getFormattedMessage().contains(" [404]: Transaction not found"));
        assertEquals(null, event.getThrowableProxy());
    }

    @Test
    void logExecutionTimeShouldLogFailureTimeAndWrapUnexpectedException() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("createTransaction");
        IllegalStateException exception = new IllegalStateException("boom");
        when(joinPoint.proceed()).thenThrow(exception);

        LoggingException thrown = assertThrows(
                LoggingException.class,
                () -> aspect.logExecutionTime(joinPoint));

        assertEquals("Error executing method! DummyService.createTransaction", thrown.getMessage());
        assertSame(exception, thrown.getCause());
        assertEquals(1, appender.list.size());

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().startsWith("Method DummyService.createTransaction failed in "));
        assertTrue(event.getFormattedMessage().endsWith(" ms"));
        assertEquals(IllegalStateException.class.getName(), event.getThrowableProxy().getClassName());
    }

    private ProceedingJoinPoint joinPoint(String methodName) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(joinPoint.getTarget()).thenReturn(new DummyService());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(new Object[] {42L});

        return joinPoint;
    }

    private static final class DummyService {
    }
}

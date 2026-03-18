package com.finance.tracker.aop;

import com.finance.tracker.exception.ApiException;
import com.finance.tracker.exception.LoggingException;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final int SLOW_THRESHOLD_MS = 500;
    private static final int VERY_SLOW_THRESHOLD_MS = 1000;
    private static final String ERROR_EXECUTING_METHOD = "Error executing method!";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        StopWatch stopWatch = new StopWatch(fullMethodName);

        try {
            stopWatch.start(fullMethodName);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing method: {} with arguments: {}",
                        fullMethodName,
                        Arrays.toString(joinPoint.getArgs()));
            }

            Object result = joinPoint.proceed();

            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            if (executionTime > VERY_SLOW_THRESHOLD_MS) {
                LOGGER.warn("Method {} finished in {} ms (exceeds {} ms threshold)",
                        fullMethodName,
                        executionTime,
                        VERY_SLOW_THRESHOLD_MS);
            } else if (executionTime > SLOW_THRESHOLD_MS) {
                LOGGER.info("Method {} finished in {} ms", fullMethodName, executionTime);
            } else {
                LOGGER.debug("Method {} finished in {} ms", fullMethodName, executionTime);
            }

            return result;
        } catch (Exception exception) {
            long executionTime = stopAndGetExecutionTime(stopWatch);
            if (exception instanceof ApiException apiException) {
                logApiException(fullMethodName, executionTime, apiException);
                throw apiException;
            }
            LOGGER.error("Method {} failed in {} ms", fullMethodName, executionTime, exception);
            throw new LoggingException(ERROR_EXECUTING_METHOD + " " + fullMethodName, exception);
        }
    }

    private void logApiException(String fullMethodName, long executionTime, ApiException exception) {
        int statusCode = exception.getStatus().value();
        if (exception.getStatus().is5xxServerError()) {
            LOGGER.error("Method {} failed in {} ms [{}]: {}",
                    fullMethodName,
                    executionTime,
                    statusCode,
                    exception.getMessage());
            return;
        }
        LOGGER.warn("Method {} failed in {} ms [{}]: {}",
                fullMethodName,
                executionTime,
                statusCode,
                exception.getMessage());
    }

    private long stopAndGetExecutionTime(StopWatch stopWatch) {
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        }
        return stopWatch.getTotalTimeMillis();
    }
}

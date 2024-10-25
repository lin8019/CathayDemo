package com.example.demo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Aspect
public class LoggingAspect extends BaseLogger {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // get all @RestController class api
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllerRequests(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        logger.debug("Incoming request to: {}", joinPoint.getSignature());

        for (Object arg : args) {
            try {
                String json = objectMapper.writeValueAsString(arg);
                logger.debug("Request Body: {}", json);
            } catch (JsonProcessingException e) {
                logger.debug("Request Body (non-serializable): {}", arg);
            }
        }

        try {
            Object result = joinPoint.proceed();

            if (result instanceof CompletableFuture) {
                CompletableFuture<?> futureResult = (CompletableFuture<?>) result;
                futureResult.thenAccept(response -> {
                    try {
                        String json = objectMapper.writeValueAsString(response);
                        logger.debug("Response Body: {}", json);
                    } catch (JsonProcessingException e) {
                        logger.debug("Response Body (non-serializable): {}", response);
                    }
                }).exceptionally(ex -> {
                    logger.debug("Error processing async response: {}", ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
            } else {
                try {
                    String json = objectMapper.writeValueAsString(result);
                    logger.debug("Response Body: {}", json);
                } catch (JsonProcessingException e) {
                    logger.debug("Response Body (non-serializable): " + result);
                }
            }

            return result;
        } catch (Throwable ex) {
            logger.debug("Error processing request: {}", ex.getMessage());
            ex.printStackTrace();
            return "An error occurred: " + ex.getMessage();
        }
    }
}

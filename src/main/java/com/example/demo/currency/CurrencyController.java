package com.example.demo.currency;

import com.example.demo.config.BaseLogger;
import com.example.demo.util.EncryptionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController extends BaseLogger {
    @Autowired
    private CurrencyEntityRepository currencyEntityRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private MessageSource messageSource;

    @Async
    @PostMapping
    @Operation(description = "Create specific Currency Information")
    public CompletableFuture<CurrencyDTO> createEntity(
            @Parameter(description = "Language for the response", required = false)
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(required = true) String code,
            @RequestParam(required = true) BigDecimal rateFloat,
            @RequestParam(required = true) String currencyNameCN) {
        CurrencyEntity currencyEntity = currencyEntityRepository.findByCode(code);
        if (currencyEntity != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency exist");
        }

        CurrencyEntity entity = new CurrencyEntity();
        entity.setUpdated(new Timestamp(System.currentTimeMillis()));
        entity.setCode(code);
        entity.setCurrencyNameCN(currencyNameCN);
        entity.setRateFloat(rateFloat);

        CurrencyEntity savedEntity = currencyEntityRepository.save(entity);
        return CompletableFuture.completedFuture(supportI18n(acceptLanguage, savedEntity));
    }

    @Async
    @GetMapping
    @Operation(description = "Get All Currency Information")
    public CompletableFuture<List<CurrencyDTO>> getAllCurrency(
            @Parameter(description = "Language for the response", required = false)
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort sort = Sort.by("code");
        sort = sortDirection.equalsIgnoreCase("DESC") ? sort.descending() : sort.ascending();


        return CompletableFuture.completedFuture(
                currencyEntityRepository.findAll(sort).stream()
                        .map(entity -> {
                            return supportI18n(acceptLanguage, entity);
                        })
                        .collect(Collectors.toList())
        );
    }

    @Async
    @GetMapping("/{encryptCode}")
    @Operation(description = "Get specific Currency Information")
    public CompletableFuture<CurrencyDTO> findCurrency(
            @Parameter(description = "Language for the response", required = false)
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable String encryptCode) {
        String decryptedCode;
        try {
            decryptedCode = EncryptionUtil.decrypt(encryptCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        CurrencyEntity currencyEntity = currencyEntityRepository.findByCode(decryptedCode);
        if (currencyEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found");
        }
        return CompletableFuture.completedFuture(supportI18n(acceptLanguage, currencyEntity));
    }

    @Async
    @PutMapping("/{code}")
    @Operation(description = "Updated specific Currency Information")
    public CompletableFuture<CurrencyDTO> updateCurrency(
            @Parameter(description = "Language for the response", required = false)
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable String code,
            @RequestParam(required = false) BigDecimal rateFloat,
            @RequestParam(required = false) String currencyNameCN) {
        CurrencyEntity existingEntity = currencyEntityRepository.findByCode(code);
        if (existingEntity != null) {
            existingEntity.setUpdated(new Timestamp(System.currentTimeMillis()));
            if (rateFloat != null) {
                existingEntity.setRateFloat(rateFloat);
            }
            if (currencyNameCN != null) {
                existingEntity.setCurrencyNameCN(currencyNameCN);
            }

            CurrencyEntity updatedEntity = currencyEntityRepository.save(existingEntity);
            return CompletableFuture.completedFuture(supportI18n(acceptLanguage, updatedEntity));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found");
        }
    }

    @Async
    @DeleteMapping("/{code}")
    @Operation(description = "Delete specific Currency Information")
    public CompletableFuture<ResponseEntity<Void>> deleteCurrency(@PathVariable String code) {
        CurrencyEntity existingEntity = currencyEntityRepository.findByCode(code);
        if (existingEntity != null) {
            currencyEntityRepository.delete(existingEntity);
            return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found");
        }
    }

    private CurrencyDTO supportI18n(String acceptLanguage, CurrencyEntity entity) {
        Locale locale;
        if (acceptLanguage == null || Locale.forLanguageTag(acceptLanguage).getLanguage().isEmpty()) {
            locale = Locale.TAIWAN;
        } else {
            locale = Locale.forLanguageTag(acceptLanguage);
        }

        CurrencyDTO dto = modelMapper.map(entity, CurrencyDTO.class);
        String language = messageSource.getMessage("language", new Object[]{locale.getDisplayLanguage(locale)}, locale);
        dto.setLanguage(language);
        return dto;
    }
}


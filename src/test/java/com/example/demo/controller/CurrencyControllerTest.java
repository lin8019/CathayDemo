package com.example.demo.controller;

import com.example.demo.currency.*;
import com.example.demo.encryption.EncryptionController;
import com.example.demo.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class CurrencyControllerTest {
    @Mock
    private CurrencyEntityRepository currencyEntityRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CurrencyController currencyController;

    private Pair<CurrencyEntity, CurrencyDTO> usdPair;
    private Pair<CurrencyEntity, CurrencyDTO> eurPair;

    private final String local_zh_TW = "zh_TW";
    private final String encryptCodeUSD = "EPq9oFB3PKMMIpa6Z6HJSw==";

    @BeforeAll
    public static void initMocks() {
        MockitoAnnotations.openMocks(CurrencyControllerTest.class);
    }

    @BeforeEach
    public void setup() {
        CurrencyEntity usdEntity = new CurrencyEntity();
        usdEntity.setCode("USD");
        usdEntity.setCurrencyNameCN("美元");
        usdEntity.setRateFloat(new BigDecimal("66370.93"));

        CurrencyDTO usdDTO = new CurrencyDTO();
        usdDTO.setCode(usdEntity.getCode());
        usdDTO.setRateFloat(usdEntity.getRateFloat());
        usdDTO.setCurrencyNameCN(usdEntity.getCurrencyNameCN());

        usdPair = Pair.of(usdEntity, usdDTO);

        CurrencyEntity eurEntity = new CurrencyEntity();
        eurEntity.setCode("EUR");
        eurEntity.setCurrencyNameCN("歐元");
        eurEntity.setRateFloat(new BigDecimal("61564.94"));

        CurrencyDTO eurDTO = new CurrencyDTO();
        eurDTO.setCode(eurEntity.getCode());
        eurDTO.setRateFloat(eurEntity.getRateFloat());
        eurDTO.setCurrencyNameCN(eurEntity.getCurrencyNameCN());

        eurPair = Pair.of(eurEntity, eurDTO);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateEntity(boolean currencyExist) {
        CurrencyEntity fakeEntity = usdPair.getFirst();
        CurrencyDTO fakeDTO = usdPair.getSecond();
        fakeDTO.setLanguage(local_zh_TW);

        // Arrange
        when(currencyEntityRepository.findByCode(fakeEntity.getCode())).thenReturn(!currencyExist ? null : fakeEntity);
        if (!currencyExist) {
            when(currencyEntityRepository.save(any(CurrencyEntity.class))).thenReturn(fakeEntity);
            when(modelMapper.map(any(CurrencyEntity.class), eq(CurrencyDTO.class))).thenReturn(fakeDTO);
            when(messageSource.getMessage(any(), any(), any())).thenReturn(fakeDTO.getLanguage());
        }

        // Act
        if (!currencyExist) {

            CurrencyDTO result = currencyController.createEntity(local_zh_TW, fakeEntity.getCode(), fakeEntity.getRateFloat(), fakeEntity.getCurrencyNameCN()).join();

            // Assert
            assertNotNull(result);
            assertEquals(fakeEntity.getCode(), result.getCode());
            assertEquals(fakeEntity.getRateFloat(), result.getRateFloat());
            assertEquals(fakeEntity.getCurrencyNameCN(), result.getCurrencyNameCN());
            assertEquals(local_zh_TW, result.getLanguage());
        } else {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                currencyController.createEntity(local_zh_TW, fakeEntity.getCode(), fakeEntity.getRateFloat(), fakeEntity.getCurrencyNameCN()).join();
            });

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Currency exist", exception.getReason());
        }

        // Verify that save was called or never called since the currency already exists
        verify(currencyEntityRepository, times(1)).findByCode(fakeEntity.getCode());
        verify(currencyEntityRepository, times(currencyExist ? 0 : 1)).save(any(CurrencyEntity.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASC", "DESC"})
    public void testGetAllCurrency_Sorted(String sortDirection) {
        CurrencyEntity usd_fakeEntity = usdPair.getFirst();
        CurrencyDTO usd_fakeDTO = usdPair.getSecond();
        CurrencyEntity eur_fakeEntity = eurPair.getFirst();
        CurrencyDTO eur_fakeDTO = eurPair.getSecond();

        // Arrange
        List<CurrencyEntity> sortedList;

        if ("ASC".equals(sortDirection)) {
            sortedList = Arrays.asList(eur_fakeEntity, usd_fakeEntity);
            when(currencyEntityRepository.findAll(Sort.by("code").ascending())).thenReturn(sortedList);
        } else {
            sortedList = Arrays.asList(usd_fakeEntity, eur_fakeEntity);
            when(currencyEntityRepository.findAll(Sort.by("code").descending())).thenReturn(sortedList);
        }

        when(modelMapper.map(eur_fakeEntity, CurrencyDTO.class)).thenReturn(eur_fakeDTO);
        when(modelMapper.map(usd_fakeEntity, CurrencyDTO.class)).thenReturn(usd_fakeDTO);

        // Act
        List<CurrencyDTO> result = currencyController.getAllCurrency(local_zh_TW, sortDirection).join();

        // Assert
        assertNotNull(result);
        assertEquals(sortedList.size(), result.size());
        assertEquals(sortedList.get(0).getCode(), result.get(0).getCode());
        assertEquals(sortedList.get(1).getCode(), result.get(1).getCode());

        // Verify that findAll was called with the correct sort direction
        if ("ASC".equals(sortDirection)) {
            verify(currencyEntityRepository, times(1)).findAll(Sort.by("code").ascending());
        } else {
            verify(currencyEntityRepository, times(1)).findAll(Sort.by("code").descending());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testFindCurrency(boolean currencyExist) {
        CurrencyEntity fakeEntity = usdPair.getFirst();
        CurrencyDTO fakeDTO = usdPair.getSecond();
        fakeDTO.setLanguage(local_zh_TW);

        // Arrange
        when(currencyEntityRepository.findByCode(fakeEntity.getCode())).thenReturn(currencyExist ? fakeEntity : null);
        if (currencyExist) {
            when(modelMapper.map(fakeEntity, CurrencyDTO.class)).thenReturn(fakeDTO);
            when(messageSource.getMessage(any(), any(), any())).thenReturn(fakeDTO.getLanguage());
        }

        // Act
        if (currencyExist) {
            CurrencyDTO result = currencyController.findCurrency(local_zh_TW, encryptCodeUSD).join();

            // Assert
            assertNotNull(result);
            assertEquals(fakeEntity.getCode(), result.getCode());
            assertEquals(fakeEntity.getCurrencyNameCN(), result.getCurrencyNameCN());
            assertEquals(fakeEntity.getRateFloat(), result.getRateFloat());
            assertEquals(local_zh_TW, result.getLanguage());
        } else {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                currencyController.findCurrency(local_zh_TW, encryptCodeUSD).join();
            });

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Currency not found", exception.getReason());
        }

        // Verify that the repository was called
        verify(currencyEntityRepository, times(1)).findByCode(fakeEntity.getCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testUpdateCurrency(boolean currencyExist) {
        String updatedName = "美金";
        BigDecimal updatedRate = new BigDecimal(99999);

        CurrencyEntity fakeEntity = usdPair.getFirst();
        CurrencyDTO fakeDTO = usdPair.getSecond();
        fakeDTO.setLanguage(local_zh_TW);

        // Arrange
        when(currencyEntityRepository.findByCode(fakeEntity.getCode())).thenReturn(currencyExist ? fakeEntity : null);
        if (currencyExist) {
            when(currencyEntityRepository.save(fakeEntity)).thenReturn(fakeEntity);
            when(modelMapper.map(fakeEntity, CurrencyDTO.class)).thenReturn(fakeDTO);
            when(messageSource.getMessage(any(), any(), any())).thenReturn(fakeDTO.getLanguage());
        }

        if (currencyExist) {
            // Act
            fakeEntity.setCurrencyNameCN(updatedName);
            fakeEntity.setRateFloat(updatedRate);
            fakeDTO.setCurrencyNameCN(updatedName);
            fakeDTO.setRateFloat(updatedRate);
            CurrencyDTO result = currencyController.updateCurrency(local_zh_TW, fakeEntity.getCode(), updatedRate, updatedName).join();

            // Assert
            assertNotNull(result);
            assertEquals(fakeEntity.getCode(), result.getCode());
            assertEquals(updatedName, result.getCurrencyNameCN());
            assertEquals(updatedRate, result.getRateFloat());
            assertEquals(local_zh_TW, result.getLanguage());
        } else {
            // Act
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                currencyController.updateCurrency(local_zh_TW, fakeEntity.getCode(), updatedRate, updatedName).join();
            });

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Currency not found", exception.getReason());
        }

        // Verify that the methods were called as expected
        verify(currencyEntityRepository, times(1)).findByCode(fakeEntity.getCode());
        verify(currencyEntityRepository, times(currencyExist ? 1 : 0)).save(fakeEntity);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testDeleteCurrency(boolean currencyExist) {
        CurrencyEntity fakeEntity = usdPair.getFirst();

        // Arrange
        when(currencyEntityRepository.findByCode(fakeEntity.getCode())).thenReturn(currencyExist ? fakeEntity : null);

        if (currencyExist) {
            // Act
            ResponseEntity<Void> result = currencyController.deleteCurrency(fakeEntity.getCode()).join();

            // Assert
            assertEquals(ResponseEntity.noContent().build(), result);
        } else {
            // Act
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                currencyController.deleteCurrency(fakeEntity.getCode()).join();
            });

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Currency not found", exception.getReason());
        }

        // Verify
        verify(currencyEntityRepository, times(1)).findByCode(fakeEntity.getCode());
        verify(currencyEntityRepository, times(currencyExist ? 1 : 0)).delete(fakeEntity);
    }
}

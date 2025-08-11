package com.santec.polenta.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerServiceTest {

    @InjectMocks
    private TokenizerService tokenizerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testServiceInitialization() {
        assertNotNull(tokenizerService);
    }
}


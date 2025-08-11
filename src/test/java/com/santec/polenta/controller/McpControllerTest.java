package com.santec.polenta.controller;

import com.santec.polenta.service.McpDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class McpControllerTest {

    @Mock
    private McpDispatcherService mcpDispatcherService;

    @InjectMocks
    private McpController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testControllerInitialization() {
        assertNotNull(controller);
    }

    // Agrega aquí más tests unitarios según los métodos públicos del controlador
}


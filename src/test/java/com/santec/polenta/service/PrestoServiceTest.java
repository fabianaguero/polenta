package com.santec.polenta.service;

import com.santec.polenta.config.PrestoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class PrestoServiceTest {

    @Test
    void appliesConnectionTimeout() throws Exception {
        PrestoConfig config = new PrestoConfig();
        config.setUrl("jdbc:presto://localhost:9/hive/default");
        config.setUser("test");
        config.setConnectionTimeout(1234);

        PrestoService service = new PrestoService();
        ReflectionTestUtils.setField(service, "prestoConfig", config);

        int originalTimeout = DriverManager.getLoginTimeout();
        int expectedTimeoutSeconds = (int) Math.ceil(config.getConnectionTimeout() / 1000.0);

        Method method = PrestoService.class.getDeclaredMethod("getConnection");
        method.setAccessible(true);
        try {
            method.invoke(service);
            fail("Expected SQLException");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.getCause() instanceof SQLException);
        }

        assertEquals(expectedTimeoutSeconds, DriverManager.getLoginTimeout());

        DriverManager.setLoginTimeout(originalTimeout);
    }
}

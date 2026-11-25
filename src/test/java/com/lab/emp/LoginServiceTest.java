package com.lab.emp;

import com.lab.emp.common.BusinessException;
import com.lab.emp.service.LoginService;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class LoginServiceTest {
    private static final String ADMIN_HASH =
            "iUt/0Asn2bHJCuVBkZcg8A==:T+ekDk7UquaRnaCou66FqGTZN6tRH1z/BqUZkKD1qDM=";
    private final LoginService login = new LoginService(name -> "admin".equals(name) ? ADMIN_HASH : null);

    @Test
    public void authenticatesUsingTheStoredHash() {
        login.authenticate("admin", "123456");
    }

    @Test
    public void rejectsAnIncorrectPassword() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> login.authenticate("admin", "incorrect"));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }

    @Test
    public void rejectsAnUnknownAccount() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> login.authenticate("unknown", "123456"));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }
}

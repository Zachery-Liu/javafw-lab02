package com.lab.emp.service;

import com.lab.emp.common.BusinessException;
import com.lab.emp.mapper.AccountMapper;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;

    private final AccountMapper accounts;

    public LoginService(AccountMapper accounts) {
        this.accounts = accounts;
    }

    public void authenticate(String username, String password) {
        String storedHash = accounts.findPasswordHash(username);
        if (storedHash == null || !matches(password, storedHash)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
    }

    private static boolean matches(String password, String storedHash) {
        String[] parts = storedHash.split(":", -1);
        if (parts.length != 2) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            if (salt.length != 16 || expected.length != KEY_LENGTH / Byte.SIZE) {
                return false;
            }
            PBEKeySpec key = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            try {
                byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded();
                return MessageDigest.isEqual(expected, actual);
            } finally {
                key.clearPassword();
            }
        } catch (IllegalArgumentException invalidHash) {
            return false;
        } catch (GeneralSecurityException unavailableAlgorithm) {
            throw new IllegalStateException("无法校验账户密码", unavailableAlgorithm);
        }
    }
}

package com.lab.emp.controller;

import com.lab.emp.common.BusinessException;
import com.lab.emp.common.Result;
import com.lab.emp.dto.LoginRequest;
import com.lab.emp.interceptor.LoginInterceptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
    private final Environment environment;

    public LoginController(Environment environment) {
        this.environment = environment;
    }

    @PostMapping(path = "/login", consumes = "application/json")
    public Result<Void> login(@Valid @RequestBody LoginRequest login, HttpServletRequest request) {
        String username = environment.getProperty("EMP_LOGIN_USER", "");
        String password = environment.getProperty("EMP_LOGIN_PASSWORD", "");
        if (username.isEmpty() || password.isEmpty()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "实验登录账号尚未配置");
        }
        boolean usernameMatches = equal(username, login.getUsername());
        boolean passwordMatches = equal(password, login.getPassword());
        if (!usernameMatches || !passwordMatches) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            request.changeSessionId();
        }
        session.setAttribute(LoginInterceptor.LOGIN_USER, username);
        return Result.ok(null);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok(null);
    }

    private static boolean equal(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}

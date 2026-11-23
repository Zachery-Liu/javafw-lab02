package com.lab.emp.controller;

import com.lab.emp.common.Result;
import com.lab.emp.dto.LoginRequest;
import com.lab.emp.interceptor.LoginInterceptor;
import com.lab.emp.service.LoginService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(path = "/login", consumes = "application/json")
    public Result<Void> login(@Valid @RequestBody LoginRequest login, HttpServletRequest request) {
        String username = login.getUsername();
        loginService.authenticate(username, login.getPassword());

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
}

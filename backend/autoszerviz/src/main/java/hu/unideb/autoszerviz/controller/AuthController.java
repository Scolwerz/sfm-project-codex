package hu.unideb.autoszerviz.controller;

import hu.unideb.autoszerviz.model.AuthService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/auth/register")
  public Map<String, Object> register(@RequestBody Map<String, String> b, HttpSession s) {
    return service.register(b, s);
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(@RequestBody Map<String, String> b, HttpSession s) {
    return service.login(b, s);
  }

  @PostMapping("/auth/logout")
  public Map<String, String> logout(HttpSession s) {
    return service.logout(s);
  }

  @GetMapping("/auth/me")
  public Map<String, Object> me(HttpSession s) {
    return service.me(s);
  }

  @PutMapping("/profile")
  public Map<String, Object> profile(@RequestBody Map<String, String> b, HttpSession s) {
    return service.profile(b, s);
  }
}

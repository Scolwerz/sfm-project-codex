package hu.unideb.autoszerviz.controller;

import hu.unideb.autoszerviz.model.TicketService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TicketController {
  private final TicketService service;

  public TicketController(TicketService service) {
    this.service = service;
  }

  @GetMapping("/tickets")
  public List<Map<String, Object>> list(
      @RequestParam(required = false) String status, HttpSession s) {
    return service.list(status, s);
  }

  @GetMapping("/tickets/{id}")
  public Map<String, Object> detail(@PathVariable Long id, HttpSession s) {
    return service.detail(id, s);
  }

  @PutMapping("/tickets/{id}/status")
  public Map<String, Object> status(
      @PathVariable Long id, @RequestBody Map<String, String> b, HttpSession s) {
    return service.status(id, b, s);
  }

  @PutMapping("/tickets/{id}/confirm")
  public Map<String, Object> confirm(
      @PathVariable Long id, @RequestBody Map<String, Boolean> b, HttpSession s) {
    return service.confirm(id, b, s);
  }
}

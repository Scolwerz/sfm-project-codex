package hu.unideb.autoszerviz.controller;

import hu.unideb.autoszerviz.model.BookingService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {
  private final BookingService service;

  public BookingController(BookingService service) {
    this.service = service;
  }

  @GetMapping("/tickets/mine")
  public List<Map<String, Object>> mine(HttpSession s) {
    return service.mine(s);
  }

  @GetMapping("/availability")
  public List<String> availability(
      @RequestParam Long serviceId, @RequestParam String date, HttpSession s) {
    return service.availability(serviceId, date, s);
  }

  @PostMapping("/tickets")
  public Map<String, Object> create(@RequestBody Map<String, Object> b, HttpSession s) {
    return service.create(b, s);
  }

  @DeleteMapping("/tickets/{id}")
  public void cancel(@PathVariable Long id, HttpSession s) {
    service.cancel(id, s);
  }
}

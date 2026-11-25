package hu.unideb.autoszerviz.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.model.TicketService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.junit.jupiter.api.Test;

class TicketControllerTest {
  TicketService service = mock(TicketService.class);
  TicketController controller = new TicketController(service);
  HttpSession session = mock(HttpSession.class);

  @Test
  void listTicketsDelegatesToModel() {
    List<Map<String, Object>> expected = List.of(Map.of("id", 1));
    when(service.list(null, session)).thenReturn(expected);
    assertEquals(expected, controller.list(null, session));
  }

  @Test
  void getTicketDetailsDelegatesToModel() {
    Map<String, Object> expected = Map.of("id", 1);
    when(service.detail(1L, session)).thenReturn(expected);
    assertEquals(expected, controller.detail(1L, session));
  }

  @Test
  void updateTicketStatusDelegatesToModel() {
    Map<String, String> body = Map.of("status", "ACCEPTED");
    Map<String, Object> expected = Map.of("id", 1);
    when(service.status(1L, body, session)).thenReturn(expected);
    assertEquals(expected, controller.status(1L, body, session));
  }

  @Test
  void confirmTicketDelegatesToModel() {
    Map<String, Boolean> body = Map.of("approved", true);
    Map<String, Object> expected = Map.of("id", 1);
    when(service.confirm(1L, body, session)).thenReturn(expected);
    assertEquals(expected, controller.confirm(1L, body, session));
  }
}

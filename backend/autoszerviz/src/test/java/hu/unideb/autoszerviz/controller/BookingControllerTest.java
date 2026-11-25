package hu.unideb.autoszerviz.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.model.BookingService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.junit.jupiter.api.Test;

class BookingControllerTest {
    BookingService service = mock(BookingService.class);
    BookingController controller = new BookingController(service);
    HttpSession session = mock(HttpSession.class);

    @Test
    void listMineDelegatesToModel() {
        List<Map<String, Object>> expected = List.of(Map.of("id", 1));
        when(service.mine(session)).thenReturn(expected);
        assertEquals(expected, controller.mine(session));
    }

    @Test
    void findAvailabilityDelegatesToModel() {
        when(service.availability(1L, "2026-12-01", session)).thenReturn(List.of("09:00"));
        assertEquals(List.of("09:00"), controller.availability(1L, "2026-12-01", session));
    }

    @Test
    void createBookingDelegatesToModel() {
        Map<String, Object> body = Map.of("vehicleId", 1), expected = Map.of("id", 2);
        when(service.create(body, session)).thenReturn(expected);
        assertEquals(expected, controller.create(body, session));
    }

    @Test
    void cancelBookingDelegatesToModel() {
        controller.cancel(2L, session);
        verify(service).cancel(2L, session);
    }
}

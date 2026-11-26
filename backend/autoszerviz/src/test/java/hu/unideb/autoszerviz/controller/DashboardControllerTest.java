package hu.unideb.autoszerviz.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.model.DashboardService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardControllerTest {
    @Test
    void delegatesDashboard() {
        DashboardService service = mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> expected = Map.of("active", 2);
        when(service.dashboard(session)).thenReturn(expected);
        assertSame(expected, controller.dashboard(session));
        verify(service).dashboard(session);
    }
}

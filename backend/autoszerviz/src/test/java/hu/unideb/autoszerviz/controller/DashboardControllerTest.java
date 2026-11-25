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
        DashboardService s = mock(DashboardService.class);
        DashboardController c = new DashboardController(s);
        HttpSession h = mock(HttpSession.class);
        Map<String, Object> expected = Map.of("active", 2);
        when(s.dashboard(h)).thenReturn(expected);
        assertSame(expected, c.dashboard(h));
        verify(s).dashboard(h);
    }
}

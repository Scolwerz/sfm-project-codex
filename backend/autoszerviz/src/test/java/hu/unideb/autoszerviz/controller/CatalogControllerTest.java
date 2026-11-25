package hu.unideb.autoszerviz.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.model.CatalogService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogControllerTest {
    @Test
    void delegatesCatalog() {
        CatalogService s = mock(CatalogService.class);
        CatalogController c = new CatalogController(s);
        Map<String, Object> expected = Map.of("services", java.util.List.of());
        when(s.catalog()).thenReturn(expected);
        assertSame(expected, c.catalog());
        verify(s).catalog();
    }
}

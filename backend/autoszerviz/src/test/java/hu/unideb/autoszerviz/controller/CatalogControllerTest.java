package hu.unideb.autoszerviz.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.model.CatalogService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogControllerTest {
    @Test
    void delegatesCatalog() {
        CatalogService service = mock(CatalogService.class);
        CatalogController controller = new CatalogController(service);
        Map<String, Object> expected = Map.of("services", java.util.List.of());
        when(service.catalog()).thenReturn(expected);
        assertSame(expected, controller.catalog());
        verify(service).catalog();
    }
}

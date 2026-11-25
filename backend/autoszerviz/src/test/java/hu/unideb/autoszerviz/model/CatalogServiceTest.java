package hu.unideb.autoszerviz.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {
  BrandRepository brands = mock(BrandRepository.class);
  VehicleModelRepository models = mock(VehicleModelRepository.class);
  ServiceTypeRepository services = mock(ServiceTypeRepository.class);
  CatalogService service = new CatalogService(brands, models, services);

  @Test
  void catalogReturnsEachCatalogGroup() {
    when(brands.findByActiveTrueOrderByName()).thenReturn(List.of());
    when(models.findAll()).thenReturn(List.of());
    when(services.findByActiveTrueOrderByName()).thenReturn(List.of());
    assertTrue(service.catalog().containsKey("brands"));
    assertTrue(service.catalog().containsKey("models"));
  }

  @Test
  void catalogMapsActiveBrandModelAndService() {
    Brand brand = new Brand();
    brand.id = 1L;
    brand.name = "Volvo";
    brand.originRegion = "SE";
    VehicleModel model = new VehicleModel();
    model.id = 2L;
    model.brand = brand;
    model.name = "V40";
    ServiceType serviceType = new ServiceType();
    serviceType.id = 3L;
    serviceType.name = "Oil";
    serviceType.description = "Oil change";
    serviceType.durationMinutes = 30;
    when(brands.findByActiveTrueOrderByName()).thenReturn(List.of(brand));
    when(models.findAll()).thenReturn(List.of(model));
    when(services.findByActiveTrueOrderByName()).thenReturn(List.of(serviceType));
    assertEquals(1, ((List<?>) service.catalog().get("models")).size());
    assertEquals(1, ((List<?>) service.catalog().get("services")).size());
  }
}

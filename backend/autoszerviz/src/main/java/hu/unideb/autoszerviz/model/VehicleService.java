package hu.unideb.autoszerviz.model;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VehicleService {

    private final ModelAccessService access;
    private final BrandRepository brands;
    private final VehicleModelRepository models;
    private final VehicleRepository vehicles;

    public VehicleService(ModelAccessService access, BrandRepository brands, VehicleModelRepository models, VehicleRepository vehicles) {
        this.access = access;
        this.brands = brands;
        this.models = models;
        this.vehicles = vehicles;
    }

    public List<Map<String, Object>> myVehicles(HttpSession s) {
        AppUser u = access.currentUser(s);
        access.requireRole(u, Role.CUSTOMER);
        return vehicles.findByOwnerIdAndActiveTrueOrderByIdDesc(u.id).stream()
            .map(v -> Map.<String, Object>of(
                "id", v.id,
                "brandId", v.brand.id,
                "brand", v.brand.name,
                "origin", v.brand.originRegion == null ? "" : v.brand.originRegion,
                "modelId", v.model.id,
                "model", v.model.name,
                "year", v.year,
                "fuel", v.fuel == null ? "" : v.fuel,
                "engine", v.engine == null ? "" : v.engine,
                "licensePlate", v.licensePlate == null ? "" : v.licensePlate))
            .toList();
    }

    public Map<String, Object> addVehicle(Map<String, Object> b, HttpSession s) {
        AppUser u = access.currentUser(s);
        access.requireRole(u, Role.CUSTOMER);

        Vehicle v = new Vehicle();
        v.owner = u;
        v.brand = brands.findById(Long.valueOf(b.get("brandId").toString())).orElseThrow();
        v.model = models.findById(Long.valueOf(b.get("modelId").toString())).orElseThrow();
        if (!v.model.brand.id.equals(v.brand.id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A modell nem ehhez a márkához tartozik.");
        v.year = Integer.parseInt(b.get("year").toString());
        v.fuel = (String) b.get("fuel");
        v.engine = (String) b.getOrDefault("engine", "");
        v.licensePlate = (String) b.getOrDefault("licensePlate", "");
        vehicles.save(v);
        return Map.of("id", v.id);
    }

    public void deleteVehicle(Long id, HttpSession s) {
        AppUser u = access.currentUser(s);
        access.requireRole(u, Role.CUSTOMER);
        Vehicle v = vehicles
            .findByIdAndOwnerIdAndActiveTrue(id, u.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        v.active = false;
        vehicles.save(v);
    }
}

package hu.unideb.autoszerviz.controller;

import hu.unideb.autoszerviz.model.VehicleService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping("/vehicles")
    public List<Map<String, Object>> myVehicles(HttpSession s) {
        return service.myVehicles(s);
    }

    @PostMapping("/vehicles")
    public Map<String, Object> addVehicle(@RequestBody Map<String, Object> b, HttpSession s) {
        return service.addVehicle(b, s);
    }

    @DeleteMapping("/vehicles/{id}")
    public void deleteVehicle(@PathVariable Long id, HttpSession s) {
        service.deleteVehicle(id, s);
    }
}

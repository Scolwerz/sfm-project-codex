package hu.unideb.autoszerviz.model;

import static org.junit.jupiter.api.Assertions.*;

import hu.unideb.autoszerviz.entity.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TicketViewMapperTest {
    TicketViewMapper mapper = new TicketViewMapper();

    @Test
    void mapsTicketAndNestedCustomerVehicleAndService() {
        AppUser customer = new AppUser();
        customer.id = 1L;
        customer.fullName = "Anna";
        customer.email = "a@b.hu";
        Brand brand = new Brand();
        brand.id = 2L;
        brand.name = "Volvo";
        VehicleModel model = new VehicleModel();
        model.id = 3L;
        model.name = "V40";
        model.brand = brand;
        Vehicle vehicle = new Vehicle();
        vehicle.id = 4L;
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.year = 2020;
        ServiceType service = new ServiceType();
        service.id = 5L;
        service.name = "Szerviz";
        service.durationMinutes = 60;
        Ticket ticket = new Ticket();
        ticket.id = 6L;
        ticket.customer = customer;
        ticket.vehicle = vehicle;
        ticket.serviceType = service;
        ticket.startAt = LocalDateTime.of(2026, 11, 24, 9, 0);
        ticket.endAt = ticket.startAt.plusHours(1);
        ticket.createdAt = ticket.startAt;
        ticket.status = TicketStatus.PENDING;
        var mapped = mapper.toView(ticket);
        assertEquals(6L, mapped.get("id"));
        assertEquals("Anna", ((java.util.Map<?, ?>) mapped.get("customer")).get("name"));
    }
}

package hu.unideb.autoszerviz.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class BookingServiceTest {
    AccessService access = mock(AccessService.class);
    TicketViewMapper mapper = mock(TicketViewMapper.class);
    UserRepository users = mock(UserRepository.class);
    VehicleRepository vehicles = mock(VehicleRepository.class);
    ServiceTypeRepository services = mock(ServiceTypeRepository.class);
    TicketRepository tickets = mock(TicketRepository.class);
    BookingService service = new BookingService(access, mapper, users, vehicles, services, tickets);
    HttpSession session = mock(HttpSession.class);
    User customer;
    ServiceType oilService;

    @BeforeEach
    void setupCustomerAndService() {
        customer = new User();
        customer.id = 1L;
        customer.role = Role.CUSTOMER;
        oilService = new ServiceType();
        oilService.id = 3L;
        oilService.durationMinutes = 60;
        when(access.currentUser(session)).thenReturn(customer);
    }

    @Test
    void listsCustomerBookings() {
        when(tickets.findByCustomerIdOrderByStartAtDesc(1L)).thenReturn(List.of());
        assertTrue(service.mine(session).isEmpty());
    }

    @Test
    void returnsNoAvailabilityForWeekend() {
        when(services.findById(3L)).thenReturn(Optional.of(oilService));
        assertTrue(service.availability(3L, "2026-11-28", session).isEmpty());
    }

    @Test
    void returnsOpenSlotsOnAWeekday() {
        when(services.findById(3L)).thenReturn(Optional.of(oilService));
        User mechanic = new User();
        mechanic.active = true;
        mechanic.role = Role.MECHANIC;
        when(users.findAll()).thenReturn(List.of(mechanic));
        when(tickets.findAll()).thenReturn(List.of());
        assertTrue(service.availability(3L, "2099-03-02", session).contains("08:00"));
    }

    @Test
    void rejectsBookingWithoutOwnedVehicle() {
        when(vehicles.findByIdAndOwnerIdAndActiveTrue(8L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.create(Map.of("vehicleId", 8, "serviceId", 3, "startAt", "2099-03-02T09:00"), session));
    }

    @Test
    void createsBookingWhenCapacityIsAvailable() {
        Brand brand = new Brand();
        brand.id = 2L;
        brand.name = "Volvo";
        VehicleModel model = new VehicleModel();
        model.id = 4L;
        model.name = "V40";
        model.brand = brand;
        Vehicle vehicle = new Vehicle();
        vehicle.id = 8L;
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.year = 2020;
        when(vehicles.findByIdAndOwnerIdAndActiveTrue(8L, 1L)).thenReturn(Optional.of(vehicle));
        when(services.lockAllForBooking()).thenReturn(List.of(oilService));
        when(users.findAll())
            .thenReturn( List.of(
                new User() {
                    {
                        active = true;
                        role = Role.MECHANIC;
                    }
                }));
        when(tickets.findAll()).thenReturn(List.of());
        when(mapper.toView(any())).thenReturn(Map.of("status", "PENDING"));
        when(tickets.save(any())).thenAnswer(call -> call.getArgument(0));
        Map<String, Object> request = Map.of(
            "vehicleId", 8,
            "serviceId", 3,
            "startAt", "2099-03-02T09:00",
            "description", "Noise");
        assertEquals("PENDING", service.create(request, session).get("status"));
        verify(tickets).save(argThat(ticket -> ticket.endAt.equals(LocalDateTime.parse("2099-03-02T10:00"))));
    }

    @Test
    void cancelsPendingTicketOwnedByCustomer() {
        Ticket ticket = new Ticket();
        ticket.customer = customer;
        ticket.status = TicketStatus.PENDING;
        when(tickets.lockById(5L)).thenReturn(Optional.of(ticket));
        service.cancel(5L, session);
        assertEquals(TicketStatus.CANCELLED, ticket.status);
    }
}

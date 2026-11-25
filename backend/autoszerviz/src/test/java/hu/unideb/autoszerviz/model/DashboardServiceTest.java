package hu.unideb.autoszerviz.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    ModelAccessService access = mock(ModelAccessService.class);
    TicketViewMapper mapper = mock(TicketViewMapper.class);
    TicketRepository tickets = mock(TicketRepository.class);
    VehicleRepository vehicles = mock(VehicleRepository.class);
    DashboardService service = new DashboardService(access, mapper, tickets, vehicles);
    HttpSession session = mock(HttpSession.class);

    @Test
    void summarizesCustomerBookingsAndVehicles() {
        AppUser customer = new AppUser();
        customer.id = 1L;
        customer.role = Role.CUSTOMER;
        when(access.currentUser(session)).thenReturn(customer);
        when(vehicles.countByOwnerIdAndActiveTrue(1L)).thenReturn(2L);
        when(tickets.findByCustomerIdOrderByStartAtDesc(1L)).thenReturn(List.of());
        assertEquals(2L, service.dashboard(session).get("vehicles"));
        assertEquals(0L, service.dashboard(session).get("active"));
    }

    @Test
    void summarizesMechanicIncomingTickets() {
        AppUser mechanic = new AppUser();
        mechanic.id = 2L;
        mechanic.role = Role.MECHANIC;
        when(access.currentUser(session)).thenReturn(mechanic);
        when(tickets.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        assertEquals(0L, service.dashboard(session).get("incoming"));
    }

    @Test
    void countsIncomingActiveAndWaitingMechanicTickets() {
        AppUser mechanic = new AppUser();
        mechanic.id = 2L;
        mechanic.role = Role.MECHANIC;
        when(access.currentUser(session)).thenReturn(mechanic);
        Ticket pending = new Ticket();
        pending.status = TicketStatus.PENDING;
        Ticket active = new Ticket();
        active.status = TicketStatus.IN_PROGRESS;
        active.mechanic = mechanic;
        Ticket waiting = new Ticket();
        waiting.status = TicketStatus.AWAITING_CUSTOMER;
        waiting.mechanic = mechanic;
        when(tickets.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pending, active, waiting));
        var result = service.dashboard(session);
        assertEquals(1L, result.get("incoming"));
        assertEquals(1L, result.get("active"));
    }
}

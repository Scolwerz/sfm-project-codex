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

class TicketServiceTest {
    AccessService access = mock(AccessService.class);
    TicketViewMapper mapper = mock(TicketViewMapper.class);
    UserRepository users = mock(UserRepository.class);
    ServiceTypeRepository services = mock(ServiceTypeRepository.class);
    TicketRepository tickets = mock(TicketRepository.class);
    TicketService service = new TicketService(access, mapper, users, services, tickets);
    HttpSession session = mock(HttpSession.class);
    User mechanic;

    @BeforeEach
    void setupMechanic() {
        mechanic = new User();
        mechanic.id = 2L;
        mechanic.role = Role.MECHANIC;
        when(access.currentUser(session)).thenReturn(mechanic);
        when(mapper.toView(any())).thenReturn(Map.of("id", 5L));
    }

    @Test
    void listsTicketsForMechanic() {
        when(tickets.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        assertTrue(service.list(null, session).isEmpty());
    }

    @Test
    void returnsRequestedTicketDetails() {
        Ticket ticket = new Ticket();
        ticket.mechanic = mechanic;
        ticket.status = TicketStatus.ACCEPTED;
        when(tickets.findById(5L)).thenReturn(Optional.of(ticket));
        assertEquals(5L, service.detail(5L, session).get("id"));
    }

    @Test
    void rejectsMissingTicketDetails() {
        when(tickets.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.detail(5L, session));
    }

    @Test
    void acceptsPendingTicketAndAssignsMechanic() {
        Ticket ticket = new Ticket();
        ticket.status = TicketStatus.PENDING;
        ticket.startAt = LocalDateTime.of(2099, 3, 2, 9, 0);
        ticket.endAt = ticket.startAt.plusHours(1);
        when(tickets.lockById(5L)).thenReturn(Optional.of(ticket));
        when(tickets.mechanicOverlap(eq(2L), any(), any(), any())).thenReturn(false);
        service.status(5L, Map.of("status", "ACCEPTED"), session);
        assertEquals(TicketStatus.ACCEPTED, ticket.status);
        assertSame(mechanic, ticket.mechanic);
    }

    @Test
    void confirmsResolvedTicketWhenCustomerApproves() {
        User customer = new User();
        customer.id = 1L;
        customer.role = Role.CUSTOMER;
        when(access.currentUser(session)).thenReturn(customer);
        Ticket ticket = new Ticket();
        ticket.customer = customer;
        ticket.status = TicketStatus.AWAITING_CUSTOMER;
        when(tickets.lockById(5L)).thenReturn(Optional.of(ticket));
        service.confirm(5L, Map.of("approved", true), session);
        assertEquals(TicketStatus.RESOLVED, ticket.status);
    }

    @Test
    void movesInProgressWorkToWaitingForParts() {
        Ticket ticket = new Ticket();
        ticket.mechanic = mechanic;
        ticket.status = TicketStatus.IN_PROGRESS;
        when(tickets.lockById(6L)).thenReturn(Optional.of(ticket));
        service.status(6L, Map.of("status", "WAITING_FOR_PARTS"), session);
        assertEquals(TicketStatus.WAITING_FOR_PARTS, ticket.status);
    }

    @Test
    void rejectsStatusChangeByUnassignedMechanic() {
        Ticket ticket = new Ticket();
        ticket.status = TicketStatus.IN_PROGRESS;
        when(tickets.lockById(6L)).thenReturn(Optional.of(ticket));
        assertThrows(ResponseStatusException.class, () -> service.status(6L, Map.of("status", "WAITING_FOR_PARTS"), session));
    }

    @Test
    void recordsCustomerComplaintWhenConfirmationIsDeclined() {
        User customer = new User();
        customer.id = 1L;
        when(access.currentUser(session)).thenReturn(customer);
        Ticket ticket = new Ticket();
        ticket.customer = customer;
        ticket.status = TicketStatus.AWAITING_CUSTOMER;
        when(tickets.lockById(7L)).thenReturn(Optional.of(ticket));
        service.confirm(7L, Map.of("approved", false), session);
        assertEquals(TicketStatus.COMPLAINT, ticket.status);
    }
}

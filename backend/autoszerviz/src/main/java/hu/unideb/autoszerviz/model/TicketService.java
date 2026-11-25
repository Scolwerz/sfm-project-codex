package hu.unideb.autoszerviz.model;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {
  private static final List<TicketStatus> BLOCKING = List.of(
                                TicketStatus.ACCEPTED,
                                TicketStatus.IN_PROGRESS,
                                TicketStatus.WAITING_FOR_PARTS,
                                TicketStatus.AWAITING_CUSTOMER);

    private final ModelAccessService access;
    private final TicketViewMapper ticketMapper;
    private final UserRepository users;
    private final ServiceTypeRepository services;
    private final TicketRepository tickets;

    public TicketService(ModelAccessService access, TicketViewMapper ticketMapper, UserRepository users, ServiceTypeRepository services, TicketRepository tickets) {
        this.access = access;
        this.ticketMapper = ticketMapper;
        this.users = users;
        this.services = services;
        this.tickets = tickets;
    }

    public List<Map<String, Object>> list(String status, HttpSession s) {
        AppUser u = access.currentUser(s);
        access.requireRole(u, Role.MECHANIC, Role.ADMIN);
        List<Ticket> ts = status == null ? tickets.findAllByOrderByCreatedAtDesc() : tickets.findByStatusOrderByCreatedAtDesc(TicketStatus.valueOf(status));
        return (u.role == Role.ADMIN ? ts : ts.stream()
                    .filter(t -> t.status == TicketStatus.PENDING || u.id.equals(t.mechanic == null ? null : t.mechanic.id))
                    .toList())
            .stream().map(ticketMapper::toView).toList();
    }

    public Map<String, Object> detail(Long id, HttpSession s) {
        AppUser u = access.currentUser(s);
        Ticket t = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.role == Role.CUSTOMER && !u.id.equals(t.customer.id))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (u.role == Role.MECHANIC && t.status != TicketStatus.PENDING && t.mechanic != null && !u.id.equals(t.mechanic.id))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ticketMapper.toView(t);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> status(Long id, Map<String, String> b, HttpSession s) {
        AppUser u = access.currentUser(s);
        access.requireRole(u, Role.MECHANIC, Role.ADMIN);
        services.lockAllForBooking();

        Ticket t = tickets.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TicketStatus next = TicketStatus.valueOf(b.get("status"));
        if (next == TicketStatus.ACCEPTED) {
            if (t.status != TicketStatus.PENDING || tickets.mechanicOverlap(u.id, BLOCKING, t.startAt, t.endAt))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A ticketet már elfogadták, vagy ütközik a saját időpontoddal.");
            t.mechanic = u;
            t.status = next;
        }
        else {
            if (t.mechanic == null || !t.mechanic.id.equals(u.id))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Csak a hozzárendelt szerelő módosíthatja ezt a munkát.");
            boolean ok = switch (t.status) {
                case ACCEPTED -> next == TicketStatus.IN_PROGRESS || next == TicketStatus.PENDING;
                case IN_PROGRESS -> next == TicketStatus.WAITING_FOR_PARTS || next == TicketStatus.AWAITING_CUSTOMER;
                case WAITING_FOR_PARTS -> next == TicketStatus.IN_PROGRESS;
                case COMPLAINT -> next == TicketStatus.IN_PROGRESS;
                default -> false;
            };
            if (!ok)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Érvénytelen státuszváltás.");
            t.status = next;
            if (next == TicketStatus.PENDING)
                t.mechanic = null;
            if (next == TicketStatus.AWAITING_CUSTOMER)
                t.closedAt = LocalDateTime.now();
        }

        return ticketMapper.toView(t);
    }

    @Transactional
    public Map<String, Object> confirm(Long id, Map<String, Boolean> b, HttpSession s) {
        AppUser u = access.currentUser(s);
        access.requireRole(u, Role.CUSTOMER);

        Ticket t = tickets.lockById(id).orElseThrow();
        if (!u.id.equals(t.customer.id) || t.status != TicketStatus.AWAITING_CUSTOMER)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ez a munka nem vár visszaigazolásra.");

        t.status = Boolean.TRUE.equals(b.get("approved")) ? TicketStatus.RESOLVED : TicketStatus.COMPLAINT;
        
        return ticketMapper.toView(t);
    }
}

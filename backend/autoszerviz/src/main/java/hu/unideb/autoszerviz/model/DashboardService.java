package hu.unideb.autoszerviz.model;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

  private final ModelAccessService access;
  private final TicketViewMapper ticketMapper;
  private final TicketRepository tickets;
  private final VehicleRepository vehicles;

  public DashboardService(
      ModelAccessService access,
      TicketViewMapper ticketMapper,
      TicketRepository tickets,
      VehicleRepository vehicles) {
    this.access = access;
    this.ticketMapper = ticketMapper;
    this.tickets = tickets;
    this.vehicles = vehicles;
  }

  public Map<String, Object> dashboard(HttpSession s) {
    AppUser u = access.currentUser(s);
    if (u.role == Role.CUSTOMER) {
      List<Ticket> mine = tickets.findByCustomerIdOrderByStartAtDesc(u.id);
      return Map.of(
          "vehicles",
          vehicles.countByOwnerIdAndActiveTrue(u.id),
          "active",
          mine.stream()
              .filter(
                  t -> !List.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED).contains(t.status))
              .count(),
          "closed",
          mine.stream().filter(t -> t.status == TicketStatus.RESOLVED).count(),
          "recent",
          mine.stream()
              .sorted(Comparator.comparing((Ticket t) -> t.createdAt).reversed())
              .limit(3)
              .map(ticketMapper::toView)
              .toList());
    }
    access.requireRole(u, Role.MECHANIC, Role.ADMIN);
    List<Ticket> all =
        u.role == Role.ADMIN
            ? tickets.findAllByOrderByCreatedAtDesc()
            : tickets.findAllByOrderByCreatedAtDesc().stream()
                .filter(
                    t ->
                        t.status == TicketStatus.PENDING
                            || u.id.equals(t.mechanic == null ? null : t.mechanic.id))
                .toList();
    return Map.of(
        "incoming",
        all.stream().filter(t -> t.status == TicketStatus.PENDING).count(),
        "active",
        all.stream()
            .filter(
                t ->
                    List.of(
                            TicketStatus.ACCEPTED,
                            TicketStatus.IN_PROGRESS,
                            TicketStatus.WAITING_FOR_PARTS)
                        .contains(t.status))
            .count(),
        "waiting",
        all.stream().filter(t -> t.status == TicketStatus.AWAITING_CUSTOMER).count(),
        "recent",
        all.stream()
            .filter(t -> t.status == TicketStatus.PENDING)
            .limit(7)
            .map(ticketMapper::toView)
            .toList());
  }
}

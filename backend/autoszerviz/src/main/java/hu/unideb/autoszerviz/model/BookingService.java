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
public class BookingService {
  private static final List<TicketStatus> HOLDING =
      List.of(
          TicketStatus.PENDING,
          TicketStatus.ACCEPTED,
          TicketStatus.IN_PROGRESS,
          TicketStatus.WAITING_FOR_PARTS,
          TicketStatus.AWAITING_CUSTOMER);

  private final ModelAccessService access;
  private final TicketViewMapper ticketMapper;
  private final UserRepository users;
  private final VehicleRepository vehicles;
  private final ServiceTypeRepository services;
  private final TicketRepository tickets;

  public BookingService(
      ModelAccessService access,
      TicketViewMapper ticketMapper,
      UserRepository users,
      VehicleRepository vehicles,
      ServiceTypeRepository services,
      TicketRepository tickets) {
    this.access = access;
    this.ticketMapper = ticketMapper;
    this.users = users;
    this.vehicles = vehicles;
    this.services = services;
    this.tickets = tickets;
  }

  public List<Map<String, Object>> mine(HttpSession s) {
    AppUser u = access.currentUser(s);
    access.requireRole(u, Role.CUSTOMER);
    return tickets.findByCustomerIdOrderByStartAtDesc(u.id).stream()
        .sorted(
            Comparator.comparingLong(
                t -> Math.abs(Duration.between(LocalDateTime.now(), t.startAt).toMinutes())))
        .map(ticketMapper::toView)
        .toList();
  }

  public List<String> availability(Long serviceId, String date, HttpSession s) {
    AppUser u = access.currentUser(s);
    access.requireRole(u, Role.CUSTOMER);
    ServiceType svc = services.findById(serviceId).orElseThrow();
    LocalDate d = LocalDate.parse(date);
    if (d.getDayOfWeek().getValue() > 5) return List.of();
    long capacity =
        users.findAll().stream().filter(a -> a.active && a.role == Role.MECHANIC).count();
    List<String> slots = new ArrayList<>();
    for (LocalTime tm = LocalTime.of(8, 0);
        !tm.plusMinutes(svc.durationMinutes).isAfter(LocalTime.of(17, 0));
        tm = tm.plusMinutes(30)) {
      LocalDateTime start = d.atTime(tm), end = start.plusMinutes(svc.durationMinutes);
      if (start.isBefore(LocalDateTime.now().plusHours(1))) continue;
      long used =
          tickets.findAll().stream()
              .filter(
                  t ->
                      HOLDING.contains(t.status)
                          && t.startAt.isBefore(end)
                          && t.endAt.isAfter(start))
              .count();
      if (capacity > used) slots.add(tm.toString());
    }
    return slots;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> create(Map<String, Object> b, HttpSession s) {
    AppUser u = access.currentUser(s);
    access.requireRole(u, Role.CUSTOMER);
    Vehicle v =
        vehicles
            .findByIdAndOwnerIdAndActiveTrue(Long.valueOf(b.get("vehicleId").toString()), u.id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "VÄ‚Ë‡lassz sajÄ‚Ë‡t jÄ‚Ë‡rmÄąÂ±vet."));
    List<ServiceType> lockedServices = services.lockAllForBooking();
    ServiceType svc =
        lockedServices.stream()
            .filter(x -> x.id.equals(Long.valueOf(b.get("serviceId").toString())))
            .findFirst()
            .orElseThrow();
    LocalDateTime start = LocalDateTime.parse((String) b.get("startAt"));
    if (start.isBefore(LocalDateTime.now().plusHours(1))
        || start.getDayOfWeek().getValue() > 5
        || start.toLocalTime().isBefore(LocalTime.of(8, 0))
        || start.toLocalTime().plusMinutes(svc.durationMinutes).isAfter(LocalTime.of(17, 0))
        || start.getMinute() % 30 != 0)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "VÄ‚Ë‡lassz legalÄ‚Ë‡bb egy Ä‚Ĺ‚rÄ‚Ë‡val kÄ‚Â©sÄąâ€bbi, hÄ‚Â©tkÄ‚Â¶znapi,"
              + " nyitvatartÄ‚Ë‡si idÄąâ€n belÄ‚Ä˝li 30 perces kezdÄ‚Â©st.");
    LocalDateTime end = start.plusMinutes(svc.durationMinutes);
    long capacity =
        users.findAll().stream().filter(a -> a.active && a.role == Role.MECHANIC).count();
    if (capacity == 0
        || tickets.findAll().stream()
                .filter(t -> HOLDING.contains(t.status))
                .filter(t -> t.startAt.isBefore(end) && t.endAt.isAfter(start))
                .count()
            >= capacity)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Erre az idÄąâ€pontra nincs szabad szervizkapacitÄ‚Ë‡s.");
    Ticket t = new Ticket();
    t.customer = u;
    t.vehicle = v;
    t.serviceType = svc;
    t.startAt = start;
    t.endAt = end;
    t.description = (String) b.getOrDefault("description", "");
    tickets.save(t);
    return ticketMapper.toView(t);
  }

  @Transactional
  public void cancel(Long id, HttpSession s) {
    AppUser u = access.currentUser(s);
    Ticket t =
        tickets.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!u.id.equals(t.customer.id) || t.status != TicketStatus.PENDING)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Csak fÄ‚Ä˝ggÄąâ€ foglalÄ‚Ë‡s mondhatÄ‚Ĺ‚ le.");
    t.status = TicketStatus.CANCELLED;
  }
}

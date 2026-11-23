package hu.unideb.autoszerviz.controller;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final UserRepository users;
  private final BrandRepository brands;
  private final VehicleModelRepository models;
  private final VehicleRepository vehicles;
  private final ServiceTypeRepository services;
  private final TicketRepository tickets;
  private final BCryptPasswordEncoder encoder;
  private static final List<TicketStatus> BLOCKING =
      List.of(
          TicketStatus.ACCEPTED,
          TicketStatus.IN_PROGRESS,
          TicketStatus.WAITING_FOR_PARTS,
          TicketStatus.AWAITING_CUSTOMER);
  private static final List<TicketStatus> HOLDING =
      List.of(
          TicketStatus.PENDING,
          TicketStatus.ACCEPTED,
          TicketStatus.IN_PROGRESS,
          TicketStatus.WAITING_FOR_PARTS,
          TicketStatus.AWAITING_CUSTOMER);

  public ApiController(
      UserRepository u,
      BrandRepository b,
      VehicleModelRepository m,
      VehicleRepository v,
      ServiceTypeRepository s,
      TicketRepository t,
      BCryptPasswordEncoder encoder) {
    users = u;
    brands = b;
    models = m;
    vehicles = v;
    services = s;
    tickets = t;
    this.encoder = encoder;
  }

  private AppUser user(HttpSession session) {
    Object id = session.getAttribute("uid");
    if (id == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bejelentkezés szükséges");
    return users
        .findById((Long) id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private void role(AppUser u, Role... allowed) {
    if (Arrays.stream(allowed).noneMatch(r -> r == u.role))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nincs jogosultság");
  }

  private Map<String, Object> account(AppUser u) {
    return Map.of("id", u.id, "name", u.fullName, "email", u.email, "role", u.role.name());
  }

  private Map<String, Object> ticket(Ticket t) {
    Map<String, Object> x = new LinkedHashMap<>();
    x.put("id", t.id);
    x.put(
        "customer",
        Map.of("id", t.customer.id, "name", t.customer.fullName, "email", t.customer.email));
    x.put(
        "vehicle",
        Map.of(
            "id",
            t.vehicle.id,
            "brand",
            t.vehicle.brand.name,
            "origin",
            t.vehicle.brand.originRegion == null ? "" : t.vehicle.brand.originRegion,
            "model",
            t.vehicle.model.name,
            "year",
            t.vehicle.year,
            "fuel",
            t.vehicle.fuel == null ? "" : t.vehicle.fuel,
            "engine",
            t.vehicle.engine == null ? "" : t.vehicle.engine,
            "licensePlate",
            t.vehicle.licensePlate == null ? "" : t.vehicle.licensePlate));
    x.put(
        "service",
        Map.of(
            "id",
            t.serviceType.id,
            "name",
            t.serviceType.name,
            "durationMinutes",
            t.serviceType.durationMinutes));
    x.put(
        "mechanic",
        t.mechanic == null ? null : Map.of("id", t.mechanic.id, "name", t.mechanic.fullName));
    x.put("startAt", t.startAt.toString());
    x.put("endAt", t.endAt.toString());
    x.put("description", t.description == null ? "" : t.description);
    x.put("status", t.status.name());
    x.put("createdAt", t.createdAt.toString());
    return x;
  }

  // Authentication and profile endpoints.
  @PostMapping("/auth/register")
  public Map<String, Object> register(@RequestBody Map<String, String> body, HttpSession session) {
    String name = body.getOrDefault("name", "").trim(),
        email = body.getOrDefault("email", "").trim().toLowerCase(),
        pass = body.getOrDefault("password", "");
    if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || pass.length() < 8)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Adj meg nevet, érvényes e-mailt és legalább 8 karakteres jelszót.");
    if (users.findByEmailIgnoreCase(email).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Az e-mail cím már regisztrálva van.");
    AppUser u = new AppUser();
    u.fullName = name;
    u.email = email;
    u.passwordHash = encoder.encode(pass);
    u.role = Role.CUSTOMER;
    users.save(u);
    session.setAttribute("uid", u.id);
    return account(u);
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(@RequestBody Map<String, String> b, HttpSession s) {
    AppUser u =
        users
            .findByEmailIgnoreCase(b.getOrDefault("email", ""))
            .filter(
                a -> a.active && encoder.matches(b.getOrDefault("password", ""), a.passwordHash))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Hibás e-mail cím vagy jelszó."));
    s.setAttribute("uid", u.id);
    return account(u);
  }

  @PostMapping("/auth/logout")
  public Map<String, String> logout(HttpSession s) {
    s.invalidate();
    return Map.of("message", "Sikeres kijelentkezés");
  }

  @GetMapping("/auth/me")
  public Map<String, Object> me(HttpSession s) {
    return account(user(s));
  }

  @PutMapping("/profile")
  public Map<String, Object> profile(@RequestBody Map<String, String> b, HttpSession s) {
    AppUser u = user(s);
    String n = b.getOrDefault("name", u.fullName).trim();
    if (n.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A név nem lehet üres.");
    u.fullName = n;
    if (b.containsKey("password") && !b.get("password").isBlank()) {
      if (b.get("password").length() < 8)
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "A jelszó legalább 8 karakter legyen.");
      u.passwordHash = encoder.encode(b.get("password"));
    }
    return account(users.save(u));
  }

  // Catalog and customer vehicle endpoints.
  @GetMapping("/catalog")
  public Map<String, Object> catalog() {
    return Map.of(
        "brands",
        brands.findByActiveTrueOrderByName().stream()
            .map(
                b ->
                    Map.of(
                        "id",
                        b.id,
                        "name",
                        b.name,
                        "origin",
                        b.originRegion == null ? "" : b.originRegion))
            .toList(),
        "models",
        models.findAll().stream()
            .filter(m -> m.active)
            .map(
                m ->
                    Map.of(
                        "id",
                        m.id,
                        "brandId",
                        m.brand.id,
                        "name",
                        m.name,
                        "fuel",
                        m.fuelType == null ? "" : m.fuelType,
                        "engine",
                        m.engineType == null ? "" : m.engineType))
            .toList(),
        "services",
        services.findByActiveTrueOrderByName().stream()
            .map(
                a ->
                    Map.of(
                        "id",
                        a.id,
                        "name",
                        a.name,
                        "description",
                        a.description == null ? "" : a.description,
                        "durationMinutes",
                        a.durationMinutes))
            .toList());
  }

  @GetMapping("/vehicles")
  public List<Map<String, Object>> myVehicles(HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
    return vehicles.findByOwnerIdAndActiveTrueOrderByIdDesc(u.id).stream()
        .map(
            v ->
                Map.<String, Object>of(
                    "id",
                    v.id,
                    "brandId",
                    v.brand.id,
                    "brand",
                    v.brand.name,
                    "origin",
                    v.brand.originRegion == null ? "" : v.brand.originRegion,
                    "modelId",
                    v.model.id,
                    "model",
                    v.model.name,
                    "year",
                    v.year,
                    "fuel",
                    v.fuel == null ? "" : v.fuel,
                    "engine",
                    v.engine == null ? "" : v.engine,
                    "licensePlate",
                    v.licensePlate == null ? "" : v.licensePlate))
        .toList();
  }

  @PostMapping("/vehicles")
  public Map<String, Object> addVehicle(@RequestBody Map<String, Object> b, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
    Vehicle v = new Vehicle();
    v.owner = u;
    v.brand = brands.findById(Long.valueOf(b.get("brandId").toString())).orElseThrow();
    v.model = models.findById(Long.valueOf(b.get("modelId").toString())).orElseThrow();
    if (!v.model.brand.id.equals(v.brand.id))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A modell nem ehhez a márkához tartozik.");
    v.year = Integer.parseInt(b.get("year").toString());
    v.fuel = (String) b.get("fuel");
    v.engine = (String) b.getOrDefault("engine", "");
    v.licensePlate = (String) b.getOrDefault("licensePlate", "");
    vehicles.save(v);
    return Map.of("id", v.id);
  }

  @DeleteMapping("/vehicles/{id}")
  public void deleteVehicle(@PathVariable Long id, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
    Vehicle v =
        vehicles
            .findByIdAndOwnerIdAndActiveTrue(id, u.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    v.active = false;
    vehicles.save(v);
  }

  // Booking, ticket workflow, and mechanic actions.
  @GetMapping("/tickets/mine")
  public List<Map<String, Object>> mine(HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
    return tickets.findByCustomerIdOrderByStartAtDesc(u.id).stream()
        .sorted(
            Comparator.comparingLong(
                t -> Math.abs(Duration.between(LocalDateTime.now(), t.startAt).toMinutes())))
        .map(this::ticket)
        .toList();
  }

  @GetMapping("/availability")
  public List<String> availability(
      @RequestParam Long serviceId, @RequestParam String date, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
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

  @GetMapping("/tickets")
  public List<Map<String, Object>> list(
      @RequestParam(required = false) String status, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.MECHANIC, Role.ADMIN);
    List<Ticket> ts =
        status == null
            ? tickets.findAllByOrderByCreatedAtDesc()
            : tickets.findByStatusOrderByCreatedAtDesc(TicketStatus.valueOf(status));
    return (u.role == Role.ADMIN
            ? ts
            : ts.stream()
                .filter(
                    t ->
                        t.status == TicketStatus.PENDING
                            || u.id.equals(t.mechanic == null ? null : t.mechanic.id))
                .toList())
        .stream().map(this::ticket).toList();
  }

  @GetMapping("/tickets/{id}")
  public Map<String, Object> detail(@PathVariable Long id, HttpSession s) {
    AppUser u = user(s);
    Ticket t =
        tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (u.role == Role.CUSTOMER && !u.id.equals(t.customer.id))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (u.role == Role.MECHANIC
        && t.status != TicketStatus.PENDING
        && t.mechanic != null
        && !u.id.equals(t.mechanic.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return ticket(t);
  }

  @PostMapping("/tickets")
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> create(@RequestBody Map<String, Object> b, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
    Vehicle v =
        vehicles
            .findByIdAndOwnerIdAndActiveTrue(Long.valueOf(b.get("vehicleId").toString()), u.id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Válassz saját járművet."));
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
          "Válassz legalább egy órával későbbi, hétköznapi, nyitvatartási időn belüli 30 perces"
              + " kezdést.");
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
          HttpStatus.CONFLICT, "Erre az időpontra nincs szabad szervizkapacitás.");
    Ticket t = new Ticket();
    t.customer = u;
    t.vehicle = v;
    t.serviceType = svc;
    t.startAt = start;
    t.endAt = end;
    t.description = (String) b.getOrDefault("description", "");
    tickets.save(t);
    return ticket(t);
  }

  @DeleteMapping("/tickets/{id}")
  @Transactional
  public void cancel(@PathVariable Long id, HttpSession s) {
    AppUser u = user(s);
    Ticket t =
        tickets.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!u.id.equals(t.customer.id) || t.status != TicketStatus.PENDING)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Csak függő foglalás mondható le.");
    t.status = TicketStatus.CANCELLED;
  }

  @PutMapping("/tickets/{id}/status")
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> status(
      @PathVariable Long id, @RequestBody Map<String, String> b, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.MECHANIC, Role.ADMIN);
    services.lockAllForBooking();
    Ticket t =
        tickets.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    TicketStatus next = TicketStatus.valueOf(b.get("status"));
    if (next == TicketStatus.ACCEPTED) {
      if (t.status != TicketStatus.PENDING
          || tickets.mechanicOverlap(u.id, BLOCKING, t.startAt, t.endAt))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "A ticketet már elfogadták, vagy ütközik a saját időpontoddal.");
      t.mechanic = u;
      t.status = next;
    } else {
      if (t.mechanic == null || !t.mechanic.id.equals(u.id))
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Csak a hozzárendelt szerelő módosíthatja ezt a munkát.");
      boolean ok =
          switch (t.status) {
            case ACCEPTED -> next == TicketStatus.IN_PROGRESS || next == TicketStatus.PENDING;
            case IN_PROGRESS ->
                next == TicketStatus.WAITING_FOR_PARTS || next == TicketStatus.AWAITING_CUSTOMER;
            case WAITING_FOR_PARTS -> next == TicketStatus.IN_PROGRESS;
            case COMPLAINT -> next == TicketStatus.IN_PROGRESS;
            default -> false;
          };
      if (!ok) throw new ResponseStatusException(HttpStatus.CONFLICT, "Érvénytelen státuszváltás.");
      t.status = next;
      if (next == TicketStatus.PENDING) t.mechanic = null;
      if (next == TicketStatus.AWAITING_CUSTOMER) t.closedAt = LocalDateTime.now();
    }
    return ticket(t);
  }

  @PutMapping("/tickets/{id}/confirm")
  @Transactional
  public Map<String, Object> confirm(
      @PathVariable Long id, @RequestBody Map<String, Boolean> b, HttpSession s) {
    AppUser u = user(s);
    role(u, Role.CUSTOMER);
    Ticket t = tickets.lockById(id).orElseThrow();
    if (!u.id.equals(t.customer.id) || t.status != TicketStatus.AWAITING_CUSTOMER)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ez a munka nem vár visszaigazolásra.");
    t.status =
        Boolean.TRUE.equals(b.get("approved")) ? TicketStatus.RESOLVED : TicketStatus.COMPLAINT;
    return ticket(t);
  }

  // Role-specific dashboard summaries.
  @GetMapping("/dashboard")
  public Map<String, Object> dashboard(HttpSession s) {
    AppUser u = user(s);
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
              .map(this::ticket)
              .toList());
    }
    role(u, Role.MECHANIC, Role.ADMIN);
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
            .map(this::ticket)
            .toList());
  }
}

package hu.unideb.autoszerviz.model;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import java.time.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DemoData {
  @Bean
  CommandLineRunner seed(UserRepository users, BrandRepository brands, VehicleModelRepository models, VehicleRepository vehicles, ServiceTypeRepository services, TicketRepository tickets, BCryptPasswordEncoder encoder) {
    return args -> {
      User customer = users.findByEmailIgnoreCase("ugyfel@auto.hu").orElseGet(User::new);
      customer.fullName = "Teszt Ügyfél";
      customer.email = "ugyfel@auto.hu";
      customer.role = Role.CUSTOMER;
      customer.active = true;
      if (customer.passwordHash == null || !customer.passwordHash.startsWith("$2"))
        customer.passwordHash = encoder.encode("ugyfel");
      users.save(customer);
      User mechanic = users.findByEmailIgnoreCase("admin").orElseGet(User::new);
      mechanic.fullName = "Autószerelő Admin";
      mechanic.email = "admin";
      mechanic.role = Role.MECHANIC;
      mechanic.active = true;
      if (mechanic.passwordHash == null || !mechanic.passwordHash.startsWith("$2"))
        mechanic.passwordHash = encoder.encode("admin");
      users.save(mechanic);
      if (brands.count() == 0) {
        String[][] data = {
          {"Audi", "Németország"},
          {"BMW", "Németország"},
          {"Ford", "Egyesült Királyság"},
          {"Honda", "Japán"},
          {"Mercedes-Benz", "Németország"},
          {"Peugeot", "Franciaország"},
          {"Renault", "Franciaország"},
          {"Suzuki", "Japán"},
          {"Toyota", "Japán"},
          {"Volkswagen", "Németország"}
        };
        for (String[] x : data) {
          Brand b = new Brand();
          b.name = x[0];
          b.originRegion = x[1];
          brands.save(b);
        }
      }
      if (models.count() == 0)
        for (Brand b : brands.findAll()) {
          String model =
              switch (b.name) {
                case "Toyota" -> "Corolla";
                case "Volkswagen" -> "Golf";
                case "BMW" -> "3 Series";
                case "Audi" -> "A4";
                case "Ford" -> "Focus";
                case "Honda" -> "Civic";
                case "Suzuki" -> "Swift";
                case "Peugeot" -> "308";
                case "Renault" -> "Clio";
                default -> "C-Class";
              };
          VehicleModel m = new VehicleModel();
          m.brand = b;
          m.name = model;
          m.fuelType = "PETROL";
          models.save(m);
        }
      if (services.count() == 0) {
        String[][] data = {
          {"Olajcsere", "Olaj és szűrők cseréje", "45"},
          {"Rutinvizsgálat", "Általános állapotfelmérés", "60"},
          {"Fékrendszer szerviz", "Fékek ellenőrzése és javítása", "90"},
          {"Műszaki vizsga felkészítés", "Átvizsgálás és felkészítés", "120"},
          {"Klímaszerviz", "Klíma ellenőrzés és töltés", "60"},
          {"Futómű beállítás", "Futómű geometria ellenőrzés", "60"}
        };
        for (String[] x : data) {
          ServiceType s = new ServiceType();
          s.name = x[0];
          s.description = x[1];
          s.durationMinutes = Integer.parseInt(x[2]);
          services.save(s);
        }
      }
      if (vehicles.findByOwnerIdAndActiveTrueOrderByIdDesc(customer.id).isEmpty()) {
        Brand brand =
            brands.findAll().stream()
                .filter(b -> b.name.equals("Volkswagen"))
                .findFirst()
                .orElse(brands.findAll().get(0));
        VehicleModel model = models.findByBrandIdAndActiveTrueOrderByName(brand.id).get(0);
        Vehicle v = new Vehicle();
        v.owner = customer;
        v.brand = brand;
        v.model = model;
        v.year = 2018;
        v.fuel = "Dízel";
        v.engine = "2.0 TDI";
        v.licensePlate = "ABC-123";
        vehicles.save(v);
      }
      if (tickets.count() == 0) {
        Vehicle v = vehicles.findByOwnerIdAndActiveTrueOrderByIdDesc(customer.id).get(0);
        ServiceType service =
            services.findByActiveTrueOrderByName().stream()
                .filter(x -> x.name.equals("Olajcsere"))
                .findFirst()
                .orElse(services.findByActiveTrueOrderByName().get(0));
        LocalDate day = LocalDate.now().plusDays(2);
        while (day.getDayOfWeek().getValue() > 5) day = day.plusDays(1);
        Ticket t = new Ticket();
        t.customer = customer;
        t.vehicle = v;
        t.serviceType = service;
        t.startAt = day.atTime(10, 0);
        t.endAt = t.startAt.plusMinutes(service.durationMinutes);
        t.description = "Olajcsere és általános átvizsgálás";
        tickets.save(t);
      }
    };
  }
}

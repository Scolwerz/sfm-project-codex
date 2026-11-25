package hu.unideb.autoszerviz.model;

import hu.unideb.autoszerviz.entity.*;
import hu.unideb.autoszerviz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final BCryptPasswordEncoder encoder;
    private final ModelAccessService access;

    public AuthService(UserRepository users, BCryptPasswordEncoder encoder, ModelAccessService access) {
        this.users = users;
        this.encoder = encoder;
        this.access = access;
    }

    private Map<String, Object> account(AppUser u) {
        return Map.of("id", u.id, "name", u.fullName, "email", u.email, "role", u.role.name());
    }

    public Map<String, Object> register(Map<String, String> body, HttpSession session) {
        String name = body.getOrDefault("name", "").trim(),
            email = body.getOrDefault("email", "").trim().toLowerCase(),
            pass = body.getOrDefault("password", "");
        if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || pass.length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adj meg nevet, érvényes e-mailt és egy legalább 8 karakteres jelszót.");
        if (users.findByEmailIgnoreCase(email).isPresent())
            throw new ResponseStatusException( HttpStatus.CONFLICT, "Az e-mail cím már regisztrálva van.");

        AppUser u = new AppUser();
        u.fullName = name;
        u.email = email;
        u.passwordHash = encoder.encode(pass);
        u.role = Role.CUSTOMER;
        users.save(u);
        session.setAttribute("uid", u.id);
        return account(u);
    }

        public Map<String, Object> login(Map<String, String> b, HttpSession s) {
        AppUser u = users.findByEmailIgnoreCase(b.getOrDefault("email", ""))
            .filter(a -> a.active && encoder.matches(b.getOrDefault("password", ""), a.passwordHash))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Hibás e-mail cím vagy jelszó."));

        s.setAttribute("uid", u.id);
        return account(u);
    }

    public Map<String, String> logout(HttpSession s) {
        s.invalidate();
        return Map.of("message", "Sikeres kijelentkezés");
    }

    public Map<String, Object> me(HttpSession s) {
        return account(access.currentUser(s));
    }

    public Map<String, Object> profile(Map<String, String> b, HttpSession s) {
        AppUser u = access.currentUser(s);
        String n = b.getOrDefault("name", u.fullName).trim();
        if (n.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A név nem lehet üres.");
        u.fullName = n;
        if (b.containsKey("password") && !b.get("password").isBlank()) {
            if (b.get("password").length() < 8)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A jelszó legalább 8 karakter legyen.");
            u.passwordHash = encoder.encode(b.get("password"));
        }
        return account(users.save(u));
    }
}

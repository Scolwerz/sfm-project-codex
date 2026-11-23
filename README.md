# Autószerviz időpontfoglaló és munkakezelő rendszer

Az alkalmazás ügyféloldali időpontfoglalást és szerelői ticketkezelést biztosít. A frontend sima HTML, CSS és JavaScript, az API Spring Boot, az adatbázis MySQL.

## Indítás Dockerrel

A projekt gyökérkönyvtárából:

```bash
docker compose up --build
```

- Webalkalmazás: <http://localhost:3000>
- Backend API: <http://localhost:8080/api>
- MySQL: `localhost:3306`

Demó belépési adatok:

| Szerepkör | Felhasználónév / e-mail | Jelszó |
|---|---|---|
| Ügyfél | `ugyfel@auto.hu` | `ugyfel` |
| Szerelő | `admin` | `admin` |

A demó jelszavak az adatbázisban BCrypt hashként szerepelnek. Az első induláskor a backend létrehozza a táblákat és néhány katalógus-, felhasználó-, autó- és ticketadatot. A `database/init.sql` az adatbázist UTF-8 karakterkészlettel hozza létre; a táblákat a Hibernate kezeli.

## Fő folyamatok

- Ügyfélregisztráció, bejelentkezés és profiladatok módosítása.
- Mentett autók kezelése, szervizkatalógus és szolgáltatási időtartamok.
- Hétköznapi, 08:00–17:00 közötti, 30 perces kezdési időpontok szerveroldali kapacitásellenőrzéssel.
- Függő foglalás lemondása, szerelői ticketelfogadás és munkastátuszok kezelése.
- Ügyfél-visszaigazolás, ticketkeresés, dashboardok és szerelői időpontlista.

Foglalás létrehozásakor a backend tranzakcióban zárolja a szolgáltatáskatalógust, így a párhuzamos foglalások kapacitásellenőrzése sorosított. A szerelői elfogadás ticketzárolással akadályozza meg a kettős hozzárendelést, az átfedő munkákat pedig szerelőnként ellenőrzi.

Az alkatrészkészlet kezelőfelülete nincs implementálva.

## Leállítás

```bash
docker compose down
```

Az adatbázis-kötetet az alábbi paranccsal lehet törölni (az adatbázis tartalma is törlődik):

```bash
docker compose down -v
```

## Projektstruktúra

- `frontend/html`, `frontend/css`, `frontend/js`: kliensfelület
- `backend/autoszerviz/src/main/java/hu/unideb/autoszerviz/controller`: REST API
- `backend/autoszerviz/src/main/java/hu/unideb/autoszerviz/entity`: JPA entitások és státuszok
- `backend/autoszerviz/src/main/java/hu/unideb/autoszerviz/repository`: Spring Data repository-k
- `backend/autoszerviz/src/main/java/hu/unideb/autoszerviz/model`: konfiguráció és induló adatok

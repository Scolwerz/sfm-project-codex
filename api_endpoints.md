# API endpoints

## `POST /api/auth/register`
**Request JSON**
``` json
{
  "name": "Teszt Név",
  "email": "te@example.com",
  "password": "Password123"
}
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "name": "Teszt Név",
  "email": "te@example.com",
  "role": "CUSTOMER"
}
```

 - a név nem lehet üres  
- az e-mailnek megfelelő formátumúnak kell lennie (kisbetűs @ .)
- a jelszó legalább 8 karakteres
- az új felhasználó default `CUSTOMER`  
- sikeres regisztrációkor `uid` sessionbe mentés - automatikusan bejelentkezés (vagy visszadobjon /login-ra?)

**Expections**  
- `400 Bad Request`: hiányzó név, hibás e-mail, 8 karakternél rövidebb jelszó  
- `409 Conflict`: az e-mail már regisztrálva van


## `POST /api/auth/login`
**Request JSON**
``` json
{
  "email": "te@example.com",
  "password": "Password123"
}
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "name": "Teszt Név",
  "email": "te@example.com",
  "role": "CUSTOMER"
}
```

- bejelentkezéskor a `user_id` a session `uid` attribútumába kerül

**Expections**  
- `401 Unauthorized`: hibás e-mail/jelszó,


## `POST /api/auth/logout`
**Request JSON:**
``` json
{
}
```

**Response JSON -- 200
``` json
{
  "message": "Sikeres kijelentkezés"
}
```


## `GET /api/auth/me`
**Request JSON:**
``` json
{
}
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "name": "Teszt Név",
  "email": "te@example.com",
  "role": "CUSTOMER"
}
```

**Expections**
-`401 Unauthorized`: nincs érvényes session


## `PUT /api/profile`
**Request JSON**
``` json
{
  "name": "Új Név",
  "password": "NewPassword123"
}
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "name": "Új Név",
  "email": "te@example.com",
  "role": "CUSTOMER"
}
```

- A `name` és a `password`sem kötelező, legalább az egyiknek szerepelnie kell

**Expections**  
- `400 Bad Request`: üres név vagy rövid jelszó  
- `401 Unauthorized`: nincs érvényes session


## `GET /api/catalog`
**Request JSON:**
``` json
{
}
```

**Response JSON -- 200**
``` json
{
  "brands": [
    {
      "id": 1,
      "name": "Audi",
      "origin": "Németország"
    }
  ],
  
  "models": [
    {
      "id": 1,
      "brandId": 1,
      "name": "A4",
      "fuel": "Benzin",
      "engine": "2.0 TFSI"
    }
  ],
  
  "services": [
    {
      "id": 1,
      "name": "Olajcsere",
      "description": "Olaj és szűrők cseréje",
      "durationMinutes": 45
    }
  ]
}
```

- Egyesített katalógus 3 tömbben, az adatbázisban fix az autószervízhez kapcsolódó elemekről
 - A `fuel`, `engine`, `origin`, `description` mezők üres szövegek is lehetnek, ha az adatbázisban nincs érték  


## `GET /api/vehicles`
**Request JSON:**
``` json
{
}
```

**Response JSON -- 200**
``` json
[
  {
    "id": 1,
    "brandId": 10,
    "brand": "Volkswagen",
    "origin": "Németország",
    "modelId": 10,
    "model": "Golf",
    "year": 2018,
    "fuel": "Dízel",
    "engine": "2.0 TDI",
    "licensePlate": "ABC-123"
  }
]
```

- Csak `CUSTOMER` szerepkörű felhasználó kérheti le 
- Csak az aktuális felhasználó aktív járművei jelennek meg  
- `id` szerint csökkenő sorrend  *(legutóbb hozzáadott)*
- A `fuel`, `engine`, `licensePlate`, `origin` értéke üres szöveg lehet.

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve
`403 Forbidden`: nem `CUSTOMER`


## `POST /api/vehicles`
**Request JSON**
``` json
{
  "brandId": 10,
  "modelId": 10,
  "year": 2018,
  "fuel": "Dízel",
  "engine": "2.0 TDI",
  "licensePlate": "ABC-123"
}
```

**Response JSON -- 200**
``` json
{
  "id": 3
}
```

- Csak `CUSTOMER` adhat hozzá  
- Az `engine` és `licensePlate` nem kötelező, üres szöveg is lehet  
- A jármű tulajdonosa automatikusan a bejelentkezett felhasználó

**Exceptions** 
`400 Bad Request`: az autó modell nem a megadott márkához tartozik  
`401 Unauthorized`: nincs bejelentkezve
`403 Forbidden`: nem `CUSTOMER`  
- Nem létező márka vagy modell


##  `DELETE /api/vehicles/{id}`
**Request JSON:**
``` json
{
}
```

**Response JSON:**
``` json
{
}  
```

- Csak `CUSTOMER` törölhet  
- Csak a bejelentkezett felhasználó aktív járműve törölhető  
 - Nem tényleges törlés, az `active` mező `false` értéket kap

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve  
`403 Forbidden`: nem `CUSTOMER`
`404 Not Found`: a jármű nem található a felhasználó saját aktív járművei között.


## `GET /api/tickets/mine`
**Request JSON:**
``` json
{
}  
```

**Response JSON -- 200**
``` json
[
  {
    "id": 1,
    "customer": {
      "id": 1,
      "name": "Teszt Név",
      "email": "te@example.com"
    },
    
    "vehicle": {
      "id": 1,
      "brand": "Volkswagen",
      "origin": "Németország",
      "model": "Golf",
      "year": 2018,
      "fuel": "Dízel",
      "engine": "2.0 TDI",
      "licensePlate": "ABC-123"
    },
    
    "service": {
      "id": 1,
      "name": "Olajcsere",
      "durationMinutes": 45
    },
    
    "mechanic": null,
    "startAt": "2026-11-25T10:00",
    "endAt": "2026-11-25T10:45",
    "description": "Olajcsere és szűrők cseréje",
    "status": "PENDING",
    "createdAt": "2026-11-25T10:00:00"
  }
]
```

 - Csak `CUSTOMER` kérdezheti le  
- Csak az aktuális felhasználó ticketeit adja vissza  
- A legutóbbi létrehozott ticket van legfelül  
- A `mechanic` értéke `null`, ha még nincs szerelő hozzárendelve

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve
`403 Forbidden`: nem `CUSTOMER`


## `GET /api/availability?serviceId={serviceId}&date={date}`
**Példa:** `GET /api/availability?serviceId=1&date=2026-11-25`

**Request JSON:**
``` json
{
}  
```

**Response JSON -- 200**
``` json
[
  "09:30",
  "10:00",
  "10:30",
  "11:00"
]
```

- Csak `CUSTOMER` kérdezheti le 
- Hétvégére üres lista érkezik  
- A vizsgált időpontok 08:00-tól indulnak, 30 percenként és a szolgáltatás időtartamának 17:00-ig bele kell fejeződnie  
- Csak legalább egy órával a lekérdezés pillanata utáni kezdés adható meeg  
- A kapacitást az aktív`MECHANIC` felhasználók száma határozza meg

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve  
`403 Forbidden`: nem `CUSTOMER`  


## `POST /api/tickets`
**Request JSON**
``` json
{
  "vehicleId": 1,
  "serviceId": 1,
  "startAt": "2026-11-25T10:00",
  "description": "Olajcsere és általános átvizsgálás"
}
```

**Response JSON -- 200**
``` json
{
  "id": 2,
  "customer": {
    "id": 1,
    "name": "Teszt Név",
    "email": "te@example.com"
  },
  
  "vehicle": {
    "id": 1,
    "brand": "Volkswagen",
    "origin": "Németország",
    "model": "Golf",
    "year": 2018,
    "fuel": "Dízel",
    "engine": "2.0 TDI",
    "licensePlate": "ABC-123"
  },
  
  "service": {
    "id": 1,
    "name": "Olajcsere",
    "durationMinutes": 45
  },
  
  "mechanic": null,
  "startAt": "2026-11-25T10:00",
  "endAt": "2026-11-25T10:45",
  "description": "Olajcsere és általános átvizsgálás",
  "status": "PENDING",
  "createdAt": "2026-11-25T10:00:00"
}
```

- Csak `CUSTOMER` foglalhat új időpontot  
- A `vehicleId` csak a felhasználó saját, aktív járműve lehet  
- A `serviceId` létező szolgáltatás kell legyen  
 - A`startAt` formátuma `2026-11-25T10:00` *(LocalDateTime)*  
- A`description` nem kötelező  
- Az`endAt` a szolgáltatás `durationMinutes` értéke alapján automatikus  
- Az ticket alapértelmezett státusza `PENDING`, szerelője `null`  
- Foglalás csak hétköznap, 08:00 - 17:00 munkaidőn belül, 30 percenként, legalább egy órával későbbre foglalható  
- A foglalás csak akkor sikeres, ha a szerelői kapacitás nem 0 az időintervallumban

**Exceptions**  
`400 Bad Request`: a jármű nem a bejelentkezett felhasználó aktív járműve, vagy a kezdés időpontja invalid  
`401 Unauthorized`: nincs bejelentkezve 
`403 Forbidden`: nem `CUSTOMER`
`409 Conflict`: nincs szabad  szerelő

## `DELETE /api/tickets/{id}`
**Request JSON:**  
``` json
{
}  
```

**Response JSON:**  
``` json
{
}  
```

- A felhasználó csak a saját ticketét mondhatja le  
- Csak `PENDING` státuszú ticket mondható le  
- Nincs tényleges törlés, csak a ticket státusza vált`CANCELLED`-re  

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve 
`404 Not Found`: a ticket nem található
`409 Conflict`: nem a felhasználó saját tickete, vagy a státusza nem `PENDING`


## `GET /api/tickets`
**Példa:** `GET /api/tickets` vagy querry opció `GET /api/tickets?status=PENDING`

**Request JSON:**
``` json
{
}  
```

**Response JSON -- 200**
``` json
[
  {
    "id": 1,
    "customer": {
      "id": 1,
      "name": "Teszt Ügyfél",
      "email": "ugyfel@auto.hu"
    },
    
    "vehicle": {
      "id": 1,
      "brand": "Volkswagen",
      "origin": "Németország",
      "model": "Golf",
      "year": 2018,
      "fuel": "Dízel",
      "engine": "2.0 TDI",
      "licensePlate": "ABC-123"
    },
    
    "service": {
      "id": 1,
      "name": "Olajcsere",
      "durationMinutes": 45
    },
    
    "mechanic": null,
    "startAt": "2026-11-25T10:00",
    "endAt": "2026-11-25T10:45",
    "description": "Olajcsere",
    "status": "PENDING",
    "createdAt": "2026-11-25T10:00:00"
  }
]
```

- Csak `MECHANIC` és `ADMIN` kérheti le  
- A `status` szűrő nem kötelező, ha van ez szerint szűr a listában  
- Az `ADMIN` az összes találatot megkapja  
- A `MECHANIC` csak a `PENDING` státuszú és a már saját magához rendelt ticketeket látja  
- A legutóbb létrehozott legfelül *(létrehozás szerint csökkenő)*

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve  
`403 Forbidden`: nem `MECHANIC` vagy `ADMIN`  


## `GET /api/tickets/{id}`
**Request JSON:**
``` json
{
}  
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "customer": {
    "id": 1,
    "name": "Teszt Ügyfél",
    "email": "ugyfel@auto.hu"
  },
  
  "vehicle": {
    "id": 1,
    "brand": "Volkswagen",
    "origin": "Németország",
    "model": "Golf",
    "year": 2018,
    "fuel": "Dízel",
    "engine": "2.0 TDI",
    "licensePlate": "ABC-123"
  },
  
  "service": {
    "id": 1,
    "name": "Olajcsere",
    "durationMinutes": 45
  },
  
  "mechanic": null,
  "startAt": "2026-11-25T10:00",
  "endAt": "2026-11-25T10:45",
  "description": "Olajcsere",
  "status": "PENDING",
  "createdAt": "2026-11-25T10:00:00"
}
```

- `CUSTOMER`: csak a saját ticketjeit kérdezheti le  
- `MECHANIC`: a `PENDING`státuszú ricketeket és a saját munkáit kérdezheti le  
- `ADMIN`: mindent lát  

**Exceptions**
`401 Unauthorized`: nincs bejelentkezve
`403 Forbidden`: nem jogosult az adott ticket elérésére
`404 Not Found`: a ticket nem található


## `PUT /api/tickets/{id}/status`
**Request JSON**
``` json
{
  "status": "ACCEPTED"
}
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "customer": {
    "id": 1,
    "name": "Teszt Ügyfél",
    "email": "ugyfel@auto.hu"
  },
  
  "vehicle": {
    "id": 1,
    "brand": "Volkswagen",
    "origin": "Németország",
    "model": "Golf",
    "year": 2018,
    "fuel": "Dízel",
    "engine": "2.0 TDI",
    "licensePlate": "ABC-123"
  },
  
  "service": {
    "id": 1,
    "name": "Olajcsere",
    "durationMinutes": 45
  },
  
  "mechanic": {
    "id": 2,
    "name": "Teszt Szerelő"
  },
  
  "startAt": "2026-11-25T10:00",
  "endAt": "2026-11-25T10:45",
  "description": "Olajcsere",
  "status": "ACCEPTED",
  "createdAt": "2026-11-25T10:00:00"
}
```

- Csak `MECHANIC` vagy `ADMIN` használhatja  
 - A kérés `status` mezője a kívánt új státusz  
   - `ACCEPTED` állításkor a bejelentkezett felhasználó lesz a
  tickethez rendelt szerelő  
    - Csak `PENDING` státuszú ticket fogadható el, és a szerelő saját időpontjaival nem lehet ütközés  
    - Más státuszváltást csak a tickethez rendelt `MECHANIC` végezhet:
`PENDING`, `ACCEPTED`, `IN_PROGRESS`, `WAITING_FOR_PARTS`, `AWAITING_CUSTOMER`, `RESOLVED`, `CANCELLED`, `COMPLAINT`

**Exceptions**
`401 Unauthorized`: nincs bejelentkezve
`403 Forbidden`: nem `ADMIN`/`MECHANIC`, vagy a ticket nem a szerelőhöz tartozik
`404 Not Found`: a ticket nem található
`409 Conflict`: már `ACCEPTED`ticket, vagy szerelői időpontütközés vagy érvénytelen státuszváltás


## `PUT /api/tickets/{id}/confirm`
**Request JSON**
``` json
{
  "approved": true
}
```

**Response JSON -- 200**
``` json
{
  "id": 1,
  "customer": {
    "id": 1,
    "name": "Teszt Ügyfél",
    "email": "ugyfel@auto.hu"
  },
  
  "vehicle": {
    "id": 1,
    "brand": "Volkswagen",
    "origin": "Németország",
    "model": "Golf",
    "year": 2018,
    "fuel": "Dízel",
    "engine": "2.0 TDI",
    "licensePlate": "ABC-123"
  },
  
  "service": {
    "id": 1,
    "name": "Olajcsere",
    "durationMinutes": 45
  },
  
  "mechanic": {
    "id": 2,
    "name": "Teszt Szerelő"
  },
  
  "startAt": "2026-11-25T10:00",
  "endAt": "2026-11-25T10:45",
  "description": "Olajcsere",
  "status": "RESOLVED",
  "createdAt": "2026-11-25T10:00:00"
}
```

- Csak `CUSTOMER` küldhet visszajelzést  
- Csak a ticket tulajdonosa igazolhatja azt vissza  
- Csak`AWAITING_CUSTOMER` státuszú ticket igazolható vissza  
- `approved: true` esetén státusz->`RESOLVED`  
- `approved: false`esetén státusz->`COMPLAINT`

**Exceptions**
`401 Unauthorized`: nincs bejelentkezve
`403 Forbidden`: nem `CUSTOMER`
`409 Conflict`: a ticket nem a felhasználóé, vagy nem `AWAITING_CUSTOMER` státuszú  
- A nem megadott `approved` mező a kódban alapértelmezetten `false
- 

## `GET /api/dashboard`
**Request JSON:**
``` json
{
}  
```

**Response JSON -- CUSTOMER, 200**
``` json
{
  "vehicles": 2,
  "active": 1,
  "closed": 5,
  
  "recent": [
    {
      "id": 1,
      "customer": {
        "id": 1,
        "name": "Teszt Ügyfél",
        "email": "ugyfel@auto.hu"
      },
      
      "vehicle": {
        "id": 1,
        "brand": "Volkswagen",
        "origin": "Németország",
        "model": "Golf",
        "year": 2018,
        "fuel": "Dízel",
        "engine": "2.0 TDI",
        "licensePlate": "ABC-123"
      },
      
      "service": {
        "id": 1,
        "name": "Olajcsere",
        "durationMinutes": 45
      },
      
      "mechanic": null,
      "startAt": "2026-11-25T10:00",
      "endAt": "2026-11-25T10:45",
      "description": "Olajcsere",
      "status": "PENDING",
      "createdAt": "2026-11-25T10:00:00"
    }
  ]

}
```

**Response JSON -- MECHANIC / ADMIN, 200**
``` json
{
  "incoming": 5,
  "active": 3,
  "waiting": 2,
  
  "recent": [
    {
      "id": 1,
      "customer": {
        "id": 1,
        "name": "Teszt Ügyfél",
        "email": "ugyfel@auto.hu"
      },
      
      "vehicle": {
        "id": 1,
        "brand": "Volkswagen",
        "origin": "Németország",
        "model": "Golf",
        "year": 2018,
        "fuel": "Dízel",
        "engine": "2.0 TDI",
        "licensePlate": "ABC-123"
      },
      
      "service": {
        "id": 1,
        "name": "Olajcsere",
        "durationMinutes": 45
      },
      
      "mechanic": null,
      "startAt": "2026-11-25T10:00",
      "endAt": "2026-11-25T10:45",
      "description": "Olajcsere",
      "status": "PENDING",
      "createdAt": "2026-11-25T10:00:00"
    }
  ]
  
}
```

 - Összevont válasz a dashbord apok felépítéséhez  
- `CUSTOMER`:
    - `vehicles`: saját aktív járművek száma  
  - `active`: saját `ACCEPTED` munkák száma  
   - `closed`: saját `RESOLVED` munkák száma  
  - `recent`: a legutóbbi 5 saját ticket létrehozási idő szerint csökkenő sorrendben  
- `MECHANIC` és `ADMIN`: *(`ADMIN` az összes, a `MECHANIC` az összes `PENDING` és a saját magához rendelt ticketeket látja)*
  - `incoming`: `PENDING` ticketek száma  
  - `active`: `ACCEPTED`, `IN_PROGRESS`, `WAITING_FOR_PARTS` ticketek száma  
  - `waiting`: `AWAITING_CUSTOMER` ticketek száma  
  - `recent`: legutóbbi 5-7 `PENDING` ticket

**Exceptions**  
`401 Unauthorized`: nincs bejelentkezve






------------------------------------------------------------------------
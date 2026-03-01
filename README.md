# How to test (manual verification)

> Nota: la API arranca en `http://localhost:8080`.  
> Para operaciones protegidas se usa **Basic Auth**:
> - **Usuario:** `admin`
> - **Contraseña:** `admin`

---

## 1) Arranque del proyecto

**Opción A — IDE (recomendado):**
- Importar el proyecto como proyecto Maven en IntelliJ IDEA o Eclipse.
- Ejecutar la clase `Application.java`.

**Opción B — Terminal:**
```bash
./mvnw clean test
./mvnw spring-boot:run
```

Una vez arrancado, la consola de H2 está disponible en:  
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`, user: `sa`, pass: `password`)

---

## 2) Spring Security

### 2.1) GET público — no requiere autenticación

```bash
curl -i http://localhost:8080/api/brands
```

**Resultado esperado:** `HTTP/1.1 200 OK` con el listado de marcas en JSON.

---

### 2.2) POST protegido — sin credenciales

```bash
curl -i -X POST http://localhost:8080/api/brands \
  -H "Content-Type: application/json" \
  -d '{"name":"Seat","description":"Spanish manufacturer"}'
```

**Resultado esperado:** `HTTP/1.1 401 Unauthorized`

---

### 2.3) POST protegido — con credenciales correctas

```bash
curl -i -X POST http://localhost:8080/api/brands \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{"name":"Seat","description":"Spanish manufacturer"}'
```

**Resultado esperado:** `HTTP/1.1 201 Created` con la marca creada en JSON.

---

### 2.4) DELETE protegido — con credenciales correctas

```bash
curl -i -X DELETE http://localhost:8080/api/brands/1 \
  -u admin:admin
```

**Resultado esperado:** `HTTP/1.1 204 No Content`

---

## 3) Endpoints de Sales

### 3.1) GET /api/sales — paginación (10 elementos por página)

**Primera página (por defecto, sin parámetro):**
```bash
curl -i http://localhost:8080/api/sales
```

**Primera página (explícita):**
```bash
curl -i "http://localhost:8080/api/sales?page=0"
```

**Página 2:**
```bash
curl -i "http://localhost:8080/api/sales?page=1"
```

**Página fuera de rango:**
```bash
curl -i "http://localhost:8080/api/sales?page=999"
```

**Resultado esperado:** `HTTP/1.1 200 OK` con array de exactamente 10 elementos (o array vacío `[]` si la página está fuera de rango).

---

### 3.2) GET /api/sales/brands/{brandId} — ventas por marca

Ventas de Renault (ID 1):
```bash
curl -i http://localhost:8080/api/sales/brands/1
```

Ventas de Opel (ID 2):
```bash
curl -i http://localhost:8080/api/sales/brands/2
```

Ventas de Volkswagen (ID 3):
```bash
curl -i http://localhost:8080/api/sales/brands/3
```

**Resultado esperado:** `HTTP/1.1 200 OK` con la lista de ventas de esa marca. Verificar que todos los objetos del array tienen `brand.id` igual al solicitado.

---

### 3.3) GET /api/sales/vehicles/{vehicleId} — ventas por vehículo

Ventas del Renault Clio (ID 1):
```bash
curl -i http://localhost:8080/api/sales/vehicles/1
```

Ventas del VW Golf (ID 7):
```bash
curl -i http://localhost:8080/api/sales/vehicles/7
```

**Resultado esperado:** `HTTP/1.1 200 OK` con la lista de ventas de ese vehículo. Verificar que todos los objetos tienen `vehicle.id` igual al solicitado.

---

### 3.4) GET /api/sales/vehicles/bestSelling — top 5 más vendidos

**Sin filtro de fechas:**
```bash
curl -i http://localhost:8080/api/sales/vehicles/bestSelling
```

**Con filtro de fecha de inicio:**
```bash
curl -i "http://localhost:8080/api/sales/vehicles/bestSelling?startDate=2025-01-01"
```

**Con rango de fechas completo:**
```bash
curl -i "http://localhost:8080/api/sales/vehicles/bestSelling?startDate=2025-01-01&endDate=2025-01-07"
```

**Resultado esperado:** `HTTP/1.1 200 OK` con un array de **máximo 5 elementos**, cada uno con:
```json
{
  "vehicleId": 9,
  "vehicleModel": "Tiguan",
  "salesCount": 50
}
```
Los elementos deben aparecer ordenados de mayor a menor `salesCount`.

---

## 4) Caché de Brand

### 4.1) Verificar que la caché funciona

Hacer dos peticiones seguidas y comprobar en los logs de consola que la segunda **no** genera una query SQL a H2:

```bash
curl http://localhost:8080/api/brands
curl http://localhost:8080/api/brands
```

En la consola del servidor, la primera llamada mostrará la query `SELECT b FROM Brand b`.  
La segunda **no debe mostrarla** (viene de caché).

---

### 4.2) Verificar invalidación de caché tras escritura

```bash
# 1. Consultar lista (se cachea)
curl http://localhost:8080/api/brands

# 2. Crear una marca nueva (invalida la caché)
curl -X POST http://localhost:8080/api/brands \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{"name":"Peugeot","description":"French manufacturer"}'

# 3. Consultar de nuevo (debe ir a BBDD y mostrar la nueva marca)
curl http://localhost:8080/api/brands
```

**Resultado esperado:** La tercera petición muestra la query SQL en consola (caché invalidada) y devuelve la marca "Peugeot" en el listado.

---

## 5) Logging de peticiones

### 5.1) Verificar que se crea el fichero de log

Después de realizar cualquier petición, comprobar que existe el fichero:
```
logs/api-requests.log
```

### 5.2) Consultar el contenido del log

**En Linux/Mac:**
```bash
cat logs/api-requests.log
```

**En Windows:**
```powershell
Get-Content logs\api-requests.log
```

**Resultado esperado:** Una línea por cada petición realizada con el formato:
```
2025-01-15T10:30:00.123 | GET /api/sales?page=1 | 200 | 45ms
2025-01-15T10:30:01.456 | POST /api/brands | 401 | 3ms
2025-01-15T10:30:05.789 | POST /api/brands | 201 | 12ms
```

### 5.3) Verificar que se registran también las peticiones rechazadas por seguridad

```bash
# Petición sin credenciales (debe quedar en el log con 401)
curl -i -X DELETE http://localhost:8080/api/brands/1
```

Comprobar en `logs/api-requests.log` que aparece la línea con `| 401 |`.

---

## 6) Resumen de endpoints disponibles

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/brands` | No | Listar marcas (con caché) |
| GET | `/api/brands/{id}` | No | Obtener marca por ID (con caché) |
| POST | `/api/brands` | Sí | Crear marca |
| PUT | `/api/brands/{id}` | Sí | Actualizar marca |
| DELETE | `/api/brands/{id}` | Sí | Eliminar marca |
| GET | `/api/vehicles` | No | Listar vehículos |
| GET | `/api/vehicles/{id}` | No | Obtener vehículo por ID |
| POST | `/api/vehicles` | Sí | Crear vehículo |
| PUT | `/api/vehicles/{id}` | Sí | Actualizar vehículo |
| DELETE | `/api/vehicles/{id}` | Sí | Eliminar vehículo |
| GET | `/api/sales?page={n}` | No | Listar ventas paginadas (10/pág) |
| GET | `/api/sales/{id}` | No | Obtener venta por ID |
| GET | `/api/sales/brands/{brandId}` | No | Ventas de una marca |
| GET | `/api/sales/vehicles/{vehicleId}` | No | Ventas de un vehículo |
| GET | `/api/sales/vehicles/bestSelling` | No | Top 5 más vendidos (fechas opcionales) |

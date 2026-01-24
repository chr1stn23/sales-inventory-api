# 📦 Sales Inventory API

API REST para la gestión de ventas e inventario, desarrollada con Spring Boot 4, aplicando buenas prácticas backend, reglas de negocio reales, arquitectura por capas y testing.

---
## 🎯 Objetivo del proyecto

Desarrollar una API REST robusta que permita administrar categorías, productos, clientes y ventas, garantizando consistencia de datos, validaciones de negocio y una base sólida para escalar futuras funcionalidades.

Este proyecto fue diseñado como un MVP backend, priorizando claridad, mantenibilidad y calidad del código.

---
## 🚀 Alcance del MVP
### 📌 Entidades principales

- Category
- Product 
- Customer
- Sale
- SaleDetail

### ⚙️ Funcionalidades implementadas

- CRUD de categorías 
- CRUD de productos 
- CRUD de clientes
- Registro de ventas
- Descuento automático de stock
- Consulta de historial de ventas 
- Validaciones de negocio en backend
- Documentación automática con Swagger / OpenAPI

### 🚫 Fuera de alcance (por ahora)

- Autenticación / JWT
- Pagos reales
- Reportes avanzados
- Devoluciones
- Multi-moneda 
- Interfaz gráfica (Frontend)

---
## 🧠 Reglas de negocio
### Obligatorias

1. Una venta debe contener al menos un producto
2. No se puede vender una cantidad mayor al stock disponible 
3. El stock se descuenta automáticamente al registrar una venta
4. El stock nunca puede ser negativo
5. Una venta registrada no puede ser editada
6. Los productos inactivos no pueden venderse
7. Las categorías inactivas no permiten nuevos productos

### Reglas adicionales implementadas

1. El precio del producto se guarda en el detalle de venta (histórico)
2. El total de la venta se calcula exclusivamente en el backend
3. Los productos utilizan soft delete para preservar el historial de ventas

### 📝 Nota de diseño
> El sistema permite desactivar (soft delete) productos aunque tengan ventas asociadas.
Esto garantiza la integridad del historial de ventas sin impedir la evolución del catálogo,
evitando inconsistencias y permitiendo bloquear el uso del producto en futuras transacciones.


---
## 🏗️ Decisiones técnicas
### Stack tecnológico

- Lenguaje: Java 21
- Framework: Spring Boot 4
- Persistencia: Spring Data JPA + Hibernate
- Base de datos: MySQL / H2 (para testing)
- Build Tool: Maven 
- Documentación: Swagger / OpenAPI (springdoc)
- Formato de intercambio: JSON

### Arquitectura

- Arquitectura por capas:
  - Controller
  -  Service 
  - Repository
- Uso de DTOs para desacoplar la API del modelo de dominio
- Manejo centralizado de errores
- Validaciones con Bean Validation (`jakarta.validation`)
---
## 📐 Convenciones del proyecto
### Naming
- Endpoints en plural:
`/api/products`, `/api/categories`, `/api/sales`

- DTOs:
  - `ProductRequest`
  - `ProductResponse`

- Servicios:
  - `ProductService`

- Repositorios:
  - `ProductRepository`

### Manejo de errores

La API utiliza un formato de error consistente basado en RFC 7807 – Problem Details for HTTP APIs:
```
{
"type": "https://example.com/errors/not-found",
"title": "Producto no encontrado",
"status": 404,
"timestamp": "2026-01-19T10:48:22Z",
"detail": "El producto con ID 4 no se pudo encontrar",
"instance": "/api/products/4"
}
```
---
## 🧪 Testing

Se implementaron pruebas automatizadas enfocadas en asegurar la lógica de negocio y el correcto funcionamiento de la API:

- Tests unitarios de Services 
- Tests de Repositories (JPA)
- Tests de Controllers usando @WebMvcTest y MockMvc 
- Uso de JUnit 5 y Mockito
---
## 📚 Documentación de la API

La documentación interactiva está disponible vía Swagger UI:
```
http://localhost:8080/swagger-ui.html
```
---
## ▶️ Ejecución del proyecto
### Requisitos
- Java 21 
- Maven 3.9+
### Perfiles disponibles
- dev: Base de datos H2 en memoria (modo desarrollo, perfil por defecto)
- prod: Preparado para base de datos real (PostgreSQL)

### Ejecutar en local (perfil dev)
``mvn spring-boot:run``

> Por defecto, la aplicación se ejecuta usando el perfil dev.

### Ejecutar explícitamente con perfil
```mvn spring-boot:run -Dspring-boot.run.profiles=dev```

### Ejecutar tests
``mvn test``

---
## 👤 Autor
***Christian Lara Vega***
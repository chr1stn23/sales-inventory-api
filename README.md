# Proyecto: Sales Inventory API

## Objetivo
Desarrollar una API REST de gestión de ventas e inventario que permita administrar productos,
clientes y ventas, aplicando reglas de negocio reales y buenas prácticas backend con Spring Boot.

## Alcance del MVP
### Entidades Principales:
* Category
* Product
* Customer
* Sale
* SaleDetail

### Funcionalidades:
* CRUD de categorías
* CRUD de productos
* CRUD de clientes
* Registro de ventas
* Descuento automático de stock 
* Consulta de ventas

### No incluye (Por ahora):
* Autenticación / JWT
* Pagos reales
* Reportes complejos
* Devoluciones
* Multi-moneda
* UI / Frontend

## Reglas de negocio
### Obligatorias:
1. Una venta debe tener al menos un producto
2. No se puede vender una cantidad mayor al stock disponible
3. El stock se descuenta automáticamente al registrar una venta
4. El stock nunca debe ser negativo
5. Una venta registrada no puede ser editada
6. Los productos inactivos no pueden venderse
7. Una categoría inactiva no permite nuevos productos
### Opcionales:
1. El precio del producto se guarda en el detalle de venta (histórico)
2. El total de la venta se calcula en el backend
3. No se permite eliminar productos con ventas asociadas

## Decisiones técnicas
### Stack definido
* Lenguaje: Java 21
* Framework: Spring Boot 4
* Persistencia: JPA + Hibernate
* Base de datos: MySQL
* Build: Maven
* Documentación: Swagger / OpenAPI
* Formato: JSON

## Convenciones del proyecto
### Naming
* Endpoints en plural: `/api/products`
* DTOs: `ProductRequestDTO`,`ProductResponseDTO`
* Servicios: `ProductService`
* Repositorios: `ProductRepository`
### Respuesta estándar
Formato de errores basado en RFC 7807 (Problem Details for HTTP APIs)
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

## Historias de usuario
* **Como** usuario del sistema, **quiero** crear, listar, actualizar y desactivar categorías, **para** organizar los productos del inventario.
* **Como** usuario del sistema, **quiero** registrar y administrar productos, **para** llevar el control de inventario.
* **Como** usuario del sistema, **quiero** registrar y administrar clientes, **para** asociarlos a las ventas realizadas.
* **Como** usuario del sistema, **quiero** registrar una venta con uno o más productos, **para** llevar el control de las transacciones realizadas.
* **Como** usuario del sistema, **quiero** consultar el historial de ventas, **para** analizar la información registrada.
* **Como** usuario del sistema, **quiero** recibir mensajes de error claros y consistentes, **para** entender por qué una operación no fue exitosa.

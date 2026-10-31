# Desafío Técnico – API de Tipo de Cambio

## Descripción

Microservicio en Java para consumir un servicio externo de tipo de cambio y exponer una API REST que permita convertir montos entre distintas monedas.

---

## Stack Tecnológico

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Cloud OpenFeign | 2025.1.1 |
| H2 (base de datos en memoria) | Runtime |
| Spring Data JPA | Incluido |
| Lombok | Incluido |
| Maven | Wrapper incluido |

---

## Lo que ya está configurado

### Cliente HTTP externo (OpenFeign)

El cliente Feign ya está implementado y apuntando a la API pública de tipo de cambio:

- **Interfaz:** `infrastructure/external/ExchangeRateApiClient.java`
- **DTO de respuesta:** `infrastructure/external/dto/ExchangeRateApiResponse.java`
- **API usada:** `https://v6.exchangerate-api.com/v6`
- **Endpoint:** `GET /{apiKey}/latest/{baseCurrency}`

La clase principal ya tiene habilitado `@EnableFeignClients`.

### Base de datos H2

Datasource, JPA y consola H2 listos en `application.yaml`:

| Parámetro | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:testdb` |
| Usuario | `sa` |
| Password | `password` |
| Consola | `http://localhost:8080/h2-console` |
| DDL | `update` (auto-creación de tablas) |

### Estructura de paquetes sugerida

```
pe.test.exchange
├── domain
│   ├── model           ← Entidades JPA
│   ├── repository      ← Interfaces de repositorio
│   ├── service         ← Contratos de dominio
│   └── exception       ← Excepciones de dominio
├── application
│   ├── controller      ← Endpoints REST
│   ├── service         ← Lógica de aplicación
│   └── dto             ← Request / Response
└── infrastructure
    ├── external        ← Cliente Feign (YA IMPLEMENTADO)
    └── persistence     ← Implementaciones de repositorio
```

---

## Lo que debes implementar

### 1. Endpoint de conversión

Exponer `POST /api/conversion` con el siguiente contrato:

**Request:**
```json
{
  "monto": 100,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN"
}
```

**Response:**
```json
{
  "monto": 100,
  "montoConvertido": 370.00,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN",
  "tipoCambio": 3.70
}
```

### 2. Integración con el cliente Feign existente

Usar `ExchangeRateApiClient` para obtener el tipo de cambio real desde:

```
https://v6.exchangerate-api.com/v6/{apiKey}/latest/{baseCurrency}
```

La API key se obtiene de la variable de entorno `EXCHANGE_RATE_API_KEY`. Puedes usar `.env.example` como plantilla.

### 3. Persistencia

Almacenar cada conversión en H2 con los siguientes datos:

- Monto original
- Moneda origen / destino
- Tipo de cambio aplicado
- Monto convertido
- Fecha de la operación

---

## Consideraciones de Diseño

- Aplicar **Domain-Driven Design (DDD)** con separación clara de capas
- Manejo adecuado de errores (moneda inválida, servicio externo no disponible)
- Código limpio y mantenible

---

## Cómo ejecutar

Configura la variable de entorno antes de iniciar la aplicación. Por ejemplo:

```bash
export EXCHANGE_RATE_API_KEY=tu_api_key
./mvnw spring-boot:run
```

La aplicación levanta en `http://localhost:8080`.

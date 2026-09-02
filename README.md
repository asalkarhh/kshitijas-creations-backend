# Kshitija's Creations Backend

Initial backend foundation for the Kshitija's Creations website. It provides the database schema, JPA model, repositories, admin bootstrap, JWT authentication, API security, health checks, and integration tests. Category, product, banner, and order controllers are intentionally not part of this phase.

## Technology

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL
- Spring Data JPA and Flyway
- Spring Security with stateless JWT authentication

## 6. Local Development

1. Create a `kshitijas_creations` database in PostgreSQL:
   ```sql
   CREATE DATABASE kshitijas_creations;
   ```
2. Set the `DB_USERNAME` and `DB_PASSWORD` variables if they differ from defaults.
3. Provide an upload directory:
   ```bash
   export UPLOAD_DIRECTORY=/absolute/path/to/uploads
   ```
4. Run the application (Flyway will automatically apply all database migrations):
   ```bash
   ./mvnw spring-boot:run
   ```

## 7. Production Deployment & Security

A complete production setup requires configuring several environment variables safely. Do NOT commit secrets to `.env`. See `.env.example` for reference.

### Required Variables
*   `DB_URL`: JDBC PostgreSQL connection string
*   `DB_USERNAME`, `DB_PASSWORD`: Database credentials
*   `JWT_SECRET`: A very strong secret string (at least 32 characters)
*   `JWT_EXPIRATION_MINUTES`: Expiration time for Admin sessions (e.g., `480`)
*   `ADMIN_EMAIL`, `ADMIN_PASSWORD`: Used by the backend to seed the first admin user
*   `WHATSAPP_ADMIN_NUMBER`: The number to receive orders (must be country code format without `+`, e.g., `918087818729`)
*   `UPLOAD_DIRECTORY`: An absolute, writable path on the server where images are stored
*   `PUBLIC_MEDIA_BASE_URL`: The external URL prefix used to serve images
*   `ALLOWED_ORIGINS`: Comma-separated list of permitted frontend URLs
*   `SWAGGER_ENABLED`: Set to `false` in production

### Static Media Serving (Nginx)
The backend does **not** serve uploaded files directly. It expects a reverse proxy like Nginx to serve them from the `UPLOAD_DIRECTORY`. Example Nginx snippet:

```nginx
location /uploads/ {
    alias /absolute/path/to/uploads/;
    expires 30d;
    add_header Cache-Control "public, no-transform";
}
```

### Guest Checkout & WhatsApp Flow
The `POST /api/v1/public/orders` endpoint allows guest checkouts.
*   **Tamper Protection:** The backend recalculates all item prices directly from the database.
*   **Rate Limiting:** Public orders are rate-limited by IP address to prevent abuse.
*   **Redirect:** A successful order returns an HTTP 201 with a `whatsappUrl`. The React frontend is responsible for redirecting the user to this URL, where they manually send the pre-filled order text message.

### Order Status Flow
Orders transition through the following states via `PATCH /api/v1/admin/orders/{id}/status`:
*   `PLACED` ➔ `CONFIRMED`
*   `PLACED` ➔ `CANCELLED`
*   `CONFIRMED` ➔ `COMPLETED`
*   `CONFIRMED` ➔ `CANCELLED`

`COMPLETED` and `CANCELLED` are terminal states.

## 8. Endpoints Overview

*   **Public:**
    *   `GET /api/v1/public/categories`
    *   `GET /api/v1/public/products` (supports `search`, `category`, and sorting)
    *   `GET /api/v1/public/products/{slug}`
    *   `GET /api/v1/public/banners`
    *   `POST /api/v1/public/orders`
*   **Admin Authentication:**
    *   `POST /api/v1/auth/login`
    *   `GET /api/v1/auth/me`
*   **Admin Management (Requires Bearer Token):**
    *   `GET|POST|PUT|DELETE /api/v1/admin/categories`
    *   `GET|POST|PUT|DELETE /api/v1/admin/products`
    *   `POST /api/v1/admin/products/{id}/media`
    *   `DELETE /api/v1/admin/products/{id}/media/{mediaId}`
    *   `GET|POST|PUT|DELETE /api/v1/admin/banners`
    *   `GET /api/v1/admin/orders` (supports filtering by `search`, `status`, `fromDate`, `toDate`)
    *   `GET /api/v1/admin/orders/{id}`
    *   `PATCH /api/v1/admin/orders/{id}/status`
    *   `GET /api/v1/admin/dashboard`
*   **Actuator (Health only):**
    *   `GET /actuator/health`
    *   `GET /actuator/info`

## 9. API Documentation (Swagger)
When `SWAGGER_ENABLED=true` is set (typically only locally), OpenAPI documentation is available at:
*   Swagger UI: `http://localhost:8080/swagger-ui.html`
*   OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 10. Database Backups
Ensure the `kshitijas_creations` database is backed up regularly, especially since `order_items` stores the immutable historical price snapshots of purchases. Also configure automated backups of the `UPLOAD_DIRECTORY`.

## Environment variables

| Variable | Required in production | Purpose | Local default |
| --- | --- | --- | --- |
| `DB_URL` | Yes | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/kshitijas_creations` |
| `DB_USERNAME` | Yes | Database username | `postgres` |
| `DB_PASSWORD` | Yes | Database password | `postgres` |
| `JWT_SECRET` | Yes | HMAC signing secret; use at least 32 UTF-8 bytes | Development-only value |
| `ADMIN_EMAIL` | For first startup | Email for the initial admin account | Empty |
| `ADMIN_PASSWORD` | For first startup | Plain password for initial bootstrap; stored as BCrypt | Empty |
| `WHATSAPP_ADMIN_NUMBER` | When WhatsApp ordering is enabled | Admin WhatsApp number, including country code | Empty |
| `UPLOAD_DIRECTORY` | Yes | Filesystem location for uploaded media | `./uploads` |
| `ALLOWED_ORIGINS` | Yes | Comma-separated browser origins allowed by CORS | `http://localhost:3000,http://localhost:5173` |
| `JWT_EXPIRATION` | No | JWT lifetime as an ISO-8601 duration | `PT8H` |
| `SPRING_PROFILES_ACTIVE` | Yes in production | Spring profile (`local` or `prod`) | `local` |

The admin bootstrap runs only when `admin_users` is empty. Once an admin exists, changing `ADMIN_EMAIL` or `ADMIN_PASSWORD` does not overwrite it.

## Run locally

```bash
export DB_URL='jdbc:postgresql://localhost:5432/kshitijas_creations'
export DB_USERNAME='kshitijas_app'
export DB_PASSWORD='change-this-local-password'
export JWT_SECRET='replace-with-a-random-secret-of-at-least-32-bytes'
export ADMIN_EMAIL='admin@example.com'
export ADMIN_PASSWORD='replace-with-a-strong-password'
export WHATSAPP_ADMIN_NUMBER='919876543210'
export UPLOAD_DIRECTORY='./uploads'
export ALLOWED_ORIGINS='http://localhost:3000,http://localhost:5173'

mvn spring-boot:run
```

To run with production settings:

```bash
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

## Verify the build

Tests use an isolated in-memory database in PostgreSQL compatibility mode and run the same Flyway migration used by the application.

```bash
mvn clean verify
```

## Initial endpoints

- `POST /api/v1/auth/login` — public admin login
- `GET /api/v1/auth/me` — returns the authenticated admin profile
- `GET /actuator/health` — public application health

All `/api/v1/admin/**` routes require a valid JWT with the `ADMIN` role. Routes under `/api/v1/public/**` are public. Pass the login token as `Authorization: Bearer <token>`.

## Catalog APIs

Admin category management is available at `/api/v1/admin/categories`; deleting a category deactivates it and is rejected while the category still contains products. Public categories are available from `GET /api/v1/public/categories` and include active categories only.

Admin product management is available at `/api/v1/admin/products`. Deleting a product is a soft delete. Admin listing supports `search`, `categoryId`, `available`, `featured`, `page`, `size`, and `sort`. Public products are available from `/api/v1/public/products`, while `/api/v1/public/products/{slug}` returns a single product.

Examples:

```bash
curl 'http://localhost:8080/api/v1/public/products?search=floral&category=floral&page=0&size=20&sort=displayOrder'
curl 'http://localhost:8080/api/v1/public/products?featured=true&sort=priceAsc'
```

Supported product sort values are `newest`, `priceAsc`, `priceDesc`, and `displayOrder`.

## Product media uploads

Set `UPLOAD_DIRECTORY` to a writable directory. Files are stored below `products/{productId}/` with generated UUID filenames; only a relative file key is persisted. The public API returns browser-safe URLs under `/media/...`, never server file paths.

Supported uploads:

| Media type | Allowed formats | Maximum size |
| --- | --- | --- |
| `IMAGE` | JPEG, PNG, WebP | 5 MB per file |
| `VIDEO` | MP4, WebM | 30 MB per file |

The service verifies the declared content type and file signatures (and decodes JPEG/PNG images) before storing a file. Original filenames are never used for stored files.

Upload one or more images with an admin JWT:

```bash
curl -X POST 'http://localhost:8080/api/v1/admin/products/42/media' \
  -H 'Authorization: Bearer <token>' \
  -F 'files=@./necklace.png;type=image/png' \
  -F 'files=@./necklace-detail.webp;type=image/webp' \
  -F 'mediaType=IMAGE' \
  -F 'altText=Pearl necklace' \
  -F 'primaryMedia=true' \
  -F 'displayOrder=0'
```

When `primaryMedia=true`, the first uploaded file becomes primary and any previous primary item for that product is cleared. Deleting media removes its database record and schedules safe removal of its physical file after the database transaction commits.

## Banner APIs

Admin banner management is available at `/api/v1/admin/banners` and requires a valid admin JWT. Public (currently visible) banners are available from `GET /api/v1/public/banners` without authentication.

### Admin endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/banners` | List all banners (paginated, filterable) |
| `POST` | `/api/v1/admin/banners` | Create a banner |
| `PUT` | `/api/v1/admin/banners/{id}` | Update a banner |
| `DELETE` | `/api/v1/admin/banners/{id}` | Delete a banner and its files |

Admin listing supports query parameters: `active`, `status` (`ACTIVE`, `INACTIVE`, `SCHEDULED`, `EXPIRED`), `page`, `size`, and `sort`.

### Creating a banner (multipart/form-data)

```bash
curl -X POST 'http://localhost:8080/api/v1/admin/banners' \
  -H 'Authorization: Bearer <token>' \
  -F 'title=Summer Sale' \
  -F 'subtitle=Up to 50% off on all items' \
  -F 'desktopImage=@./banner-desktop.png;type=image/png' \
  -F 'mobileImage=@./banner-mobile.jpg;type=image/jpeg' \
  -F 'buttonText=Shop Now' \
  -F 'targetUrl=https://example.com/sale' \
  -F 'startAt=2026-09-10T00:00:00Z' \
  -F 'endAt=2026-09-20T23:59:59Z' \
  -F 'displayOrder=1' \
  -F 'active=true'
```

### Updating a banner

When updating, `desktopImage` and `mobileImage` are optional. If omitted, the existing images are preserved. When a new image is provided, the old file is safely deleted after the database update commits.

```bash
curl -X PUT 'http://localhost:8080/api/v1/admin/banners/1' \
  -H 'Authorization: Bearer <token>' \
  -F 'title=Updated Sale Banner' \
  -F 'subtitle=Now 60% off'
```

### Public banners

```bash
curl 'http://localhost:8080/api/v1/public/banners'
```

Returns only banners where `active=true`, `startAt` is null or in the past, and `endAt` is null or in the future. Sorted by `displayOrder` ascending, then `createdAt` descending.

### UTC and IST scheduling

All dates are stored and processed in UTC. The `startAt` and `endAt` fields accept ISO-8601 timestamps (e.g., `2026-09-10T00:00:00Z`). The frontend displays these in India Standard Time (IST, UTC+05:30). To schedule a banner to start at 9:00 AM IST, send `startAt=2026-09-10T03:30:00Z`.

### Banner status

The `currentStatus` field in admin responses is computed at read time:

| Status | Condition |
| --- | --- |
| `INACTIVE` | `active` is `false` |
| `SCHEDULED` | `active` is `true` and current time is before `startAt` |
| `ACTIVE` | `active` is `true` and within the configured schedule |
| `EXPIRED` | `active` is `true` and current time is after `endAt` |

### Banner image storage

Files are stored below `banners/{bannerId}/` with generated UUID filenames. Only a relative file key is persisted in the database; the public API returns browser-safe URLs under `/media/...`.

| Allowed formats | Maximum size per image |
| --- | --- |
| JPEG, PNG, WebP | 5 MB |

The service verifies declared content types against file header magic bytes (and decodes JPEG/PNG via ImageIO) before storing. Original filenames are never used as stored filenames. Target URLs must be relative paths (`/...`) or use `http`/`https` schemes; `javascript:` and other unsafe schemes are rejected.

## Order APIs

Guest checkout creates an order and generates a WhatsApp redirect URL for order submission. The cart is maintained on the frontend. Admin order management supports status transitions and a dashboard.

### WhatsApp Configuration

The admin WhatsApp number is required to generate the redirect URL. Configure it in `application.yml` or via environment variable:

```properties
app.whatsapp.admin-number=918087818729
```
*(Format: Country code + Mobile number without `+` or spaces)*

### Placing a Guest Order

`POST /api/v1/public/orders`

```json
{
  "customerName": "Saurabh Asalkar",
  "mobileNumber": "8087818729",
  "addressLine": "Complete delivery address",
  "city": "Vita",
  "state": "Maharashtra",
  "postalCode": "415311",
  "customerNote": "Optional instructions",
  "items": [
    {
      "productId": 12,
      "quantity": 2
    }
  ]
}
```

The response includes the unique `orderNumber` (e.g., `KC-2026-00001`), server-calculated `subtotal` and `totalAmount`, and the `whatsappUrl`. The frontend should redirect the customer to this URL, where they must manually press "Send" in WhatsApp. The order is securely persisted in the database independently of whether the customer sends the WhatsApp message.

### Admin Order Listing & Filtering

`GET /api/v1/admin/orders` (Requires Admin JWT)

Supports pagination (`page`, `size`) and filtering:
- `status`: Filter by `PLACED`, `CONFIRMED`, `COMPLETED`, or `CANCELLED`.
- `search`: Search across order number, customer name, and mobile number.
- `fromDate` / `toDate`: Filter by creation date range (ISO-8601).

### Order Status Transitions

`PATCH /api/v1/admin/orders/{id}/status`

Orders begin in the `PLACED` state. Allowed transitions:
- `PLACED` ➔ `CONFIRMED` or `CANCELLED`
- `CONFIRMED` ➔ `COMPLETED` or `CANCELLED`

`COMPLETED` and `CANCELLED` are terminal states. Invalid transitions will return a 409 Conflict.

### Admin Dashboard

`GET /api/v1/admin/dashboard`

Returns aggregated counts for products and orders (broken down by status) and the 5 most recently created orders.

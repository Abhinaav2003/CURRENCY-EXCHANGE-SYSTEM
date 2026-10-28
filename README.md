# Currency Exchange (Spring Boot)

Rates are fetched daily (00:30 UTC) from open.er-api.com, plus on startup and
automatically if the newest stored data is older than today.

## Endpoints
- `GET  /api/rates`                          latest rates (base USD)
- `GET  /api/convert?from=USD&to=INR&amount=100`
- `GET  /api/history/INR?days=30`
- `POST /api/refresh`                        force update
- `/`                                         simple converter UI

## Run locally
    mvn spring-boot:run
    # or
    docker build -t currency-exchange . && docker run -p 8080:8080 currency-exchange

## Neon database
From the Neon dashboard > Connect, take the host, database, user and password and build:

    DB_URL=jdbc:postgresql://ep-xxxx-pooler.region.aws.neon.tech/neondb?sslmode=require
    DB_USER=neondb_owner
    DB_PASSWORD=your_password

Note: the JDBC URL must start with `jdbc:postgresql://` and contain no user/password
(unlike the `postgresql://user:pass@host/db` string Neon shows). The table is created
automatically on first start.

Run locally with Neon:

    export DB_URL=... DB_USER=... DB_PASSWORD=...
    mvn spring-boot:run

## Deploy on Render
1. Push this folder to GitHub.
2. Render > New > Web Service > connect repo (it detects the Dockerfile), or New > Blueprint (render.yaml).
3. Under Environment, add `DB_URL`, `DB_USER`, `DB_PASSWORD`.
4. Deploy. Health check: /api/rates

History now persists in Neon across restarts.

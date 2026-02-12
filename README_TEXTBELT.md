Textbelt self-host instructions and notes

Overview
--------
This project includes a `docker-compose.yml` that runs a development Textbelt instance and a Redis service.

Quick start (dev)
------------------
1. Install Docker and Docker Compose.
2. From the project root run:

```bash
docker compose up -d
```

3. The Textbelt HTTP API will be available at `http://localhost:8080/text`.

Using Textbelt
--------------
POST JSON to `/text` with fields `phone`, `message`, `key`.
Example:

```bash
curl -X POST http://localhost:8080/text -H 'Content-Type: application/json' -d '{"phone":"+1555...","message":"hi","key":"textbelt"}'
```

Customizing and building Textbelt from source
--------------------------------------------
1. Get the repository: Textbelt's open-source server is available at https://github.com/textbelt/textbelt
2. The server is a Node.js app. To modify it locally:
   - Install Node.js (16+), npm/yarn
   - Clone the repo
   - Edit server code (API behavior is in `server.js` / `src` depending on version)
   - Run `npm install` and `npm run start` for local dev

3. Building your own Docker image:
   - Create a Dockerfile (the upstream repo includes one). Example:

```
FROM node:18-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .
EXPOSE 80
CMD ["node", "server.js"]
```

4. Configure transports:
   - Textbelt front-ends often support multiple "transports" (HTTP SMS providers or a local modem script).
   - To integrate a USB modem, modify the transport layer to call `gammu` or `smstools` CLI.
   - For higher throughput or commercial use, register sender IDs and configure provider credentials in environment variables.

Security & production notes
--------------------------
- Textbelt's public API is rate-limited and not suitable for production OTPs without proper provider integration.
- For production deliverability use a paid provider or integrate multiple carriers and fallback logic.
- Store provider credentials in a secure secrets store and do not bake them into images.

Questions? I can help scaffold a custom transport or integrate your SMS provider into Textbelt.

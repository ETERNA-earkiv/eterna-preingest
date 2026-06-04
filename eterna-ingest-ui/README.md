# eterna-ingest-ui

React-webgränssnitt för manuell inleverans mot [eterna-ingest-service](../eterna-ingest-service/README.md).

UI:t hämtar schemat dynamiskt från `/api/schema` och renderar ett formulär baserat på fältdefinitionerna. Inga ändringar i UI-koden krävs vid nya fälttyper eller scheman.

---

## Funktioner

- Dynamiskt formulär baserat på `schema.yaml`
- Stöd för alla fälttyper: text, datum, enum, bool, fil-upload
- Statusvisning för pågående ingest-jobb
- Stöd för RECORD och ITEM (ärende/handling)

---

## Starta lokalt

Ingår i Docker Compose-stacken — starta med:

```bash
docker compose up -d
```

UI är tillgängligt på **http://localhost:8082**.

---

## Bygga separat

```bash
cd eterna-ingest-ui
npm install
npm run build   # Producerar dist/ som tjänas av eterna-ingest-service
npm run dev     # Lokal dev-server mot eterna-ingest-service
```

Konfigurera API-bas-URL i `.env`:

```
VITE_API_BASE_URL=http://localhost:8082
```

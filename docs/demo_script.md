# Demo Script (2–4 min) — Local Knowledge Ops Hub

Objetivo: demostrar integración real (IA + automatización + backend + DB + auth) 100% local y reproducible.

---

## 0) Checklist antes de grabar / enseñar (30s)
- Docker Desktop encendido.
- Postgres + pgvector, n8n y Ollama levantados.
- Backend Spring Boot levantado.
- n8n: WF1 y WF2 en **Active**.
- Carpeta `inbox/` visible en el explorador.
- Tener un PDF de ejemplo (texto seleccionable).

---

## 1) Apertura (15–20s)
“Este proyecto convierte PDFs en una base de conocimiento local. Se indexan con embeddings y luego puedo preguntar cosas y obtener respuestas con citas por página. Además, automatizo la ingesta y un digest diario con n8n. Todo sin APIs de pago.”

---

## 2) Enseñar el pipeline (20–30s)
Abro el repo y enseño:
- `inbox/` (entrada)
- `outbox/processed/` (PDFs ya procesados)
- `outbox/digests/` (digests diarios)
- `workflows/` (exports n8n)
- `docs/evidence/` (capturas)

Frase clave:
“Esto está pensado para ser reproducible y demostrable en local.”

---

## 3) Ingesta automática (60–90s)
### 3.1 Meter PDF
1) Arrastro/copio un PDF a `inbox/`.

### 3.2 Mostrar n8n WF1
1) Abro n8n → workflow `WF1_ingest_inbox`.
2) Muestro el flujo:
   - Schedule Trigger (cada minuto) → LOGIN → RUN_INBOX.

### 3.3 Mostrar ejecución
1) Voy a `Executions` del workflow.
2) Abro la última ejecución exitosa.
3) Señalo el output de `RUN_INBOX`:
   - `processed: 1`
   - `pagesExtracted: X`
   - `chunksCreated: Y`
   - `documentId`

Frase clave:
“El backend hace ingest+extract+index y mueve el PDF. n8n solo orquesta.”

### 3.4 Confirmar movimiento de archivo
Vuelvo al explorador:
- El PDF ya está en `outbox/processed/` y ya no está en `inbox/`.

---

## 4) RAG con fuentes (60–90s)
### 4.1 Abrir Swagger o Postman
Opción Swagger:
- `http://localhost:8080/swagger-ui/index.html`

### 4.2 Hacer una pregunta
1) `POST /ask`
2) Pregunta ejemplo:
   - “Resume el documento en 5 puntos y cita fuentes [1], [2].”
3) Enseño la respuesta:
   - `answer` con referencias
   - `sources[]` con `pageStart/pageEnd`, `snippet`, `distance`

Frase clave:
“No es un chatbot genérico: responde usando solo mis documentos y devuelve evidencias. Si no hay evidencia, responde exactamente: ‘No encuentro evidencia en tus documentos.’”

---

## 5) Digest diario (30–60s)
### 5.1 Mostrar WF2
1) Abro n8n → workflow `WF2_daily_digest`
2) Muestro nodos:
   - Schedule Trigger diario → LOGIN → ASK → SAVE_DIGEST

### 5.2 Ejecutar (sin esperar al horario)
1) Ejecuto el workflow manualmente (o ejecuto nodos).
2) Confirmo output del `SAVE_DIGEST` (path).

### 5.3 Ver el archivo generado
En el explorador:
- `outbox/digests/digest_YYYY-MM-DD.md`

Frase clave:
“Esto deja un resumen persistente en markdown, útil como ‘bitácora’ diaria.”

---

## 6) Cierre (15–20s)
“En resumen: stack completo local, automatizado con n8n, con guardrails anti-alucinación y trazabilidad por fuentes. Está documentado, exportado y listo para replicarse en otra máquina.”

---

## 7) Preguntas típicas de entrevista (respuestas cortas)
- ¿Cómo evitas errores de veracidad?
  - “Si la similitud no supera el umbral, devuelvo exactamente ‘No encuentro evidencia…’ y siempre envío `sources[]`.”
- ¿Qué parte es IA exactamente?
  - “Embeddings para búsqueda semántica + generación local para redactar la respuesta usando solo los chunks recuperados.”
- ¿Qué aporta n8n?
  - “Orquesta procesos: ingesta desde `inbox/` y digest diario sin intervención manual.”
- ¿Cómo escalarías?
  - “Jobs async, colas internas, index vectorial con HNSW/IVFFLAT, y control de concurrencia en embeddings.”

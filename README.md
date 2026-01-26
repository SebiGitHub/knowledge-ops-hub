# Local Knowledge Ops Hub (RAG + n8n + Spring Boot)

Pipeline local 100% gratuito para convertir PDFs en una “base de conocimiento” consultable con RAG (respuestas con citas) y automatizar la ingesta/digest con n8n.

## Problema que resuelve
Cuando acumulas documentación (PDFs, guías, apuntes, specs), encontrar respuestas rápidas y fiables es lento. Este proyecto crea un flujo local donde:
- subes PDFs,
- se indexan automáticamente,
- haces preguntas y obtienes respuestas **con fuentes y páginas**,
- y generas resúmenes/digests automáticos.

## Qué hace (features)
- Ingesta de PDFs (texto seleccionable) → extracción por páginas (PDFBox)
- Indexado semántico con embeddings locales (Ollama + `nomic-embed-text`)
- RAG: endpoint `/ask` que devuelve respuesta + `sources[]` con páginas/snippets
- Automatizaciones con n8n:
  - WF1: procesa PDFs desde `inbox/` cada minuto
  - WF2: genera digest diario y lo guarda en `outbox/digests/`
- Todo en local, sin APIs de pago

## Demo rápida (2 minutos)
1) Levanta el stack (Docker Desktop):
   - Postgres + pgvector
   - n8n
   - Ollama
   - backend Spring Boot
2) Copia un PDF a `./inbox/`
3) Espera a la ejecución de WF1 (cada minuto) o ejecuta `POST /jobs/inbox/run?limit=1`
4) Comprueba que el PDF se movió a `./outbox/processed/`
5) Haz una pregunta:
   - `POST /ask` con: `{"question":"Resume el documento en 5 puntos."}`
6) Verás `answer` + `sources[]` con páginas.
7) (Opcional) Ejecuta WF2 o `POST /jobs/digest/save` para generar un `.md` en `./outbox/digests/`

## Arquitectura
                PDFs
                 |
                 v
        +---------------------------+
        |       inbox/ folder       |
        +---------------------------+
                 |
                 | (WF1: cada minuto / diario)
                 v
        +---------------------------+
        |            n8n            |
        |         WF1 / WF2         |
        +---------------------------+
                 |
                 | HTTP (calls)
                 v
        +---------------------------+
        |      Spring Boot API      |
        |     Auth / Docs / RAG     |
        +---------------------------+
                 |
                 | JPA / JDBC
                 v
        +--------------------------+
        |   Postgres + pgvector    |
        |   tables:                |
        |   - documents            |
        |   - document_pages       |
        |   - chunks (vector)      |
        +--------------------------+
                 |
                 | embeddings / generate
                 v
        +--------------------------+
        |          Ollama          |
        |        local LLM         |
        +--------------------------+


Outputs:
- PDFs procesados -> outbox/processed/
- Digests -> outbox/digests/


## Instalación local

### Requisitos
- Windows + Docker Desktop
- Java 17+
- Maven
- Ollama (modelo local)
- n8n (self-hosted)
- Postman (o Swagger como alternativa)
- Swagger UI (para probar)

### Estructura de carpetas
En la raíz del repo:
- `inbox/` → deja PDFs aquí para que WF1 los procese
- `outbox/processed/` → PDFs ya procesados
- `outbox/digests/` → digests en markdown
- `workflows/` → exports JSON de n8n
- `docs/evidence/` → capturas de evidencias
- `postman/` → colección Postman

### Variables de entorno (backend)
Estas variables ya tienen defaults, pero puedes sobreescribirlas:

| Variable | Default | Para qué sirve |
|---|---|---|
| `KOH_OLLAMA_URL` | `http://localhost:11434` | URL de Ollama local |
| `KOH_EMBED_MODEL` | `nomic-embed-text` | Modelo de embeddings |
| `KOH_CHAT_MODEL` | `mistral` | Modelo de generación |
| `KOH_INBOX_DIR` | `../inbox` | Carpeta donde el backend busca PDFs |
| `KOH_OUTBOX_DIR` | `../outbox` | Carpeta donde el backend escribe outputs |

## Endpoints clave

| Endpoint | Qué hace | Input | Output |
|---|---|---|---|
| `POST /auth/register` | Crea usuario y devuelve JWT | `{email,password}` | `{token}` |
| `POST /auth/login` | Login y devuelve JWT | `{email,password}` | `{token}` |
| `POST /jobs/inbox/run?limit=1` | Procesa 1 PDF de `inbox/` (ingest+extract+index+mueve) | Header `Authorization: Bearer <token>` | `{processed, files[], message}` |
| `POST /ask` | RAG: responde usando chunks indexados | Header `Authorization` + `{question}` | `{answer, sources[]}` |
| `POST /jobs/digest/save` | Guarda markdown en `outbox/digests/` | Header `Authorization` + `{content}` | `{path}` |

## Automatizaciones (n8n)
- Workflows exportados: `workflows/*.json`
- Documentación de flujos: `workflows/README.md`
- Evidencias (capturas): `docs/evidence/*`

## Guardrails (anti-alucinación)
- Si no hay chunks relevantes, el sistema responde exactamente: `No encuentro evidencia en tus documentos.`
- Se devuelven siempre `sources[]` con páginas/snippets para trazabilidad.

## Tests
Ejecutar:
```bash
cd backend
mvn test

## Postman

Colección: postman/KnowledgeOpsHub.postman_collection.json

Flujo típico:
1. Auth — Login (sets token) (rellena {{token}})
2. Jobs — Run inbox (limit=1)
3. RAG — Ask (with sources)
4. (Opcional) Jobs — Save digest

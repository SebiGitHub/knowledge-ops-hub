# Local Knowledge Ops Hub (RAG + n8n + Spring Boot)

Aplicación local para consultar PDF con fuentes. La interfaz web procesa los PDF y responde por sí sola; n8n es opcional y permite programar tareas separadas.

## Problema que resuelve
Cuando acumulas documentación (PDFs, guías, apuntes, specs), encontrar respuestas rápidas y fiables es lento. Este proyecto crea un flujo local donde:
- seleccionas un PDF en la web y pulsas «Subir y preparar PDF»,
- la aplicación extrae e indexa su texto,
- haces preguntas y obtienes respuestas **con fuentes y páginas**,
- opcionalmente puedes configurar n8n para procesar archivos y generar resúmenes programados.

## Qué hace (features)
- Ingesta de PDFs (texto seleccionable) → extracción por páginas (PDFBox)
- Indexado semántico con embeddings locales (Ollama + `nomic-embed-text`)
- RAG: endpoint `/ask` que devuelve respuesta + `sources[]` con páginas/snippets
- Automatizaciones con n8n:
  - WF1: procesa PDFs desde `inbox/` cada minuto
  - WF2: genera digest diario y lo guarda en `outbox/digests/`
- Todo en local, sin APIs de pago

## Dos recorridos diferentes

**Interfaz web:** inicia sesión, selecciona un archivo, pulsa «Subir y preparar PDF» y espera a que se confirme. En la consulta puedes elegir uno o varios documentos preparados. Este recorrido usa el backend, PostgreSQL y Ollama; no necesita n8n ni Postman.

**Automatización opcional:** los archivos de `workflows/` son flujos exportados que deben importarse, configurarse y activarse en n8n. Arrancar el contenedor no implica que estén configurados ni ejecutándose. WF1 solicita al backend procesar PDF de `inbox/`, y WF2 solicita un resumen y lo guarda en `outbox/digests/`.

Los flujos requieren credenciales de la aplicación y conexión con el backend. El backend escucha en `127.0.0.1` por defecto, mientras que los flujos usan `host.docker.internal`; según la configuración de Docker puede no ser accesible. Comprueba las ejecuciones de n8n y la conectividad antes de dar por activa la automatización. No publiques el backend en Internet.

Las fuentes se agrupan por documento y página. La distancia semántica es una medida técnica de similitud, no un porcentaje de veracidad; la respuesta puede contener errores aun con fuentes.

## Demo por API
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
| `GET /documents/ready` | Lista PDF preparados del usuario | Header `Authorization` | Documentos indexados |
| `POST /ask` | RAG: responde usando chunks indexados | Header `Authorization` + `{question, documentIds}`; IDs opcionales | `{answer, sources[]}` |
| `POST /jobs/digest/save` | Guarda markdown en `outbox/digests/` | Header `Authorization` + `{content}` | `{path}` |

## Automatizaciones (n8n)
- Workflows exportados: `workflows/*.json`
- Documentación de flujos: `workflows/README.md`
- Evidencias (capturas): `docs/evidence/*`

## Reindexar documentos ya preparados

Al actualizar el fragmentador, los documentos indexados con una versión anterior deben prepararse de nuevo. Vuelve a subir cada PDF en la interfaz y espera a ver «Documento preparado» antes de consultar. Los registros y fragmentos anteriores no se borran automáticamente; las instalaciones que los conserven pueden devolver duplicados hasta que se gestionen por separado.

Cuando ningún fragmento supera el umbral semántico, se intenta una búsqueda por palabras en los documentos del usuario. La selección en la web restringe ambos tipos de búsqueda a los PDF elegidos. La relevancia y fidelidad de la respuesta siguen requiriendo revisión.

## Guardrails (anti-alucinación)
- Si no hay chunks relevantes, el sistema responde exactamente: `No encuentro evidencia en tus documentos.`
- Se devuelven `sources[]` con páginas/snippets cuando hay resultados. Sin evidencia, la lista está vacía. Las fuentes no garantizan que cada afirmación generada sea correcta.

## Tests
Ejecutar:
```bash
cd backend
./mvnw verify
```

## Postman

Colección: postman/KnowledgeOpsHub.postman_collection.json

Flujo típico:
1. Auth — Login (sets token) (rellena {{token}})
2. Jobs — Run inbox (limit=1)
3. RAG — Ask (with sources)
4. (Opcional) Jobs — Save digest

## Interfaz web local

La interfaz de `backend/src/main/resources/static/` permite registro/inicio de sesión, subida de PDF, extracción, indexado y consulta con fuentes. El token se mantiene en memoria y se pierde al recargar. Las respuestas y fuentes se insertan como texto, sin ejecutar HTML generado.

### Arranque

El Compose existente arranca PostgreSQL y n8n; Ollama y Java se ejecutan en el host.

1. Desde la raíz: `docker compose -f infra/docker-compose.yml up -d`.
2. Instala e inicia Ollama. Descarga `ollama pull nomic-embed-text` y `ollama pull mistral`.
3. Define `KOH_JWT_SECRET` con al menos 32 bytes aleatorios. Se ha retirado el secreto predeterminado: las instalaciones existentes deben cambiarlo e iniciar sesión de nuevo.
4. Desde `backend/`: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
5. Abre http://127.0.0.1:8080/ y crea una cuenta local. Usa PDF ficticios con texto seleccionable, de hasta 20 MB.

PowerShell para generar el secreto:

```powershell
$kohBytes = New-Object byte[] 32
$kohRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$kohRng.GetBytes($kohBytes)
$kohRng.Dispose()
$env:KOH_JWT_SECRET = [Convert]::ToBase64String($kohBytes)
```

El backend escucha en loopback por defecto. No publiques este entorno en Internet: la ingesta por rutas y carpetas compartidas está diseñada para una demostración local. La contraseña del Compose es solo de desarrollo.

### Validación

`./mvnw verify` ejecuta las clases `*IT` mediante Failsafe y requiere PostgreSQL y el secreto. El test de ausencia de documentos usa dobles de Ollama y del repositorio. La nueva interfaz requiere comprobación en navegador y el flujo completo requiere un modelo instalado. La presencia de tests no implica que hayan pasado.


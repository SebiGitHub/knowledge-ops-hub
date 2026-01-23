# Workflows n8n (CP6)

Este proyecto incluye 2 workflows n8n para automatizar la ingesta de PDFs y la generación de un digest diario.

> Nota: en esta instalación de n8n (v2.4.6) no usamos “Execute Command”. n8n **orquesta** y el backend ejecuta el trabajo sobre las carpetas `inbox/` y `outbox/`.

---

## WF1 — `WF1_ingest_inbox.json` (Ingesta automática)

### Trigger
- **Schedule Trigger**: **cada 1 minuto**

### Qué hace
- Comprueba si hay PDFs en `inbox/`
- Si hay, procesa **1 PDF por ejecución** (limit=1):
  - lo ingesta en la app
  - extrae texto por páginas
  - indexa (chunks + embeddings)
  - mueve el PDF a `outbox/processed/`

### Endpoints que llama
1) `POST /auth/login`  
2) `POST /jobs/inbox/run?limit=1`

### Input esperado
- En el host: colocar uno o más PDFs en `./inbox/`
- En n8n: credenciales válidas para login (email/password)

### Output esperado
- Respuesta del backend en `POST /jobs/inbox/run`:
  - Si hay PDF:
    ```json
    {
      "processed": 1,
      "files": [
        {
          "filename": "archivo.pdf",
          "documentId": "...",
          "pagesExtracted": 30,
          "chunksCreated": 17
        }
      ],
      "message": "OK"
    }
    ```
  - Si no hay PDFs:
    ```json
    { "processed": 0, "files": [], "message": "No hay PDFs en inbox/" }
    ```
- En el host:
  - el PDF se mueve a `./outbox/processed/archivo.pdf`
- En la app:
  - el documento queda indexado y se puede consultar con `POST /ask`

---

## WF2 — `WF2_daily_digest.json` (Digest diario)

### Trigger
- **Schedule Trigger**: **diario** (hora configurada en el workflow)

### Qué hace
- Genera un “digest” preguntando al sistema RAG
- Guarda el resultado como markdown en `outbox/digests/`

### Endpoints que llama
1) `POST /auth/login`  
2) `POST /ask`  
3) `POST /jobs/digest/save`

### Input esperado
- En n8n: credenciales válidas para login (email/password)
- El sistema debe tener documentos previamente indexados (idealmente por WF1)

### Output esperado
- `POST /ask` devuelve:
  ```json
  { "answer": "...", "sources": [ ... ] }

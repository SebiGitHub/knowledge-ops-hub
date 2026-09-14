"use strict";
const byId = id => document.getElementById(id);
let token = "", pendingDocument = null, processing = false;
function showError(message) { byId("error").textContent = message; }
function closeSession() {
  token = ""; pendingDocument = null;
  byId("workspace").hidden = true; byId("account").hidden = false;
  byId("result").hidden = true; byId("answer").textContent = ""; byId("sources").replaceChildren();
  byId("upload-form").reset(); byId("ask-form").reset(); byId("upload-status").textContent = "";
}
async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", "Bearer " + token);
  const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 180000);
  try {
    const response = await fetch(path, {...options, headers, signal: controller.signal});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      if (response.status === 401) { closeSession(); throw new Error("La sesión ha caducado. Inicia sesión de nuevo."); }
      if (response.status === 413) throw new Error("El PDF supera el tamaño permitido.");
      throw new Error(response.status === 400 ? "Revisa los datos. Si estás creando una cuenta, el correo puede estar registrado." : "No se ha podido completar la operación (HTTP " + response.status + ").");
    }
    return data;
  } finally { clearTimeout(timer); }
}
async function perform(task) {
  if (processing) return;
  processing = true; showError(""); byId("status").textContent = "Procesando…";
  const controls = [...document.querySelectorAll("button, input, textarea")], disabled = controls.map(c => c.disabled);
  controls.forEach(c => c.disabled = true);
  try { await task(); byId("status").textContent = "Operación completada."; }
  catch (error) { showError(error.name === "AbortError" ? "El proceso ha tardado demasiado. Comprueba el servidor antes de reintentar." : error.message); byId("status").textContent = ""; }
  finally { controls.forEach((c,i) => c.disabled = disabled[i]); processing = false; }
}
byId("auth-form").addEventListener("submit", event => {
  event.preventDefault();
  const action = event.submitter?.value === "register" ? "register" : "login";
  const email = byId("email").value.trim(), password = byId("password").value;
  perform(async () => {
    const data = await request("/auth/" + action, {method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({email,password})});
    if (typeof data.token !== "string" || !data.token) throw new Error("Respuesta de autenticación no válida.");
    token = data.token; byId("password").value = "";
    byId("account").hidden = true; byId("workspace").hidden = false; byId("session-label").textContent = email;
  });
});
byId("logout").addEventListener("click", () => { closeSession(); byId("status").textContent = "Sesión cerrada."; byId("email").focus(); });
byId("pdf").addEventListener("change", () => { pendingDocument = null; byId("upload-status").textContent = ""; });
byId("upload-form").addEventListener("submit", event => {
  event.preventDefault();
  const file = byId("pdf").files[0];
  if (!file || !/\.pdf$/i.test(file.name) || file.size > 20 * 1024 * 1024) { showError("Selecciona un PDF de hasta 20 MB."); return; }
  perform(async () => {
    if (!pendingDocument) {
      byId("upload-status").textContent = "Subiendo PDF…";
      const body = new FormData(); body.append("file", file);
      const data = await request("/documents", {method:"POST",body});
      if (!data.documentId) throw new Error("El servidor no devolvió el identificador del documento.");
      pendingDocument = {id:data.documentId,extracted:false};
    }
    if (!pendingDocument.extracted) {
      byId("upload-status").textContent = "Extrayendo páginas…";
      await request("/documents/" + encodeURIComponent(pendingDocument.id) + "/extract", {method:"POST"});
      pendingDocument.extracted = true;
    }
    byId("upload-status").textContent = "Preparando búsqueda semántica…";
    const data = await request("/documents/" + encodeURIComponent(pendingDocument.id) + "/index", {method:"POST"});
    byId("upload-status").textContent = "Documento preparado: " + data.chunksCreated + " fragmentos.";
    pendingDocument = null; byId("upload-form").reset();
  });
});
byId("ask-form").addEventListener("submit", event => {
  event.preventDefault();
  const question = byId("question").value.trim();
  if (!question) { showError("Escribe una pregunta."); return; }
  perform(async () => {
    byId("result").hidden = true;
    const data = await request("/ask", {method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({question})});
    byId("answer").textContent = data.answer || "Sin respuesta."; byId("sources").replaceChildren();
    for (const [index, source] of (Array.isArray(data.sources) ? data.sources : []).entries()) {
      const item = document.createElement("li");
      item.textContent = "[" + (index + 1) + "] Documento " + source.documentId + " · páginas " + source.pageStart + "–" + source.pageEnd + "\n" + (source.snippet || "");
      byId("sources").append(item);
    }
    byId("result").hidden = false;
  });
});

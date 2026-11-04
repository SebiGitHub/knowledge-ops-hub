import {test,expect} from "@playwright/test";
test("login, upload, sources rendered as text and logout",async({page})=>{
 const errors=[]; page.on("pageerror",e=>errors.push(e.message));
 const calls=[];
 await page.route("**/documents/ready",r=>r.fulfill({json:[{id:"demo-1",title:"demo.pdf"},{id:"demo-2",title:"otro.pdf"}]}));
 await page.route("**/auth/login",r=>r.fulfill({json:{token:"synthetic-test-token"}}));
 await page.route("**/documents",r=>{
  calls.push("upload"); expect(r.request().headers().authorization).toBe("Bearer synthetic-test-token");
  return r.fulfill({status:201,json:{documentId:"demo-1"}});
 });
 await page.route("**/documents/demo-1/extract",r=>{calls.push("extract");return r.fulfill({json:{pagesExtracted:1}})});
 await page.route("**/documents/demo-1/index",r=>{calls.push("index");return r.fulfill({json:{chunksCreated:1}})});
 await page.route("**/ask",r=>{ expect(r.request().postDataJSON().documentIds).toEqual(["demo-1"]); return r.fulfill({json:{answer:"<img src=x onerror=alert(1)>",sources:[{documentId:"demo-1",pageStart:1,pageEnd:1,snippet:"Texto de ejemplo.",title:"demo.pdf"}]}})});
 await page.goto("/");
 await page.getByLabel("Correo",{exact:true}).fill("demo@example.com");
 await page.getByLabel("Contraseña",{exact:true}).fill("example-password");
 await page.getByRole("button",{name:"Iniciar sesión"}).click();
 await expect(page.locator("#workspace")).toBeVisible();
 await page.locator("#pdf").setInputFiles({name:"demo.pdf",mimeType:"application/pdf",buffer:Buffer.from("%PDF-1.4 synthetic fixture")});
 await page.getByRole("button",{name:"Subir y preparar PDF"}).click();
 await expect(page.locator("#upload-status")).toContainText("Documento preparado");
 expect(calls).toEqual(["upload","extract","index"]);
 await page.getByLabel("otro.pdf").uncheck();
 await page.locator("#question").fill("¿Qué dice?");
 await page.getByRole("button",{name:"Consultar",exact:true}).click();
 await expect(page.locator("#answer")).toHaveText("<img src=x onerror=alert(1)>");
 await expect(page.locator("#answer img")).toHaveCount(0);
 await expect(page.locator("#sources li")).toHaveCount(1);
 await expect(page.locator("#sources li")).toContainText("demo.pdf · página 1");
 await expect(page.locator("#sources li")).not.toContainText("[1]");
 await page.locator("#sources-details summary").click();
 await expect(page.locator("#sources")).not.toBeVisible();
 for(const width of [360,768,1440]){
  await page.setViewportSize({width,height:900});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  await page.screenshot({path:"test-results/hub-"+width+".png",fullPage:true});
 }
 await page.getByRole("button",{name:"Cerrar sesión"}).click();
 await expect(page.locator("#account")).toBeVisible();
 await expect(page.locator("#workspace")).toBeHidden();
 expect(errors).toEqual([]);
});
test("unauthorized response clears session",async({page})=>{
 await page.route("**/documents/ready",r=>r.fulfill({json:[{id:"demo-1",title:"demo.pdf"}]}));
 await page.route("**/auth/login",r=>r.fulfill({json:{token:"synthetic-test-token"}}));
 await page.route("**/ask",r=>r.fulfill({status:401,json:{}}));
 await page.goto("/");
 await page.locator("#email").fill("demo@example.com");await page.locator("#password").fill("example-password");
 await page.getByRole("button",{name:"Iniciar sesión"}).click();
 await expect(page.locator("#workspace")).toBeVisible();
 await page.locator("#question").fill("Question");await page.getByRole("button",{name:"Consultar",exact:true}).click();
 await expect(page.locator("#error")).toContainText("sesión ha caducado");
 await expect(page.locator("#workspace")).toBeHidden();
});

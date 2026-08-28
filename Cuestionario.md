# Cuestionario — Parte A del examen de la Unidad IV

> **Cómo se llena este archivo.** Responda **dentro de este mismo archivo**, debajo de cada pregunta, en el bloque marcado como `**Respuesta:**`. No borre ni reescriba los enunciados: el evaluador compara pregunta por pregunta. No añada ni quite secciones.
>
> **Este archivo se versiona en el repositorio.** Debe existir en la raíz, llamarse exactamente `Cuestionario.md`, y sus respuestas deben llegar por *commits* sucesivos hechos cuando el docente lo indique. Un archivo que aparece completo en un único *commit* al final de la sesión no cumple el protocolo y se trata según el criterio de piso 4 del examen.
>
> Se valora la precisión técnica y la justificación, **no la extensión**. Una respuesta correcta de seis líneas vale más que una página imprecisa. Cuando la pregunta pida referirse al proyecto base, hágalo con nombres concretos de clases o de *endpoints*.

---

## Datos del estudiante

| Campo | Valor |
|---|---|
| Apellidos y nombres | |
| Número de carnet | |
| Correo institucional | |
| Fecha | |
| URL del repositorio | |

---

## A1. Restricciones de REST aplicadas a un caso concreto — 8 puntos

**a) Enuncie las seis restricciones del estilo arquitectónico REST según Fielding. (3 puntos)**

**Respuesta:**

Las seis restricciones son: 1. Cliente-Servidor, 2. Sin Estado (Stateless), 3. Cacheable, 4. Interfaz Uniforme, 5. Sistema en Capas, y 6. Código bajo Demanda (Code-on-Demand, opcional).

**b) El proyecto base expone `GET /api/v1/autores` y guarda el estado de la sesión del usuario solo en el JWT que el cliente envía en cada petición. Explique qué restricción concreta se está cumpliendo con esa decisión y qué consecuencia práctica tiene para escalar el sistema a varios servidores detrás de un balanceador. (3 puntos)**

**Respuesta:**

Se cumple la restricción de **Sin Estado (Stateless)**. Como el servidor no guarda sesión en memoria y delega el estado al JWT que viaja en cada petición, se logra escalabilidad horizontal inmediata: un balanceador puede redistribuir las peticiones a cualquier instancia del backend sin requerir sesiones pegajosas (*sticky sessions*) ni sincronización entre nodos.

**c) De las seis restricciones, indique cuál es opcional y dé un ejemplo real de una API que la use. (2 puntos)**

**Respuesta:**

La restricción opcional es **Código bajo Demanda (Code-on-Demand)**. Ejemplo: Una API que devuelve scripts JavaScript ejecutables o módulos WebAssembly para que el cliente (navegador) extienda su funcionalidad dinámicamente.

---

## A2. Anatomía y ciclo de vida de un JWT — 8 puntos

**a) Un JWT tiene tres partes separadas por puntos. Nómbrelas en orden e indique qué contiene cada una. (3 puntos)**

**Respuesta:**

1. **Header**: Contiene el tipo de token (`JWT`) y el algoritmo de firma (ej. `HS256` o `RS256`). 2. **Payload**: Contiene los *claims* o afirmaciones del usuario (ej. `sub`, `roles`, `exp`, `iat`). 3. **Signature**: Firma criptográfica obtenida al aplicar el algoritmo sobre (Header + Payload) usando una clave secreta.

**b) Un compañero afirma: «como el JWT va firmado, puedo guardar en el *payload* la contraseña del usuario sin riesgo». Explique por qué está equivocado, precisando la diferencia entre firmar y cifrar. (2 puntos)**

**Respuesta:**

Está equivocado porque un JWT está **firmado, no cifrado**. La firma garantiza integridad y autenticidad (evita alteración), pero el Payload se codifica en Base64URL, por lo que cualquier entidad que intercepte el token puede decodificarlo y leer la contraseña en texto plano.

**c) El JWT es *stateless* por diseño, lo que genera un problema conocido: no se puede invalidar un token antes de que expire. Describa dos estrategias distintas para revocarlo y señale la desventaja de cada una. (3 puntos)**

**Respuesta:**

Estrategia 1: **Lista de Revocación / Blacklist en Redis**: Se almacena el `jti` o token revocado hasta su expiración. *Desventaja*: Introduce un acceso con estado a base de datos/cache en cada petición, reduciendo la naturaleza stateless de REST. Estrategia 2: **Tokens de acceso con TTL muy corto + Refresh Tokens**: Los JWT de acceso expiran en minutos (ej. 5 min) y los refresh tokens se revocan en servidor. *Desventaja*: Durante la ventana de vida del token de acceso corto, este no se puede invalidar de forma instantánea.

---

## A3. SOAP frente a REST — 8 puntos

**a) Complete la tabla comparativa con seis criterios entre SOAP y REST. (5 puntos)**

**Respuesta:**

| Criterio | SOAP | REST |
|---|---|---|
| Formato del mensaje | Exclusivamente XML | Multiformato (principalmente JSON, XML, HTML) |
| Contrato de descripción | WSDL / Esquema XSD rígido | OpenAPI (Swagger) / Autodocumentado |
| Sobrecarga de serialización | Alta (envoltorio SOAP Envelope, Header, Body) | Baja (payloads JSON ligeros) |
| Tipado | Fuerte y validado nativamente por esquema | Débil/Flexible (validado a nivel de aplicación) |
| Facilidad de consumo desde un cliente móvil | Complejo y pesado (requiere parsers XML) | Simple y nativo (deserialización JSON directa) |
| Manejo de errores | Estándar formal SOAP Fault | Códigos de estado HTTP + RFC 9457 (ProblemDetails) |

**b) El Servicio de Rentas Internas del Ecuador expone la autorización de comprobantes electrónicos mediante servicios SOAP. Explique dos razones técnicas por las que una institución de ese tipo mantiene SOAP en lugar de migrar a REST. (3 puntos)**

**Respuesta:**

1. **Validación estricta de contrato vía XSD**: El SRI requiere validar la estructura sintáctica y restricciones de tipo de comprobantes tributarios antes de procesar código de negocio. 2. **Estándares de Seguridad y No Repudio**: Implementa perfiles nativos como WS-Security para firmas digitales XML compuestas a nivel de mensaje, esenciales para validez jurídica fiscal.

---

## A4. Cache-aside sobre un servicio externo — 8 puntos

> El proyecto base define en `CacheConfig` dos espacios de caché: `libros` con TTL de 2 minutos y `openlibrary` con TTL de 24 horas.

**a) Describa el patrón *cache-aside* en sus cuatro pasos, desde que llega la petición hasta que se responde. (3 puntos)**

**Respuesta:**

Pasos del patrón cache-aside: 1. Llega la petición de lectura al backend. 2. El sistema consulta la cache (Redis) con una clave única. 3. Si ocurre un *Cache Hit*, retorna inmediatamente el dato almacenado. 4. Si ocurre un *Cache Miss*, el backend consulta el servicio externo/BD, guarda la respuesta en Redis asociando un TTL, y devuelve la respuesta al cliente.

**b) Justifique técnicamente por qué el TTL de `openlibrary` es doce veces mayor que el de `libros`, y qué criterio general debe guiar la elección de un TTL. (3 puntos)**

**Respuesta:**

El dato de OpenLibrary (metadatos bibliográficos) es estático y casi nunca cambia en el tiempo, permitiendo un TTL largo de 24h sin servir información obsoleta. El catálogo local de libros cambia frecuentemente debido a préstamos y devoluciones, requiriendo un TTL corto de 2 min. Criterio general: **A mayor volatilidad de los datos, menor debe ser el TTL; a mayor estabilidad y costo de consulta externa, mayor TTL**.

**c) Explique por qué nunca debe almacenarse en caché la respuesta de un fallo del servicio externo, y describa qué le ocurriría al sistema si se hiciera. (2 puntos)**

**Respuesta:**

Nunca se deben cachear fallos porque un error de red temporal o un *timeout* momentáneo del servicio externo quedaría congelado en la cache durante la vida del TTL, impidiendo que peticiones legítimas posteriores obtengan datos cuando el servicio externo ya se haya recuperado.

---

## A5. Diagnóstico de códigos de estado y contrato de errores — 8 puntos

> Todos los errores del proyecto base salen en formato *Problem Details* conforme a la RFC 9457, que obsoleta a la RFC 7807.

Para cada escenario indique el código HTTP correcto y explique en una línea por qué. **Cada fila vale 1 punto** (0,5 por el código y 0,5 por la justificación); el literal g) vale 2 puntos.

| # | Escenario | Código | Justificación (una línea) |
|---|---|---|---|
| a | `GET /api/v1/libros/999999` y ese identificador no existe | 404 Not Found | El recurso solicitado no existe. |
| b | `POST /api/v1/libros` sin cabecera `Authorization` | 401 Unauthorized | Falta autenticación previa (cabecera `Authorization` ausente). |
| c | Usuario autenticado con rol `LECTOR` envía `POST /api/v1/libros` | 403 Forbidden | El usuario está autenticado pero carece del rol necesario (`ADMIN`) para realizar la acción. |
| d | `POST /api/v1/libros` con el campo `titulo` vacío | 400 Bad Request (o 422 Unprocessable Content) | La sintaxis/payload viola la regla de validación de campo requerido. |
| e | Prestar un libro a un socio que ya tiene tres préstamos activos | 400 Bad Request (o 409 Conflict) | Infracción de regla de negocio del dominio (límite de préstamos excedido). |
| f | La API de Open Library no responde dentro del *timeout* configurado | 502 Bad Gateway (o 504 Gateway Timeout) | Un servidor intermedio o API externa falló o no respondió a tiempo. |

**g) Explique por qué devolver `200 OK` con un cuerpo `{"success": false}` es un error de diseño, y qué restricción de REST se incumple al hacerlo. (2 puntos)**

**Respuesta:**

Es un error de diseño porque rompe la **Interfaz Uniforme y Semántica de HTTP** de REST. Los clientes, cachés intermedias y proxies dependen del código de estado HTTP en la cabecera para determinar el éxito o fracaso de la transacción sin tener que procesar el cuerpo de la respuesta.



---

## Declaración de honestidad académica

Marque con una `x` y complete:

- [ ] Declaro que estas respuestas son de mi autoría, redactadas durante la sesión de examen, sin asistencia de inteligencia artificial ni comunicación con terceros.

Firma (nombre completo): ______________________________

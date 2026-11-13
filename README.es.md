# VLC Sheets

[English](README.md) · [Català](README.ca.md) · **Español**

> Tu música, siempre a punto.

**VLC Sheets** es un lector de partituras para Android pensado para tocar en directo:
rápido, oscuro cuando hace falta y totalmente sin conexión. Importa PDF o imágenes,
organiza la biblioteca, anota sobre el papel, crea listas de concierto y pasa página
con pedales Bluetooth o con las teclas de volumen.

Este proyecto es una aplicación nueva, con identidad propia y código original, hecha
con Kotlin, Jetpack Compose y Material 3.

---

## Instalar

Descarga el APK desde [Releases](../../releases) y ábrelo en la tableta. La primera
vez, Android pide permiso para instalar apps desde el navegador.

Nota: el APK de la v1.0.0 es una versión de pruebas (debug), no una versión de producción.

---

## Funciones

### Biblioteca
- Importación múltiple de **PDF** e **imágenes** (JPG, PNG, WebP…) desde cualquier
  gestor de archivos.
- **Texto y ChordPro** (`.txt`, `.cho`, `.chopro`…): se convierten en un PDF con los
  acordes encima de la letra, así se leen y se anotan como cualquier partitura.
- Archivos `.msf` (documento, título, anotaciones y audios).
- Archivo propio **`.vlcs`**: una partitura o una lista entera con los documentos, las
  anotaciones y los audios, para pasarla a otra tableta.
- Portada automática (miniatura de la primera página).
- Metadatos: título, compositor, género, tonalidad, BPM, compás, año, duración,
  etiquetas, notas y favorita.
- Búsqueda instantánea por título, compositor, etiqueta, colección o notas, sin
  distinguir acentos ni mayúsculas.
- Orden por título, compositor, año, última apertura o fecha de alta.
- Vista de cuadrícula o de lista, favoritas y filtros rápidos.
- Colecciones por estilo, instrumento o concierto.

### Lectura
- Visor a pantalla completa con paso horizontal e indicador de página.
- **Zoom** con el gesto de pellizcar y desplazamiento libre.
- Pasa página tocando los laterales (se puede desactivar), con los botones, con un
  teclado o con un **pedal Bluetooth** (teclas de volumen, flechas, Re Pág/Av Pág,
  espacio y dos teclas que eliges en Ajustes).
- **Dos páginas** a la vez en horizontal, **media página** (primero cambia la mitad de
  arriba y luego la de abajo) y **recorte de márgenes blancos**.
- **Audio por partitura**: vincula uno o más audios y reprodúcelos mientras lees, más
  lentos (50–90 %) sin cambiar el tono, con bucle y con puntos que pasan página solos.
- **Botones de audio** sobre la partitura y **puntos de salto** a otra página.
- **Modo concierto** (solo se puede pasar página), **papel cálido** y **tapar zonas**.
- Acciones al mantener pulsado un lateral (primera o última página, partitura anterior
  o siguiente, audio, metrónomo).
- **Exporta a PDF** con las anotaciones dibujadas.
- Modo inmersivo con barras que se ocultan solas y pantalla siempre encendida.
- **Colores invertidos** para leer con poca luz.
- Salto de página con control deslizante y marcadores.

### Anotaciones
- Bolígrafo y rotulador fluorescente con seis colores y tres grosores.
- Solo dibuja el lápiz (S Pen o similar): con los dedos se pasa página y se hace zoom.
- Goma que borra arrastrando; con el botón del lápiz pulsado, el lápiz hace de goma.
- Deshacer, borrar la página entera y ocultar las anotaciones.
- **Letra a mano → texto**: números y dinámicas escritos a mano se convierten en texto
  con un reconocedor propio, entrenado con tu letra y sin internet.
- Anotaciones guardadas por página y relativas a ella, así se ven igual en cualquier
  tamaño de pantalla.

### Listas de concierto
- Crea listas, añade partituras y ordénalas, con una nota por pieza y la duración total.
- Al tocar una lista: en la última página, el botón «Partitura siguiente» abre la
  siguiente. Pasar página nunca salta de partitura por sí solo.
- Recuento de partituras, páginas y partituras anotadas.

### Metrónomo
- Metrónomo integrado con AudioTrack (clics precisos, sin desfases).
- 30–260 BPM, 2/3/4/6 pulsos por compás, acento en el primero.
- Arranca con el tempo y el compás guardados en la partitura.
- Puede pasar página cada N compases, con cuenta de entrada.
- Sigue sonando mientras lees y se puede parar desde el visor.

### Afinador
- Afinador cromático con el micrófono: nota, cents y Hz; La de referencia de 415 a 466 Hz.

### Datos
- Copia de seguridad completa en un **ZIP** (metadatos, archivos y anotaciones).
- Restauración íntegra de la biblioteca.
- Borrado completo con confirmación.

### Detalles
- Tema claro, oscuro o automático; colores dinámicos opcionales (Android 12+).
- Catalán, castellano e inglés.
- Icono e imagen de marca propios.
- Sin permisos de almacenamiento: todo pasa por el **Storage Access Framework**.
- Sin red, sin cuentas, sin telemetría.

---

## Capturas

*(Pronto.)*

---

## Arquitectura

La estructura del código está en el [README en inglés](README.md#architecture).

**Sin base de datos externa ni ORM.** La biblioteca es un `index.json` pequeño y
atómico (escritura a un temporal + `fsync` + renombrar) dentro de `filesDir/library`,
junto a los PDF. Las anotaciones de cada partitura van aparte
(`songs/<id>/annotations.json`), para que un trazo nuevo no reescriba todo el índice.
La copia de seguridad es solo comprimir una carpeta y la app arranca al instante.

**Sin librerías de PDF de terceros.** El renderizado usa `android.graphics.pdf.PdfRenderer`.
Las páginas se renderizan a unas 1,4 veces el ancho de la pantalla en `ARGB_8888`, el
único formato que acepta `PdfRenderer`; las imágenes se decodifican en `RGB_565` para
gastar menos memoria.

---

## Compilar y probar

Requisitos: JDK 17+ y Android SDK con la plataforma 36.

```bash
# 1. Configura el SDK
echo "sdk.dir=C\:\\Android\\sdk" > local.properties

# 2. Compila
./gradlew :app:assembleDebug

# 3. Pruebas y lint
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug

# 4. Instala
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

---

## Privacidad

VLC Sheets no hace ninguna conexión de red (no tiene el permiso de internet). El único
permiso es el del micrófono, para el afinador: se pide la primera vez que lo abres y el
sonido se analiza en la tableta, sin guardarlo ni enviarlo. Los archivos importados se
copian al espacio privado de la app y solo salen del dispositivo cuando exportas un
PDF, un `.vlcs` o una copia de seguridad.

---

## Estado

Versión 1.0.0. Cada versión está en [Releases](../../releases).

---

<p align="center">Developed by <a href="https://github.com/Vlarac03">Vlarac</a></p>

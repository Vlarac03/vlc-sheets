# VLC Sheets

[English](README.md) · **Català** · [Español](README.es.md)

> La teva música, sempre a punt.

**VLC Sheets** és un lector de partitures per a Android pensat per tocar en directe:
ràpid, fosc quan cal, i totalment fora de línia. Importa PDF o imatges, organitza la
biblioteca, anota sobre el paper, crea llistes de concert i passa pàgina amb pedals
Bluetooth o tecles de volum.

Aquest projecte és una implementació nova, amb identitat pròpia i codi original.
No conté marques, textos ni recursos de MobileSheets: només recull la idea de
l'experiència d'ús d'un lector de partitures professional i la reinventa amb
Kotlin, Jetpack Compose i Material 3.

---

## Instal·lar

Baixa l'APK de [Releases](../../releases) i obre'l a la tauleta. El primer cop,
Android demana permís per instal·lar apps des del navegador.

---

## Funcionalitats

### Biblioteca
- Importació múltiple de **PDF** i **imatges** (JPG, PNG, WebP…) des de qualsevol
  gestor de fitxers del sistema.
- **Text i ChordPro** (`.txt`, `.cho`, `.chopro`…): es converteixen en un PDF amb els
  acords damunt la lletra, així es llegeixen i s'anoten com qualsevol partitura.
- Fitxers `.msf` exportats de MobileSheets (document, títol, anotacions i àudios).
- Fitxer propi **`.vlcs`**: una partitura o una llista sencera amb els documents,
  les anotacions i els àudios, per passar-la a una altra tauleta.
- Portada generada automàticament (miniatura de la primera pàgina).
- Metadades: títol, compositor, gènere, tonalitat, BPM, compàs, any, durada,
  etiquetes, notes i preferida.
- Cerca instantània per títol, compositor, etiqueta, col·lecció o notes, sense
  distingir accents ni majúscules.
- Ordenació per títol, compositor, any, última obertura o data d'alta.
- Vista de graella o de llista, preferida i filtres ràpids.
- Col·leccions per estil, instrument o concert.

### Lectura
- Visor a pantalla completa amb navegació horitzontal i indicador de pàgina.
- **Zoom** amb el gest de pinça, amb desplaçament lliure.
- Passa pàgina tocant les zones laterals (es pot desactivar), amb els botons, amb el
  teclat o amb un **pedal Bluetooth** (tecles de volum, fletxes, Re Pàg/Av Pàg, espai
  i dues tecles que tu tries a Ajustos).
- **Dues pàgines** alhora en horitzontal, **mitja pàgina** (primer canvia la meitat de
  dalt i després la de baix) i **retallar els marges blancs**.
- **Àudio per partitura**: lliga-hi un o més àudios i reprodueix-los mentre llegeixes,
  més lents (50–90 %) sense canviar el to, amb bucle i amb punts que passen pàgina sols.
- **Botons d'àudio** damunt la partitura i **punts de salt** a una altra pàgina.
- **Mode concert**: només es pot passar pàgina; **paper càlid**; **tapar zones**.
- Accions en mantenir premut un costat (primera o última pàgina, partitura anterior o
  següent, àudio, metrònom).
- **Exporta en PDF** amb les anotacions dibuixades.
- Mode immersiu amb barres auto-ocultes i pantalla sempre encesa.
- **Inversió de colors** per llegir amb poca llum.
- Salt de pàgina amb control lliscant i marcadors.

### Anotacions
- Bolígraf i marcador fluorescent amb sis colors i tres gruixos.
- Només dibuixa el llapis (S Pen o similar): amb els dits es passa pàgina i es fa zoom.
- Goma per esborrar arrossegant; amb el botó del llapis premut, el llapis fa de goma.
- Desfer i esborrar tota la pàgina; ocultar les anotacions.
- **Lletra a mà → text**: números i dinàmiques escrits a mà es converteixen en text amb
  un reconeixedor propi, entrenat amb la teva lletra i sense internet.
- Anotacions guardades per pàgina i normalitzades, així es veuen igual a qualsevol
  mida de pantalla.

### Llistes de concert
- Crea llistes, afegeix-hi cançons i ordena-les amunt/avall, amb una nota per peça i
  la durada total.
- Mode de reproducció: a l'última pàgina, el botó «Partitura següent» obre la
  següent de la llista. Passar pàgina mai salta de partitura tot sol.
- Estadístiques de cançons, pàgines i partitures anotades.

### Metrònom
- Metrònom integrat amb AudioTrack (clics precisos, sense drifts).
- 30–260 BPM, 2/3/4/6 pulsacions per compàs, accent a la primera.
- Arrenca amb el tempo i el compàs guardats a la partitura.
- Pot passar pàgina cada N compassos, amb compte d'entrada.
- Continua sonant mentre llegeixes i es pot aturar des del visor.

### Afinador
- Afinador cromàtic amb el micròfon: nota, cents i Hz; La de referència de 415 a 466 Hz.

### Dades
- Còpia de seguretat completa en un **ZIP** (metadades + fitxers + anotacions).
- Restauració íntegra de la biblioteca.
- Esborrat complet amb confirmació.

### Detalls
- Tema clar, fosc o automàtic; colors dinàmics opcionals (Android 12+).
- Català, castellà i anglès.
- Icona i imatge de marca pròpies.
- Sense permisos d'emmagatzematge: tot passa pel **Storage Access Framework**.
- Sense xarxa, sense comptes, sense telemetria.

---

## Captures

*(Aviat.)*

---

## Arquitectura

```
app/src/main/java/com/vlcsheets/app
├── MainActivity.kt          Edge-to-edge, gestió de tecles de pedal, intents VIEW
├── MainViewModel.kt         Estat de la biblioteca, filtres, accions, metrònom
├── VlcSheetsApp.kt          Contenidor d'injecció manual
├── data
│   ├── Models.kt            Song, Setlist, Collection, Stroke, Bookmark + JSON
│   ├── LibraryRepository.kt Índex JSON atòmic, importació, còpia, restauració, àudios
│   ├── ChordPro.kt          Lectura de text i ChordPro
│   ├── Handwriting.kt       Reconeixedor de lletra a mà (núvol de punts, $P)
│   ├── MsfReader.kt         Lectura dels .msf exportats de MobileSheets
│   └── SettingsRepository.kt SharedPreferences reactiu
├── media
│   ├── Media.kt             PdfRenderer, BitmapFactory, EXIF, retall de marges, miniatures
│   ├── AudioPlayer.kt       Reproductor dels àudios de cada partitura (MediaPlayer)
│   ├── PdfExporter.kt       PDF amb les anotacions, pàgina a pàgina
│   ├── ChordProPdf.kt       Text i ChordPro convertits en PDF
│   ├── Practice.kt          Punts d'àudio, bucle i pas de pàgina del metrònom
│   ├── Tuner.kt             Detecció de la nota (mètode YIN)
│   └── MetronomeEngine.kt   Generació de clics amb AudioTrack
└── ui
    ├── App.kt               Navegació, snackbars, barra inferior
    ├── theme/Theme.kt       Paleta, tipografia, formes (Material 3)
    ├── components/Common.kt Marca, miniatures, estats buits, diàlegs
    └── screens
        ├── LibraryScreen.kt      Graella/llista, filtres, importació
        ├── SongEditorSheet.kt    Edició de metadades i col·leccions
        ├── SetlistsScreen.kt     Llistes + detall + selector de cançons
        ├── CollectionsScreen.kt  Col·leccions
        ├── SettingsScreen.kt     Ajustos, còpia de seguretat, quant a
        ├── HandwritingTrainer.kt Ensenyar la lletra a mà
        ├── TunerSheet.kt         Afinador
        └── ViewerScreen.kt       Visor, zoom, anotacions, marcadors, metrònom, àudio
```

**Sense base de dades externa ni ORM.** La biblioteca és un `index.json` petit i
atòmic (escriptura a temporal + `fsync` + rename) dins `filesDir/library`, al costat
dels PDFs. Les anotacions de cada partitura van a part (`songs/<id>/annotations.json`),
perquè un traç nou no hagi de reescriure tot l'índex. Això fa que la còpia de seguretat sigui només comprimir una carpeta i que
l'app arrenqui a l'instant.

**Sense llibreries de PDF de tercers.** El rasteritzat usa `android.graphics.pdf.PdfRenderer`.
Les pàgines es renderitzen a unes 1,4 vegades l'amplada de la pantalla
en `ARGB_8888`, l'únic format que accepta `PdfRenderer`; les imatges es descodifiquen
en `RGB_565` per gastar menys memòria.

---

## Compilar i provar

Requisits: JDK 17+ i Android SDK amb la plataforma 36.

```bash
# 1. Configura el SDK
echo "sdk.dir=C\:\\Android\\sdk" > local.properties

# 2. Compila
./gradlew :app:assembleDebug

# 3. Proves i lint
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug

# 4. Instal·la
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L'APK resultant és `app/build/outputs/apk/debug/app-debug.apk`.

---

## Privacitat

VLC Sheets no fa cap connexió de xarxa (no té el permís d'internet). L'únic permís
és el del micròfon, per a l'afinador: es demana el primer cop que l'obres i el so
s'analitza a la tauleta, sense guardar-lo ni enviar-lo. Els fitxers importats es
copien a l'espai privat de l'aplicació i només surten del dispositiu quan tu
exportes un PDF, un `.vlcs` o una còpia de seguretat.

---

## Estat

Versió 1.0.0. Cada versió és a [Releases](../../releases).

package ax.amender;

import ax.amender.io.Directory;
import ax.amender.io.FileProperties;
import ax.amender.uix.TextComponentBinding;
import ax.amender.util.TextCorrectionService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import javax.swing.text.JTextComponent;

/**
 * Gestor global y estático para instalar la corrección de texto.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class Amender {

    private static final Object LOCK = new Object();
    private static final Map<JTextComponent, WeakReference<TextComponentBinding>> BINDINGS = new WeakHashMap<>();

    private static volatile Language language = Language.ES;
    private static volatile File dictionaryDirectory = Directory.DICTIONARY;
    private static volatile File languageDirectory = Directory.LANGUAGE;

    private static volatile TextCorrectionService service = null;
    private static volatile FileProperties languageProperties = null;
    private static final String PROPERTIES_EXT = ".properties";
    private static volatile boolean isInternalResource = false;

    static {
        TextCorrectionService.warmUpHeadless();
    }

    private Amender() {
        //Evita la creación de instancias.
    }

    /**
     * Establece el idioma de trabajo global.
     *
     * @param lang idioma activo
     */
    public static void setLanguage(final Language lang) {
        Objects.requireNonNull(lang, "El idioma no puede ser nulo");
        synchronized (LOCK) {
            if (language != lang) {
                language = lang;
                languageProperties = null; // Invalidar caché de traducción
                if (service != null) {
                    refreshService();
                }
            }
        }
    }

    /**
     * Define el origen y la ruta de los recursos de idioma.
     *
     * @param dir Directorio base donde se encuentran los archivos .properties.
     */
    public static void setLanguagePath(final File dir) {
        Amender.setLanguagePath(dir, false);
    }

    /**
     * Define el origen y la ruta de los recursos de idioma.
     *
     * @param resource true para buscar dentro del JAR, false para buscar en el
     * disco físico.
     * @param dir Directorio base donde se encuentran los archivos .properties.
     */
    public static void setLanguagePath(final File dir, final boolean resource) {
        // Si es externo, usamos getAbsoluteFile(). Si es interno (JAR), mantenemos la ruta tal cual.
        final File nextDir = Objects.requireNonNull(dir, "Ruta no puede ser nula");

        synchronized (LOCK) {
            if (!languageDirectory.equals(nextDir) || isInternalResource != resource) {
                // Solo borramos el directorio si antes era externo y la aplicación ya estaba corriendo
                if (!isInternalResource && service != null) {
                    safeDelete(languageDirectory);
                }
                languageDirectory = resource ? nextDir : nextDir.getAbsoluteFile();
                isInternalResource = resource;
                languageProperties = null; // Fuerza la recarga de traducciones
            }
        }
    }

    /**
     * Define el directorio del diccionario personalizado global.
     *
     * @param dir directorio donde se guardará el archivo del idioma activo
     */
    public static void setDictionaryPath(final File dir) {
        final File nextDir = Objects.requireNonNull(dir, "Ruta no puede ser nula").getAbsoluteFile();
        synchronized (LOCK) {
            if (!dictionaryDirectory.equals(nextDir)) {
                if (service != null) {
                    safeDelete(dictionaryDirectory);
                }
                dictionaryDirectory = nextDir;
                if (service != null) {
                    refreshService();
                }
            }
        }
    }

    /**
     * Obtiene la traducción asociada a una propiedad (clave) según el idioma
     * actual. Si no se encuentra, devuelve la clave proporcionada.
     *
     * @param key la clave de traducción
     * @return el texto traducido, o la clave si no existe la traducción
     */
    public static String translate(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String val = getLanguageProperties().getValue(key);
        return (val != null) ? val : key;
    }

    /**
     * Instala el corrector en un componente de texto Swing.
     *
     * @param comp componente objetivo
     */
    public static void spellChecker(JTextComponent comp) {
        Objects.requireNonNull(comp, "Componente no puede ser nulo");
        synchronized (LOCK) {
            TextComponentBinding binding = lookupBinding(comp);
            if (binding == null) {
                binding = new TextComponentBinding(comp, getService());
                BINDINGS.put(comp, new WeakReference<>(binding));
                binding.install();
            } else {
                binding.reconfigure(getService());
            }
        }
    }

    /**
     * Obtiene el servicio actual. Si no existe, lo inicializa de forma
     * perezosa.
     */
    private static TextCorrectionService getService() {
        TextCorrectionService tcs = Amender.service;
        if (tcs == null) {
            synchronized (LOCK) {
                tcs = Amender.service;
                if (tcs == null) {
                    Amender.service = tcs = TextCorrectionService.create(language, dictionaryDirectory);
                    tcs.warmUp(); // Sincroniza la instancia real en segundo plano
                }
            }
        }
        return tcs;
    }

    /**
     * Obtiene el gestor de propiedades actual para las traducciones. Si no
     * existe o el idioma/ruta cambió, lo inicializa de forma perezosa.
     */
    private static FileProperties getLanguageProperties() {
        FileProperties fp = Amender.languageProperties;
        if (fp == null) {
            synchronized (LOCK) {
                fp = Amender.languageProperties;
                if (fp == null) {
                    String fileName = language.name() + PROPERTIES_EXT;

                    if (isInternalResource) {
                        // --- MODO JAR (Lectura de recursos empaquetados) ---
                        String path = languageDirectory.getPath().replace("\\", "/");
                        if (!path.startsWith("/")) {
                            path = "/" + path;
                        }
                        if (!path.endsWith("/")) {
                            path += "/";
                        }

                        String fullPath = path + fileName;
                        InputStream in = Amender.class.getResourceAsStream(fullPath);

                        // Aquí llamamos al nuevo constructor que añadiremos en el Paso 2
                        Amender.languageProperties = fp = new FileProperties(in);

                    } else {
                        // --- MODO EXTERNO (Lectura/Escritura en disco) ---
                        if (!languageDirectory.exists()) {
                            languageDirectory.mkdirs();
                        }
                        File file = new File(languageDirectory, fileName);
                        ensureFileExists(file);

                        // Llamada clásica al constructor de archivo[cite: 30]
                        Amender.languageProperties = fp = new FileProperties(file);
                    }
                }
            }
        }
        return fp;
    }

    /**
     * Reconstruye el motor de corrección ortográfica y actualiza todos los
     * componentes visuales vinculados. Se invoca cuando cambia el idioma o la
     * ruta del diccionario para asegurar que las nuevas reglas se apliquen de
     * inmediato.
     */
    private static void refreshService() {
        service = TextCorrectionService.create(language, dictionaryDirectory);
        reconfigureBindings(service);
    }

    /**
     * Ejecuta una eliminación preventiva del directorio especificado solo si
     * este existe en el sistema de archivos. Actúa como un guardafuegos para
     * evitar excepciones de tipo NoSuchFileException al intentar limpiar rutas
     * por defecto que nunca llegaron a crearse.
     *
     * @param dir Directorio.
     */
    private static void safeDelete(File dir) {
        if (dir != null && dir.exists()) {
            Directory.delete(dir);
        }
    }

    /**
     * Garantiza la existencia física de un archivo de propiedades en el disco.
     * Si el archivo no existe (por ejemplo, al cambiar a un nuevo idioma cuyas
     * traducciones aún no se han creado), intenta generarlo para evitar errores
     * de lectura en el gestor de propiedades.
     *
     * @param f Archivo.
     */
    private static void ensureFileExists(File f) {
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Propaga un nuevo servicio de corrección a todos los componentes de texto
     * registrados (JTextComponent). Utiliza un flujo de datos para filtrar las
     * referencias que aún están vivas y actualizar su lógica interna sin
     * interferir con el recolector de basura.
     *
     * @param next Siguiente Servicio de coreccion de texto.
     */
    private static void reconfigureBindings(TextCorrectionService next) {
        BINDINGS.values().stream()
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .forEach(b -> b.reconfigure(next));
    }

    /**
     * Recupera la instancia de vinculación (TextComponentBinding) asociada a un
     * componente específico. Al trabajar con WeakReference, este método permite
     * verificar si un componente ya tiene instalado el corrector sin impedir
     * que el componente sea liberado de la memoria si el formulario se cierra.
     *
     * @param comp Componente de texto.
     *
     * @return
     */
    private static TextComponentBinding lookupBinding(JTextComponent comp) {
        WeakReference<TextComponentBinding> ref = BINDINGS.get(comp);
        return (ref != null) ? ref.get() : null;
    }
}

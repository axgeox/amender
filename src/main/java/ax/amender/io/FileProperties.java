package ax.amender.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestor simplificado de archivos de propiedades para la traducción. Solo
 * implementa funciones de lectura necesarias para la internacionalización.
 *
 * @version 1
 * @author Alexy Hernández
 */
public class FileProperties {

    /**
     * Crea una instancia asociada a un archivo físico.
     *
     * @param file Archivo .properties
     */
    public FileProperties(File file) {
        this.file = file;
    }

    /**
     * NUEVO CONSTRUCTOR: Permite cargar propiedades desde un recurso interno
     * (JAR).
     *
     * @param in Flujo de entrada de datos (InputStream). Puede ser nulo si no
     * se encuentra.
     */
    public FileProperties(InputStream in) {
        // Asumiendo que tu clase tiene una variable interna como:
        // this.properties = new java.util.Properties();

        if (in != null) {
            try {
                this.properties.load(in);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(FileProperties.class.getName())
                        .log(java.util.logging.Level.WARNING, "No se pudo leer el archivo de idioma del JAR", ex);
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        } else {
            // Si el InputStream es nulo (el archivo ES.properties no existe en el JAR),
            // la clase FileProperties se instanciará vacía sin provocar bloqueos (crashes).
            java.util.logging.Logger.getLogger(FileProperties.class.getName())
                    .log(java.util.logging.Level.INFO, "Archivo de idioma no encontrado en los recursos del JAR.");
        }
    }

    /**
     * Obtiene el nombre del archivo de propiedades.
     *
     * @return Nombre y extension.
     */
    public String getName() {
        return file.getName();
    }

    /**
     * Obtiene el valor asociado a una clave desde el archivo de propiedades. Si
     * es un archivo externo, lo recarga en cada llamada para reflejar cambios
     * en tiempo real. Si es interno (JAR), lo lee directamente de la memoria.
     *
     * @param key Identificador de la traducción
     * @return El texto traducido o null si no se encuentra o hay un error
     */
    public String getValue(String key) {
        if (key == null) {
            return null;
        }

        // 1. MODO EXTERNO: Si existe 'file', recargamos para leer cambios en tiempo real.
        if (this.file != null) {
            try (FileInputStream fis = new FileInputStream(this.file)) {
                this.properties.load(fis);
            } catch (IOException ex) {
                // Si falla la lectura del archivo en disco, fallamos de forma segura
                return null;
            }
        }

        // 2. RETORNO MAESTRO: Si es JAR, 'file' es null y pasamos directo aquí.
        // Si es externo, ya se actualizó en el bloque anterior y pasa aquí para devolver el valor.
        return this.properties.getProperty(key);
    }

    private File file;
    private final Properties properties = new Properties();
}

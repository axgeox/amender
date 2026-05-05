package ax.amender.io;

import ax.amender.Amender;
import ax.amender.Language;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Directorios disponibles.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public class Directory {

    /**
     * Obtiene el directorio predeterminado del diccionario e idioma.
     */
    public static final File DEFAULT
            = new File(System.getProperty("user.home"), ".amender").getAbsoluteFile();

    /**
     * Obtiene el directorio predeterminado del diccionario.
     */
    public static final File DICTIONARY
            = new File(DEFAULT, "dictionary").getAbsoluteFile();

    /**
     * Obtiene el directorio predeterminado del idioma.
     */
    public static final File LANGUAGE
            = new File(DEFAULT, "language").getAbsoluteFile();

    /**
     * Elimina el directorio con todo y sus carpetas
     *
     * @param dir Directorio raiz a eliminar
     */
    public static void delete(File dir) {
        Path path = dir.toPath();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            System.getLogger(Amender.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * Resuelve y prepara el archivo del del idioma indicado, en el directorio.
     *
     * @param directory directorio base
     * @param language idioma activo
     * @return archivo preparado
     */
    public static File resolveFile(final File directory, final Language language) {
        final File baseDirectory = Objects.requireNonNull(directory, "directory").getAbsoluteFile();
        final Language currentLanguage = Objects.requireNonNull(language, "language");
        final Path directoryPath = baseDirectory.toPath();

        if (Files.exists(directoryPath) && !Files.isDirectory(directoryPath)) {
            throw new IllegalArgumentException("Dictionary path must be a directory");
        }

        final Path dictionaryFile = directoryPath.resolve(currentLanguage.name() + ".txt");

        try {

            // Asegura que el directorio base exista antes de crear el archivo.
            Files.createDirectories(directoryPath);

            if (Files.notExists(dictionaryFile)) {

                // Crea el archivo del idioma si todavía no existe.
                Files.createFile(dictionaryFile);
            }

            return dictionaryFile.toFile();
        } catch (final IOException ex) {
            throw new IllegalStateException("No se pudo preparar el diccionario personalizado.", ex);
        }
    }

}

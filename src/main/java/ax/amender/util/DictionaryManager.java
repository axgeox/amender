package ax.amender.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.languagetool.JLanguageTool;
import org.languagetool.rules.Rule;
import org.languagetool.rules.spelling.SpellingCheckRule;

/**
 * Gestiona la carga, persistencia y consulta del diccionario personalizado.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class DictionaryManager {

    private final Path file;
    private final LinkedHashSet<String> words = new LinkedHashSet<>(128);

    /**
     * Crea un gestor asociado a un archivo de diccionario.
     *
     * @param file archivo del diccionario
     */
    public DictionaryManager(final File file) {
        this.file = Objects.requireNonNull(file, "file").toPath();

        // Carga las palabras persistidas al iniciar el gestor.
        load();
    }

    /**
     * Indica si una palabra ya está ignorada.
     *
     * @param word palabra a consultar
     * @return si la palabra existe en el diccionario
     */
    public synchronized boolean contains(final String word) {

        // Normaliza la palabra antes de consultar el conjunto.
        return words.contains(normalize(word));
    }

    /**
     * Agrega una palabra al diccionario y la persiste.
     *
     * @param word palabra a guardar
     * @return palabra normalizada si fue agregada
     */
    public synchronized Optional<String> add(final String word) {
        final String normalizedWord = normalize(word);

        if (normalizedWord.isBlank() || words.contains(normalizedWord)) {
            return Optional.empty();
        }

        // Inserta la nueva palabra antes de persistir el archivo.
        words.add(normalizedWord);

        try {

            // Guarda el estado actualizado del diccionario en disco.
            save();
            return Optional.of(normalizedWord);
        } catch (final RuntimeException ex) {

            // Revierte la inserción si la persistencia falla.
            words.remove(normalizedWord);
            throw ex;
        }
    }

    /**
     * Aplica todas las palabras ignoradas al analizador activo.
     *
     * @param languageTool analizador objetivo
     */
    public synchronized void applyTo(final JLanguageTool languageTool) {
        final List<String> tokens = List.copyOf(words);

        if (tokens.isEmpty()) {
            return;
        }

        // Propaga todas las palabras ignoradas a las reglas ortográficas.
        applyTokens(languageTool, tokens);
    }

    /**
     * Aplica una colección concreta de palabras ignoradas.
     *
     * @param languageTool analizador objetivo
     * @param tokens palabras a ignorar
     */
    public static void applyTokens(final JLanguageTool languageTool, final List<String> tokens) {
        final JLanguageTool tool = Objects.requireNonNull(languageTool, "languageTool");
        final List<String> ignoredTokens = List.copyOf(tokens);

        if (ignoredTokens.isEmpty()) {
            return;
        }

        // Recorre las reglas para extender solo las de ortografía.
        for (final Rule rule : tool.getAllRules()) {
            if (rule instanceof SpellingCheckRule spellingRule) {

                // Registra las palabras personalizadas como ignoradas.
                spellingRule.addIgnoreTokens(ignoredTokens);
            }
        }
    }

    /**
     * Lee el diccionario desde disco.
     */
    private void load() {
        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            // Optimización: Uso de Stream API para procesar y normalizar sin bucles manuales pesados
            lines.map(this::normalize)
                    .filter(s -> !s.isBlank())
                    .forEach(words::add);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el diccionario personalizado.", ex);
        }
    }

    /**
     * Persiste el diccionario actual en disco.
     */
    private void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            // Escribe cada palabra ignorada en una línea independiente.
            for (final String word : words) {
                writer.write(word);
                writer.newLine();
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("No se pudo guardar el diccionario personalizado.", ex);
        }
    }

    /**
     * Normaliza una palabra para usarla como clave del diccionario.
     *
     * @param word palabra original
     * @return palabra normalizada
     */
    private String normalize(final String word) {
        // Optimización: Evitar trim() y toLowerCase() si la palabra ya cumple (ahorro de String pool)
        if (word == null || word.isEmpty()) {
            return "";
        }
        return word.strip().toLowerCase(Locale.ROOT);
    }
}

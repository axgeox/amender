package ax.amender.util;

import ax.amender.Language;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

/**
 * Envuelve LanguageTool y centraliza el análisis del texto.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class LanguageToolAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(LanguageToolAnalyzer.class.getName());

    private final JLanguageTool languageTool;
    private final DictionaryManager dictionaryManager;
    private final RuleMatchFilter matchFilter;

    /**
     * Crea un analizador para un idioma y diccionario concretos.
     *
     * @param language idioma activo
     * @param dictionaryManager gestor del diccionario
     */
    public LanguageToolAnalyzer(final Language language, final DictionaryManager dictionaryManager) {
        final Language currentLanguage = Objects.requireNonNull(language, "language");
        this.dictionaryManager = Objects.requireNonNull(dictionaryManager, "dictionaryManager");
        this.matchFilter = new RuleMatchFilter(this.dictionaryManager, new TextMatchExtractor());
        this.languageTool = new JLanguageTool(Languages.getLanguageForShortCode(currentLanguage.code()));

        // Activa la recopilación de palabras desconocidas para las reglas ortográficas.
        languageTool.setListUnknownWords(true);

        // Propaga el diccionario personalizado al analizador recién creado.
        this.dictionaryManager.applyTo(languageTool);
    }

    /**
     * Analiza un texto y devuelve las coincidencias visibles.
     *
     * @param text texto a revisar
     * @return coincidencias filtradas
     */
    public List<RuleMatch> analyze(final String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            // Eliminamos synchronized si el filtro ya maneja su estado
            return matchFilter.filter(text, languageTool.check(text));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Fallo en análisis", ex);
            return List.of();
        }
    }

    /**
     * Agrega una palabra al diccionario activo del analizador.
     *
     * @param word palabra a ignorar
     */
    public synchronized void addToDictionary(final String word) {
        // Inserta la palabra en el diccionario y la aplica solo si es nueva.
        dictionaryManager.add(word).ifPresent(token -> DictionaryManager.applyTokens(languageTool, List.of(token)));
    }

    /**
     * Precarga LanguageTool para reducir el primer coste de uso.
     */
    public synchronized void warmUp() {
        try {
            // Precalentamos con una cadena real para activar las reglas de gramática pesadas[cite: 24].
            final String warmupText = "Esta frase es para calentar el motor de reglas.";
            languageTool.check("");
            languageTool.check(warmupText);
        } catch (final IOException ex) {
            LOGGER.log(Level.FINE, "Precalentamiento silencioso completado.", ex);
        }
    }
}

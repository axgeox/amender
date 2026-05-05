package ax.amender.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.languagetool.rules.RuleMatch;

/**
 * Utilidades para clasificar y consultar coincidencias sintácticas.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class SyntaxAnalysisUtils {

    /**
     * Tipos lógicos de incidencias detectadas.
     */
    public enum IssueType {
        SPELLING,
        GRAMMAR,
        STYLE,
        PUNCTUATION,
        OTHER
    }

    /**
     * Evita la creación de instancias utilitarias.
     */
    private SyntaxAnalysisUtils() {

        // Impide instanciar la utilidad estática.
        throw new AssertionError("No instances");
    }

    /**
     * Resuelve el tipo lógico de una coincidencia.
     *
     * @param match coincidencia detectada
     * @return tipo lógico resuelto
     */
    public static IssueType resolveType(final RuleMatch match) {
        // Normalizamos una sola vez para ahorrar ciclos de CPU y memoria[cite: 26].
        final String catId = match.getRule().getCategory().getId().toString().toLowerCase(Locale.ROOT);
        final String ruleId = match.getRule().getId().toLowerCase(Locale.ROOT);

        // 1. PRIORIDAD: ORTOGRAFÍA (El error más común)[cite: 26]
        // Captura "MORFOLOGIK", "HUNSPELL", "TYPOS", "SPELL_CHECK", etc.
        if (catId.contains("spell") || catId.contains("typo")
                || ruleId.contains("morfol") || ruleId.contains("spell")) {
            return IssueType.SPELLING;
        }

        // 2. GRAMÁTICA Y CONCORDANCIA[cite: 26]
        // Captura "AGREEMENT", "SYNTAX", "TENSE", "GENDER_BIAS", etc.
        if (catId.contains("gramm") || catId.contains("agree")
                || catId.contains("syntax") || ruleId.contains("verb")) {
            return IssueType.GRAMMAR;
        }

        // 3. PUNTUACIÓN Y ESPACIADO[cite: 26]
        // Captura "PUNCTUATION", "WHITESPACE", "COMMA", "QUOTES".
        if (catId.contains("punct") || catId.contains("space")
                || ruleId.contains("comma") || ruleId.contains("point")) {
            return IssueType.PUNCTUATION;
        }

        // 4. ESTILO Y REDACCIÓN[cite: 26]
        // Captura "STYLE", "REDUNDANCY", "READABILITY", "LONG_SENTENCE".
        if (catId.contains("style") || catId.contains("readab")
                || catId.contains("plain") || ruleId.contains("redundant")) {
            return IssueType.STYLE;
        }

        // Fallback para reglas misceláneas[cite: 26].
        return IssueType.OTHER;
    }

    /**
     * Busca una coincidencia en una posición concreta del texto.
     *
     * @param matches coincidencias disponibles
     * @param position posición consultada
     * @return coincidencia encontrada, si existe
     */
    public static Optional<RuleMatch> findMatchAt(final List<RuleMatch> matches, final int position) {

        // Recorre la lista hasta encontrar la coincidencia que cubre la posición.
        for (final RuleMatch match : matches) {
            if (contains(match, position)) {
                return Optional.of(match);
            }
        }

        return Optional.empty();
    }

    /**
     * Indica si una coincidencia cubre una posición dada.
     *
     * @param match coincidencia consultada
     * @param position posición del texto
     * @return si la coincidencia cubre la posición
     */
    public static boolean contains(final RuleMatch match, final int position) {
        final RuleMatch currentMatch = Objects.requireNonNull(match, "match");

        // Comprueba si la posición cae dentro del rango marcado.
        return position >= currentMatch.getFromPos() && position < currentMatch.getToPos();
    }

    /**
     * Obtiene un subconjunto ordenado de sugerencias.
     *
     * @param match coincidencia analizada
     * @param limit número máximo de resultados
     * @return sugerencias limitadas
     */
    public static List<String> firstSuggestions(final RuleMatch match, final int limit) {
        final RuleMatch currentMatch = Objects.requireNonNull(match, "match");

        if (limit <= 0) {
            return List.of();
        }

        final List<String> suggestions = currentMatch.getSuggestedReplacements();
        final int size = Math.min(limit, suggestions.size());
        final List<String> limitedSuggestions = new ArrayList<>(size);

        // Copia solo la cantidad solicitada de sugerencias visibles.
        for (int index = 0; index < size; index++) {
            limitedSuggestions.add(suggestions.get(index));
        }

        return List.copyOf(limitedSuggestions);
    }
}

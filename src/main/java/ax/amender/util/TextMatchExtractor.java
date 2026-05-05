package ax.amender.util;

import org.languagetool.rules.RuleMatch;

/**
 * Extrae fragmentos de texto a partir de las posiciones de una coincidencia.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class TextMatchExtractor {

    /**
     * Obtiene el texto cubierto por una coincidencia.
     *
     * @param text texto completo
     * @param match coincidencia detectada
     * @return fragmento asociado
     */
    public String extract(final String text, final RuleMatch match) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Usamos variables locales para evitar múltiples llamadas a métodos del objeto 'match'.
        final int length = text.length();
        int start = match.getFromPos();
        int end = match.getToPos();

        // Clamp magistral: Asegura que los índices jamás rompan el String.substring[cite: 28].
        if (start < 0) {
            start = 0;
        }
        if (end > length) {
            end = length;
        }
        if (start >= end) {
            return "";
        }

        return text.substring(start, end);
    }
}

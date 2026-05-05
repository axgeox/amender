package ax.amender.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.languagetool.rules.RuleMatch;

/**
 * Filtra coincidencias según el diccionario personalizado activo.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class RuleMatchFilter {

    private final DictionaryManager dictionaryManager;
    private final TextMatchExtractor extractor;

    /**
     * Crea un filtro enlazado a un diccionario y a un extractor de texto.
     *
     * @param dictionaryManager gestor del diccionario
     * @param extractor extractor de fragmentos
     */
    public RuleMatchFilter(final DictionaryManager dictionaryManager, final TextMatchExtractor extractor) {
        this.dictionaryManager = Objects.requireNonNull(dictionaryManager, "dictionaryManager");
        this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    /**
     * Elimina las coincidencias que ya están ignoradas por el usuario.
     *
     * @param text texto analizado
     * @param matches coincidencias detectadas
     * @return coincidencias visibles
     */
    public List<RuleMatch> filter(final String text, final List<RuleMatch> matches) {
        if (matches.isEmpty()) {
            return List.of();
        }

        // Optimización: Pre-asignar capacidad inicial para evitar expansiones de memoria
        final List<RuleMatch> filtered = new ArrayList<>(matches.size());
        for (int i = 0; i < matches.size(); i++) {
            RuleMatch m = matches.get(i);
            if (!dictionaryManager.contains(extractor.extract(text, m))) {
                filtered.add(m);
            }
        }
        return filtered; // Retornar la lista directamente si no se requiere inmutabilidad estricta
    }
}

package ax.amender.util;

import ax.amender.Language;
import ax.amender.io.Directory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.languagetool.rules.RuleMatch;

/**
 * Agrupa los servicios de lógica para una configuración concreta.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class TextCorrectionService {

    private final LanguageToolAnalyzer analyzer;
    private final AtomicBoolean warmedUp = new AtomicBoolean();

    /**
     * Crea un servicio lógico completo.
     *
     * @param language idioma activo
     * @param dictionaryDirectory directorio de diccionario
     * @param analyzer analizador configurado
     */
    private TextCorrectionService(final LanguageToolAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    /**
     * Construye un servicio listo para producción.
     *
     * @param language idioma activo
     * @param dictionaryDirectory directorio base del diccionario
     * @return servicio configurado
     */
    public static TextCorrectionService create(final Language language, final File dictionaryDirectory) {
        final Language currentLanguage = Objects.requireNonNull(language, "language");
        final File baseDirectory = Objects.requireNonNull(dictionaryDirectory, "dictionaryDirectory").getAbsoluteFile();

        final File dictionaryFile = Directory.resolveFile(baseDirectory, currentLanguage);
        final DictionaryManager dictionaryManager = new DictionaryManager(dictionaryFile);
        final LanguageToolAnalyzer analyzer = new LanguageToolAnalyzer(currentLanguage, dictionaryManager);

        return new TextCorrectionService(analyzer);
    }

    /**
     * Analiza un texto usando la configuración activa.
     *
     * @param text texto a revisar
     * @return coincidencias visibles
     */
    public List<RuleMatch> analyze(final String text) {
        return analyzer.analyze(text);
    }

    /**
     * Agrega una palabra al diccionario activo.
     *
     * @param word palabra a ignorar
     */
    public void addToDictionary(final String word) {
        analyzer.addToDictionary(word);
    }

    /**
     * Ejecuta una única vez el precalentamiento del analizador.
     */
    public void warmUp() {
        if (warmedUp.compareAndSet(false, true)) {
            Async.work(analyzer::warmUp);
        }
    }

    /**
     * Precalienta los motores subyacentes de LanguageTool en segundo plano SIN
     * tocar el disco duro ni instanciar gestores de diccionarios. Útil para
     * precargar clases de Java al inicio de la aplicación.
     */
    public static void warmUpHeadless() {
        Async.work(() -> {
            try {
                // Instancia pura en memoria del motor (por defecto Español)
                org.languagetool.JLanguageTool tool = new org.languagetool.JLanguageTool(
                        org.languagetool.Languages.getLanguageForShortCode(Language.ES.code())
                );
                tool.check("Inicializando corrector ortográfico.");
            } catch (IOException ignored) {
                // Precalentamiento silencioso, no requiere manejo de errores.
            }
        });
    }
}

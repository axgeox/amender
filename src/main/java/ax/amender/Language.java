package ax.amender;

/**
 * Enumerador de Lenguajes disponibles.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public enum Language {

    AR("Arabic"),
    AST("Asturian"),
    BE("Belarusian"),
    BR("Breton"),
    CA("Catalan"),
    CRH("Crimean Tatar"),
    DA("Danish"),
    DE("German"),
    EL("Greek"),
    EN("English (US)"),
    EN_GB("English (UK)"),
    EO("Esperanto"),
    ES("Spanish"),
    FA("Persian"),
    FR("French"),
    GA("Irish"),
    IS("Icelandic"),
    IT("Italian"),
    JA("Japanese"),
    KM("Khmer"),
    LT("Lithuanian"),
    ML("Malayalam"),
    NL("Dutch"),
    PL("Polish"),
    PT("Portuguese"),
    RO("Romanian"),
    RU("Russian"),
    SK("Slovak"),
    SL("Slovenian"),
    SR("Serbian"),
    SV("Swedish"),
    TA("Tamil"),
    TL("Filipino"),
    UK("Ukrainian");

    private final String defaultName;

    // Construir nombre por defecto
    Language(String defaultName) {
        this.defaultName = defaultName;
    }

    /**
     * Obtener el nombre de esta constante en minuscula.
     *
     * @return Nombre de constante en minuscula.
     */
    public final String code() {
        String code = name().toLowerCase();
        return switch (this) {
            case DE -> code + "-DE";
            case EN -> code + "-US";
            case EN_GB -> "en-GB";
            default -> code;
        };
    }

    /**
     * Obtener Nombre por defecto de cada constante.
     *
     * @return Nombre por defecto.
     */
    public final String defaultName() {
        return defaultName;
    }
}

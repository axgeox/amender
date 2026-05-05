# Amender 

**Amender** es una librería de alto rendimiento para Java Swing dedicada a la **corrección gramatical, ortográfica y gestión de internacionalización (I18n)**. Construida sobre el motor de **JLanguageTool**, optimizada para el análisis mediante hilos asíncronos y un sistema de renderizado visual de baja latencia que no bloquea la interfaz de usuario.

---

## 🖇️ Dependencia

Añade la dependencia a tu proyecto mediante JitPack: [![](https://jitpack.io/v/axgeox/amender.svg)](https://jitpack.io/#axgeox/amender).

---

## 📦 Paquetes Principales

Solo necesitas dos clases para empezar a trabajar:

```java
import ax.amender.Amender;
import ax.amender.Language;
```

---

## 🌐 Gestión de Idiomas

Por defecto, Amender utiliza el idioma Español (`ES`) antes de iniciar cualquier proceso de corrección o internacionalización.

Puedes consultar los idiomas disponibles mediante el algoritmo:

```java
for (Language lang : Language.values()) {
    System.out.printf("Código: %s | Nombre: %s%n", lang.name(), lang.defaultName());
}
```

Utiliza este metodo para establecer el idioma activo:

```java
Amender.setLanguage(Language.ES);
```

> [!NOTE]
> El idioma establecido será utilizado tanto para la corrección gramatical como para la internacionalización.

---

## 📁 Configuracion de Rutas

Amender crea automáticamente un entorno de trabajo en `C:\Users\[nombre]\.amender\directory\`. En este directorio se generan los archivos de diccionario correspondientes a cada idioma configurado. Estos archivos almacenan las palabras que el usuario decide ignorar durante la corrección.

Puedes definir un directorio personalizado para almacenar los archivos `.txt` de palabras ignoradas:

```java
Amender.setDictionaryPath(new File("dir"));
```

También puedes establecer el directorio donde se almacenan los archivos `.properties` utilizados para la traducción del idioma activo.

Si tu intencion es establecer una ruta donde todos los archivos están en una carpeta física en el ordenador del usuario (Lectura/Escritura):

```java
Amender.setLanguagePath(new File("C:/user/language/"));
```

Si deseas establecer una ruta donde los archivos `.properties` están empaquetados dentro del proyecto/JAR (Solo lectura):

```java
Amender.setLanguagePath(new File("/language/"), true);
```

> [!IMPORTANT]  
> Este método es obligatorio cuando se utiliza la internacionalización.  
> El programador debe colocar en el directorio los archivos de recursos necesarios para cada idioma.  
> Amender cargará automáticamente las traducciones según el idioma establecido con `setLanguage(lang)`.

---

## 📝 Corrección de Ortografía y Gramática

El módulo de corrección automática ofrece un sistema intuitivo basado en un menú emergente y subrayados dinámicos aplicados directamente sobre el componente de texto seleccionado.

Para activar la corrección en un `JTextComponent`, utiliza:

```java
Amender.spellChecker(jTextComponent);
```

Amender utiliza los siguientes colores estándar para identificar cada tipo de incidencia:

* 🟥 Errores ortográficos.  
* 🟦 Problemas gramaticales y de concordancia.  
* 🟧 Sugerencias de estilo, redundancia o mejoras de redacción.  
* 🟪 Errores de puntuación.  
* 🟩 Otros tipos de incidencias no clasificadas.  

---

## 🌍 Internacionalización Dinámica

El sistema de internacionalización de Amender permite cambiar el idioma de toda la interfaz en tiempo real. Para ello, el programador debe proporcionar los archivos `.properties` correspondientes a cada idioma que la aplicación soporte.

### 📂 Estructura de Archivos de Traducción

Los archivos deben colocarse dentro del directorio definido mediante `Amender.setLanguagePath(dir)`, y cada archivo debe llevar el nombre del idioma según la nomenclatura del enum `Language`.

**Ejemplo de carpeta: `Language/`**

```
Language/
 ├── ES.properties
 └── EN.properties
```

### 🗝 Formato Clave/Valor

Todos los archivos `.properties` deben compartir las mismas claves, cambiando únicamente el valor traducido.

**Archivo: `ES.properties`**
```properties
HELLO = Hola Mundo
```

**Archivo: `EN.properties`**
```properties
HELLO = Hello world
```

### 🔄 Obtener Traducciones

Puedes obtener cualquier traducción mediante:

```java
Amender.translate("HELLO");
```
>[!NOTE]
>Amender buscará automáticamente el archivo `.properties` correspondiente al idioma activo y devolverá el valor asociado a la clave solicitada.

---

## 🛠 Requisitos Técnicos

* Java 25+

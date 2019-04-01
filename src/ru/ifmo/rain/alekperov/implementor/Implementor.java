package ru.ifmo.rain.alekperov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * A simple implementation of the {@link JarImpler} interface.
 */
public class Implementor implements JarImpler {

    /**
     * A wrapper for {@link Method methods} used to store them in a {@link Set set} or a {@link Map map}
     * with custom {@link Object#equals(Object)} and {@link Object#hashCode()} implementations.
     */
    private static class MethodWrapper {

        /**
         * The wrapped instance of {@link Method}.
         */
        private final Method method;

        /**
         * The base used for calculating {@link #hashCode()}.
         */
        private final static int BASE = 23;
        /**
         * The module used for calculating {@link #hashCode()}.
         */
        private final static int MOD = 1000992299;

        /**
         * Constructs a wrapper for the provided {@link Method method}.
         *
         * @param method the method to wrap.
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Indicates whether another object is "equal to" this one.
         *
         * @param object the reference object with which to compare.
         * @return {@code true} if the other object is an instance of a {@link MethodWrapper} and their wrapped
         * methods have equal names, return types and parameter types.
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object object) {
            if (object instanceof MethodWrapper) {
                final var other = (MethodWrapper) object;
                return Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes())
                        && method.getReturnType().equals(other.method.getReturnType())
                        && method.getName().equals(other.method.getName());
            }
            return false;
        }

        /**
         * Returns a hash code value for the object. Follows the general contract of {@link Object#hashCode()}.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return ((Arrays.hashCode(method.getParameterTypes())
                    + BASE * method.getReturnType().hashCode()) % MOD
                    + method.getName().hashCode() * BASE * BASE) % MOD;
        }

        /**
         * Returns the wrapped method.
         *
         * @return the wrapped method.
         */
        Method getMethod() {
            return method;
        }

    }

    /**
     * The suffix added to the name of the interface being implemented to generate the name of the resulting class.
     */
    private static final String IMPL_SUFFIX = "Impl";
    /**
     * The suffix added to the name of the resulting class to generate the name of the generated source file.
     */
    private static final String SOURCE_EXTENSION = ".java";
    /**
     * The suffix added to the name of the resulting class to generate the name of the compiled class file.
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * The string used as a space in the generated source file.
     */
    private static final String SPACE = " ";
    /**
     * The string used as a comma in the generated source file.
     */
    private static final String COMMA = ",";
    /**
     * The string used as a semicolon in the generated source file.
     */
    private static final String SEMICOLON = ";";
    /**
     * The string used as an opening brace in the generated source file.
     */
    private static final String OPENING_BRACE = "{";
    /**
     * The string used as a closing brace in the generated source file.
     */
    private static final String CLOSING_BRACE = "}";
    /**
     * The string used as a opening parenthesis in the generated source file.
     */
    private static final String OPENING_PARENTHESIS = "(";
    /**
     * The string used as a closing parenthesis in the generated source file.
     */
    private static final String CLOSING_PARENTHESIS = ")";
    /**
     * The string used as an opening angle bracket in the generated source file.
     */
    private static final String OPENING_ANGLE_BRACKET = "<";
    /**
     * The string used as a closing angle bracket in the generated source file.
     */
    private static final String CLOSING_ANGLE_BRACKET = ">";
    /**
     * The string used as an indentation in the generated source file.
     */
    private static final String INDENTATION = "    ";
    /**
     * The string used as an end of line in the generated source file.
     */
    private static final String EOLN = System.lineSeparator();

    /**
     * The string used as a {@code package} keyword in the generated source file.
     */
    private static final String PACKAGE = "package";
    /**
     * The string used as a {@code public} keyword in the generated source file.
     */
    private static final String PUBLIC = "public";
    /**
     * The string used as a {@code class} keyword in the generated source file.
     */
    private static final String CLASS = "class";
    /**
     * The string used as an {@code implements} keyword in the generated source file.
     */
    private static final String IMPLEMENTS = "implements";
    /**
     * The string used as an {@code extends} keyword in the generated source file.
     */
    private static final String EXTENDS = "extends";
    /**
     * The string used as a {@code super} keyword in the generated source file.
     */
    private static final String SUPER = "super";
    /**
     * The string used as a {@code return} keyword in the generated source file.
     */
    private static final String RETURN = "return";
    /**
     * The string used as a {@code throws} keyword in the generated source file.
     */
    private static final String THROWS = "throws";

    /**
     * The empty string.
     */
    private static final String EMPTY = "";
    /**
     * The string used as a {@code false} value in the generated source file.
     */
    private static final String FALSE = "false";
    /**
     * The string used as a {@code 0} value in the generated source file.
     */
    private static final String ZERO = "0";
    /**
     * The string used as a {@code null} value in the generated source file.
     */
    private static final String NULL = "null";

    /**
     * Collects the {@link String strings} in the {@link Stream stream} by joining them
     * with a {@link #COMMA} and a {@link #SPACE}.
     *
     * @param stream the stream of strings to collect.
     * @return the resulting string.
     * @throws NullPointerException if {@code stream} is {@code null}.
     */
    private static String collectCommaList(final Stream<String> stream) {
        return stream.collect(Collectors.joining(COMMA + SPACE));
    }

    /**
     * Collects the {@link String strings} in the {@link Stream stream} by joining them
     * with a {@link #COMMA} and a {@link #SPACE} and then surrounds the resulting string
     * with an {@link #OPENING_PARENTHESIS} and a {@link #CLOSING_PARENTHESIS}.
     *
     * @param stream the stream of strings to collect.
     * @return the resulting string.
     * @throws NullPointerException if {@code stream} is {@code null}.
     */
    private static String collectParameters(final Stream<String> stream) {
        return OPENING_PARENTHESIS + collectCommaList(stream.map(s -> s.replace('$', '.'))) + CLOSING_PARENTHESIS;
    }

    /**
     * Transforms the {@link String string} by {@link Function#apply(Object) applying} the provided transformer
     * {@link Function function} if the string is not equal to {@link #EMPTY}.
     *
     * @param string      the string to transform.
     * @param transformer the transformer function.
     * @return {@code transformer.apply(string)} if the string is considered empty, {@link #EMPTY} otherwise.
     * @throws NullPointerException if {@code string} is {@code null} or
     *                              if the string is not considered empty and {@code transformer} is {@code null}.
     */
    private static String transformIfNotEmpty(final String string, final Function<String, String> transformer) {
        if (!string.equals(EMPTY)) {
            return transformer.apply(string);
        }
        return EMPTY;
    }

    /**
     * Collects the {@link String strings} in the {@link Stream stream} by joining them
     * with a {@link #COMMA} and a {@link #SPACE} and then prepends a {@link #THROWS} keyword
     * surrounded by {@link #SPACE}s to the resulting string if it is not equal to {@link #EMPTY}.
     *
     * @param stream the stream of strings to collect.
     * @return the resulting string.
     * @throws NullPointerException if the stream is null.
     */
    private static String collectExceptions(final Stream<String> stream) {
        return transformIfNotEmpty(collectCommaList(stream), s -> SPACE + THROWS + SPACE + s);
    }

    /**
     * Collects the {@link String strings} in the {@link Stream stream} by joining them
     * with a {@link #COMMA} and a {@link #SPACE} and then surrounds the resulting string
     * an {@link #OPENING_ANGLE_BRACKET} and a {@link #CLOSING_ANGLE_BRACKET} if it is not equal to {@link #EMPTY}.
     *
     * @param stream the stream of strings to collect.
     * @return the resulting string.
     * @throws NullPointerException if {@code stream} is {@code null}.
     */
    private static String collectGenericParameters(final Stream<String> stream) {
        return transformIfNotEmpty(collectCommaList(stream), s -> OPENING_ANGLE_BRACKET + s + CLOSING_ANGLE_BRACKET);
    }

    /**
     * Generates a name for a class implementing the interface (or extending the class)
     * represented by the provided {@link Class class}.
     *
     * @param clazz the interface to implement (or the class to extend).
     * @return the simple name of the provided class concatenated with {@link #IMPL_SUFFIX}.
     * @throws NullPointerException if {@code clazz} is {@code null}.
     */
    private static String getImplName(final Class<?> clazz) {
        return clazz.getSimpleName().concat(IMPL_SUFFIX);
    }

    /**
     * Returns a {@link Path path} formed by joining the provided path,
     * the package name of the provided class converted to a path,
     * a generated name for the successor of the provided class, and the provided extension.
     *
     * @param clazz     the interface to implement (or the class to extend).
     * @param path      the path to the parent directory of the desired file.
     * @param extension the desired file extension.
     * @return the resulting path.
     * @throws NullPointerException if {@code clazz}, {@code path} or {@code extension} is {@code null}.
     */
    private static Path getImplFilePath(final Class<?> clazz, final Path path, final String extension) {
        return path.resolve(clazz.getPackage().getName().replace('.', File.separatorChar)).
                resolve(getImplName(clazz).concat(extension));
    }

    /**
     * Returns a {@link String string} representing the default value for the provided {@link Class class}.
     *
     * <p>The default value is defined as {@link #FALSE} for {@code boolean.class},
     * {@link #EMPTY} for {@code void.class}, {@link #ZERO} for any other primitive, and {@link #NULL} for all other.</p>
     *
     * @param clazz the class to return the default value for.
     * @return the default value.
     * @throws NullPointerException if {@code clazz} is {@code null}.
     */
    private static String getDefaultValue(final Class<?> clazz) {
        if (clazz.equals(boolean.class)) {
            return FALSE;
        } else if (clazz.equals(void.class)) {
            return EMPTY;
        } else if (clazz.isPrimitive()) {
            return ZERO;
        }
        return NULL;
    }

    /**
     * Returns a {@link String string} representing the return type name and the name
     * of the provided {@link Executable executable}.
     *
     * @param executable the executable to return the return type name and the name of.
     * @return the {@link #getImplName(Class) implementation name} if {@code executable} is
     * an instance of {@link Constructor}, the return type name and the name of {@code executable}
     * joined with a {@link #SPACE} otherwise.
     * @throws NullPointerException if {@code executable} is {@code null}.
     */
    private static String getExecutableTypeAndName(final Executable executable) {
        if (executable instanceof Constructor) {
            return getImplName(executable.getDeclaringClass());
        } else {
            return executable.getAnnotatedReturnType().getType().getTypeName().replace('$', '.') + SPACE + executable.getName();
        }
    }

    /**
     * Converts the given {@link String string} to a string with escape sequences
     * instead of non-ASCII compatible chars.
     *
     * @param string the string to convert.
     * @return the converted string.
     * @throws NullPointerException if {@code string} is {@code null}.
     */
    private static String escapeUnicode(final String string) {
        final var sb = new StringBuilder();
        for (final char c : string.toCharArray()) {
            if (c >= 128) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Puts all abstract {@link Method methods} present in the given array in the given {@link Set set}.
     *
     * @param methods    the array of methods to add.
     * @param collection the set to be filled with methods.
     * @throws NullPointerException if {@code methods} or {@code collection} is {@code null}.
     */
    private static void addAbstractMethods(final Method[] methods, final Set<MethodWrapper> collection) {
        Arrays.stream(methods).filter(method -> Modifier.isAbstract(method.getModifiers())).map(MethodWrapper::new)
                .collect(Collectors.toCollection(() -> collection));
    }

    /**
     * Returns a {@link Set set} filled with abstract {@link Method methods} have to be implemented in a successor
     * of the specified {@link Class class} to be non-abstract.
     *
     * @param clazz the class to be checked.
     * @return the set filled with the required methods.
     * @throws NullPointerException if {@code clazz} is {@code null}.
     */
    private static Set<MethodWrapper> getAbstractMethods(final Class<?> clazz) {
        final Set<MethodWrapper> collection = new HashSet<>();
        addAbstractMethods(clazz.getMethods(), collection);
        for (var token = clazz; token != null; token = token.getSuperclass()) {
            addAbstractMethods(token.getDeclaredMethods(), collection);
        }
        return collection;
    }

    /**
     * Returns a {@link String string} consisting of the names of the type parameters
     * of the specified {@link GenericDeclaration generic declaration} separated with {@link #COMMA}s
     * and surrounded by an {@link #OPENING_ANGLE_BRACKET} and a {@link #CLOSING_ANGLE_BRACKET}.
     *
     * @param decl the declaration to collect type parameters of.
     * @return the resulting string.
     * @throws NullPointerException if {@code decl} is {@code null}.
     */
    private static String getTypeParametersString(final GenericDeclaration decl) {
        return collectGenericParameters(Arrays.stream(decl.getTypeParameters()).map(TypeVariable::getName));
    }

    /**
     * Creates all nonexistent parent directories.
     *
     * @param path the path to create parent directories of.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    private static void createParentDirectories(final Path path) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Returns a {@link Writer writer} to be used for writing the generated successor
     * of the provided {@link Class class} with the class path equal to the provided {@link Path path}.
     *
     * @param clazz the class to generate successor of.
     * @param path  the class path.
     * @return the prepared writer.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code clazz} or {@code path} is {@code null}.
     */
    private static Writer prepareWriter(final Class<?> clazz, final Path path) throws IOException {
        final var sourcePath = getImplFilePath(clazz, path, SOURCE_EXTENSION);
        createParentDirectories(sourcePath);
        return Files.newBufferedWriter(sourcePath, StandardOpenOption.CREATE);
    }

    /**
     * Writes the header of the generated successor of the specified {@link Class class}
     * using the specified {@link Writer writer}.
     *
     * @param clazz  the class to generate and write successor of.
     * @param writer the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code clazz} or {@code writer} is {@code null}.
     */
    private static void writeHeader(final Class<?> clazz, final Writer writer) throws IOException {
        writer.append(PACKAGE).append(SPACE).append(clazz.getPackageName()).append(SEMICOLON).append(EOLN);
        writer.write(EOLN);
        writer.append(PUBLIC).append(SPACE).append(CLASS).
                append(SPACE).append(escapeUnicode(getImplName(clazz))).append(escapeUnicode(getTypeParametersString(clazz))).
                append(SPACE).append(clazz.isInterface() ? IMPLEMENTS : EXTENDS).
                append(SPACE).append(escapeUnicode(clazz.getSimpleName())).append(escapeUnicode(getTypeParametersString(clazz))).
                append(SPACE).append(OPENING_BRACE).append(EOLN);
    }

    /**
     * Writes the body of the generated implementation of the provided {@link Constructor constructor}
     * using the specified {@link Writer writer}.
     *
     * @param constructor the constructor to generate and write.
     * @param writer      the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code constructor} or {@code writer} is {@code null}.
     */
    private static void writeConstructorBody(final Constructor constructor, final Writer writer) throws IOException {
        writer.append(INDENTATION).append(INDENTATION).append(SUPER).
                append(escapeUnicode(collectParameters(Arrays.stream(constructor.getParameters()).map(Parameter::getName)))).
                append(SEMICOLON).append(EOLN);
    }

    /**
     * Writes the body of the generated implementation of the provided {@link Method method}
     * using the specified {@link Writer writer}.
     *
     * @param method the method to generate and write.
     * @param writer the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code method} or {@code writer} is {@code null}.
     */
    private static void writeMethodBody(final Method method, final Writer writer) throws IOException {
        writer.append(INDENTATION).append(INDENTATION).append(RETURN).
                append(escapeUnicode(transformIfNotEmpty(getDefaultValue(method.getReturnType()), s -> SPACE + s))).
                append(SEMICOLON).append(EOLN);
    }

    /**
     * Writes the body of the generated implementation of the provided {@link Executable executable}
     * using the specified {@link Writer writer}.
     *
     * @param executable the executable to generate and write.
     * @param writer     the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code executable} or {@code writer} is {@code null}.
     */
    private static void writeExecutableBody(final Executable executable, final Writer writer) throws IOException {
        if (executable instanceof Constructor) {
            writeConstructorBody((Constructor) executable, writer);
        } else {
            writeMethodBody((Method) executable, writer);
        }
    }

    /**
     * Writes the generated implementation of the provided {@link Executable executable}
     * using the specified {@link Writer writer}.
     *
     * @param executable the executable to generate and write.
     * @param writer     the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code executable} or {@code writer} is {@code null}.
     */
    private static void writeExecutable(final Executable executable, final Writer writer) throws IOException {
        writer.write(EOLN);
        writer.append(INDENTATION).append(Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)).
                append(SPACE).append(escapeUnicode(transformIfNotEmpty(getTypeParametersString(executable), s -> s + SPACE))).
                append(escapeUnicode(getExecutableTypeAndName(executable))).
                append(escapeUnicode(collectParameters(Arrays.stream(executable.getParameters()).map(Parameter::toString)))).
                append(escapeUnicode(collectExceptions(Arrays.stream(executable.getGenericExceptionTypes()).map(Type::getTypeName)))).
                append(SPACE).append(OPENING_BRACE).append(EOLN);
        writeExecutableBody(executable, writer);
        writer.append(INDENTATION).append(CLOSING_BRACE).append(EOLN);
    }

    /**
     * Writes constructors of the generated successor of the specified {@link Class class}
     * using the specified {@link Writer writer}.
     *
     * @param clazz  the class to generate and write successor of.
     * @param writer the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code clazz} or {@code writer} is {@code null}.
     */
    private static void writeConstructors(final Class<?> clazz, final Writer writer) throws IOException {
        final var constructors = Arrays.stream(clazz.getDeclaredConstructors()).
                filter(constructor -> !Modifier.isPrivate(constructor.getModifiers())).toArray(Constructor<?>[]::new);
        for (final var constructor : constructors) {
            writeExecutable(constructor, writer);
        }
    }

    /**
     * Writes methods of the generated successor of the specified {@link Class class}
     * using the specified {@link Writer writer}.
     *
     * @param clazz  the class to generate and write successor of.
     * @param writer the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code clazz} or {@code writer} is {@code null}.
     */
    private static void writeMethods(final Class<?> clazz, final Writer writer) throws IOException {
        final var methods = getAbstractMethods(clazz);
        for (final var method : methods) {
            writeExecutable(method.getMethod(), writer);
        }
    }

    /**
     * Writes the header of the generated successor using the specified {@link Writer writer}.
     *
     * @param writer the writer to use.
     * @throws IOException          if an I/O error occurs.
     * @throws NullPointerException if {@code writer} is {@code null}.
     */
    private static void writeFooter(final Writer writer) throws IOException {
        writer.write(EOLN);
        writer.write(CLOSING_BRACE);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code clazz} or {@code path} is {@code null}.
     */
    @Override
    public void implement(final Class<?> clazz, final Path path) throws ImplerException {
        final var constructors = clazz.getDeclaredConstructors();
        if (clazz.isPrimitive() || clazz.isArray() || Modifier.isFinal(clazz.getModifiers()) || clazz.equals(Enum.class)
                || constructors.length != 0 && Stream.of(constructors).allMatch(c -> Modifier.isPrivate(c.getModifiers()))) {
            throw new ImplerException(clazz.getCanonicalName().concat(" cannot be implemented"));
        }
        Objects.requireNonNull(path);
        try (final var writer = prepareWriter(clazz, path)) {
            writeHeader(clazz, writer);
            writeConstructors(clazz, writer);
            writeMethods(clazz, writer);
            writeFooter(writer);
        } catch (final IOException e) {
            throw new ImplerException("Could not write to the output file", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code clazz} or {@code path} is {@code null}.
     */
    @Override
    public void implementJar(final Class<?> clazz, final Path path) throws ImplerException {
        final Path tempDir;
        try {
            tempDir = Files.createTempDirectory(null);
        } catch (final IOException e) {
            throw new ImplerException("Could not create a temporary directory", e);
        }
        implement(clazz, tempDir);
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String cp;
        try {
            final var source = clazz.getProtectionDomain().getCodeSource();
            cp = source == null ? "." : Path.of(source.getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Could not resolve the class path", e);
        }
        final String[] args = new String[]{
                "-cp", cp,
                getImplFilePath(clazz, tempDir, SOURCE_EXTENSION).toString()
        };
        if (compiler == null || compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Could not compile the generated file");
        }
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Ali Alekperov");
        try (final var writer = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            writer.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/').
                    concat(IMPL_SUFFIX).concat(CLASS_FILE_EXTENSION)));
            Files.copy(getImplFilePath(clazz, tempDir, CLASS_FILE_EXTENSION), writer);
        } catch (final IOException e) {
            throw new ImplerException("Could not write to the output JAR file", e);
        }
    }

    /**
     * Prints a helper message to the standard output.
     */
    private static void showUsage() {
        System.out.printf("Usage:%n " +
                "First way: %1$s [full name of the class to implement]%n" +
                "Second way: %1$s -jar [full name of the class to implement] [path to the root dir]%n", Implementor.class.getName());
    }

    /**
     * Creates an {@link Implementor} and runs it depending on the arguments provided.
     * <p>
     * If there is one argument, runs the {@link #implement(Class, Path)} method, converting the provided argument
     * to a class using the {@link Class#forName(String)} method and resolving the current working directory as the path.
     * <p>
     * If there are three arguments and the first one equals to "-jar", runs the {@link #implementJar(Class, Path)} method,
     * converting the second argument to a class using the {@link Class#forName(String)} method and the third argument
     * to a path using the {@link Path#of(String, String...)} method.
     * <p>
     * If the arguments are incorrect or an error occurs during implementation
     * an error message is printed to the standard output.
     *
     * @param args the provided arguments.
     */
    public static void main(final String[] args) {
        if (args == null || Stream.of(args).anyMatch(Objects::isNull) ||
                (args.length != 1 && !(args.length == 3 && args[0].equals("-jar")))) {
            showUsage();
            return;
        }

        final JarImpler implementor = new Implementor();
        try {
            if (args.length == 1) {
                implementor.implement(Class.forName(args[0]), Path.of("."));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (final InvalidPathException e) {
            System.out.println("The provided target path is malformed:");
            System.out.println(e.getMessage());
        } catch (final ClassNotFoundException e) {
            System.out.println("Could not find the specified class:");
            System.out.println(e.getMessage());
        } catch (final ImplerException e) {
            System.out.println("An error occurred while implementing the specified class:");
            System.out.println(e.getMessage());
        }
    }

}
package ru.ifmo.rain.alekperov;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Implementor implements Impler {

    private static class MethodWrapper {

        private final Method method;

        private final static int BASE = 23;
        private final static int MOD = 1000992299;

        MethodWrapper(Method method) {
            this.method = method;
        }

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

        @Override
        public int hashCode() {
            return ((Arrays.hashCode(method.getParameterTypes())
                    + BASE * method.getReturnType().hashCode()) % MOD
                    + method.getName().hashCode() * BASE * BASE) % MOD;
        }

        Method getMethod() {
            return method;
        }

    }

    private static final String IMPL_SUFFIX = "Impl";
    private static final String SOURCE_EXTENSION = ".java";

    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String SEMICOLON = ";";
    private static final String OPENING_BRACE = "{";
    private static final String CLOSING_BRACE = "}";
    private static final String OPENING_PARENTHESIS = "(";
    private static final String CLOSING_PARENTHESIS = ")";
    private static final String OPENING_ANGLE_BRACKET = "<";
    private static final String CLOSING_ANGLE_BRACKET = ">";
    private static final String INDENTATION = "    ";
    private static final String EOLN = System.lineSeparator();

    private static final String PACKAGE = "package";
    private static final String PUBLIC = "public";
    private static final String CLASS = "class";
    private static final String IMPLEMENTS = "implements";
    private static final String EXTENDS = "extends";
    private static final String SUPER = "super";
    private static final String RETURN = "return";
    private static final String THROWS = "throws";

    private static final String EMPTY = "";
    private static final String FALSE = "false";
    private static final String ZERO = "0";
    private static final String NULL = "null";

    private static String collectCommaList(final Stream<String> stream) {
        return stream.collect(Collectors.joining(COMMA + SPACE));
    }

    private static String collectParameters(final Stream<String> stream) {
        return OPENING_PARENTHESIS + collectCommaList(stream) + CLOSING_PARENTHESIS;
    }

    private static String transformIfNotEmpty(final String string, final Function<String, String> transformer) {
        if (!string.equals(EMPTY)) {
            return transformer.apply(string);
        }
        return EMPTY;
    }

    private static String collectExceptions(final Stream<String> stream) {
        return transformIfNotEmpty(collectCommaList(stream), s -> SPACE + THROWS + SPACE + s);
    }

    private static String collectGenericParameters(final Stream<String> stream) {
        return transformIfNotEmpty(collectCommaList(stream), s -> OPENING_ANGLE_BRACKET + s + CLOSING_ANGLE_BRACKET);
    }

    private static String getImplName(final Class<?> clazz) {
        return clazz.getSimpleName().concat(IMPL_SUFFIX);
    }

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

    private static String getExecutableTypeAndName(final Executable executable) {
        if (executable instanceof Constructor) {
            return getImplName(executable.getDeclaringClass());
        } else {
            return executable.getAnnotatedReturnType().getType().getTypeName().replace('$', '.') + SPACE + executable.getName();
        }
    }

    private static void getAbstractMethodsImpl(final Class<?> clazz, Set<MethodWrapper> storage, Set<MethodWrapper> implemented) {
        final Set<MethodWrapper> implementedHere = new HashSet<>();
        for (final var method : clazz.getDeclaredMethods()) {
            final var wrapper = new MethodWrapper(method);
            if (Modifier.isAbstract(method.getModifiers())) {
                if (!implemented.contains(wrapper)) {
                    storage.add(wrapper);
                }
            } else {
                implementedHere.add(wrapper);
            }
        }
        implemented.addAll(implementedHere);
        final var superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.isInterface()) {
            getAbstractMethodsImpl(clazz.getSuperclass(), storage, implemented);
        }
        for (final var intrf : clazz.getInterfaces()) {
            getAbstractMethodsImpl(intrf, storage, implemented);
        }
        implemented.removeAll(implementedHere);
    }

    private static Set<MethodWrapper> getAbstractMethods(final Class<?> clazz) {
        final Set<MethodWrapper> collection = new HashSet<>();
        getAbstractMethodsImpl(clazz, collection, new HashSet<>());
        return collection;
    }

    private static String getTypeParametersString(final GenericDeclaration decl) {
        return collectGenericParameters(Arrays.stream(decl.getTypeParameters()).map(TypeVariable::getName));
    }

    private static Writer prepareWriter(final Class<?> clazz, final Path path) throws IOException {
        final var sourcePath = path.resolve(clazz.getPackageName().replace('.', File.separatorChar)).
                resolve(getImplName(clazz).concat(SOURCE_EXTENSION));
        Files.createDirectories(sourcePath.getParent());
        return Files.newBufferedWriter(sourcePath, StandardOpenOption.CREATE);
    }

    private static void writeHeader(final Class<?> clazz, final Writer writer) throws IOException {
        writer.append(PACKAGE).append(SPACE).append(clazz.getPackageName()).append(SEMICOLON).append(EOLN);
        writer.write(EOLN);
        writer.append(PUBLIC).append(SPACE).append(CLASS).
                append(SPACE).append(getImplName(clazz)).append(getTypeParametersString(clazz)).
                append(SPACE).append(clazz.isInterface() ? IMPLEMENTS : EXTENDS).
                append(SPACE).append(clazz.getSimpleName()).append(getTypeParametersString(clazz)).
                append(SPACE).append(OPENING_BRACE).append(EOLN);
    }

    private static void writeConstructorBody(final Constructor constructor, final Writer writer) throws IOException {
        writer.append(INDENTATION).append(INDENTATION).append(SUPER).
                append(collectParameters(Arrays.stream(constructor.getParameters()).map(Parameter::getName))).
                append(SEMICOLON).append(EOLN);
    }

    private static void writeMethodBody(final Method method, final Writer writer) throws IOException {
        writer.append(INDENTATION).append(INDENTATION).append(RETURN).
                append(transformIfNotEmpty(getDefaultValue(method.getReturnType()), s -> SPACE + s)).
                append(SEMICOLON).append(EOLN);
    }

    private static void writeExecutableBody(final Executable executable, final Writer writer) throws IOException {
        if (executable instanceof Constructor) {
            writeConstructorBody((Constructor) executable, writer);
        } else {
            writeMethodBody((Method) executable, writer);
        }
    }

    private static void writeExecutable(final Executable executable, final Writer writer) throws IOException {
        writer.write(EOLN);
        writer.append(INDENTATION).append(Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)).
                append(SPACE).append(transformIfNotEmpty(getTypeParametersString(executable), s -> s + SPACE)).
                append(getExecutableTypeAndName(executable)).
                append(collectParameters(Arrays.stream(executable.getParameters()).map(Parameter::toString))).
                append(collectExceptions(Arrays.stream(executable.getGenericExceptionTypes()).map(Type::getTypeName))).
                append(SPACE).append(OPENING_BRACE).append(EOLN);
        writeExecutableBody(executable, writer);
        writer.append(INDENTATION).append(CLOSING_BRACE).append(EOLN);
    }

    private static void writeConstructors(final Class<?> clazz, final Writer writer) throws IOException {
        final var constructors = Arrays.stream(clazz.getDeclaredConstructors()).
                filter(constructor -> !Modifier.isPrivate(constructor.getModifiers())).toArray(Constructor<?>[]::new);
        for (final var constructor : constructors) {
            writeExecutable(constructor, writer);
        }
    }

    private static void writeMethods(final Class<?> clazz, final Writer writer) throws IOException {
        final var methods = getAbstractMethods(clazz);
        for (final var method : methods) {
            writeExecutable(method.getMethod(), writer);
        }
    }

    private static void writeFooter(final Writer writer) throws IOException {
        writer.write(EOLN);
        writer.write(CLOSING_BRACE);
    }

    @Override
    public void implement(final Class<?> clazz, final Path path) throws ImplerException {
        if (clazz.isPrimitive() || clazz.isArray() || Modifier.isFinal(clazz.getModifiers()) || clazz.equals(Enum.class)) {
            throw new ImplerException(String.format("You may not implement %s", clazz.getCanonicalName()));
        }
        Objects.requireNonNull(path);
        try (final var sourceWriter = prepareWriter(clazz, path)) {
            writeHeader(clazz, sourceWriter);
            writeConstructors(clazz, sourceWriter);
            writeMethods(clazz, sourceWriter);
            writeFooter(sourceWriter);
        } catch (final Exception e) {
            throw new ImplerException(e);
        }
    }

    private static void showUsage() {
        System.out.printf("Usage: %s [full name of class to implement]%n", Implementor.class.getName());
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 1 || args[0] == null) {
            showUsage();
            return;
        }

        try {
            final var clazz = Class.forName(args[0]);
            (new Implementor()).implement(clazz, Path.of("."));
        } catch (final ClassNotFoundException e) {
            System.out.println("Could not find the specified class:");
            System.out.println(e.getMessage());
        } catch (final ImplerException e) {
            System.out.println("An error occurred while implementing the specified class:");
            System.out.println(e.getMessage());
        }
    }

}
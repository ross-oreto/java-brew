package io.oreto.brew.constants;

import io.oreto.brew.io;
import io.oreto.brew.str.Noun;
import io.oreto.brew.str.Str;
import io.oreto.brew.str.Word;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class Generator {
    public static final String STRING = "String";
    static final String defaultFileName = "c.txt";
    static final String defaultPackage = Generator.class.getPackage().getName();
    static final String defaultClassName = "C";
    static final String PUBLIC_STATIC_FINAL = "public static final";
    static final String scream = "scream";
    static final String kebab = "kebab";

    enum ConstantType {
        Int, Double, Long, String, comment;

        @Override public String toString() {
            return STRING.equals(name()) ? name() : name().toLowerCase();
        }
    }

    static class Options {
        boolean capitalize = true;
        boolean scream = true;
        boolean cardinality = true;
        boolean words = true;
        boolean kebab = true;
        boolean numbers = true;
        boolean overwrite = false;
        String inputFile = defaultFileName;
        String packageName = defaultPackage;
        String className = defaultClassName;
        String extend = null;
        String module = null;
        String outputFile = outputFileName(null, packageName, className);
    }

    static String outputFileName(String module, String packageName, String className) {
        return (module == null
                ? Str.empty().path("src", "main", "java")
                : Str.empty().path(module, "src", "main", "java") )
                .sep()
                .path(packageName.split("\\."))
                .sep().add(className, ".java").toString();
    }

    public static void main(String[] args) {

        Options cOptions = commandOptions(args);

        Str str = Str.of("package").space().add(cOptions.packageName, ";").br(2)
                .add("@SuppressWarnings(\"unused\")").br()
                .add("public class ", cOptions.className);
        if (Str.isNotEmpty(cOptions.extend)) {
            str.add(Str.SPACE, "extends", Str.SPACE, cOptions.extend);
        }
        str.space().add("{").br();

        File file = loadInputFile(cOptions);
        if (file.exists()) {
            LinkedHashMap<String, ConstantType> constants = new LinkedHashMap<>();
            Map<String, String> values = new HashMap<>();
            Str tmp = Str.empty();

            int i = 0;
            try {
                for (String line : Files.readAllLines(file.toPath())) {
                    i++;
                    if (Str.isBlank(line) || line.trim().startsWith("//")) {
                        String key = String.valueOf(i);
                        constants.put(key, ConstantType.comment);
                        values.put(key, line);
                    } else {
                        defineConstant(line, tmp, constants, values, cOptions);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            defineConstants(constants, values, str);
        }
        str.add('}').br();

        try {
            handleOutput(str, cOptions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Options commandOptions(String[] args) {
        Options cOptions = new Options();
        if (args.length > 0) {
            String flag = Str.DASH + "D";
            Map<String, String> options = Arrays.stream(args)
                    .filter(it -> it.startsWith(flag))
                    .map(it -> it.replaceFirst(flag, Str.EMPTY).split(Str.EQUALS))
                    .filter(it -> it.length > 0)
                    .collect(Collectors.toMap(it -> it[0], it -> it[1]));

            cOptions.cardinality = Str.toBoolean(options.getOrDefault("cardinality", String.valueOf(cOptions.cardinality)))
                    .orElse(cOptions.capitalize);
            cOptions.capitalize = Str.toBoolean(options.getOrDefault("capitals", String.valueOf(cOptions.capitalize)))
                    .orElse(cOptions.capitalize);
            cOptions.scream = Str.toBoolean(options.getOrDefault("scream", String.valueOf(cOptions.scream)))
                    .orElse(cOptions.scream);
            cOptions.numbers = Str.toBoolean(options.getOrDefault("numbers", String.valueOf(cOptions.numbers)))
                    .orElse(cOptions.numbers);
            cOptions.words = Str.toBoolean(options.getOrDefault("words", String.valueOf(cOptions.words)))
                    .orElse(cOptions.words);
            cOptions.kebab = Str.toBoolean(options.getOrDefault("kebab", String.valueOf(cOptions.kebab)))
                    .orElse(cOptions.kebab);

            cOptions.overwrite = Str.toBoolean(options.getOrDefault("overwrite", String.valueOf(cOptions.overwrite)))
                    .orElse(cOptions.overwrite);
            cOptions.inputFile = options.getOrDefault("input", cOptions.inputFile);
            cOptions.packageName = options.getOrDefault("package", cOptions.packageName);
            cOptions.className = options.getOrDefault("class", cOptions.className);
            cOptions.extend = options.getOrDefault("extend", cOptions.extend);
            cOptions.module = options.getOrDefault("module", cOptions.module);
            cOptions.outputFile = options.getOrDefault("output"
                    , outputFileName(cOptions.module, cOptions.packageName, cOptions.className));

            System.out.println(options);
        }
        return cOptions;
    }

    static File loadInputFile(Options options) {
        return io.loadResourceFile(options.inputFile, "web","src", "main", "resources");
    }

    static Optional<ConstantType> typeFromNumberString(String number) {
        if (number.contains("\\."))
            return Optional.of(ConstantType.Double);
        Str str = Str.of(number);
        if (str.toInteger().isPresent()) return Optional.of(ConstantType.Int);
        else if (str.toLong().isPresent()) return Optional.of(ConstantType.Long);
        return Optional.empty();
    }

    static void defineConstant(String line
            , Str tmp
            , Map<String, ConstantType> constants
            , Map<String, String> values
            , Options cOptions) {
        tmp.setTo(line);
        boolean implicit;
        String value, var;
        if (tmp.contains(Str.EQUALS)) {
            implicit = false;
            int i = line.indexOf(Str.Chars.EQUALS);
            tmp.slice(0, i - 1);
            var = tmp.toVariableName().toSnake().toUpper().toString();
            value = line.substring(i + 1);
        } else {
            implicit = true;
            value = tmp.toString();
            var = tmp.toVariableName().toString();
        }

        tmp.setTo(value);
        if (implicit) {
            if (cOptions.numbers && tmp.isNum()) {
                Optional<String> word = Word.fromNumber(value);
                Optional<ConstantType> type = typeFromNumberString(value);
                if (word.isPresent() && type.isPresent()) {
                    putConstant(
                            Str.of(word.get()).toSnake().toUpper().toString()
                            , value, type.get(), constants, values);
                    putConstant(var, value, ConstantType.String, constants, values);
                }
            } else {
                // implicit variables are always equal to their value, i.e. test = "test", Test = "Test"
                // the only exception is when the variable is a reserved language word such as final.
                // reserved words are prefaced with '_'. _final = "final"
                putConstant(var, value, ConstantType.String, constants, values);
                String cardinality = changeCardinality(value);
                if (cOptions.cardinality) {
                    putConstant(tmp.setTo(cardinality).toVariableName().toString()
                            , cardinality, ConstantType.String, constants, values);
                }
                // create capitalized constant if specified
                if (cOptions.capitalize) {
                    String capitalize = Str.capitalize(value);
                    putConstant(tmp.setTo(capitalize).toVariableName().toString()
                            , capitalize, ConstantType.String, constants, values);
                    if (cOptions.cardinality) {
                        putConstant(tmp.setTo(cardinality).capitalize().toVariableName().toString()
                                , Str.capitalize(cardinality), ConstantType.String, constants, values);
                    }
                }
                // create all upper case screaming caps if specified. TEST_SCREAM = "TEST"
                if (cOptions.scream) {
                    String screaming = tmp.setTo(String.format("%s%s%s", value, Str.UNDER_SCORE, scream))
                            .toVariableName().toUpper().toString();
                    putConstant(screaming, value.toUpperCase(), ConstantType.String, constants, values);
                    if (cOptions.cardinality) {
                        screaming = tmp.setTo(String.format("%s%s%s", cardinality, Str.UNDER_SCORE, scream))
                                .toVariableName().toUpper().toString();
                        putConstant(screaming, cardinality.toUpperCase(), ConstantType.String, constants, values);
                    }
                }
                // create kebab if specified. SOME_TEST_KEBAB = "some-test"
                if (cOptions.kebab && value.contains(Str.SPACE)) {
                    String kebabed = tmp.setTo(String.format("%s%s%s", value, Str.UNDER_SCORE, kebab))
                            .toVariableName().toUpper().toString();
                    putConstant(kebabed, Str.of(value).toKebab().toString(), ConstantType.String, constants, values);
                    if (cOptions.cardinality) {
                        kebabed = tmp.setTo(String.format("%s%s%s", cardinality, Str.UNDER_SCORE, kebab))
                                .toVariableName().toUpper().toString();
                        putConstant(kebabed
                                , Str.of(cardinality).toKebab().toString(), ConstantType.String, constants, values);
                    }
                }
                // try to split up the the variable into strings if specified. i.e. camelCase => camel Case
                if (cOptions.words) {
                    String words = Str.toWords(value);
                    if (!value.contains(Str.SPACE) && words.contains(Str.SPACE))
                        defineConstant(words, tmp, constants, values, cOptions);
                }
            }
        } else {
            String val = tmp.toString();
            if (cOptions.numbers && tmp.isNum()) {
                Optional<ConstantType> type = typeFromNumberString(val);
                type.ifPresent(s -> putConstant(var, val, s, constants, values));
                putConstant(Str.UNDER_SCORE + var, val, ConstantType.String, constants, values);
            } else {
                putConstant(var, val, ConstantType.String, constants, values);
            }
        }
    }

    static void putConstant(String name
            , String value
            , ConstantType type
            , Map<String, ConstantType> constants
            , Map<String, String> values) {
        constants.put(name, type);
        values.put(name, value);
    }

    static String changeCardinality(String s) {
        return Noun.isSingular(s) ? Noun.plural(s) : Noun.singular(s);
    }

    static void defineConstants(LinkedHashMap<String, ConstantType> constants, Map<String, String> values, Str str) {
        constants.forEach((name, type) -> {
            if (type == ConstantType.comment) {
                str.tab().add(values.get(name)).br();
            } else {
                String value = type == ConstantType.String ? Str.quote(values.get(name)) : values.get(name);
                String L = type == ConstantType.Long ? "L" : "";
                str.tab().add(String.format("%s %s %s = %s%s;", PUBLIC_STATIC_FINAL, type, name, value, L)).br();
            }
        });
    }

    static void handleOutput(Str str, Options options) throws IOException {
        File file = new File(options.outputFile);
        if (options.overwrite)
            Files.write(file.toPath(), str.getBytes());
        else if (file.exists()) {
            Scanner scanner = new Scanner(System.in);
            System.out.printf("Overwrite existing file (%s)?  ", file.getPath());
            String command = scanner.nextLine();
            if (command.trim().toLowerCase().startsWith("y")) {
                Files.write(file.toPath(), str.getBytes());
                System.out.println("file written to " + file.getPath());
            }
        } else {
            Files.write(file.toPath(), str.getBytes());
            System.out.println("file created at " + file.getPath());
        }
    }
}

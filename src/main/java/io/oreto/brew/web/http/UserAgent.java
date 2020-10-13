package io.oreto.brew.web.http;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.str.Str;

import java.util.*;

//https://developer.mozilla.org/en-US/docs/Web/HTTP/Browser_detection_using_the_user_agent
public class UserAgent {
    public static UserAgent of(String userAgent) {
        return new UserAgent(userAgent);
    }

    private final String value;

    private Browser browser;
    private RenderEngine engine;
    private final List<Query> query;

    protected UserAgent(String value) {
        this.value = value;
        for(Name name : Name.values()) {
            Optional<String> version = name.version(value);
            if (version.isPresent()) {
                this.browser = new Browser(name, version.get());
                this.engine = RenderEngine.of(value, version.get()).orElse(null);
                break;
            }
        }
        this.query = new ArrayList<>();
    }

    public boolean satisfied() {
        return query.stream().allMatch(Query::match);
    }

    public UserAgent require(Name name, String version) {
        query.add(new Query(this, name, version, false));
        return this;
    }

    public UserAgent requireFor(Name name, String version) {
        query.add(new Query(this, name, version, true));
        return this;
    }

    public UserAgent require(Name name) {
        return require(name, null);
    }

    public UserAgent require(Engine engine, String version) {
        query.add(new Query(this, engine, version, false));
        return this;
    }

    public UserAgent requireFor(Engine engine, String version) {
        query.add(new Query(this, engine, version, true));
        return this;
    }

    public UserAgent require(Engine engine) {
        return require(engine, null);
    }

    public String getValue() {
        return value;
    }

    public Browser getBrowser() {
        return browser;
    }

    public RenderEngine getEngine() {
        return engine;
    }

    static final String RV = "rv:";
    static final String MSIE = "MSIE ";

    public static Integer[] parseVersion(String version) {
        return Arrays.stream(version.split("\\."))
                .filter(Str::isNumber)
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
    }

    public static int versionCompare(String v1, String v2) {
        if (Str.isEmpty(v1) && Str.isEmpty(v2)) return 0;
        if (Str.isEmpty(v2)) return -1;
        if (Str.isEmpty(v1)) return 1;
        if (v1.equals(v2)) return 0;

        Integer[] ver1 = parseVersion(v1);
        Integer[] ver2 = parseVersion(v2);

        for(int i = 0; i < ver1.length; i++) {
            int ver2Num = i >= ver2.length ? 0 : ver2[i];
            if (ver1[i] > ver2Num) return 1;
            else if (ver1[i] < ver2Num) return -1;
        }
        return 0;
    }

    private static String pickVersion(String userAgent, int i) {
        String version = userAgent.substring(i);
        i = 0;
        for (; i < version.length(); i++) {
            if (version.charAt(i) != '.' && !Character.isDigit(version.charAt(i))) break;
        }
        return version.substring(0, i).trim();
    }

    public static class Browser {
        private final Name name;
        private final String version;

        public Browser(Name name, String version) {
            this.name = name;
            this.version = version;
        }

        public Name getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

    public enum Name {
        explorer("Internet Explorer", Lists.array( MSIE, "Trident/"))
        , edge("Edge", Lists.array("Edge/", "Edg/"))
        , chromium("Chromium", Lists.array("Chromium/"))
        , safari("Safari", Lists.array("Safari/"), Lists.array("Chrome/", "Chromium/"))
        , vivaldi("Vivaldi", Lists.array("Vivaldi/"))
        , yandex("Yandex",Lists.array("YaBrowser/"))
        , brave("Brave", Lists.array("Brave/"))
        , seamonkey("Seamonkey", Lists.array("Seamonkey/"))
        , chrome("Chrome", Lists.array("Chrome/"), Lists.array("Chromium/"))
        , firefox("Firefox", Lists.array("Firefox/"), Lists.array("Seamonkey/"))
        , opera("Opera", Lists.array("Opera/","OPR/"));

        private final String name;
        private final String[] includes;
        private final String[] excludes;

        Name(String name, String[] includes, String[] excludes) {
            this.name = name;
            this.includes = includes;
            this.excludes = excludes;
        }

        Name(String name, String[] includes) {
            this.name = name;
            this.includes = includes;
            this.excludes = Lists.array();
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static boolean isValid(String s) {
            return Arrays.stream(values()).anyMatch(it -> it.name.equals(s));
        }

        public int match(String userAgent) {
            for(String s : excludes){
                if (userAgent.contains(s)) return -1;
            }
            for (String s : includes) {
                int i = userAgent.indexOf(s);
                if (i > 0)
                    return i + s.length();
            }
            return -1;
        }

        private Optional<String> version(String userAgent) {
            int i = match(userAgent);
            if (i > 0) {
                i = this == Name.explorer
                        ? (userAgent.contains(MSIE) ?
                        userAgent.indexOf(MSIE) + MSIE.length() : userAgent.indexOf(RV) + RV.length())
                        : i;
            }
            if (i > 0) {
                String version = pickVersion(userAgent, i);
                return version.isEmpty() ? Optional.empty() : Optional.of(version);
            } else {
                return Optional.empty();
            }
        }
    }

    public static class RenderEngine {
        public static Optional<RenderEngine> of(String userAgent, String version) {
            for (Engine engine : Engine.values()) {
                int i = engine.match(userAgent);
                if (i > 0 && versionCompare(version, engine.maxVersion) < 0) {
                    return Optional.of(new RenderEngine(engine, pickVersion(userAgent, i)));
                }
            }
            return Optional.empty();
        }

        private final Engine engine;
        private final String version;

        public RenderEngine(Engine engine, String version) {
            this.engine = engine;
            this.version = version;
        }

        public Engine getType() {
            return engine;
        }

        public String getVersion() {
            return version;
        }
    }

    public enum Engine {
        Webkit("AppleWebKit/")
        , Gecko("Gecko/")
        , Presto("Opera/", "15")
        , Trident("Trident/")
        , EdgeHTML("Edge/", "79")
        , Blink("Chrome/");

        private final String include;
        private final String maxVersion;

        Engine(String include) {
            this.include = include;
            this.maxVersion = null;
        }

        Engine(String include, String maxVersion) {
            this.include = include;
            this.maxVersion = maxVersion;
        }

        public int match(String userAgent) {
            int i = userAgent.indexOf(include);
            return i > 0 ? i + include.length() : -1;
        }
    }

    public static class Query {
        private final UserAgent userAgent;
        private Name name;
        private Engine engine;

        private String op;
        private String version;

        boolean specific;

        static String[] parseOp(String s) {
            String[] opVersion = Arrays.stream(s.trim()
                    .replaceAll("  +", " ")
                    .split(" ", 2))
                    .map(String::trim).toArray(String[]::new);
            return opVersion.length == 1 ? new String[] { "=", opVersion[0] } : opVersion;
        }

        private Query(UserAgent userAgent, String version) {
            this.userAgent = userAgent;
            if (Objects.nonNull(version) && !version.isEmpty()) {
                String[] opVersion = parseOp(version);
                this.op = opVersion[0];
                this.version = opVersion[1];
            }
        }

        Query(UserAgent userAgent, Name name, String version, boolean specific) {
            this(userAgent, version);
            this.name = name;
            this.specific = specific;
        }

        Query(UserAgent userAgent, Engine engine, String version, boolean specific) {
            this(userAgent, version);
            this.engine = engine;
            this.specific = specific;
        }

        boolean match() {
            boolean pass;
            int comp;
            if (Objects.nonNull(name)) {
                pass = userAgent.getBrowser().getName() == name;
                comp = versionCompare(userAgent.getBrowser().getVersion(), version);
            } else {
                pass = userAgent.getEngine().getType() == engine;
                comp = versionCompare(userAgent.getEngine().getVersion(), version);
            }
            // only apply this query to specific software
            if(specific && !pass) return true;

            if (Objects.nonNull(op))
                switch (op) {
                    case ">=":
                        pass = pass && comp >= 0;
                        break;
                    case "<=":
                        pass = pass && comp <= 0;
                        break;
                    case "!=":
                        pass = pass && comp != 0;
                        break;
                    case ">":
                        pass = pass && comp > 0;
                        break;
                    case "<":
                        pass = pass && comp < 0;
                        break;
                    default:
                        pass = pass && comp == 0;
                        break;
                }
            return pass;
        }
    }
}

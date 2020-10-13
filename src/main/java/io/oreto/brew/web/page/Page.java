package io.oreto.brew.web.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.oreto.brew.web.page.constants.C;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Page implements Notifiable {

    public static final String LANG_ACCEPT_HEADER = "Accept-Language";

    public static Locale toLocale(String s) {
        String[] codes = s.split("-");
        return codes.length > 1 ? new Locale(codes[0], codes[1].toUpperCase()) : new Locale(codes[0]);
    }

    public static Locale parseLocale(Map<String, String> params, Map<String, String> headers) {
        if (Objects.nonNull(params) && params.containsKey(C.lang))
            return toLocale(params.get(C.lang));
        else if (Objects.nonNull(headers) && headers.containsKey(LANG_ACCEPT_HEADER))
            return toLocale(headers.get(LANG_ACCEPT_HEADER));
        return toLocale("en-US");
    }

    private static Optional<String> international(ResourceBundle resourceBundle, String key, boolean fuzzy, Object... args) {
        Optional<String> property = Optional.empty();
        if (resourceBundle == null || key == null) return property;

        // look for exact match first
        if (resourceBundle.containsKey(key)) {
            property = Optional.of(resourceBundle.getString(key));
        } else if (fuzzy) {
            List<String> path = Arrays.asList(key.split("\\."));

            // check regex keys
            for(String regex : resourceBundle.keySet().stream()
                    .filter(it -> it.contains("*"))
                    .collect(Collectors.toList())) {
                if(path.size() == key.split("\\.").length &&
                        key.matches(regex.replaceAll("\\.", "\\.")
                        .replaceAll("\\*", ".*"))) {
                    property = Optional.of(resourceBundle.getString(regex));
                    break;
                }
            }

            // next look for the closest fuzzy match
            // break apart key to find sub matches
            if (!property.isPresent()) {
                int pathLength = path.size();
                if (pathLength > 1) {
                    for (int i = 1; i < pathLength; i++) {
                        String subKey = String.join(".", path.subList(i, pathLength));
                        if (resourceBundle.containsKey(subKey)) {
                            property = Optional.of(resourceBundle.getString(subKey));
                            break;
                        }
                    }
                    if (!property.isPresent() && resourceBundle.containsKey(path.get(0)))
                        property = Optional.of(resourceBundle.getString(path.get(0)));
                }
            }
            // finally check for multiple key matches regardless of order
            if (!property.isPresent()) {
                long count = 1;
                Set<String> props = resourceBundle.keySet();
                int currentDepth = 0;
                for (String prop : props) {
                    List<String> currentPath = Arrays.asList(prop.split("\\."));
                    long matches = path.stream().filter(currentPath::contains).count();
                    if (matches > count || (matches == count && currentPath.size() < currentDepth)) {
                        count = matches;
                        currentDepth = currentPath.size();
                        property = Optional.of(resourceBundle.getString(prop));
                    }
                }
            }
        }

        return args == null
                ? property
                : property.map(s -> MessageFormat.format(s, Arrays.stream(args)
                    .map(it -> it instanceof String && resourceBundle.containsKey((String) it)
                            ? international(resourceBundle, (String)it, false) : it)
                    .map(it -> it instanceof Optional<?> && ((Optional<?>)it).isPresent() ? ((Optional<?>) it).get() : it)
                    .toArray()));
    }

    public static Optional<String> I18n(ResourceBundle resourceBundle, String key, Object... args) {
        return international(resourceBundle, key, false, args);
    }

    public static Optional<String> I19n(ResourceBundle resourceBundle, String key, Object... args) {
        return international(resourceBundle, key, true, args);
    }

    private Map<String, Object> data = new HashMap<>();

    private List<Form<?>> forms = new ArrayList<>();

    protected @JsonIgnore ResourceBundle resourceBundle;
    protected List<Notification> notifications = new ArrayList<>();

    public Map<String, Object> getData() { return data; }

    public List<Form<?>> getForms() { return forms; }

    protected Field Field() { return new Field(); }
    protected <T> Form<T> Form(String name) { return new Form<T>(name); }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    @Override
    public List<Notification> getNotifications() {
        return notifications;
    }

    public Page withData(Map<String, ?> data) {
        this.data.putAll(data);
        return this;
    }

    public Page withForms(List<Form<?>> forms) {
        this.forms = forms;
        return this;
    }

    public Page withResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        return this;
    }

    public Page withNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        return this;
    }
}

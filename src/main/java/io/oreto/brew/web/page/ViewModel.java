package io.oreto.brew.web.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.security.UserDetails;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.page.constants.C;
import io.oreto.brew.web.route.Routing;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ViewModel extends Page {

    public static ViewModel of() {
        return new ViewModel();
    }

    public static ViewModel of(Locale locale) {
        return new ViewModel()
                .withLocale(locale)
                .withResourceBundle(ResourceBundle.getBundle(C.messages, locale));
    }

    public static ViewModel of(ResourceBundle resourceBundle) {
        return new ViewModel(resourceBundle);
    }

    public static ViewModel of(Map<String, String> params, Map<String, String> headers) {
        return new ViewModel()
                .withResourceBundle(ResourceBundle.getBundle(C.messages, parseLocale(params, headers)))
                .withData(params);
    }

    public static ViewModel of(Map<String, String> params) {
        return of().withData(params);
    }

    @JsonIgnore private String view;

    private String assetPath;
    private String distPath;
    private Routing routing;
    private UserDetails user;

    protected ViewModel() {}
    protected ViewModel(ResourceBundle resourceBundle) { this.resourceBundle = resourceBundle; }

    public ViewModel withView(String... view) {
        this.view = Str.of().path(view).toString();
        return this;
    }

    public String getView() {
        return view;
    }
    public String getAssetPath() { return assetPath; }
    public String getDistPath() {
        return distPath;
    }
    public Routing getRouting() {
        return routing;
    }
    public UserDetails getUser() {
        return user;
    }

    public ViewModel withUser(UserDetails user) {
        this.user = user;
        return this;
    }

    public ViewModel withFlash(List<Notification> flashNotifications) {
        this.notifications.addAll(flashNotifications);
        return this;
    }

    public ViewModel withRouting(Routing routing) {
        this.routing = routing;
        return this;
    }

    public ViewModel withData(String name, Object value) {
        getData().put(name, value);
        return this;
    }

    public ViewModel withData(Class<?> cls) {
        return withData(Reflect.getAllFields(cls).stream()
                .filter(field -> !field.getName().startsWith("_"))
                .map(java.lang.reflect.Field::getName).collect(Collectors.toList()));
    }

    public ViewModel withData(Object o) {
        for (java.lang.reflect.Field field : Reflect.getAllFields(o.getClass())) {
            if (!field.getName().startsWith("_")) {
                try {
                    withData(field.getName(), Reflect.getFieldValue(o, field));
                } catch (Exception e) {
                    withData(field.getName(), e);
                }
            }
        }
        return this;
    }

    public ViewModel withTitle(String title) {
        withData(C.title, I18n(resourceBundle, String.format("%s.%s", title, C.title)).orElse(title));
        return this;
    }

    public <T> ViewModel withForm(String name, Class<T> cls) {
        Form<T> form = Form(name);
        this.getForms().add(form.withField(cls));
        return this;
    }

    public <T> ViewModel withForm(String name) {
        Form<T> form = Form(name);
        this.getForms().add(form);
        return this;
    }

    public <T> ViewModel withForm(String name, Class<T> cls, Consumer<Form<T>> formConsumer) {
        Form<T> form = Form(name);
        this.getForms().add(form.withField(cls));
        formConsumer.accept(form);
        return this;
    }

    public <T> ViewModel withForm(Form<T> form, Consumer<Form<T>> formConsumer) {
        this.getForms().add(form);
        formConsumer.accept(form);
        return this;
    }

    public <T> ViewModel withForm(Form<T> form) {
        this.getForms().add(form);
        return this;
    }

    public <T> ViewModel withForm(String name, Consumer<Form<T>> formConsumer) {
        formConsumer.accept(Form(name));
        return this;
    }

    public <T> Form<T> FormWithFields(String name, Class<T> tClass) {
        Form<T> newForm = Form(name);
        newForm.withField(tClass);
        getForms().add(newForm);
        return newForm;
    }

    @SuppressWarnings("unchecked")
    public <T> Form<T> form() {
        return this.getForms().size() > 0 ? (Form<T>)this.getForms().get(this.getForms().size() - 1) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> Form<T> form(String name) {
        String needle = Str.toCamel(name);
        return (Form<T>)this.getForms().stream()
                .filter(it -> it.getName().equals(needle))
                .findFirst().orElse(null);
    }

    public Object at(String name) {
        return getData().get(name);
    }

    public ViewModel localize() {
        notifications.forEach(it -> it.localize(locale));
        getForms().forEach(it -> it.localize(locale));
        return this;
    }

    public String i18n(String key) {
        return I18n(resourceBundle, key).orElse(key) ;
    }

    public String i18n(String key, String args) {
        return args == null ? i18n(key) :  I18n(resourceBundle, key, (Object[]) args.split(",")).orElse(key);
    }

    public String i18n(String key, List<String> args) {
        return args == null ? i18n(key) : I18n(resourceBundle, key, args.toArray()).orElse(key);
    }

    public String i19n(String key) {
        return I19n(resourceBundle, key).orElse(key) ;
    }

    public String i19n(String key, String args) {
        return args == null ? i19n(key) :  I19n(resourceBundle, key, (Object[]) args.split(",")).orElse(key);
    }

    public String i19n(String key, List<String> args) {
        return args == null ? i19n(key) : I19n(resourceBundle, key, args.toArray()).orElse(key);
    }

    public String query(String s) {
        return routing == null || routing.getInfo() == null
                ? null
                : routing.getInfo().getQuery().get(s);
    }
    public String path(String s) {
        return routing == null || routing.getInfo() == null
                ? null
                : routing.getInfo().getPathParams().get(s);
    }

    @Override
    public ViewModel withData(Map<String, ?> data) {
        super.withData(data);
        return this;
    }

    @Override
    public ViewModel withForms(List<Form<?>> forms) {
        super.withForms(forms);
        return this;
    }

    @Override
    public ViewModel withLocale(Locale locale) {
        super.withLocale(locale);
        return this;
    }

    @Override
    public ViewModel withResourceBundle(ResourceBundle resourceBundle) {
        super.withResourceBundle(resourceBundle);
        return this;
    }

    @Override
    public ViewModel withNotifications(List<Notification> notifications) {
        super.withNotifications(notifications);
        return this;
    }
}

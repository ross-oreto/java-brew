package io.oreto.brew.web.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.security.UserDetails;
import io.oreto.brew.serialize.json.JSON;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.page.constants.C;
import io.oreto.brew.web.route.Router;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
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
                .model(params);
    }

    public static ViewModel of(Map<String, String> params) {
        return of().model(params);
    }

    @JsonIgnore private String view;

    private Router router;
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
    public Router getRouter() {
        return router;
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

    public ViewModel withRouter(Router router) {
        this.router = router;
        return this;
    }

    public ViewModel model(String name, Object value) {
        data.put(name, value);
        return this;
    }

    public ViewModel model(Class<?> cls) {
        return model(Reflect.getAllFields(cls).stream()
                .filter(field -> Reflect.getGetter(field, cls).isPresent())
                .map(java.lang.reflect.Field::getName).collect(Collectors.toList()));
    }

    public ViewModel model(Object o) {
        for (java.lang.reflect.Field field : Reflect.getAllFields(o)) {
            if (Reflect.getGetter(field, o).isPresent()) {
                try {
                    model(field.getName(), Reflect.getFieldValue(o, field));
                } catch (Exception e) {
                    model(field.getName(), e);
                }
            }
        }
        return this;
    }

    public ViewModel withTitle(String title) {
        model(C.title, I18n(resourceBundle, String.format("%s.%s", title, C.title)).orElse(title));
        return this;
    }

    public <T> ViewModel withForm(String name, Class<T> cls) {
        Form<T> form = Form(name);
        this.getForms().add(form.withFields(cls));
        return this;
    }

    public <T> ViewModel withForm(String name) {
        Form<T> form = Form(name);
        this.getForms().add(form);
        return this;
    }

    public <T> ViewModel withForm(String name, Class<T> cls, Consumer<Form<T>> formConsumer) {
        Form<T> form = Form(name);
        this.getForms().add(form.withFields(cls));
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

    public boolean validate() {
        return getForms().stream().allMatch(Form::validate);
    }

    public ViewModel validate(BiConsumer<Boolean, ViewModel> consumer) {
        consumer.accept(validate(), this);
        return this;
    }

    public <T> Form<T> FormWithFields(String name, Class<T> tClass) {
        Form<T> newForm = Form(name);
        newForm.withFields(tClass);
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

    @Override @JsonIgnore
    public List<Form<?>> getForms() {
        return super.getForms();
    }

    @Override
    public ViewModel notify(String message, Notification.Type type, String group, String... args) {
        super.notify(message, type, group, args);
        return this;
    }

    @Override
    public ViewModel notify(String message, Notification.Type type) {
        super.notify(message, type);
        return this;
    }

    @Override
    public ViewModel notify(String message, String... args) {
        super.notify(message, args);
        return this;
    }

    @Override
    public ViewModel notify(Notification notification) {
        super.notify(notification);
        return this;
    }

    @Override
    public ViewModel notify(List<Notification> notifications) {
        super.notify(notifications);
        return this;
    }

    public ViewModel localize() {
        Locale locale = getLocale();
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
        return router == null || router.getActive() == null
                ? null
                : router.getActive().getQuery().get(s);
    }
    public String path(String s) {
        return router == null || router.getActive() == null
                ? null
                : router.getActive().getPathParams().get(s);
    }

    @Override
    public ViewModel model(Map<String, ?> data) {
        super.model(data);
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

    public Map<String, Object> model() {
        Map<String, Object> asMap = JSON.asMap(this);
        asMap.putAll(getForms().stream().collect(Collectors.toMap(Form::getName, JSON::asMap)));
        asMap.putAll(data);
        return asMap;
    }
}

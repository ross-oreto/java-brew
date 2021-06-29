package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Sortable;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.str.Str;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class QStr {
    public static QStr of() {
        return new QStr();
    }

    public static class Func {
        public static String of(Function function, String name) {
            return String.format("%s{%s}", function.name(), name);
        }
    }

    private final Str str = Str.of();

    private boolean or;
    Integer page;
    Integer max;
    String[] order;
    private boolean started;

    protected QStr() {
        this.order = new String[] {};
    }

    @Override
    public String toString() {
        return str.toString();
    }

    public String queryString() {
        Str q = Str.of("q=").add(str);
        if (Objects.nonNull(page))
            q.add("&page=").add(page);

        if (Objects.nonNull(max))
            q.add("&size=").add(max);

        if (order.length > 0)
            q.add("&sort=").add(String.join(",", order));
        return q.toString();
    }

    private Str logic() {
        if (started) {
            str.space()
                    .add(or ? QueryReader.Logical.Operator.or.name() : QueryReader.Logical.Operator.and.name())
                    .space();
        } else
            started = true;
        return str;
    }

    private QStr op(String name, QueryReader.Expression.Operator op, Object value, Opt... opts) {
        String val = Objects.nonNull(value) ? value.toString() : null;
        val = Objects.nonNull(val) && val.contains(" ") && !val.trim().startsWith("\"")
                ? String.format("\"%s\"", val)
                : val;
        logic()
                .add(name)
                .add("::")
                .add(Arrays.stream(opts).anyMatch(it -> it == Opt.not) ? QueryReader.Expression.NOT : Str.EMPTY)
                .add(op.name())
                .add(Arrays.stream(opts).anyMatch(it -> it == Opt.prop) ? QueryReader.Expression.PROP : Str.EMPTY)
                .add(':')
                .add(val);
        return this;
    }

    public QStr page(int page) {
        this.page = page;
        return this;
    }

    public QStr order(String... order) {
        this.order = order;
        return this;
    }

    public QStr order(List<Sortable> order) {
        this.order = order.stream().map(Sortable::toString).toArray(String[]::new);
        return this;
    }

    public QStr limit(int max) {
        this.max = max;
        return this;
    }

    public QStr eq(String name, Object value, Opt... opts) {
        boolean negate = Arrays.stream(opts).anyMatch(it -> it == Opt.not);
        if (value == null)
            return negate ? isNotNull(name) : isNull(name);

        return op(name, QueryReader.Expression.Operator.eq, Safe.of(value).q(Object::toString).val(), opts);
    }

    public QStr gt(String name, Object value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.gt, value, opts);
    }

    public QStr gte(String name, Object value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.gte, value, opts);
    }

    public QStr lt(String name, Object value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.lt, value, opts);
    }

    public QStr lte(String name, Object value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.lte, value, opts);
    }

    public QStr isNull(String name) {
        logic().add(name).add("::").add(QueryReader.Expression.Operator.isnull.name());
        return this;
    }

    public QStr isNotNull(String name) {
        logic().add(name).add("::").add(QueryReader.Expression.NOT).add(QueryReader.Expression.Operator.isnull.name());
        return this;
    }

    public QStr contains(String name, String value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.contains, value, opts);
    }

    public QStr iContains(String name, String value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.icontains, value, opts);
    }

    public QStr startsWith(String name, String value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.startswith, value, opts);
    }

    public QStr iStartsWith(String name, String value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.istartswith, value, opts);
    }

    public QStr endsWith(String name, String value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.endswith, value, opts);
    }

    public QStr iEndsWith(String name, String value, Opt... opts) {
        return op(name, QueryReader.Expression.Operator.iendswith, value, opts);
    }

    public QStr or() {
        or = true;
        return this;
    }

    public QStr endOr() {
        or = false;
        return this;
    }

    public QStr group() {
        logic().add('(');
        started = false;
        return this;
    }

    public QStr endGroup() {
        str.add(')');
        return this;
    }
}

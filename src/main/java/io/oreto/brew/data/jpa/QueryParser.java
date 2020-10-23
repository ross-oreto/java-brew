package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Paged;
import io.oreto.brew.str.Str;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryParser {

    static final char QUOTE = '"';

    public static class QueryState<T> {

        int i = 0;
        int depth = 0;
        int length;
        String q;
        boolean quoted = false;
        boolean escaped = false;
        Str str;
        Map<String, From> joins = new HashMap<>();
        CriteriaBuilder cb;
        Root<T> root;

        QueryState(String q, CriteriaBuilder cb, Root<T> root) {
            this.q = q;
            this.cb = cb;
            this.root = root;
            length = q == null ? 0 : q.length();
            str = Str.empty();
        }

        Stack<String> logical = new Stack<>();
        Stack<String> groups = new Stack<>();

        String getString() {
            String s = str.trim().toString();
            str.delete();
            return s;
        }

        QueryState<T> increment() {
            i++;
            depth++;
            return this;
        }

        QueryState<T> toggleQuote() {
            quoted = !quoted;
            return this;
        }

        void escape() { escaped = true; }
        void unescape() { escaped = false; }
        void open() { groups.push("("); }
        void close() {
            if (groups.isEmpty())
                throw new BadQueryException("no matching opening paren '('", i);
            groups.pop();
        }
        boolean isOpen() {
            return groups.size() > 0;
        }

        boolean isLast() { return i == length - 1; }
    }

    public static class Predicates {
        public Predicate where;
        public boolean having;
        public List<javax.persistence.criteria.Expression<?>> grouping = new ArrayList<>();

        public javax.persistence.criteria.Expression<?>[] groupBy() {
            return grouping.stream().toArray(javax.persistence.criteria.Expression[]::new);
        }
    }

    public static <T> Predicates parse(QueryState<T> state) {
        Predicates predicates = new Predicates();
        if (state.q == null || state.q.trim().equals("")) {
            predicates.where = state.cb.conjunction();
            return predicates;
        }

        for (; state.i < state.length; state.i++) {
            char c = state.q.charAt(state.i);

            if (c == QUOTE) {
                if (state.escaped) {
                    state.str.add(c);
                    state.unescape();
                } else {
                    state.toggleQuote();
                }
                checkFinal(state, predicates);
                continue;
            } else if (state.quoted) {
                if (c == '\\' && !state.escaped) {
                    state.escape();
                } else {
                    state.str.add(c);
                }
                continue;
            } else if (c == '\\') {
                state.escape();
                continue;
            }

            switch (c) {
                case '(':
                    state.open();
                    if (state.logical.size() > 0 && Logical.isValid(state.logical.peek())) {
                        Predicates newPredicates = parse(state.increment());

                        if (predicates.where == null)
                            predicates.where = newPredicates.where;
                        else
                            predicates.where = Logical.apply(state.logical.pop()
                                    , state.cb
                                    , predicates.where
                                    , newPredicates.where);
                        predicates.grouping.addAll(newPredicates.grouping);
                        if (newPredicates.having)
                            predicates.having = true;
                    } else {
                        predicates = parse(state.increment());
                    }
                    break;
                case ')':
                    state.close();
                    if (state.depth == 0) {
                        checkFinal(state, predicates);
                        break;
                    } else {
                        state.depth--;
                        return state.str.isEmpty()
                                ? predicates
                                : addExpression(state.getString(), state, predicates);
                    }
                case ' ':
                    String s = state.getString();
                    if (!s.contains(":") && Logical.isValid(s)) {
                        state.logical.push(s);
                    } else if (s.length() > 0) {
                        addExpression(s, state, predicates);
                    }
                    break;
                default:
                    state.str.add(c);
                    checkFinal(state, predicates);
            }
        }

        if (state.isOpen())
            throw new BadQueryException("no matching closing paren ')'", state.i);
        return predicates;
    }

    protected static <T> Predicates addExpression(String s
            , QueryState<T> state
            , Predicates predicates) {
        Expression<T> expression = new Expression<>(s);
        Predicate p = expression.apply(state);

        if (expression.isAggregate()) {
            predicates.having = true;
        } else {
            if (Objects.nonNull(expression.p1))
                predicates.grouping.add(expression.p1);
            if (Objects.nonNull(expression.p2))
                predicates.grouping.add(expression.p2);
        }
        predicates.where = state.logical.isEmpty() || predicates.where == null
                ? p
                : Logical.apply(state.logical.pop(), state.cb, predicates.where, p);
        return predicates;
    }

    private static <T> void checkFinal(QueryState<T> state
            , Predicates predicates) {
        if (state.isLast() && state.str.isNotEmpty()) {
            addExpression(state.getString(), state, predicates);
        }
    }

    public static <T> Long count(String q, EntityManager em, Class<T> entityClass) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<T> root = countQuery.from(entityClass);
        countQuery.select(builder.count(root));

        Predicates predicates = parse(new QueryState<T>(q, builder, root));

        if (predicates.having)
            countQuery.having(predicates.where).distinct(true);
        else
            countQuery.where(predicates.where);
        return em.createQuery(countQuery).getSingleResult();
    }

    public static <T> Paged<T> query(String q
            , Paged.Page page
            , EntityManager em
            , Class<T> entityClass, String... fetch) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery(entityClass);
        Root<T> root = criteriaQuery.from(entityClass);
        for(String name : fetch) {
            root.fetch(name);
        }

        Predicates predicates = parse(new QueryState<T>(q, builder, root));

        if (predicates.having) {
            predicates.grouping.add(root);
            criteriaQuery.groupBy(predicates.groupBy()).having(predicates.where).distinct(true);
        } else
            criteriaQuery.where(predicates.where);

        List<Order> orders = page.getSorting().stream()
                .map(it -> it.isAscending()
                        ? builder.asc(root.get(it.getName()))
                        : builder.desc(root.get(it.getName())))
                .collect(Collectors.toList());
        criteriaQuery.orderBy(orders);

        List<T> results = em.createQuery(criteriaQuery)
                .setFirstResult(page.getOffset())
                .setMaxResults(page.getSize())
                .getResultList();

        return Paged.of(results, page.withCount(count(q, em, entityClass)));
    }

    public static <T> Paged<T> query(String q
            , EntityManager em
            , Class<T> entityClass, String... fetch) {
        return query(q, Paged.Page.of(), em, entityClass, fetch);
    }

    static class Expression<T> {
        static final String NOT = "not_";
        static final String PROP = "_prop";

        String key;
        String s;
        Object value;
        Operator operator;
        boolean negate;
        boolean prop;
        Function f1;
        Function f2;
        Path p1;
        Path p2;

        public enum Operator {
            eq, isnull, gt, lt, gte, lte
            , contains, icontains, startswith, istartswith, endswith, iendswith;

            public static boolean isValid(String s) {
                return Arrays.stream(values()).map(Enum::name).collect(Collectors.toList()).contains(s);
            }
        }

        public boolean isAggregate() {
            return (Objects.nonNull(f1) && f1.isAggregate())
                    || (Objects.nonNull(f2) && f2.isAggregate());
        }

        Expression(String expr) {
            String[] parts = expr.split(":", 2);
            key = parts[0].trim();

            String accessor = key + "::";
            if (expr.startsWith(accessor)) {
                String op = expr.substring(expr.indexOf(accessor) + accessor.length())
                        .split(":", 2)[0].trim();
                String rawOp = op;

                if (op.startsWith(NOT)) {
                    negate = true;
                    op = op.substring(op.indexOf(NOT) + NOT.length());
                }
                if (op.endsWith(PROP)) {
                    prop = true;
                    op = op.substring(op.indexOf(PROP) + PROP.length());
                }
                if (!Operator.isValid(op)) {
                    throw new BadQueryException("Unexpected operator: " + op);
                }

                operator = Operator.valueOf(op);
                String val = expr.substring(expr.indexOf(accessor + rawOp) + (accessor + rawOp).length()).trim();
                value = val.startsWith(":") ? val.substring(1) : "";
            } else {
                operator = Operator.eq;
                value = parts.length > 1 ? parts[1] : true;
            }
            s = value.toString();

            String funcRegex = "^[a-zA-Z_0-9]+\\{.*}$";
            if (key.matches(funcRegex)) {
                String func = key.substring(0, key.indexOf('{'));
                f1 = toFunction(func);
                key = key.substring(key.indexOf('{') + 1, key.indexOf('}'));
            }
            if (s.matches(funcRegex)) {
                String func = s.substring(0, s.indexOf('{'));
                f2 = toFunction(func);
                s = s.substring(s.indexOf('{') + 1, s.indexOf('}'));
            }
            if (s.matches("^'.*'$")) {
                value = s.substring(1, s.length() - 1);
                s = value.toString();
            }
        }

        protected Function toFunction(String func) {
             if (Function.isValid(func))
                 return Function.valueOf(func);
             else
                 throw new BadQueryException(String.format("%s is not a valid function", func));
        }

        protected void setValue(Path<T> path) {
            if (Str.EMPTY.equals(s))
                value = null;
            else if (path.getJavaType() == LocalDate.class) {
                value = LocalDate.parse(s);
            } else if (path.getJavaType() == LocalDateTime.class) {
                value = LocalDateTime.parse(s);
            } else if (path.getJavaType() == Date.class || path.getJavaType() == Timestamp.class) {
                try {
                    value = SimpleDateFormat.getTimeInstance().parse(s);
                } catch (ParseException e) {
                    throw new BadQueryException(String.format("Bad date format %s: %s", value, e.getMessage()));
                }
            } else if (path.getJavaType() == Long.class) {
                value = Str.toLong(s)
                        .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
            } else if (path.getJavaType() == Integer.class) {
                value = Str.toInteger(s)
                        .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
            } else if (path.getJavaType() == Short.class) {
                value = Str.toShort(s)
                        .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
            } else if (path.getJavaType() == Float.class) {
                value = Str.toFloat(s)
                        .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
            } else if (path.getJavaType() == Byte.class) {
                value = Str.toByte(s)
                        .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
            } else if (path.getJavaType() == Double.class) {
                value = Str.toDouble(s)
                        .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));
            }
        }

        public javax.persistence.criteria.Expression applyFunction(Function function, Path path, CriteriaBuilder cb) {
            if (function == Function.count) {
                return cb.count(path);
            } else if (function == Function.avg) {
                return cb.avg(path);
            } else if (function == Function.sum) {
                return cb.sum(path);
            } else if (function == Function.max) {
                return cb.max(path);
            } else if (function == Function.min) {
                return cb.min(path);
            } else if (function == Function.greatest) {
                return cb.greatest(path);
            } else if (function == Function.least) {
                return cb.least(path);
            } else if (function == Function.count_distinct) {
                return cb.countDistinct(path);
            }
            return cb.count(path);
        }

        protected From findJoin(String property, From from, QueryState<T> state, boolean l) {
            Optional<String> attribute = getEntityIdName(property, state.root, state.cb);
            if (attribute.isPresent()) {
                property = String.format("%s.%s", property, attribute.get());
            }

            if (property.contains(".")) {
                String path = property.substring(0, property.lastIndexOf('.'));
                String[] fields = property.split("\\.");

                if (state.joins.containsKey(path)) {
                    from = state.joins.get(path);
                } else {
                    for (int i = 0; i < fields.length - 1; i++) {
                        from = from.join(fields[i], JoinType.LEFT);
                    }
                    state.joins.put(path, from);
                }

                if (l)
                    key = fields[fields.length - 1];
                else
                    s = fields[fields.length - 1];
            }

            return from;
        }

        protected Optional<String> getEntityIdName(String path, Root<?> root, CriteriaBuilder cb) {
            if (Str.isBlank(path)) return Optional.empty();
            String[] names = path.split("\\.");
            Optional<Attribute<?, ?>> attribute = Optional.empty();

            for(String name : names) {
                attribute = (Optional<Attribute<?, ?>>)
                        root.getModel().getAttributes().stream()
                                .filter(Attribute::isAssociation)
                                .filter(it -> it.getName().equals(name))
                                .findFirst();
                if (attribute.isPresent()) {
                    Class entityType = attribute.get().isCollection()
                            ? ((PluralAttribute) attribute.get()).getBindableJavaType()
                            : attribute.get().getJavaType();
                    root = cb.createQuery(entityType).from(entityType);
                }
            }
            return attribute.isPresent()
                    ? Optional.of(root.getModel().getId(root.getModel().getIdType().getJavaType()).getName())
                    : Optional.empty();
        }

        public Predicate apply(QueryState state) {
            Root root = state.root;
            CriteriaBuilder cb = state.cb;
            Predicate predicate;
            try {
                From l = findJoin(key, root, state, true);
                From r = prop ? findJoin(s, root, state, false) : root;

                if (!prop)
                    setValue(l.get(key));

                p1 = l.get(key);
                p2 = prop ? r.get(s) : null;
                javax.persistence.criteria.Expression exp1 = f1 == null ? p1 : applyFunction(f1, p1, cb);
                javax.persistence.criteria.Expression exp2 = f2 == null ? p2 : applyFunction(f2, p2, cb);

                switch (operator) {
                    case eq:
                        predicate = prop
                                ? cb.equal(exp1, cb.nullif(exp2, ""))
                                : cb.equal(exp1, value);
                        break;
                    case lt:
                        if (prop)
                            predicate = cb.lt(exp1, exp2);
                        else
                            predicate = value instanceof Number
                                    ? cb.lt(exp1, (Number) value)
                                    : cb.lessThan(exp1, s);
                        break;
                    case lte:
                        if (prop)
                            predicate = cb.lessThanOrEqualTo(exp1, exp2);
                        else
                            predicate = value instanceof Number
                                    ? cb.lessThanOrEqualTo(exp1, (Comparable) value)
                                    : cb.lessThanOrEqualTo(exp1, s);
                        break;
                    case gt:
                        if (prop)
                            predicate = cb.gt(exp1, exp2);
                        else
                            predicate = value instanceof Number
                                ? cb.gt(exp1, (Number) value)
                                : cb.greaterThan(exp1, s);
                        break;
                    case gte:
                        if (prop)
                            predicate = cb.greaterThanOrEqualTo(exp1, exp2);
                        else
                            predicate = value instanceof Number
                                    ? cb.greaterThanOrEqualTo(exp1, (Comparable) value)
                                    : cb.greaterThanOrEqualTo(exp1, s);
                        break;
                    case isnull:
                        predicate = cb.isNull(exp1);
                        break;
                    case contains:
                        predicate = prop
                                ? cb.like(exp1, cb.concat("%", cb.concat(exp2, "%")))
                                : cb.like(exp1, String.format("%%%s%%", s));
                        break;
                    case icontains:
                        predicate = prop
                                ? cb.like(cb.upper(exp1), cb.concat("%", cb.concat(cb.upper(exp2), "%")))
                                : cb.like(cb.upper(exp1), String.format("%%%s%%", s.toUpperCase()));
                        break;
                    case startswith:
                        predicate = prop
                                ? cb.like(exp1, cb.concat(exp2, "%"))
                                : cb.like(exp1, String.format("%s%%", s));
                        break;
                    case istartswith:
                        predicate = prop
                                ? cb.like(cb.upper(exp1), cb.concat(cb.upper(exp2), "%"))
                                : cb.like(cb.upper(exp1), String.format("%s%%", s.toUpperCase()));
                        break;
                    case endswith:
                        predicate = prop
                                ? cb.like(exp1, cb.concat("%", exp2))
                                : cb.like(exp1, String.format("%%%s", s));
                        break;
                    case iendswith:
                        predicate = prop
                                ? cb.like(cb.upper(exp1), cb.concat("%", cb.upper(exp2)))
                                : cb.like(cb.upper(exp1), String.format("%%%s", s.toUpperCase()));
                        break;
                    default:
                        throw new BadQueryException("Unexpected operator: " + operator.name());
                }
            } catch (IllegalArgumentException e) {
                throw new BadQueryException("Invalid attribute: " + e.getMessage());
            } catch (IllegalStateException e) {
                throw new BadQueryException("Invalid path: " + e.getMessage());
            }
            return negate ? cb.not(predicate) : predicate;
        }
    }

    static class Logical {
        public enum Operator {
            and, or, not
        }

        public static boolean isValid(String op) {
            for (Operator operator : Operator.values()) {
                if (operator.name().equals(op)) {
                    return true;
                }
            }
            return false;
        }

        public static Predicate apply(Operator operator, CriteriaBuilder builder, Predicate... predicates) {
            Predicate predicate = null;
            switch (operator) {
                case and:
                    predicate = builder.and(predicates);
                    break;
                case or:
                    predicate = builder.or(predicates);
                    break;
                case not:
                    if (predicates.length > 0)
                        predicate = builder.not(predicates[0]);
                    break;
                default:
                    throw new BadQueryException("Unexpected logical operator: " + operator.name());
            }
            return predicate;
        }

        public static Predicate apply(String operator, CriteriaBuilder builder, Predicate... predicates) {
            return apply(Operator.valueOf(operator), builder, predicates);
        }
    }

    public static class BadQueryException extends RuntimeException {
        private int i = -1;

        public BadQueryException(String message) {
            super(message);
        }

        public BadQueryException(String message, int i) {
            super(message);
            this.i = i;
        }

        public int at() { return i; }
    }
}
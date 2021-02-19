package io.oreto.brew.data.jpa;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Pager;
import io.oreto.brew.data.Paginate;
import io.oreto.brew.data.Sortable;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.page.constants.C;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryReader {
    public static final int MAX_SIZE = 999;
    static final char QUOTE = '"';

    public static String[] idNames(Root root) {
        EntityType entityType = root.getModel();
        if (entityType.getIdType() == null) {
            Set<SingularAttribute> attributes = root.getModel().getIdClassAttributes();
            if (Objects.nonNull(attributes) && attributes.size() > 0) {
                return attributes.stream().map(Attribute::getName).toArray(String[]::new);
            }
        }
        return new String[]{ root.getModel().getId(root.getModel().getIdType().getJavaType()).getName() };
    }

    public static Selection[] idSelections(Root root) {
        return Arrays.stream(idNames(root)).map((java.util.function.Function<String, Path>) root::get)
                .toArray(Selection[]::new);
    }

    public static class QueryState<T> {
        int i = 0;
        int depth = 0;
        int length;
        String q;
        boolean quoted = false;
        boolean escaped = false;
        boolean collecting = false;
        List<Object> list;
        Str str;
        String tmp;
        Map<String, From> joins = new HashMap<>();
        CriteriaBuilder cb;
        Root<T> root;

        public static <T> void buildFetchJoins(String fetch, Root<T> root, Map<String, From> joins) {
            if (Objects.nonNull(fetch) && !fetch.isEmpty()) {
                String path = fetch;
                From from = root;
                path = path.trim();
                if (!path.isEmpty()) {
                    if (joins.containsKey(path)) {
                        joins.get(path);
                    } else {
                        String[] fields = path.split("\\.");
                        for (String field : fields) {
                            from = (From) from.fetch(field, JoinType.LEFT);
                        }
                        joins.put(path, from);
                    }
                }
            }
        }

        QueryState(String q, CriteriaBuilder cb, Root<T> root, String fetch) {
            this.q = q;
            this.cb = cb;
            this.root = root;
            length = q == null ? 0 : q.length();
            str = Str.empty();

            buildFetchJoins(fetch, root, joins);
        }

        QueryState(String q, CriteriaBuilder cb, Root<T> root) {
            this(q, cb, root, null);
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

        void toggleQuote() {
            quoted = !quoted;
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
        void openCollection() {
            this.collecting = true;
            this.list = new ArrayList<>();
            this.tmp = getString();
        }
        void closeCollection() {
            this.collect();
            this.collecting = false;
        }
        void collect() {
            if (str.trim().isInt()) {
                list.add(str.toInteger().orElse(0));
            } else if (str.isNum()) {
                list.add(str.toDouble().orElse(0.0));
            } else {
                if (str.startsWith("'") && str.endsWith("'"))
                    str.trim("'");
                list.add(str.toString());
            }
            str.delete();
        }

        boolean isLast() { return i == length - 1; }
    }

    public static class Predicates {
        public Predicate where;
        public boolean having;
        public List<javax.persistence.criteria.Expression<?>> grouping = new ArrayList<>();

        public javax.persistence.criteria.Expression<?>[] groupBy() {
            return grouping.toArray(new javax.persistence.criteria.Expression[0]);
        }
    }

    public static <T> Predicates parse(QueryState<T> state) {
        Predicates predicates = new Predicates();
        if (state.q == null || state.q.trim().equals("")) {
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
            } else if (state.collecting) {
                if (c == ']') {
                    state.closeCollection();
                    checkFinal(state, predicates);
                } else {
                    if (c == ',')
                        state.collect();
                    else
                        state.str.add(c);
                }
                continue;
            } else if (c == '[') {
               state.openCollection();
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
                    } else if (state.list.size() > 0) {
                        addExpression(s, state, predicates);
                        state.list = null;
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
        Expression<T> expression = state.list == null ? new Expression<>(s) : new Expression<>(state.tmp, state.list);
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
        if (state.isLast() && (state.str.isNotEmpty() || Str.isNotEmpty(state.tmp))) {
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
        else if(Objects.nonNull(predicates.where))
            countQuery.where(predicates.where);
        return em.createQuery(countQuery).getSingleResult();
    }

    private static <T> List<Order> sortToOrder(Collection<Sortable> sorting
            , CriteriaBuilder builder
            , Root<T> root) {
        return sorting.stream()
                .map(it -> it.isAscending()
                        ? builder.asc(root.get(it.getName()))
                        : builder.desc(root.get(it.getName())))
                .collect(Collectors.toList());
    }

    private static <T> void parsePredicates(String q
            , CriteriaBuilder builder
            , Root<T> root
            , CriteriaQuery<?> criteriaQuery) {
        Predicates predicates = parse(new QueryState<T>(q, builder, root, null));
        if (predicates.having) {
            predicates.grouping.add(root);
            criteriaQuery.groupBy(predicates.groupBy()).having(predicates.where).distinct(true);
        } else if(Objects.nonNull(predicates.where))
            criteriaQuery.where(predicates.where).distinct(true);
    }

    public static boolean isLoaded(PersistenceUnitUtil util, Object o, String field) {
        return Reflect.getField(o, field).isPresent() && util.isLoaded(o, field);
    }

    public static <T> List<T> queryList(String q, Paginate pager, EntityManager em, Class<T> entityClass) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery(entityClass);
        Root<T> root = criteriaQuery.from(entityClass);
        parsePredicates(q, builder, root, criteriaQuery);

        criteriaQuery.orderBy(sortToOrder(pager.getSorting(), builder, root));
        TypedQuery<T> query = em.createQuery(criteriaQuery);

        if (pager.getOffset() > 0)
            query.setFirstResult((int) pager.getOffset());
        if (pager.getSize() > 0)
            query.setMaxResults(pager.getSize());
        return query.getResultList();
    }

    public static <T> Paged<T> query(String q
            , Paginate pager
            , EntityManager em
            , Class<T> entityClass
            , Fetch.Plan fetchPlan) {
        List<T> results = null;
        if (fetchPlan.isEmpty()) {
            results = queryList(q, pager, em, entityClass);
        } else {
            if (fetchPlan.hasJoins() && Safe.of(fetchPlan.joins("")).q(List::size).orElse(0) > 0) {
                CriteriaBuilder builder = em.getCriteriaBuilder();
                CriteriaQuery<Tuple> idQuery = builder.createTupleQuery();
                Root<T> root = idQuery.from(entityClass);

                idQuery.multiselect(idSelections(root));
                parsePredicates(q, builder, root, idQuery);
                idQuery.orderBy(sortToOrder(pager.getSorting(), builder, root));
                TypedQuery<Tuple> query = em.createQuery(idQuery);

                if (pager.getOffset() > 0)
                    query.setFirstResult((int) pager.getOffset());
                if (pager.getSize() > 0)
                    query.setMaxResults(pager.getSize());

                // first just get the ids with pagination
                List<Tuple> ids = query.getResultList();

                String[] joinPaths = fetchPlan.getJoinPaths();
                results = joinFetchQuery(ids, entityClass, em, builder, fetchPlan.joins(joinPaths[0]));
                joinFetch(results, Arrays.copyOfRange(joinPaths, 1, joinPaths.length), em, builder, fetchPlan);
            }
            if (results == null)
                results = queryList(q, pager, em, entityClass);

            if (fetchPlan.hasQueries()) {
                PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
                CriteriaBuilder builder = em.getCriteriaBuilder();
                for (String path: fetchPlan.getQueryPaths()) {
                    for (Fetch fetch : fetchPlan.queries(path)) {
                        List<?> gathered = path.isEmpty() ? results : traversePath(results, path);
                        Root root = null;
                        if (gathered.size() > 0) {
                            root = builder.createQuery().from(gathered.get(0).getClass());
                        }
                        for (Object result : gathered) {
                            try {
                                if (!isLoaded(util, result, fetch.getName())) {
                                    Reflect.setFieldValue(result
                                            , fetch.getName()
                                            , lazyFetchQuery(result, root, em, builder, fetch));
                                }
                            } catch (ReflectiveOperationException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (fetchPlan.hasJoins()) {
                joinFetch(results, fetchPlan.getJoinPaths(), em, em.getCriteriaBuilder(), fetchPlan);
            }
        }
        return Paged.of(results, pager.isCountEnabled() ? pager.withCount(count(q, em, entityClass)) : pager);
    }

    private static <T> void joinFetch(List<T> results
            , String[] joinPaths
            , EntityManager em
            , CriteriaBuilder builder
            , Fetch.Plan fetchPlan) {
        if (results.size() > 0) {
            for (String path : joinPaths) {
                List<Object> gathered = traversePath(results, path);
                if (gathered.size() > 0)
                    joinFetchRemaining(gathered
                            , gathered.get(0).getClass()
                            , em
                            , builder
                            , null
                            , fetchPlan.joins(path));
            }
            fetchPlan.clearJoins();
        }
    }

    private static void _traversePath(Collection<?> results
            , String path
            , List<String> fields
            , List<Object> collected) {
        if (Objects.nonNull(fields) && fields.isEmpty())
            return;
        for (Object result : results) {
            fields = new ArrayList<>(Arrays.asList(path.split("\\.")));
            String field = fields.get(0);
            try {
                Object o = Reflect.getFieldValue(result, field);
                if (fields.size() < 2) {
                    if (Objects.nonNull(o)) {
                        if (o instanceof Collection)
                            collected.addAll((Collection<?>) o);
                        else
                            collected.add(o);
                    }
                } else {
                    fields.remove(0);
                    _traversePath(o instanceof Collection ? (Collection) o : Lists.of(o)
                            , path.replaceFirst(field + "\\.", "")
                            , fields
                            , collected);
                }
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<Object> traversePath(List<?> results, String path) {
        List<Object> list = new ArrayList<>();
        _traversePath(results, path, null, list);
        return list;
    }

    private static Class getEntityClass(Attribute attribute, EntityType entityType) {
       if (attribute.isCollection()) {
           Class cls = attribute.getJavaType();
           if (cls == List.class)
               return entityType.getList(attribute.getName()).getElementType().getJavaType();
           else if (cls == Set.class)
               return entityType.getSet(attribute.getName()).getElementType().getJavaType();
           else if (cls == Map.class)
               return entityType.getMap(attribute.getName()).getElementType().getJavaType();
       }
       return attribute.getJavaType();
    }

    private static <T> String findMappedBy(Object parent
            , Root<T> r
            , Attribute attribute
            , Attribute.PersistentAttributeType annotationType
            , Class type) {
        String mappedBy = Reflect.getField(parent, attribute.getName())
                .map(it -> {
                    if (annotationType == Attribute.PersistentAttributeType.ONE_TO_MANY)
                        return it.getAnnotation(OneToMany.class).mappedBy();
                    else if (annotationType == Attribute.PersistentAttributeType.MANY_TO_MANY)
                        return it.getAnnotation(ManyToMany.class).mappedBy();
                    else if (annotationType == Attribute.PersistentAttributeType.ONE_TO_ONE)
                        return it.getAnnotation(OneToOne.class).mappedBy();
                    return null;
                }).orElse(null);

        Attribute.PersistentAttributeType targetAnnotation = (annotationType == Attribute.PersistentAttributeType.ONE_TO_MANY
                ? Attribute.PersistentAttributeType.MANY_TO_ONE
                : annotationType);
        EntityType<T> entityType = r.getModel();
        if (Str.isEmpty(mappedBy)) {
            // no mapped by so we have to try to infer the attribute
            for (Attribute attr : entityType.getAttributes()) {
                if (attr.isAssociation()
                        && getEntityClass(attr, entityType) == parent.getClass()
                        && attr.getPersistentAttributeType() == targetAnnotation) {
                    if (Str.isNotEmpty(mappedBy))
                        throw new BiDirectionException(parent.getClass(), attribute.getName(), type
                                , targetAnnotation.name());
                    mappedBy = attr.getName();
                }
            }
        }
        if (Str.isEmpty(mappedBy))
            throw new BiDirectionException(parent.getClass(), attribute.getName(), type, targetAnnotation.name());
        return mappedBy;
    }

    public static String determineDbIdName(Object o, String field) {
        return Reflect.getField(o, field).map(f -> f.isAnnotationPresent(Column.class)
                ? f.getAnnotation(Column.class).name()
                : Str.of(o.getClass().getSimpleName()).add('_').add(field).toSnake().toString()).orElse(null);
    }

    public static String determineDbColName(Object o, String field) {
        return Reflect.getField(o, field).map(f -> f.isAnnotationPresent(Column.class)
                ? f.getAnnotation(Column.class).name()
                : Str.toSnake(field)).orElse(null);
    }

    public static String determineDbTableName(Object o) {
        return o.getClass().isAnnotationPresent(Table.class)
                ? o.getClass().getAnnotation(Table.class).name()
                : Str.toSnake(o.getClass().getSimpleName());
    }

    private static <T> List<T> joinFetchQuery(List<Tuple> ids
            , Class<T> entityClass
            , EntityManager em
            , CriteriaBuilder builder
            , List<Fetch> fetch) {
        // The second query will use the previously extracted identifiers to fetch the join associations.
        CriteriaQuery<T> criteriaQuery = builder.createQuery(entityClass);
        Root<T> root = criteriaQuery.from(entityClass);
        Map<String, From> joins = new HashMap<>();
        QueryState.buildFetchJoins(fetch.get(0).getName(), root, joins);

        String[] idNames = idNames(root);
        Map<String, Collection<Object>> idMap = new HashMap<>();
        for(int i = 0; i < idNames.length; i++) {
            int finalI = i;
            idMap.put(idNames[i], ids.stream().map(it-> it.get(finalI)).collect(Collectors.toList()));
        }
        explicitIn(criteriaQuery, idMap, builder, root);

        List<T> results = em.createQuery(criteriaQuery).getResultList();
        // fetch remaining associations one by one
        joinFetchRemaining(results, entityClass, em, builder, idMap, fetch);

        return results;
    }

    private static void joinFetchRemaining(List<?> results
            , Class<?> entityClass
            , EntityManager em
            , CriteriaBuilder builder
            , Map<String, Collection<Object>> idMap
            , List<Fetch> fetch) {
        if (results.size() > 0 && fetch.size() > 0) {
            PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
            Map<String, From> joins = new HashMap<>();
            for (Fetch f : fetch) {
                String name = f.getName().split("\\.")[0];
                if (isLoaded(util, results.get(0), name))
                    continue;

                CriteriaQuery criteriaQuery = builder.createQuery(entityClass);
                Root root = criteriaQuery.from(entityClass);
                joins.clear();
                QueryState.buildFetchJoins(f.getName(), root, joins);
                if (idMap == null) {
                    String[] idNames = idNames(root);
                    idMap = new HashMap<>();
                    for(String id : idNames) {
                        idMap.put(id, results.stream().map(it-> {
                            try {
                                return Reflect.getFieldValue(it, id);
                            } catch (ReflectiveOperationException e) {
                                System.out.println(e.getMessage());
                                return null;
                            }
                        }).collect(Collectors.toList()));
                    }
                }

                explicitIn(criteriaQuery, idMap, builder, root);
                List<Object> fetchResults = em.createQuery(criteriaQuery.distinct(true)).getResultList();

                for(int i = 0; i < results.size(); i++) {
                    try {
                        if (!isLoaded(util, results.get(i), name))
                            Reflect.setFieldValue(results.get(i)
                                , name
                                , Reflect.getFieldValue(fetchResults.get(i), name));
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void explicitIn(CriteriaQuery criteriaQuery
            , Map<String, Collection<Object>> idMap
            , CriteriaBuilder builder
            , Root root) {
        for(String idName : idMap.keySet()) {
            List<Predicate> idPredicates = new ArrayList<>();
            for (Object id : idMap.get(idName)) {
                idPredicates.add(builder.equal(root.get(idName), id));
            }
            criteriaQuery.where(builder.or(idPredicates.toArray(new Predicate[0]))).distinct(true);
        }
    }

    private static <T> Object lazyFetchQuery(Object parent
            , Root root
            , EntityManager em
            , CriteriaBuilder builder
            , Fetch fetch) throws BiDirectionException {
        Attribute attribute = root.getModel().getAttribute(fetch.getName());
        if (Objects.nonNull(attribute)) {
            PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
            // is this a basic @ElementCollection with something like @CollectionTable(name = "entity1_strings")
            // private List<String> strings;
            if (attribute.isCollection() && !attribute.isAssociation()) {
                Optional<Field> collectionField = Reflect.getField(parent, attribute.getName());
                if (collectionField.isPresent()
                        && collectionField.get().isAnnotationPresent(ElementCollection.class)
                        && collectionField.get().isAnnotationPresent(CollectionTable.class)) {
                    CollectionTable collectionTable = collectionField.get().getAnnotation(CollectionTable.class);
                    String table = collectionTable.name();
                    String[] joinColumns = Arrays.stream(collectionTable.joinColumns())
                            .map(JoinColumn::name).toArray(String[]::new);
                    Map<String, String> idNames = Arrays.stream(idNames(root))
                            .collect(Collectors.toMap(it -> determineDbIdName(parent, it), it -> it));
                    for(String joinColumn : joinColumns) idNames.remove(joinColumn);

                    boolean isMap = attribute.getJavaType().isAssignableFrom(Map.class);
                    Set<String> ids = idNames.keySet();
                    String where = ids.stream().map(it -> String.format("%s=:%s", it, it))
                            .collect(Collectors.joining(" AND "));
                    // Yes this is raw sql however it is so basic that the statement will be universal.
                    String col = determineDbColName(parent, attribute.getName());
                    String select = isMap ? String.format("%s_key, %s", col, col) : col;
                    String order = fetch.getSorting().isEmpty()
                            ? ""
                            : String.format(" order by %s", fetch.getSorting().stream()
                                .map(it -> String.format("%s %s", it.getName(), it.getDirection())).collect(Collectors.joining(", ")));
                    String sql = String.format("SELECT %s FROM %s WHERE %s%s", select, table, where, order);

                    Query query = em.createNativeQuery(sql);
                    for (String id : ids) {
                        try {
                            query.setParameter(id, Reflect.getFieldValue(parent, idNames.get(id)));
                        } catch (ReflectiveOperationException e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    if (fetch.getOffset() > 0)
                        query.setFirstResult((int) fetch.getOffset());
                    if (fetch.getLimit() > 0)
                        query.setMaxResults(fetch.getLimit());
                    List<Object[]> resultList = query.getResultList();
                    if (isMap) {
                        return resultList.stream().collect(Collectors.toMap(it -> it[0], it -> it[1]));
                    } else {
//                        Object test = resultList.stream().map(it -> it[0]).collect(Collectors.toList());
                        return resultList;
                    }
                }
            }

            // the type of the object being fetched.
            Class<T> type = getEntityClass(attribute, root.getModel());
            // identify the association type (OneToMany, ManyToMany, OneToOne)
            Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
            if (attributeType == Attribute.PersistentAttributeType.ONE_TO_ONE) {
                try {
                    // first if using @MapsId you can find by parent id
                    Object oneToOne = em.find(type, Reflect.getFieldValue(parent, C.id));
                    // if not using @MapsId we will have to search by child attribute id
                    return oneToOne == null ?
                            em.find(type, Reflect.getFieldValue(Reflect.getFieldValue(parent, attribute.getName()), C.id))
                            : oneToOne;
                } catch (ReflectiveOperationException e) {
                    System.out.println(e.getMessage());
                    return null;
                }
            }

            // query for the fetch type
            CriteriaQuery<T> lazyQuery = builder.createQuery(type);
            Root<T> r = lazyQuery.from(type);

            // now we have to find the bi directional association
            String mappedBy = findMappedBy(parent, r, attribute, attributeType, type);

            String[] idName = idNames(root);
            Object identifier = util.getIdentifier(parent);
            if (attributeType == Attribute.PersistentAttributeType.ONE_TO_MANY) {
                if (idName.length > 1) {
                    Predicate[] predicates = new Predicate[idName.length];
                    for (int i = 0; i < idName.length; i++) {
                        try {
                            predicates[i] = builder.equal(r.get(mappedBy).get(idName[i])
                                    , Reflect.getFieldValue(identifier, idName[i]));
                        } catch (ReflectiveOperationException e) {
                           System.out.println(e.getMessage());
                        }
                    }
                    lazyQuery.where(predicates);
                } else
                    lazyQuery.where(builder.equal(r.get(mappedBy).get(idName[0]), identifier));
            } else if (attributeType == Attribute.PersistentAttributeType.MANY_TO_MANY) {
                From from = r.join(mappedBy, JoinType.LEFT);
                if (idName.length > 1) {
                    Predicate[] predicates = new Predicate[idName.length];
                    for (int i = 0; i < idName.length; i++) {
                        try {
                            predicates[i] = builder.equal(from.get(idName[i])
                                    , Reflect.getFieldValue(identifier, idName[i]));
                        } catch (ReflectiveOperationException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    lazyQuery.where(predicates);
                } else {
                    lazyQuery.where(builder.equal(from.get(idName[0]), identifier));
                }
            }

            lazyQuery.orderBy(sortToOrder(fetch.getSorting(), builder, r));
            TypedQuery<T> query = em.createQuery(lazyQuery);
            if (fetch.getOffset() > 0)
                query.setFirstResult((int) fetch.getOffset());
            if (fetch.getLimit() > 0)
                query.setMaxResults(fetch.getLimit());

            return query.getResultList();
        }
        return null;
    }

    public static <T> Paged<T> query(String q
            , EntityManager em
            , Class<T> entityClass
            , Fetch.Plan fetchPlan) {
        return query(q, Pager.of(), em, entityClass, fetchPlan);
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
            eq, isnull, gt, lt, gte, lte, in
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
            this(expr, null);
        }

        Expression(String expr, List<Object> values) {
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
                    op = op.substring(0, op.lastIndexOf(PROP));
                }
                if (!Operator.isValid(op)) {
                    throw new BadQueryException("Unexpected operator: " + op);
                }

                operator = Operator.valueOf(op);
                if (Objects.nonNull(values)) {
                    value = values;
                } else {
                    String val = expr.substring(expr.indexOf(accessor + rawOp) + (accessor + rawOp).length()).trim();
                    value = val.startsWith(":") ? val.substring(1) : "";
                }
            } else {
                operator = Operator.eq;
                value = values == null ? parts.length > 1 ? parts[1] : true : values;
            }
            s = value.toString();

            String funcRegex = "^[a-zA-Z_0-9]+\\{.*}$";
            // check for a left function
            if (key.matches(funcRegex)) {
                String func = key.substring(0, key.indexOf('{'));
                f1 = toFunction(func);
                key = key.substring(key.indexOf('{') + 1, key.indexOf('}'));
            }
            // check for a quote string literal
            if (s.matches("^'.*'$")) {
                value = s.substring(1, s.length() - 1);
                s = value.toString();
            }
            // check for a function
            else if (s.matches(funcRegex)) {
                String func = s.substring(0, s.indexOf('{'));
                f2 = toFunction(func);
                s = s.substring(s.indexOf('{') + 1, s.indexOf('}'));
            }
        }

        protected Function toFunction(String func) {
             if (Function.isValid(func))
                 return Function.valueOf(func);
             else
                 throw new BadQueryException(String.format("%s is not a valid function", func));
        }

        protected void setValue(Path<T> path) {
            if (value instanceof Collection)
                return;
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

        protected From findMapJoin(String property, From from, QueryState<T> state, boolean l) {
            String[] entry = property.split("\\.");
            String path = entry[0];

            if (state.joins.containsKey(path)) {
                from = state.joins.get(path);
            } else {
                from = from.join(path, JoinType.LEFT);
                state.joins.put(path, from);
            }
            if (l)
                key = entry.length > 1 ? entry[1] : "key";
            else
                s = entry.length > 1 ? entry[1] : "key";

            return from;
        }

        protected From findJoin(String property, From from, QueryState<T> state, boolean l) {
            Optional<String> attribute = getEntityIdName(property, state.root, state.cb);
            if (attribute.isPresent()) {
                property = String.format("%s.%s", property, attribute.get());
            }

            if (property.contains(".")) {
                if (Map.class.isAssignableFrom(from.get(property.substring(0, property.indexOf('.'))).getJavaType())) {
                    return findMapJoin(property, from, state, l);
                }
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
            } else if (Map.class.isAssignableFrom(from.get(property).getJavaType())) {
                from = findMapJoin(property, from, state, l);
            }
            else if (Collection.class.isAssignableFrom(from.get(property).getJavaType())) {
                if (state.joins.containsKey(property)) {
                    from = state.joins.get(property);
                } else {
                    from = from.join(property, JoinType.LEFT);
                    state.joins.put(property, from);
                }
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
                    ? Optional.of(idNames(root)[0])
                    : Optional.empty();
        }

        protected Path assignPath(From from, String prop) {
            if (from instanceof MapJoin){
                MapJoin<?, ?, ?> mapJoin = ((MapJoin<?, ?, ?>) from);
                return prop.equals("key") ? mapJoin.key() : mapJoin.value();
            } else if (from instanceof Join
                    && ((Join<?, ?>) from).getAttribute().getPersistentAttributeType().name().equals("ELEMENT_COLLECTION")) {
                return from;
            } else {
                return from.get(prop);
            }
        }

        public Predicate apply(QueryState state) {
            Root root = state.root;
            CriteriaBuilder cb = state.cb;
            Predicate predicate;
            try {
                if (key == null || key.isEmpty()) {
                    key = idNames(root)[0];
                }
                From l = findJoin(key, state.root, state, true);
                From r = prop ? findJoin(s, state.root, state, false) : root;

                p1 = assignPath(l, key);
                if (!prop)
                    setValue(p1);

                p2 = prop ? assignPath(r, s) : null;
                javax.persistence.criteria.Expression exp1 = f1 == null ? p1 : applyFunction(f1, p1, cb);
                javax.persistence.criteria.Expression exp2 = f2 == null ? p2 : applyFunction(f2, p2, cb);

                switch (operator) {
                    case eq:
                        predicate = prop
                                ? cb.equal(exp1, cb.nullif(exp2, value instanceof Comparable ? cb.sum(exp2, 1) : ""))
                                : cb.equal(exp1, value);
                        break;
                    case lt:
                        if (prop)
                            predicate = cb.lt(exp1, exp2);
                        else
                            predicate = value instanceof Comparable
                                    ? cb.lessThan(exp1, (Comparable) value)
                                    : cb.lessThan(exp1, s);
                        break;
                    case lte:
                        if (prop)
                            predicate = cb.lessThanOrEqualTo(exp1, exp2);
                        else
                            predicate = value instanceof Comparable
                                    ? cb.lessThanOrEqualTo(exp1, (Comparable) value)
                                    : cb.lessThanOrEqualTo(exp1, s);
                        break;
                    case gt:
                        if (prop)
                            predicate = cb.gt(exp1, exp2);
                        else
                            predicate = value instanceof Comparable
                                ? cb.greaterThan(exp1, (Comparable) value)
                                : cb.greaterThan(exp1, s);
                        break;
                    case gte:
                        if (prop)
                            predicate = cb.greaterThanOrEqualTo(exp1, exp2);
                        else
                            predicate = value instanceof Comparable
                                    ? cb.greaterThanOrEqualTo(exp1, (Comparable) value)
                                    : cb.greaterThanOrEqualTo(exp1, s);
                        break;
                    case isnull:
                        predicate = cb.isNull(exp1);
                        break;
                    case in:
                        predicate = exp1.in((Collection<?>) value);
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

    public static class BiDirectionException extends RuntimeException {
        public BiDirectionException(Class parent, String field, Class type, String persistenceType) {
           super(String.format("lazy fetching requires a defined bi-directional association using " +
                   "explicit mappedBy annotation attribute or just one implicit reverse mapping: %s.%s -> %s(%s)"
                   , parent.getSimpleName(), field, persistenceType, type.getSimpleName()));
        }
    }
}

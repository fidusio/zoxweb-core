package org.zoxweb.server.util;

import org.zoxweb.shared.api.APIBatchResult;
import org.zoxweb.shared.api.APIConfigInfo;
import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.api.APIExceptionHandler;
import org.zoxweb.shared.api.APISearchResult;
import org.zoxweb.shared.db.QueryMarker;
import org.zoxweb.shared.db.QueryMatch;
import org.zoxweb.shared.data.LongSequence;
import org.zoxweb.shared.util.Const.RelationalOperator;
import org.zoxweb.shared.util.DynamicEnumMap;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.IDGenerator;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A minimal in-memory {@link APIDataStore} for unit tests.
 *
 * <p>Entities are grouped into "collections" keyed by their
 * {@link NVConfigEntity#getName() NVConfigEntity name} (e.g. {@code subject_identifier},
 * {@code permission_info}, {@code ci_password}); within a collection each entity is keyed
 * by its GUID. {@link #insert(NVEntity)} auto-assigns a random-UUID GUID when the entity
 * has none. {@code search}, {@code searchByID}, {@code update}, {@code delete} and
 * {@code countMatch} operate against those collections; {@code search} evaluates
 * {@link QueryMatch} criteria with {@link NVEntity#lookupValue(String) field lookups}
 * (dotted/canonical names supported) and AND semantics.</p>
 *
 * <p>Everything outside that core CRUD/query surface (sequences, dynamic enum maps,
 * batch/user search, connection lifecycle) is intentionally unsupported and throws
 * {@link UnsupportedOperationException} - this is a test double, not a real store.</p>
 */
public class MockAPIDataStore
        implements APIDataStore<Void, Void> {

    // collection name -> (guid -> entity)
    private final Map<String, Map<String, NVEntity>> collections = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    private String name = "mock-data-store";
    private String description;
    private APIConfigInfo configInfo;
    private APIExceptionHandler exceptionHandler;

    private Map<String, NVEntity> collection(String collectionName) {
        return collections.computeIfAbsent(collectionName, k -> new ConcurrentHashMap<>());
    }

    private static String collectionOf(NVConfigEntity nvce) {
        return nvce.getName();
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <V extends NVEntity> V insert(V nve) {
        SUS.checkIfNulls("Null entity", nve);
        if (SUS.isEmpty(nve.getGUID())) {
            nve.setGUID(UUID.randomUUID().toString());
        }
        collection(nve.getNVConfig().getName()).put(nve.getGUID(), nve);
        return nve;
    }

    @Override
    public <V extends NVEntity> V update(V nve) {
        SUS.checkIfNulls("Null entity", nve);
        if (SUS.isEmpty(nve.getGUID())) {
            throw new IllegalArgumentException("Entity has no GUID");
        }
        collection(nve.getNVConfig().getName()).put(nve.getGUID(), nve);
        return nve;
    }

    @Override
    public <V extends NVEntity> boolean delete(V nve, boolean withReference) {
        if (nve == null || SUS.isEmpty(nve.getGUID())) {
            return false;
        }
        return collection(nve.getNVConfig().getName()).remove(nve.getGUID()) != null;
    }

    @Override
    public <V extends NVEntity> V patch(V nve, boolean updateTS, boolean sync, boolean updateRefOnly,
                                        boolean includeParam, String... nvConfigNames) {
        // the mock does not model partial updates - it replaces the whole entity
        return update(nve);
    }

    @Override
    public <V extends NVEntity> boolean delete(NVConfigEntity nvce, QueryMarker... queryCriteria) {
        List<NVEntity> matches = matching(collectionOf(nvce), queryCriteria);
        Map<String, NVEntity> col = collection(collectionOf(nvce));
        boolean removed = false;
        for (NVEntity nve : matches) {
            removed |= col.remove(nve.getGUID()) != null;
        }
        return removed;
    }

    // ------------------------------------------------------------------
    // search
    // ------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <V extends NVEntity> List<V> search(NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria) {
        return (List<V>) matching(collectionOf(nvce), queryCriteria);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends NVEntity> List<V> search(String className, List<String> fieldNames, QueryMarker... queryCriteria) {
        return (List<V>) matching(collectionForClassName(className), queryCriteria);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends NVEntity> List<V> searchByID(NVConfigEntity nvce, String... ids) {
        return (List<V>) byID(collectionOf(nvce), ids);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends NVEntity> List<V> searchByID(String className, String... ids) {
        return (List<V>) byID(collectionForClassName(className), ids);
    }

    @Override
    public long countMatch(NVConfigEntity nvce, QueryMarker... queryCriteria) {
        return matching(collectionOf(nvce), queryCriteria).size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <NT, RT> NT lookupByReferenceID(String metaTypeName, RT objectId) {
        return (NT) collection(metaTypeName).get(String.valueOf(objectId));
    }

    @Override
    public <NT, RT, NIT> NT lookupByReferenceID(String metaTypeName, RT objectId, NIT projection) {
        return lookupByReferenceID(metaTypeName, objectId);
    }

    @Override
    public boolean isValidReferenceID(String refID) {
        return SUS.isNotEmpty(refID);
    }

    // ------------------------------------------------------------------
    // matching helpers
    // ------------------------------------------------------------------

    private List<NVEntity> byID(String collectionName, String... ids) {
        List<NVEntity> ret = new ArrayList<>();
        Map<String, NVEntity> col = collection(collectionName);
        if (ids != null) {
            for (String id : ids) {
                NVEntity nve = col.get(id);
                if (nve != null) {
                    ret.add(nve);
                }
            }
        }
        return ret;
    }

    private List<NVEntity> matching(String collectionName, QueryMarker... queryCriteria) {
        List<NVEntity> ret = new ArrayList<>();
        for (NVEntity nve : collection(collectionName).values()) {
            if (matchesAll(nve, queryCriteria)) {
                ret.add(nve);
            }
        }
        return ret;
    }

    private static boolean matchesAll(NVEntity nve, QueryMarker... queryCriteria) {
        if (queryCriteria == null) {
            return true;
        }
        for (QueryMarker qm : queryCriteria) {
            if (qm instanceof QueryMatch && !matches(nve, (QueryMatch<?>) qm)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean matches(NVEntity nve, QueryMatch<?> qm) {
        Object actual = nve.lookupValue(qm.getName());
        Object expected = qm.getValue();
        RelationalOperator op = qm.getOperator();

        if (actual instanceof String || expected instanceof String) {
            boolean equal = SharedStringUtil.equals(
                    actual != null ? actual.toString() : null,
                    expected != null ? expected.toString() : null,
                    true);
            return op == RelationalOperator.NOT_EQUAL ? !equal : equal;
        }

        if (op == RelationalOperator.EQUAL || op == RelationalOperator.NOT_EQUAL) {
            boolean equal = actual == null ? expected == null : actual.equals(expected);
            return op == RelationalOperator.NOT_EQUAL ? !equal : equal;
        }

        if (actual instanceof Comparable && expected != null) {
            int cmp = ((Comparable) actual).compareTo(expected);
            switch (op) {
                case GT:  return cmp > 0;
                case GTE: return cmp >= 0;
                case LT:  return cmp < 0;
                case LTE: return cmp <= 0;
                default:  return false;
            }
        }
        return false;
    }

    private String collectionForClassName(String className) {
        // Map a fully-qualified/simple class name to a stored collection by matching
        // any entity already present whose concrete class name matches.
        String simple = SharedStringUtil.valueAfterRightToken(className, ".");
        for (Map.Entry<String, Map<String, NVEntity>> e : collections.entrySet()) {
            for (NVEntity nve : e.getValue().values()) {
                if (nve.getClass().getName().equals(className)
                        || nve.getClass().getSimpleName().equals(simple)) {
                    return e.getKey();
                }
            }
        }
        return className;
    }

    // ------------------------------------------------------------------
    // store identity
    // ------------------------------------------------------------------

    @Override
    public String getStoreName() {
        return name;
    }

    @Override
    public Set<String> getStoreTables() {
        return new LinkedHashSet<>(collections.keySet());
    }

    // ------------------------------------------------------------------
    // sequences (simple in-memory counters)
    // ------------------------------------------------------------------

    @Override
    public LongSequence createSequence(String sequenceName) {
        return createSequence(sequenceName, 0, 1);
    }

    @Override
    public LongSequence createSequence(String sequenceName, long startValue, long defaultIncrement) {
        sequences.putIfAbsent(sequenceName, new AtomicLong(startValue));
        return null;
    }

    @Override
    public void deleteSequence(String sequenceName) {
        sequences.remove(sequenceName);
    }

    @Override
    public long currentSequenceValue(String sequenceName) {
        AtomicLong seq = sequences.get(sequenceName);
        if (seq == null) {
            throw new IllegalArgumentException("No such sequence: " + sequenceName);
        }
        return seq.get();
    }

    @Override
    public long nextSequenceValue(String sequenceName) {
        return nextSequenceValue(sequenceName, 1);
    }

    @Override
    public long nextSequenceValue(String sequenceName, long increment) {
        return sequences.computeIfAbsent(sequenceName, k -> new AtomicLong(0)).addAndGet(increment);
    }

    // ------------------------------------------------------------------
    // APIServiceProvider lifecycle / config
    // ------------------------------------------------------------------

    @Override
    public APIConfigInfo getAPIConfigInfo() {
        return configInfo;
    }

    @Override
    public void setAPIConfigInfo(APIConfigInfo configInfo) {
        this.configInfo = configInfo;
    }

    @Override
    public Void connect() {
        return null;
    }

    @Override
    public Void newConnection() {
        return null;
    }

    @Override
    public void close() {
        collections.clear();
        sequences.clear();
    }

    @Override
    public boolean isProviderActive() {
        return true;
    }

    @Override
    public APIExceptionHandler getAPIExceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public void setAPIExceptionHandler(APIExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public <T> T lookupProperty(GetName propertyName) {
        return null;
    }

    @Override
    public long lastTimeAccessed() {
        return 0;
    }

    @Override
    public long inactivityDuration() {
        return 0;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toCanonicalID() {
        return name;
    }

    // ------------------------------------------------------------------
    // unsupported surface
    // ------------------------------------------------------------------

    @Override
    public <T> APISearchResult<T> batchSearch(NVConfigEntity nvce, QueryMarker... queryCriteria) {
        throw new UnsupportedOperationException("batchSearch not supported by the mock data store");
    }

    @Override
    public <T> APISearchResult<T> batchSearch(String className, QueryMarker... queryCriteria) {
        throw new UnsupportedOperationException("batchSearch not supported by the mock data store");
    }

    @Override
    public <T, V extends NVEntity> APIBatchResult<V> nextBatch(APISearchResult<T> results, int startIndex, int batchSize) {
        throw new UnsupportedOperationException("nextBatch not supported by the mock data store");
    }

    @Override
    public <V extends NVEntity> List<V> userSearch(String userID, NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria) {
        return search(nvce, fieldNames, queryCriteria);
    }

    @Override
    public <V extends NVEntity> List<V> userSearch(String userID, String className, List<String> fieldNames, QueryMarker... queryCriteria) {
        return search(className, fieldNames, queryCriteria);
    }

    @Override
    public <V extends NVEntity> List<V> userSearchByID(String userID, NVConfigEntity nvce, String... ids) {
        return searchByID(nvce, ids);
    }

    @Override
    public DynamicEnumMap insertDynamicEnumMap(DynamicEnumMap dynamicEnumMap) {
        throw new UnsupportedOperationException("DynamicEnumMap not supported by the mock data store");
    }

    @Override
    public DynamicEnumMap updateDynamicEnumMap(DynamicEnumMap dynamicEnumMap) {
        throw new UnsupportedOperationException("DynamicEnumMap not supported by the mock data store");
    }

    @Override
    public DynamicEnumMap searchDynamicEnumMapByName(String name) {
        throw new UnsupportedOperationException("DynamicEnumMap not supported by the mock data store");
    }

    @Override
    public void deleteDynamicEnumMap(String name) {
        throw new UnsupportedOperationException("DynamicEnumMap not supported by the mock data store");
    }

    @Override
    public List<DynamicEnumMap> getAllDynamicEnumMap(String domainID, String userID) {
        throw new UnsupportedOperationException("DynamicEnumMap not supported by the mock data store");
    }

    @Override
    public <NID> IDGenerator<String, NID> getIDGenerator() {
        return null;
    }
}

/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.api;

import org.zoxweb.shared.data.LongSequence;
import org.zoxweb.shared.db.QueryMarker;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The data store API. Defines the contract for a persistent store of {@link NVEntity}
 * objects, providing CRUD operations, search and batch retrieval, dynamic enum map
 * management, and named long sequence support.
 *
 * @param <P> the native connection or driver type of the underlying data store
 * @param <S> the native session or client type of the underlying data store
 */
public interface APIDataStore<P, S>
        extends APIServiceProvider<P, S> {

    /**
     * Returns the data store name.
     *
     * @return the data store name
     */
    String getStoreName();

    /**
     * This method retrieves the storage tables.
     *
     * @return the set of table (collection) names
     */
    Set<String> getStoreTables();


    /**
     * Searches for entities matching the query criteria and returns the first match.
     *
     * @param <V>           the entity type
     * @param nvce          the meta type of the entity to search for
     * @param fieldNames    the names of the fields to be returned, null or empty for all fields
     * @param queryCriteria the search criteria
     * @return the first matching entity, null if no match was found
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    default <V extends NVEntity> V findOne(NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        List<V> ret = search(nvce, fieldNames, queryCriteria);

        return (ret == null || ret.isEmpty()) ? null : ret.get(0);
    }

    /**
     * Begins a transaction if the underlying data store supports transactions.
     *
     * @param <T> the native transaction or session type
     * @return the transaction object, null if transactions are not supported
     */
    default <T> T beginTransaction() {
        return null;
    }

    /**
     * Commits and ends the current transaction, no-op if transactions are not supported.
     */
    default void endTransaction() {
    }

    /**
     * Aborts the current transaction, no-op if transactions are not supported.
     */
    default void abortTransaction() {
    }

    /**
     * This method searches for documents.
     *
     * @param <V>           the entity type
     * @param nvce          the meta type of the entity to search for
     * @param fieldNames    the names of the fields to be returned, null or empty for all fields
     * @param queryCriteria the search criteria
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <V extends NVEntity> List<V> search(NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Searches for entities matching the query criteria and returns the first match,
     * using the class name as the collection.
     *
     * @param <V>           the entity type
     * @param className     the entity class name used as the collection name
     * @param fieldNames    the names of the fields to be returned, null or empty for all fields
     * @param queryCriteria the search criteria
     * @return the first matching entity, null if no match was found
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    default <V extends NVEntity> V findOne(String className, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        List<V> ret = search(className, fieldNames, queryCriteria);

        return (ret == null || ret.isEmpty()) ? null : ret.get(0);
    }

    /**
     * Search based on the class name as collection.
     *
     * @param <V>           the entity type
     * @param className     the entity class name used as the collection name
     * @param fieldNames    the names of the fields to be returned, null or empty for all fields
     * @param queryCriteria the search criteria
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <V extends NVEntity> List<V> search(String className, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Report search based on the NVConfigEntity collection type. The returned result
     * holds the matching IDs and can be paged via {@link #nextBatch(APISearchResult, int, int)}.
     *
     * @param <T>           the report ID type
     * @param nvce          the meta type of the entity to search for
     * @param queryCriteria the search criteria
     * @return the search result holding the matching IDs
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <T> APISearchResult<T> batchSearch(NVConfigEntity nvce, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Report search based on the class collection. The returned result holds the
     * matching IDs and can be paged via {@link #nextBatch(APISearchResult, int, int)}.
     *
     * @param <T>           the report ID type
     * @param className     the entity class name used as the collection name
     * @param queryCriteria the search criteria
     * @return the search result holding the matching IDs
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <T> APISearchResult<T> batchSearch(String className, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Utility method that converts field names into a list.
     *
     * @param names the field names
     * @return the field names as a list
     */
    static List<String> fieldNames(String... names) {
        return new ArrayList<String>(Arrays.asList(names));
    }

    /**
     * Utility method that converts {@link GetName} field names into a list,
     * skipping null entries and null names.
     *
     * @param names the field names
     * @return the field names as a list
     */
    static List<String> fieldNames(GetName... names) {
        List<String> ret = new ArrayList<>();
        for (GetName name : names) {
            if (name != null && name.getName() != null)
                ret.add(name.getName());
        }
        return ret;
    }

    /**
     * Batch result retrieval. Reads the next batch of entities from a previous
     * {@link #batchSearch(NVConfigEntity, QueryMarker...)} result.
     *
     * @param <T>        the report ID type
     * @param <V>        the entity type
     * @param results    the search result returned by a batchSearch call
     * @param startIndex the index of the first entity to be returned
     * @param batchSize  the maximum number of entities to be returned
     * @return the batch of matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the retrieval fails
     */
    <T, V extends NVEntity> APIBatchResult<V> nextBatch(APISearchResult<T> results, int startIndex, int batchSize)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * User specific search, the results are scoped to the given user.
     *
     * @param <V>           the entity type
     * @param userID        the ID of the user owning the entities
     * @param nvce          the meta type of the entity to search for
     * @param fieldNames    the names of the fields to be returned, null or empty for all fields
     * @param queryCriteria the search criteria
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <V extends NVEntity> List<V> userSearch(String userID, NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * User specific search based on the class name as collection, the results are
     * scoped to the given user.
     *
     * @param <V>           the entity type
     * @param userID        the ID of the user owning the entities
     * @param className     the entity class name used as the collection name
     * @param fieldNames    the names of the fields to be returned, null or empty for all fields
     * @param queryCriteria the search criteria
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <V extends NVEntity> List<V> userSearch(String userID, String className, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * This method searches for documents based on id.
     *
     * @param <V>  the entity type
     * @param nvce the meta type of the entity to search for
     * @param ids  the IDs of the entities to be retrieved
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <V extends NVEntity> List<V> searchByID(NVConfigEntity nvce, String... ids)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * This method searches for documents based on id.
     *
     * @param <V>       the entity type
     * @param className the class must extend NVEntity otherwise it will throw APIException
     * @param ids       the IDs of the entities to be retrieved
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails or the class does not extend NVEntity
     */
    <V extends NVEntity> List<V> searchByID(String className, String... ids)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * This method searches for documents based on id, the results are scoped to the given user.
     *
     * @param <V>    the entity type
     * @param userID the ID of the user owning the entities
     * @param nvce   the meta type of the entity to search for
     * @param ids    the IDs of the entities to be retrieved
     * @return the matching entities
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the search fails
     */
    <V extends NVEntity> List<V> userSearchByID(String userID, NVConfigEntity nvce, String... ids)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * This method inserts a document.
     *
     * @param <V> the entity type
     * @param nve the entity to be inserted
     * @return the inserted entity with its assigned identifiers
     * @throws NullPointerException     if nve is null
     * @throws IllegalArgumentException if the entity is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the insert fails
     */
    <V extends NVEntity> V insert(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * This method deletes a document.
     *
     * @param <V>           the entity type
     * @param nve           the entity to be deleted
     * @param withReference if true the entities referenced by nve are deleted as well
     * @return true if the entity was deleted
     * @throws NullPointerException     if nve is null
     * @throws IllegalArgumentException if the entity is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the delete fails
     */
    <V extends NVEntity> boolean delete(V nve, boolean withReference)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * This method will delete documents that match the query criteria.
     *
     * @param <V>           the entity type
     * @param nvce          the meta type of the entities to be deleted
     * @param queryCriteria the criteria that the entities to be deleted must match
     * @return true if at least one entity was deleted
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the delete fails
     */
    <V extends NVEntity> boolean delete(NVConfigEntity nvce, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * This method updates a document.
     *
     * @param <V> the entity type
     * @param nve the entity to be updated
     * @return the updated entity
     * @throws NullPointerException     if nve is null
     * @throws IllegalArgumentException if the entity is invalid
     * @throws APIException             if the update fails
     */
    <V extends NVEntity> V update(V nve)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * This method patches a document, updating only the selected fields.
     *
     * @param <V>           the entity type
     * @param nve           to be updated
     * @param updateTS      if true the last update timestamp will be updated
     * @param sync          if true the datastore update become synchronized
     * @param updateRefOnly will update the reference only
     * @param includeParam  if true the nvConfigNames list will be updated, if false the nvConfigNames will be excluded
     * @param nvConfigNames to be updated if null or empty the whole object will be updated
     * @return the patched entity
     * @throws NullPointerException     if nve is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws APIException             if the patch fails
     */
    <V extends NVEntity> V patch(V nve, boolean updateTS, boolean sync, boolean updateRefOnly, boolean includeParam, String... nvConfigNames)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * This method counts the number of matched documents found.
     *
     * @param nvce          the meta type of the entities to be counted
     * @param queryCriteria the criteria that the entities must match
     * @return the matching count
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws APIException             if the count fails
     */
    long countMatch(NVConfigEntity nvce, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, APIException;


    /**
     * This method inserts the dynamic enum into the database.
     *
     * @param dynamicEnumMap the dynamic enum map to be inserted
     * @return the inserted enum map
     * @throws NullPointerException     if dynamicEnumMap is null
     * @throws IllegalArgumentException if the enum map is invalid
     * @throws APIException             if the insert fails
     */
    DynamicEnumMap insertDynamicEnumMap(DynamicEnumMap dynamicEnumMap)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * This method updates the dynamic enum already in the database.
     *
     * @param dynamicEnumMap the dynamic enum map to be updated
     * @return the updated enum map
     * @throws NullPointerException     if dynamicEnumMap is null
     * @throws IllegalArgumentException if the enum map is invalid
     * @throws APIException             if the update fails
     */
    DynamicEnumMap updateDynamicEnumMap(DynamicEnumMap dynamicEnumMap)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * This method searches for the dynamic enum by name.
     *
     * @param name the name of the dynamic enum map
     * @return the matching enum map
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if name is invalid
     * @throws APIException             if the search fails
     */
    DynamicEnumMap searchDynamicEnumMapByName(String name)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * This method deletes a dynamic enum based on name.
     *
     * @param name the name of the dynamic enum map to be deleted
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if name is invalid
     * @throws APIException             if the delete fails
     */
    void deleteDynamicEnumMap(String name)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * Returns the ID generator used by the data store to create entity identifiers.
     *
     * @param <NID> the native ID type of the data store
     * @return the ID generator
     */
    <NID> IDGenerator<String, NID> getIDGenerator();


    /**
     * This method returns a list of dynamic enum map in the dynamic enum map collection.
     *
     * @param domainID the domain ID
     * @param userID   the user ID
     * @return all the enum maps
     * @throws NullPointerException     if a required parameter is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the retrieval fails
     */
    List<DynamicEnumMap> getAllDynamicEnumMap(String domainID, String userID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Looks up an object by its reference ID.
     *
     * @param <NT>         the returned object type
     * @param <RT>         the reference ID type
     * @param metaTypeName the meta type name used as the collection name
     * @param objectId     the reference ID of the object
     * @return the matching object, null if no match was found
     */
    <NT, RT> NT lookupByReferenceID(String metaTypeName, RT objectId);


    /**
     * Looks up an object by its reference ID with a projection.
     *
     * @param <NT>         the returned object type
     * @param <RT>         the reference ID type
     * @param <NIT>        the projection type
     * @param metaTypeName the meta type name used as the collection name
     * @param objectId     the reference ID of the object
     * @param projection   the fields to be included in the returned object
     * @return the matching object, null if no match was found
     */
    <NT, RT, NIT> NT lookupByReferenceID(String metaTypeName, RT objectId, NIT projection);

    /**
     * Creates a named long sequence with the default start value and increment,
     * if the sequence already exists the existing one is returned.
     *
     * @param sequenceName the name of the sequence
     * @return the sequence
     * @throws NullPointerException     if sequenceName is null
     * @throws IllegalArgumentException if sequenceName is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the creation fails
     */
    LongSequence createSequence(String sequenceName)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Creates a named long sequence.
     *
     * @param sequenceName     the name of the sequence
     * @param startValue       the start value of the sequence
     * @param defaultIncrement the default increment of the sequence
     * @return the sequence
     * @throws NullPointerException     if sequenceName is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the creation fails
     */
    LongSequence createSequence(String sequenceName, long startValue, long defaultIncrement)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Deletes a named long sequence.
     *
     * @param sequenceName the name of the sequence to be deleted
     * @throws NullPointerException     if sequenceName is null
     * @throws IllegalArgumentException if sequenceName is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the delete fails
     */
    void deleteSequence(String sequenceName)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Returns the current value of a named long sequence without incrementing it.
     *
     * @param sequenceName the name of the sequence
     * @return the current sequence value
     * @throws NullPointerException     if sequenceName is null
     * @throws IllegalArgumentException if sequenceName is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the retrieval fails
     */
    long currentSequenceValue(String sequenceName)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Increments a named long sequence by its default increment and returns the new value.
     *
     * @param sequenceName the name of the sequence
     * @return the next sequence value
     * @throws NullPointerException     if sequenceName is null
     * @throws IllegalArgumentException if sequenceName is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the increment fails
     */
    long nextSequenceValue(String sequenceName)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Increments a named long sequence by the given increment and returns the new value.
     *
     * @param sequenceName the name of the sequence
     * @param increment    the increment to be applied
     * @return the next sequence value
     * @throws NullPointerException     if sequenceName is null
     * @throws IllegalArgumentException if a parameter is invalid
     * @throws AccessException          if access is denied
     * @throws APIException             if the increment fails
     */
    long nextSequenceValue(String sequenceName, long increment)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Checks if the given reference ID is valid for the underlying data store.
     *
     * @param refID the reference ID to be checked
     * @return true if the reference ID is valid
     */
    boolean isValidReferenceID(String refID);
}

package cz.pikadorama.simpleorm.dao;

import java.util.List;

public interface Dao<T> {

    /**
     * Finds object in database by the given ID.
     *
     * @param id object ID
     * @return object or null
     */
    T getById(int id);

    /**
     * Finds objects in database with the given IDs.
     *
     * @param ids object IDs
     * @return objects or empty list
     */
    List<T> getByIds(List<Integer> ids);

    /**
     * Creates a new object in database.
     *
     * @param obj instance to create
     * @return ID of the created object, -1 if the creation failed
     */
    long create(T obj);

    /**
     * Updates old object in database with the same ID with the new values.
     *
     * @param obj instance with new values, ID must not be changed
     */
    void update(T obj);

    /**
     * Deletes object from database.
     *
     * @param obj object to delete
     */
    void delete(T obj);


    /**
     * Deletes object from database.
     *
     * @param id id of object to delete
     */
    void delete(int id);

    /**
     * Deletes all objetcs of the type from the database. Does not cascade.
     */
    void deleteAll();

    /**
     * Finds all objects in database.
     *
     * @return list of all objects or empty list
     */
    List<T> findAll();

    /**
     * Custom query.
     *
     * @param query       query
     * @param columnNames column names to select
     * @return list of all objects that satisfy the query or empty list
     */
    List<T> query(String query, String[] columnNames);
}

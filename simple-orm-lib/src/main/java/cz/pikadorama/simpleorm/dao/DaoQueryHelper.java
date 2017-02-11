package cz.pikadorama.simpleorm.dao;

import android.content.ContentValues;
import android.database.Cursor;

import cz.pikadorama.simpleorm.annotation.DbTable;

/**
 * Each database table (e.i. entity with {@link DbTable} annotation}) must add the
 * {@link DbTable#mappingClass()} attribute which accepts implementation of this interface.
 *
 * This is used to transform POJO --- Cursor
 */
public interface DaoQueryHelper<T> {

    /**
     * Transform cursor data to object of type T.
     *
     * @param cursor cursor
     * @return object with data from the cursor
     */
    T cursorToObject(Cursor cursor);

    /**
     * Create content values from the given object for further use in database access.
     *
     * @param obj object
     * @return content values from the object
     */
    ContentValues objectToContentValues(T obj);

    /**
     * Return object's ID. If the object is null or the ID is not set, return null.
     *
     * @param obj object
     * @return ID or null
     */
    Integer getId(T obj);

    /**
     * Helper method to set ID of newly created object. Callback.
     *
     * @param obj new object (no ID yet)
     * @param id  id to set (provided by database)
     */
    void setId(T obj, Integer id);

}

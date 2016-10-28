package cz.pikadorama.simpleorm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.pikadorama.simpleorm.annotation.DbColumn;
import cz.pikadorama.simpleorm.annotation.DbTable;
import cz.pikadorama.simpleorm.dao.Dao;
import cz.pikadorama.simpleorm.dao.DaoQueryHelper;
import cz.pikadorama.simpleorm.util.Const;
import cz.pikadorama.simpleorm.util.Strings;

/**
 * Class responsible for dynamic creation of DAO objects. Use {@link #getDao(Class)} method
 * to get implementation for your entity class. The entity must be properly annotated by
 * {@link DbTable} and {@link DbColumn} annotations.
 */
public class DaoManager {

    private static final Map<Class<?>, DaoQueryHelper<?>> daoQueryHelpers = new HashMap<>();
    private static final Map<Class<?>, Dao<?>> daos = new HashMap<>();

    /**
     * Register DAO for the given DAO type. Overrides any DAO of the same type registered before.
     * If not necessary, use {@link #getDao(Class)} to give you default DAO implementation. This
     * way you don't need to implement it by yourself.
     *
     * @param daoType DAO type
     * @param dao     DAO implementation
     */
    public static void registerDao(Class<?> daoType, Dao<?> dao) {
        daos.put(daoType, dao);
    }

    /**
     * Get DAO implementation for the given DAO type.
     *
     * @param daoType DAO type
     * @return DAO implementation
     */
    public static <T> Dao<T> getDao(Class<T> daoType) {
        if (daos.containsKey(daoType)) {
            return (Dao<T>) daos.get(daoType);
        } else {
            Dao<T> dao = new DefaultDao<T>(daoType);
            registerDao(daoType, dao);
            return dao;
        }
    }

    /**
     * Register dao query helper implementation for the given DAO type. For each class you want to
     * obtain DAO, query helper needs to be registered. The helper is responsible for conversion
     * between POJO and DB cursor.
     *
     * @param daoType        DAO type
     * @param daoQueryHelper DAO query helper implementation
     */
    static void registerDaoQueryHelper(Class<?> daoType, DaoQueryHelper<?> daoQueryHelper) {
        daoQueryHelpers.put(daoType, daoQueryHelper);
    }

    private static <T> DaoQueryHelper<T> checkAndGetQueryHelper(Class<?> daoType) {
        DaoQueryHelper<T> helper = (DaoQueryHelper<T>) daoQueryHelpers.get(daoType);
        if (helper == null) {
            throw new IllegalArgumentException("There is no Database Query Helper registered for class " + daoType);
        }
        return helper;
    }

    private static final class DefaultDao<T> implements Dao<T> {

        private final String tableName;
        private final String[] columnNames;
        private final DaoQueryHelper<T> helper;

        private DefaultDao(Class<T> daoType) {
            this.helper = checkAndGetQueryHelper(daoType);
            this.tableName = daoType.getAnnotation(DbTable.class).name();

            List<String> columnNamesList = DbManager.getColumnNames(tableName);
            this.columnNames = columnNamesList.toArray(new String[columnNamesList.size()]);
        }

        @Override
        public T getById(int id) {
            SQLiteDatabase db = DbManager.getHelper().getReadableDatabase();
            try (Cursor cursor = db.query(tableName, columnNames, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    return helper.cursorToObject(cursor);
                }
                throw new IllegalStateException(String.format("No record found in table=%s for ID=%s.", id, tableName));
            }
        }

        @Override
        public List<T> getByIds(List<Integer> ids) {
            List<String> stringIds = Lists.transform(ids, new Function<Integer, String>() {
                @Override
                public String apply(Integer input) {
                    return String.valueOf(input);
                }
            });

            SQLiteDatabase db = DbManager.getHelper().getReadableDatabase();
            try (Cursor cursor = db.query(tableName, columnNames,
                    BaseColumns._ID + " IN " + Strings.makeSqlPlaceholders(stringIds.size()),
                    stringIds.toArray(new String[stringIds.size()]), null, null, null)) {
                List<T> list = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    list.add(helper.cursorToObject(cursor));
                }
                return list;
            }
        }

        @Override
        public long create(T obj) {
            long id = -1;
            SQLiteDatabase db = DbManager.getHelper().getWritableDatabase();
            try {
                db.beginTransaction();
                ContentValues values = helper.objectToContentValues(obj);
                id = db.insertOrThrow(tableName, null, values);
                helper.setId(obj, (int) id);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return id;
        }

        @Override
        public void update(T obj) {
            SQLiteDatabase db = DbManager.getHelper().getWritableDatabase();
            try {
                db.beginTransaction();
                ContentValues values = helper.objectToContentValues(obj);
                db.update(tableName, values, BaseColumns._ID + " = ?", new String[]{String.valueOf(helper.getId(obj))});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public void delete(T obj) {
            delete(helper.getId(obj));
        }

        @Override
        public void delete(int id) {
            SQLiteDatabase db = DbManager.getHelper().getWritableDatabase();
            try {
                db.beginTransaction();
                db.delete(tableName, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public void deleteAll() {
            SQLiteDatabase db = DbManager.getHelper().getWritableDatabase();
            try {
                db.beginTransaction();
                db.execSQL("delete from " + tableName);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public List<T> findAll() {
            SQLiteDatabase db = DbManager.getHelper().getReadableDatabase();
            try (Cursor cursor = db.query(tableName, columnNames, null, null, null, null, null)) {
                List<T> list = new ArrayList<>();
                while (cursor.moveToNext()) {
                    list.add(helper.cursorToObject(cursor));
                }
                return list;
            }
        }

        @Override
        public List<T> query(String query, String[] columnNames) {
            SQLiteDatabase db = DbManager.getHelper().getReadableDatabase();
            try (Cursor cursor = db.rawQuery(query, columnNames)) {
                List<T> list = new ArrayList<>();
                while (cursor.moveToNext()) {
                    list.add(helper.cursorToObject(cursor));
                }
                return list;
            }
        }
    }
}

package cz.pikadorama.simpleorm;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.pikadorama.simpleorm.annotation.DbColumn;
import cz.pikadorama.simpleorm.annotation.DbTable;
import cz.pikadorama.simpleorm.dao.DaoQueryHelper;
import cz.pikadorama.simpleorm.util.Const;


/**
 * Entry point of the DB framework. First of all, you need to register implementation of
 * {@link SQLiteOpenHelper}, just skip {@link SQLiteOpenHelper#onCreate(SQLiteDatabase)}, it will be
 * done automatically.
 */
public final class DbManager {

    private static final Map<String, List<String>> tablesAndColumns = new HashMap<>();

    private static boolean initialized = false;
    private static SQLiteOpenHelper helper = null;

    /**
     * Register your {@link SQLiteOpenHelper} implementation. After the helper is registered, whole database
     * structure is created and initialized. You can than use {@link DaoManager#getDao(Class)}
     * to get DAO implementations for desired classes.
     * <p/>
     * The helper defines your database name and version. It can also provide possible upgrade scripts between
     * versions.
     *
     * @param helperToRegister {@link SQLiteOpenHelper} implementation
     * @param entityClasses    array of entity classes that you want to be handled automatically by the framework
     *                         (they must be annotated with the {@link DbTable} annotation}
     * @see DaoManager
     */
    public synchronized static final void registerHelper(SQLiteOpenHelper helperToRegister, Class<?>... entityClasses) throws IllegalAccessException, InstantiationException {
        if (helper != null) {
            Log.w(Const.TAG, "Another SQLiteOpenHelper is already registered. Skipping this one.");
        } else {
            helper = helperToRegister;
            initDatabase(entityClasses);
        }
    }

    /**
     * Returns instance of database helper.
     *
     * @return database helper instance
     * @throws IllegalStateException in case there is no {@link SQLiteOpenHelper} implementation
     */
    public synchronized static final SQLiteOpenHelper getInstance() {
        if (helper == null) {
            throw new IllegalStateException("There is no SQLiteOpenHelper implementation registered.");
        }
        return helper;

    }

    /**
     * Returns list of columns names for the given table.
     *
     * @param tableName table name
     * @return list of column names
     * @throws IllegalArgumentException if the table does not exist
     */
    static List<String> getColumnNames(String tableName) {
        List<String> columnNames = tablesAndColumns.get(tableName);
        if (columnNames == null) {
            throw new IllegalArgumentException("There is no Table named " + tableName);
        }
        return columnNames;
    }

    /**
     * Initialize database - create tables for the given entities.
     *
     * @param entityClasses entity classes that were used to create a new database
     */
    private synchronized static void initDatabase(Class<?>... entityClasses) throws InstantiationException, IllegalAccessException {
        if (entityClasses == null) {
            throw new IllegalArgumentException("No Entity classes have been specified.");
        }

        for (Class<?> clazz : entityClasses) {
            checkIfEntityClass(clazz);

            DbTable table = clazz.getAnnotation(DbTable.class);
            tablesAndColumns.put(table.name(), getColumnNames(clazz));

            // register DaoQueryHelper for this table
            try {
                DaoQueryHelper<?> helper = (DaoQueryHelper<?>) table.mappingClass().newInstance();
                DaoManager.registerDaoQueryHelper(clazz, helper);
            } catch (InstantiationException | IllegalAccessException e) {
                Log.e(Const.TAG, "Unable to instantiate DaoQueryHelper for " + table.mappingClass(), e);
                throw e;
            }
        }

        createTables(entityClasses);
    }

    /**
     * Automatically prepare tables for the given entity classes. All classes must be annotated
     * with {@link DbTable}, they must have at least one field annotated with {@link DbColumn}
     * and they must implement {@link BaseColumns} and {@link DaoQueryHelper} interfaces.
     * <p/>
     * Should only be called by {@link android.database.sqlite.SQLiteOpenHelper} implementation in onCreate method.
     *
     * @param entityClasses entity classes
     * @see DbTable
     * @see DbColumn
     * @see BaseColumns
     * @see DaoQueryHelper
     */
    private synchronized static void createTables(Class<?>... entityClasses) {
        if (initialized) {
            Log.i(Const.TAG, "Tables have already been created. Skipping recreation.");
            return;
        }

        if (entityClasses == null) {
            throw new IllegalArgumentException("No Entity classes have been specified.");
        }

        for (Class<?> clazz : entityClasses) {
            checkIfEntityClass(clazz);

            // prepare SQL to execute
            String sqlStart = String.format("create table %s (", clazz.getAnnotation(DbTable.class).name());
            String sqlMiddle = "";
            for (DbColumn column : getDbColumns(clazz)) {
                sqlMiddle += String.format("%s %s %s, ", column.name(), column.type(), column.properties());
            }

            // remove the last comma
            StringBuilder builder = new StringBuilder(sqlMiddle);
            builder.replace(sqlMiddle.lastIndexOf(", "), sqlMiddle.lastIndexOf(", ") + 1, "");
            sqlMiddle = builder.toString();

            String sqlEnd = ");";

            // execute CREATE TABLE SQL
            SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();
            try {
                db.beginTransaction();
                String sql = sqlStart + sqlMiddle + sqlEnd;
                db.execSQL(sql);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        initialized = true;
    }

    private static List<String> getColumnNames(Class<?> clazz) {
        List<DbColumn> columns = getDbColumns(clazz);
        return Lists.transform(columns, new Function<DbColumn, String>() {
            @Override
            public String apply(DbColumn input) {
                return input.name();
            }
        });
    }

    private static List<DbColumn> getDbColumns(Class<?> clazz) {
        List<DbColumn> columns = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            DbColumn dbColumnAnnotation = field.getAnnotation(DbColumn.class);
            if (dbColumnAnnotation != null) {
                columns.add(dbColumnAnnotation);
            }
        }
        return columns;
    }

    private static void checkIfEntityClass(Class<?> clazz) {
        if (!BaseColumns.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " does not implement BaseColumns interface.");
        }

        DbTable classAnnotation = clazz.getAnnotation(DbTable.class);
        if (classAnnotation == null) {
            throw new IllegalArgumentException(clazz + " does not have @DbTable annotation.");
        }

        boolean atLeastOneColumnAnnotation = false;
        for (Field field : clazz.getDeclaredFields()) {
            atLeastOneColumnAnnotation |= field.getAnnotation(DbColumn.class) != null;
        }
        if (!atLeastOneColumnAnnotation) {
            throw new IllegalArgumentException(clazz + " does not have any field annotated with @DbColumn annotation.");
        }
    }

}

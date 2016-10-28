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
     * structure is created and initialized. You can then use {@link DaoManager#getDao(Class)}
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
    public synchronized static final void registerHelper(SQLiteOpenHelper helperToRegister,
                                                         Class<?>... entityClasses) throws
            IllegalAccessException, InstantiationException {
        if (initialized) {
            Log.i(Const.TAG, "Database is already initialized. Skipping.");
            return;
        }

        if (entityClasses == null || entityClasses.length == 0) {
            throw new IllegalArgumentException("No Entity classes have been specified.");
        }

        helper = helperToRegister;
        initDatabase(entityClasses);

        initialized = true;
    }

    /**
     * Returns instance of database helper. It need to be registered with
     * {@link #registerHelper(SQLiteOpenHelper, Class[])} before you call this method.
     *
     * @return database helper instance
     * @throws IllegalStateException in case there is no {@link SQLiteOpenHelper} implementation
     */
    public synchronized static final SQLiteOpenHelper getHelper() {
        if (helper == null) {
            throw new IllegalStateException(
                    "There is no SQLiteOpenHelper implementation registered.");
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

    private synchronized static void initDatabase(Class<?>... entityClasses) throws
            InstantiationException, IllegalAccessException {
        for (Class<?> clazz : entityClasses) {
            validateEntityClass(clazz);

            DbTable table = clazz.getAnnotation(DbTable.class);
            tablesAndColumns.put(table.name(), getColumnNames(clazz));

            // register DaoQueryHelper for this table
            try {
                DaoQueryHelper<?> helper = (DaoQueryHelper<?>) table.mappingClass().newInstance();
                DaoManager.registerDaoQueryHelper(clazz, helper);
            } catch (InstantiationException | IllegalAccessException e) {
                Log.e(Const.TAG, "Unable to instantiate DaoQueryHelper for " + table.mappingClass(),
                        e);
                throw e;
            }
        }

        createTables(entityClasses);
    }

    private synchronized static void createTables(Class<?>... entityClasses) {
        for (Class<?> clazz : entityClasses) {
            validateEntityClass(clazz);

            SQLiteDatabase db = DbManager.getHelper().getWritableDatabase();
            try {
                String sql = composeCreateTableSql(clazz);
                db.beginTransaction();
                db.execSQL(sql);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private static String composeCreateTableSql(Class<?> clazz) {
        String sqlStart = String
                .format("create table %s (", clazz.getAnnotation(DbTable.class).name());
        String sqlEnd = ");";

        String sqlMiddle = "";
        for (DbColumn column : getDbColumns(clazz)) {
            sqlMiddle += String
                    .format("%s %s %s, ", column.name(), column.type(), column.properties());
        }

        // remove the last comma
        StringBuilder builder = new StringBuilder(sqlMiddle);
        builder.replace(sqlMiddle.lastIndexOf(","), sqlMiddle.lastIndexOf(",") + 1, "");
        sqlMiddle = builder.toString();

        return sqlStart + sqlMiddle + sqlEnd;
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

    private static void validateEntityClass(Class<?> clazz) {
        DbTable classAnnotation = clazz.getAnnotation(DbTable.class);
        if (classAnnotation == null) {
            throw new IllegalArgumentException(clazz + " does not have @DbTable annotation.");
        }

        boolean baseColumnId = false;
        for (Field field : clazz.getDeclaredFields()) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && BaseColumns._ID.equals(column.name()) &&
                    DbDataType.INTEGER == column.type()) {
                baseColumnId = true;
            }
        }
        if (!baseColumnId) {
            throw new IllegalArgumentException(clazz +
                    " does not have mandatory BaseColumn._ID field of DbDataType.INTEGER defined by @DbColumn annotation.");
        }
    }
}

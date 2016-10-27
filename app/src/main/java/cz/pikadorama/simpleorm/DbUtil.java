package cz.pikadorama.simpleorm;

import android.database.sqlite.SQLiteDatabase;
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

public class DbUtil {

    private static final Map<String, List<String>> tablesAndColumns = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Returns list of columns names for the given table.
     *
     * @param tableName table name
     * @return list of column names
     * @throws IllegalArgumentException if the table does not exist
     */
    synchronized static List<String> getColumnNames(String tableName) {
        List<String> columnNames = tablesAndColumns.get(tableName);
        if (columnNames == null) {
            throw new IllegalArgumentException("There is no Table named " + tableName);
        }
        return columnNames;
    }

    /**
     * Initialize DbUtil and data for {@link DaoManager}.
     *
     * @param entityClasses entity classes that were used to create a new database
     */
    synchronized static void initDatabase(Class<?>... entityClasses) throws InstantiationException, IllegalAccessException {
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
    private static void createTables(Class<?>... entityClasses) {
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
                sqlMiddle += String.format("%s %s %s,", column.name(), column.type(), column.properties());
            }

            // remove the last comma
            StringBuilder builder = new StringBuilder(sqlMiddle);
            builder.replace(sqlMiddle.lastIndexOf(","), sqlMiddle.lastIndexOf(",") + 1, "");
            sqlMiddle = builder.toString();

            String sqlEnd = ");";

            // execute CREATE TABLE SQL
            SQLiteDatabase db = DbHelperManager.getInstance().getWritableDatabase();
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

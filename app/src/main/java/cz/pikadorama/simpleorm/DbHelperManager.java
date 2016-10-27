package cz.pikadorama.simpleorm;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import cz.pikadorama.simpleorm.annotation.DbTable;
import cz.pikadorama.simpleorm.util.Const;


/**
 * Entry point of the DB framework. First of all, you need to register implementation of
 * {@link SQLiteOpenHelper}, just skip {@link SQLiteOpenHelper#onCreate(SQLiteDatabase)}, it will be
 * done automatically.
 */
public final class DbHelperManager {

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
            DbUtil.initDatabase(entityClasses);
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

}

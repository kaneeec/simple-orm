package cz.pikadorama.simpleorm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import cz.pikadorama.simpleorm.annotation.DbColumn;
import cz.pikadorama.simpleorm.annotation.DbTable;
import cz.pikadorama.simpleorm.dao.Dao;
import cz.pikadorama.simpleorm.dao.DaoQueryHelper;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase("test.db");

        DbHelperManager.registerHelper(new TestSQLiteHelper(context), new Class<?>[] { Entity.class });

        Dao<Entity> dao = DaoManager.getDao(Entity.class);
        dao.create(new Entity("foo"));
        dao.create(new Entity("bar"));

        assertEquals(2, dao.findAll().size());
    }

    @DbTable(name = "EntityTable", mappingClass = EntityOrmMapping.class)
    private static final class Entity implements BaseColumns {

        @DbColumn(name = "id", type = DbDataType.INTEGER, properties = "primary key autoincrement")
        private Integer id;

        @DbColumn(name = "name", type = DbDataType.TEXT)
        private String name;

        public Entity(String name) {
            this.name = name;
        }

        public Entity(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static final class EntityOrmMapping implements DaoQueryHelper<Entity> {

        @Override
        public Entity cursorToObject(Cursor cursor) {
            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            return new Entity(id, name);
        }

        @Override
        public ContentValues objectToContentValues(Entity obj) {
            ContentValues cv = new ContentValues();
            cv.put(BaseColumns._ID, obj.getId());
            cv.put("name", obj.getName());
            return cv;
        }

        @Override
        public Integer getId(Entity obj) {
            return obj.getId();
        }

        @Override
        public void setId(Entity obj, Integer id) {
            obj.setId(id);
        }
    }

    private static final class TestSQLiteHelper extends SQLiteOpenHelper {

        public TestSQLiteHelper(Context context) {
            super(context, "test.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }
}

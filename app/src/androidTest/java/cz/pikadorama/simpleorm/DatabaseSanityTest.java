package cz.pikadorama.simpleorm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import cz.pikadorama.simpleorm.annotation.DbColumn;
import cz.pikadorama.simpleorm.annotation.DbTable;
import cz.pikadorama.simpleorm.dao.Dao;
import cz.pikadorama.simpleorm.dao.DaoQueryHelper;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DatabaseSanityTest {

    public static final String DATABASE_NAME = "test.db";
    public static final String TEXT_COLUMN_NAME = "text";
    public static final String TEST_TABLE_NAME = "TestEntityTable";

    @BeforeClass
    public static void prepareDatabase() throws InstantiationException, IllegalAccessException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
        DbManager.registerHelper(new TestSQLiteHelper(context), TestEntity.class);
    }

    @Before
    public void clearDatabase() {
        DaoManager.getDao(TestEntity.class).deleteAll();
    }

    @Test
    public void testInsert() {
        Dao<TestEntity> dao = DaoManager.getDao(TestEntity.class);
        dao.create(new TestEntity());
        dao.create(new TestEntity());
        assertEquals(2, dao.findAll().size());
    }

    @Test
    public void testDelete() {
        Dao<TestEntity> dao = DaoManager.getDao(TestEntity.class);
        TestEntity entity = new TestEntity();
        dao.create(entity);
        dao.delete(entity);
        assertEquals(0, dao.findAll().size());
    }

    @Test
    public void testGetById() {
        Dao<TestEntity> dao = DaoManager.getDao(TestEntity.class);
        TestEntity entity = new TestEntity();
        dao.create(entity);
        TestEntity foundEntity = dao.getById(entity.getId());
        assertEquals(entity, foundEntity);
    }

    @Test
    public void testUpdate() {
        Dao<TestEntity> dao = DaoManager.getDao(TestEntity.class);
        TestEntity entity = new TestEntity();
        dao.create(entity);

        entity.setText("bar");
        dao.update(entity);

        TestEntity foundEntity = dao.getById(entity.getId());
        assertEquals("bar", foundEntity.getText());
    }


    @DbTable(name = TEST_TABLE_NAME, mappingClass = TestEntityQueryHelper.class)
    private static final class TestEntity {

        @DbColumn(name = BaseColumns._ID, type = DbDataType.INTEGER, properties = "primary key autoincrement")
        private Integer id;

        @DbColumn(name = TEXT_COLUMN_NAME, type = DbDataType.TEXT)
        private String text;

        public TestEntity() {
            this.text = "foo";
        }

        public TestEntity(Integer id, String text) {
            this.id = id;
            this.text = text;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestEntity entity = (TestEntity) o;

            if (!id.equals(entity.id)) return false;
            return text != null ? text.equals(entity.text) : entity.text == null;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (text != null ? text.hashCode() : 0);
            return result;
        }
    }

    public static final class TestEntityQueryHelper implements DaoQueryHelper<TestEntity> {

        @Override
        public TestEntity cursorToObject(Cursor cursor) {
            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            String text = cursor.getString(cursor.getColumnIndexOrThrow(TEXT_COLUMN_NAME));
            return new TestEntity(id, text);
        }

        @Override
        public ContentValues objectToContentValues(TestEntity obj) {
            ContentValues cv = new ContentValues();
            cv.put(BaseColumns._ID, obj.getId());
            cv.put(TEXT_COLUMN_NAME, obj.getText());
            return cv;
        }

        @Override
        public Integer getId(TestEntity obj) {
            return obj.getId();
        }

        @Override
        public void setId(TestEntity obj, Integer id) {
            obj.setId(id);
        }
    }

    private static final class TestSQLiteHelper extends SQLiteOpenHelper {

        public TestSQLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        }
    }
}

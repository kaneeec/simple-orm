# simple-orm
Simple ORM framework for Android

# Purpose
The main purpose of this library is to get rid of all the boilerplate code and have a type safe ORM mapping for Android.

# Usage
## Implement SQLite helper as usual
... just specify your database name and version and you would normally do.
```
public class MyHelper extends SQLiteOpenHelper {
    public TestSQLiteHelper(Context context) {
        super(context, "my_database.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // nothing here
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // add custom code if your need to do upgrades between versions
    }
}
```

## Define entity classes
Each entity class must follow two main rules:
- it must have an `Integer id` field annotated as `@DbColumn(name = BaseColumns._ID, type = DbDataType.INTEGER)`
- implement and register `DaoQueryHelper` interface where you define how the ORM mapping is done and set it for the entity in `@DbTable` annotation

### Entity example
```
@DbTable(name = "MyEntity", mappingClass = MyDaoQueryHelper.class)
public class MyEntity {

    @DbColumn(name = BaseColumns._ID, type = DbDataType.INTEGER, properties = "primary key autoincrement")
    private Integer id;

    public TestEntity(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}    
```

### DaoQueryHelper example
```
public class MyDaoQueryHelper implements DaoQueryHelper<MyEntity> {

    @Override
    public MyEntity cursorToObject(Cursor cursor) {
        Integer id = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        return new MyEntity(id);
    }

    @Override
    public ContentValues objectToContentValues(MyEntity obj) {
        ContentValues cv = new ContentValues();
        cv.put(BaseColumns._ID, obj.getId());
        return cv;
    }

    @Override
    public Integer getId(MyEntity obj) {
        return obj.getId();
    }

    @Override
    public void setId(MyEntity obj, Integer id) {
        obj.setId(id);
    }
}
```

## Register the helper
Register your helper and provide your entity classes.
```
DbManager.registerHelper(new MyHelper(context), MyEntity.class);
```


## Use it
```
Dao<MyEntity> dao = DaoManager.getDao(MyEntity.class);
dao.create(myEntityInstance); // save entity to database table
...
```

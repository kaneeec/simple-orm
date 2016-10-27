package cz.pikadorama.simpleorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cz.pikadorama.simpleorm.dao.DaoQueryHelper;

/**
 * Annotation for classes that represent database tables.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DbTable {

    /**
     * Database table name.
     */
    public String name();

    /**
     * Class name for {@link DaoQueryHelper} which implement bi-directional cursor mapping and
     * other mandatory stuff for the DB framework.
     */
    public Class<?> mappingClass();

}

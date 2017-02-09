package cz.pikadorama.simpleorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cz.pikadorama.simpleorm.DbDataType;


/**
 * Annotation for database columns.
 * <p/>
 * NOTE: if you annotate an ID column with autoincrement option, make sure that the data type is
 * {@link DbDataType#INTEGER}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbColumn {

    /**
     * Database column name.
     */
    public String name();

    /**
     * Database column data type.
     */
    public DbDataType type();

    /**
     * (OPTIONAL) Additional database column properties.
     *
     * @return additional database column properties
     */
    public String properties() default "";

}

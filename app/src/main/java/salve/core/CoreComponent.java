package salve.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca una clase del núcleo para que el analizador la detecte. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CoreComponent { }
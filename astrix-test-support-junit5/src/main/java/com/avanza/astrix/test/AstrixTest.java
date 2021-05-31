package com.avanza.astrix.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(AstrixExtension.class)
public @interface AstrixTest {

    /**
     * TestApis
     */
    Class<? extends TestApi>[] value();

    /**
     * Reset TestApis after each test
     */
    boolean resetAfterEach() default true;

}

package com.example.emr_server.security.encryption;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {
    Mode mode() default Mode.RANDOM; // domyślnie losowe
    String value() default "";      // nazwa logiczna (jeśli pusta -> entityName.field)
    enum Mode { DETERMINISTIC, RANDOM }
}


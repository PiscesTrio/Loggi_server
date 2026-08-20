package com.example.api.annotation;

import com.example.api.model.enums.BusinessType;
import java.lang.annotation.*;

@Target(ElementType.METHOD) // target type
@Retention(RetentionPolicy.RUNTIME) // retention policy
@Documented
public @interface Log {
    /*
       module
    */
    String module() default "";

    /*
       operation type
    */
    BusinessType type();
}

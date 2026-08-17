package com.example.api.annotation;

import com.example.api.model.enums.BusincessType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)   //target type
@Retention(RetentionPolicy.RUNTIME)  //retention policy
@Documented
public @interface Log {
    /*
        module
     */
    String moudle() default "";

    /*
        operation type
     */
    BusincessType type();
}

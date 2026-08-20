package com.example.api.annotation;

import com.example.api.model.enums.BusinessType;
import com.example.api.model.enums.LogModule;
import java.lang.annotation.*;

@Target(ElementType.METHOD) // target type
@Retention(RetentionPolicy.RUNTIME) // retention policy
@Documented
public @interface Log {
    /*
       module

       No default. It used to be "", so an @Log that forgot to say which module it belonged
       to compiled and wrote a blank into the audit table. Every one of the twenty-five call
       sites names one; requiring it makes that a property of the annotation rather than a
       habit.
    */
    LogModule module();

    /*
       operation type
    */
    BusinessType type();
}

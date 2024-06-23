package com.example.travelDiary.common.permissions.aop;

import com.example.travelDiary.domain.IdentifiableResource;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(value = CheckAccesses.class)
public @interface CheckAccess {
    Class<? extends IdentifiableResource> resourceType();
    String spelResourceId();
    String permission();
    boolean isList() default false;
}




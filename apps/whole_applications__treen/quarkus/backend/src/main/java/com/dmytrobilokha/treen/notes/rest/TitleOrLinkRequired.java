package com.dmytrobilokha.treen.notes.rest;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ChangeNoteTitleLinkValidator.class})
public @interface TitleOrLinkRequired {

    String message() default "Title or link should be provided";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}

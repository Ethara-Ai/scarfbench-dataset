package com.dmytrobilokha.treen.notes.rest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChangeNoteTitleLinkValidator implements ConstraintValidator<TitleOrLinkRequired, ChangeNoteRequest> {

    @Override
    public void initialize(TitleOrLinkRequired constraintAnnotation) {
        //No action
    }

    @Override
    public boolean isValid(ChangeNoteRequest value, ConstraintValidatorContext context) {
        var title = value.getTitle();
        var link = value.getLink();
        return title != null && !title.isBlank()
                || link != null && !link.isBlank();
    }

}

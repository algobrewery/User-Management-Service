package com.userapi.converters;

public interface BiDirectionalConverter<I, O> {

    O doForward(I external);

    I doBackward(O internal);

}

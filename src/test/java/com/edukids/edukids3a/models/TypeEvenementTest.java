package com.edukids.edukids3a.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeEvenementTest {

    @Test
    void fromCode_atelier() {
        assertEquals(TypeEvenement.ATELIER, TypeEvenement.fromCode("atelier"));
    }

    @Test
    void fromCode_null() {
        assertNull(TypeEvenement.fromCode(null));
        assertNull(TypeEvenement.fromCode(" "));
    }
}

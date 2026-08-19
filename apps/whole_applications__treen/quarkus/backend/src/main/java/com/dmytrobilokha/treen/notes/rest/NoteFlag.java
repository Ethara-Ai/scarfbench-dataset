package com.dmytrobilokha.treen.notes.rest;

public enum NoteFlag {

    STRIKE(0),
    CHECK(1),
    CROSS(2);

    private final int bitPosition;

    NoteFlag(int bitPosition) {
        if (bitPosition > 63 || bitPosition < 0) {
            throw new IllegalArgumentException(
                    "Flags container is 64 bits, unable to have bit position " + bitPosition);
        }
        this.bitPosition = bitPosition;
    }

    public int getBitPosition() {
        return bitPosition;
    }

}

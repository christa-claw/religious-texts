package org.religioustext.app.util;

import com.github.f4b6a3.uuid.UuidCreator;

/**
 * Generates typed, time-ordered IDs in the format {prefix}-{uuidv7}.
 *
 * Examples:
 *   usr-018e7b2a-3f4c-7d8e-9a1b-2c3d4e5f6a7b  user
 *   cmt-018e7b2d-8a1c-7b3f-8d2e-4a5b6c7d8e9f  comment
 *   ref-018e7b30-d2f4-7a9b-bc3d-5e6f7a8b9c0d  comment reference
 *   nte-018e7b34-1b6a-7c8d-9e4f-6a7b8c9d0e1f  personal note
 *   blk-018e7b37-4e92-7d1e-af5a-7b8c9d0e1f2a  blocked domain
 *
 * Properties:
 *   - Type readable from first 3 characters
 *   - Time-ordered (UUID v7) — sortable by creation time
 *   - Globally unique and unguessable
 *   - Safe for use in URLs and APIs
 *   - Fixed length: 40 chars (fits CHAR(40))
 */
public final class TypedId {

    public enum Type {
        USER        ("usr"),
        COMMENT     ("cmt"),
        COMMENT_REF ("ref"),
        NOTE        ("nte"),
        BLOCKED     ("blk");

        private final String prefix;
        Type(final String prefix) { this.prefix = prefix; }
        public String prefix() { return prefix; }
    }

    /** Generates a new typed ID: {3-char prefix}-{uuid-v7} */
    public static String generate(final Type type) {
        return type.prefix() + "-" + UuidCreator.getTimeOrderedEpoch().toString();
    }

    /** Returns the type of an existing typed ID. */
    public static Type typeOf(final String id) {
        if (id == null || id.length() < 4)
            throw new IllegalArgumentException("Invalid typed ID: " + id);
        final String prefix = id.substring(0, 3);
        for (final Type t : Type.values()) {
            if (t.prefix().equals(prefix)) return t;
        }
        throw new IllegalArgumentException("Unknown ID prefix: " + prefix);
    }

    /** Validates that an ID belongs to the expected type. */
    public static void assertType(final String id, final Type expected) {
        final Type actual = typeOf(id);
        if (actual != expected)
            throw new IllegalArgumentException(
                "Expected " + expected.prefix() + "-* but got " + actual.prefix() + "-*");
    }

    private TypedId() {}
}

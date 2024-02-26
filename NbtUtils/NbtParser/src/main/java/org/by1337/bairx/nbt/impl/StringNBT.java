package org.by1337.bairx.nbt.impl;

import org.by1337.bairx.nbt.NBT;
import org.by1337.bairx.nbt.NbtType;

import java.util.Objects;

public class StringNBT extends NBT {
    private final String value;

    public StringNBT(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return quoteAndEscape(value);
    }

    @Override
    public NbtType getType() {
        return NbtType.STRING;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toStringBeautifier(int lvl) {
        return toString();
    }

    @Override
    public Object getAsObject() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringNBT stringNBT = (StringNBT) o;
        return Objects.equals(value, stringNBT.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
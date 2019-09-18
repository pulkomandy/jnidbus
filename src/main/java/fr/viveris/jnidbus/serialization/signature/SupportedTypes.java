/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.serialization.signature;

/**
 * List of the types supported by the library, primitive or container
 */
public enum SupportedTypes {
    STRING('s',String.class,String.class),
    INTEGER('i',Integer.TYPE,Integer.class),
    BOOLEAN('b',Boolean.TYPE,Boolean.class),
    BYTE('y',Byte.TYPE,Byte.class),
    SHORT('n',Short.TYPE,Short.class),
    LONG('x',Long.TYPE,Long.class),
    DOUBLE('d',Double.TYPE,Double.class),
    ARRAY('a',null,null),
    OBJECT_BEGIN('(',null,null),
    OBJECT_END(')',null,null);

    private char value;
    private Class primitiveType;
    private Class boxedType;

    SupportedTypes(char s,Class primitiveType, Class boxedType) {
        this.value = s;
        this.primitiveType = primitiveType;
        this.boxedType = boxedType;
    }

    public char getValue(){
        return this.value;
    }
    public Class getPrimitiveType(){ return this.primitiveType; }
    public Class getBoxedType(){ return this.boxedType; }


    /**
     * Get the enum entry for the given char
     *
     * @param c char to match
     * @return the enum entry for the given char
     */
    public static SupportedTypes forChar(char c){
        switch(c){
            case 's': return STRING;
            case 'i': return INTEGER;
            case 'b': return BOOLEAN;
            case 'y': return BYTE;
            case 'n': return SHORT;
            case 'x': return LONG;
            case 'd': return DOUBLE;
            case 'a': return ARRAY;
            case '(': return OBJECT_BEGIN;
            case ')': return OBJECT_END;
            default: throw new IllegalArgumentException("No supported type for the given char");
        }
    }
}

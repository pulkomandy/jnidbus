/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.serialization.signature;

import fr.viveris.jnidbus.exception.SignatureParsingException;

import java.util.Iterator;

/**
 * This class does the actual parsing of the signature string and transform it in proper SignatureElement.
 * The inner workings are simple, we have a position integer that tells us at which position we are, and each call to next()
 * will increment this position according to the type read (1 for primitive types, undetermined for container types).
 *
 * This iterator is immutable and the remove() method will throw if called
 */
public class SignatureIterator  implements Iterator<SignatureElement> {
    /**
     * Dbus signature string
     */
    private String signature;

    private char[] rawSignature;

    /**
     * Current position in the signature
     */
    private int position = 0;

    public SignatureIterator(String signature) {
        this.signature = signature;
        this.rawSignature = signature.toCharArray();
    }

    @Override
    public boolean hasNext() {
        return this.position < this.rawSignature.length;
    }

    @Override
    public SignatureElement next() {
        //get current char and increment
        SupportedTypes current = SupportedTypes.forChar(this.rawSignature[this.position++]);

        //if the current element has a primitive type, we have a single, non recursive element
        if(current.getPrimitiveType() != null){
            return new SignatureElement(String.valueOf(current.getValue()),current,null);
        }

        //else, we have a complex container type (array, object or dict_entry)
        switch (current){
            case ARRAY:
                //peek next element to check if the array contains a primitive type
                SupportedTypes firstArrayElement = SupportedTypes.forChar(this.rawSignature[this.position]);
                if(firstArrayElement.getPrimitiveType() != null){
                    this.position++;
                    return new SignatureElement(String.valueOf(firstArrayElement.getValue()),firstArrayElement,SupportedTypes.ARRAY);
                }else{
                    return new SignatureElement(this.generateArraySignature(),null,SupportedTypes.ARRAY);
                }
            case OBJECT_BEGIN:
                return new SignatureElement(this.generateStructSignature(),null,SupportedTypes.OBJECT_BEGIN);
            default: throw new IllegalStateException("Unknown non-primitive type:" + current);
        }
    }

    public void reset(){
        this.position = 0;
    }

    /**
     * Generate an array signature containing the signature of its content. This method supports nested arrays
     * (ex: aai)
     *
     * @return String of the array signature
     */
    private String generateArraySignature(){
        StringBuilder builder = new StringBuilder();
        SupportedTypes firstElement = SupportedTypes.forChar(this.rawSignature[this.position++]);

        //we reached a primitive element, return
        if(firstElement.getPrimitiveType() != null){
            builder.append(firstElement.getValue());
        }else{
            switch (firstElement){
                case ARRAY:
                    //append the array char
                    builder.append(firstElement.getValue());
                    //append the array signature
                    builder.append(this.generateArraySignature());
                    break;
                case OBJECT_BEGIN:
                    //append the object_begin char
                    builder.append(firstElement.getValue());
                    //generate the struct signature
                    builder.append(this.generateStructSignature());
                    //the last object_end char is ignored by generateStructSignature(), add it manually
                    builder.append(this.rawSignature[this.position-1]);
                    break;
            }

        }
        return builder.toString();
    }

    /**
     * Generate an object signature. This method supports nested object and will ignore the object delimiter signature
     * ex: (si) will become si and (s(i)) will become s(i)
     *
     * @return
     */
    private String generateStructSignature(){
        StringBuilder builder = new StringBuilder();
        int depth = 1;
        while(depth > 0){
            if(this.position == this.rawSignature.length) throw new SignatureParsingException("Malformed struct, perhaps there is a closing parenthesis missing");
            char current = this.rawSignature[this.position++];
            if(current == SupportedTypes.OBJECT_END.getValue()) depth--;
            if(current == SupportedTypes.OBJECT_BEGIN.getValue()) depth++;
            //allow us to ignore OBJECT_END token if it's the end of the parsing
            if(depth > 0){
                builder.append(current);
            }
        }
        return builder.toString();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("A signature iterator is immutable");
    }
}

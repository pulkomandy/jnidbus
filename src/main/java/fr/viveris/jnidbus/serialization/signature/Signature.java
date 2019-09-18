/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.serialization.signature;

import fr.viveris.jnidbus.cache.Cache;

import java.util.Iterator;

/**
 * The Signature object allows the developer to parse and explore DBus signature in a proper iterator-oriented way.
 */
public class Signature implements Iterable<SignatureElement>{
    private static final Cache<String,CachedSignatureIterator> CACHE = new Cache<>();
    /**
     * Dbus signature string
     */
    private String signature;

    public Signature(String signature) {
        this.signature = signature;
    }

    @Override
    public Iterator<SignatureElement> iterator() {
        CachedSignatureIterator iter = CACHE.getCachedEntity(this.signature);
        if(iter == null){
            iter = new CachedSignatureIterator(this.signature);
            CACHE.addCachedEntity(this.signature, iter);
        }
        return iter.iterator();
    }

    public String getSignature() {
        return signature;
    }

    /**
     * Method creating an iterator and returning the first element , this is useful when dealing with arrays as they only
     * contain one signature element (the array type)
     *
     * @return the first SignatureElement of this signature
     */
    public SignatureElement getFirst(){
        return this.iterator().next();
    }

    /**
     * Method creating an iterator and returning the second element, this is usefull when dealing with dict_en tries as they
     * only have two elements (the key and item).
     * @return
     */
    public SignatureElement getSecond(){
        Iterator<SignatureElement> iter = this.iterator();
        iter.next();
        return iter.next();
    }

    public static void clearCache(){
        CACHE.clear();
    }
}

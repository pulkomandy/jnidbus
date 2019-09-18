/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.serialization.signature.Signature;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SignatureParserTest {

    @Test
    public void complexSignatureTest(){
        String signature = "issaa(si(s))ias(si)";
        Object[] expectations = {
                SupportedTypes.INTEGER,
                SupportedTypes.STRING,
                SupportedTypes.STRING,
                SupportedTypes.ARRAY,
                new Object[]{
                        SupportedTypes.ARRAY,
                        new Object[]{
                                SupportedTypes.OBJECT_BEGIN,
                                new Object[]{
                                        SupportedTypes.STRING,
                                        SupportedTypes.INTEGER,
                                        SupportedTypes.OBJECT_BEGIN,
                                        new Object[]{
                                                SupportedTypes.STRING
                                        }
                                }
                        }
                },
                SupportedTypes.INTEGER,
                SupportedTypes.ARRAY,
                new Object[]{
                        SupportedTypes.STRING,
                },
                SupportedTypes.OBJECT_BEGIN,
                new Object[]{
                        SupportedTypes.STRING,
                        SupportedTypes.INTEGER,
                },
        };
        Signature object = new Signature(signature);
        int i = 0;
        for(SignatureElement ele : object){
            if(ele.isPrimitive()) assertTrue(ele.getPrimitive() == expectations[i++]);
            else{
                assertTrue(ele.getContainerType() == expectations[i++] );
                assertTrue(checkElement(ele,(Object[]) expectations[i++]));
            }
        }
    }

    private boolean checkElement(SignatureElement element, Object[] values){
        boolean result = true;
        Signature subSignature = element.getSignature();
        int i = 0;
        for(SignatureElement sub : subSignature){
            if(sub.isPrimitive()) assertTrue(sub.getPrimitive() == values[i++]);
            else{
                assertTrue(sub.getContainerType() == values[i++] );
                assertTrue(checkElement(sub,(Object[]) values[i++]));
            }
        }
        return result;
    }
}

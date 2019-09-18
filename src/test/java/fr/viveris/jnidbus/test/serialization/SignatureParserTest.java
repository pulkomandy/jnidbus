/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.exception.SignatureParsingException;
import fr.viveris.jnidbus.serialization.signature.Signature;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SignatureParserTest {

    @Test
    public void allPrimitivesTest(){
        Object[] expectations = {
                SupportedTypes.INTEGER,
                SupportedTypes.SHORT,
                SupportedTypes.STRING,
                SupportedTypes.LONG,
                SupportedTypes.BOOLEAN,
                SupportedTypes.BYTE,
                SupportedTypes.DOUBLE,
        };
        assertTrue(this.checkSignature("insxbyd",expectations));
    }

    @Test
    public void primitiveMessageTest(){
        Object[] expectations = {
                SupportedTypes.INTEGER,
                SupportedTypes.INTEGER,
                SupportedTypes.STRING,
                SupportedTypes.INTEGER,
        };
        assertTrue(this.checkSignature("iisi",expectations));
    }

    @Test
    public void primitiveArrayTest(){
        Object[] expectations = {
                SupportedTypes.ARRAY,
                new Object[]{
                        SupportedTypes.INTEGER,
                }
        };
        assertTrue(this.checkSignature("ai",expectations));
    }

    @Test
    public void nestedObjectArrayTest(){
        Object[] expectations = new Object[]{
                SupportedTypes.ARRAY,
                new Object[]{
                        SupportedTypes.ARRAY,
                        new Object[]{
                                SupportedTypes.OBJECT_BEGIN,
                                new Object[]{
                                        SupportedTypes.STRING,
                                        SupportedTypes.INTEGER
                                }
                        }
                }
        };
        assertTrue(this.checkSignature("aa(si)",expectations));
    }

    @Test
    public void objectTest(){
        Object[] expectations = new Object[]{
                SupportedTypes.OBJECT_BEGIN,
                new Object[]{
                        SupportedTypes.STRING,
                        SupportedTypes.INTEGER
                }
        };
        assertTrue(this.checkSignature("(si)",expectations));
    }

    @Test
    public void nestedObjectTest(){
        Object[] expectations = new Object[]{
                SupportedTypes.OBJECT_BEGIN,
                new Object[]{
                        SupportedTypes.STRING,
                        SupportedTypes.OBJECT_BEGIN,
                        new Object[]{
                                SupportedTypes.STRING,
                                SupportedTypes.INTEGER
                        }
                }
        };
        assertTrue(this.checkSignature("(s(si))",expectations));
    }

    @Test
    public void complexSignatureTest(){
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
        assertTrue(this.checkSignature("issaa(si(s))ias(si)",expectations));
    }

    @Test(expected = SignatureParsingException.class)
    public void malformedSignatureTest(){
        Object[] expectations = new Object[]{
                SupportedTypes.OBJECT_BEGIN,
                new Object[]{
                        SupportedTypes.STRING,
                        SupportedTypes.OBJECT_BEGIN,
                        new Object[]{
                                SupportedTypes.STRING,
                                SupportedTypes.INTEGER
                        }
                }
        };
        assertTrue(this.checkSignature("(s(si)",expectations));
    }

    private boolean checkSignature(String signatureString, Object[] expectations){
        int i = 0;
        for(SignatureElement ele : new Signature(signatureString)){
            if(ele.isPrimitive()) assertSame(ele.getPrimitive(), expectations[i++]);
            else{
                assertSame(ele.getContainerType(),expectations[i++] );
                assertTrue(checkElement(ele,(Object[]) expectations[i++]));
            }
        }
        return true;
    }

    private boolean checkElement(SignatureElement element, Object[] values){
        boolean result = true;
        Signature subSignature = element.getSignature();
        int i = 0;
        for(SignatureElement sub : subSignature){
            if(sub.isPrimitive()) assertSame(sub.getPrimitive(),values[i++]);
            else{
                assertSame(sub.getContainerType(),values[i++] );
                assertTrue(checkElement(sub,(Object[]) values[i++]));
            }
        }
        return result;
    }
}

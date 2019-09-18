/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common;

import fr.viveris.jnidbus.BusType;
import fr.viveris.jnidbus.Dbus;
import fr.viveris.jnidbus.exception.ConnectionException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public abstract class DBusTestCase {
    private static final Logger LOG = LoggerFactory.getLogger(DBusTestCase.class);
    protected Dbus sender;
    protected Dbus receiver;

    protected String senderBusName;
    protected String receiverBusName;

    /**
     * We have to setup a new connection for each test for nice stateless tests, in addition we must make the bus name
     * unique as DBus needs a few millis to free a bus name, meaning each test would be delayed, which is not acceptable
     */
    @Before
    public void setup() throws ConnectionException {
        LOG.info("----------- Start Test Case -----------");
        this.senderBusName = "fr.viveris.vizada.jnidbus.test.sender."+generateRandomString();
        this.receiverBusName = "fr.viveris.vizada.jnidbus.test.receiver."+generateRandomString();
        this.sender = new Dbus(BusType.SESSION,this.senderBusName);
        this.receiver = new Dbus(BusType.SESSION,this.receiverBusName);
    }

    @After
    public void dispose() {
        try{
            this.sender.close();
            this.receiver.close();
            LOG.info("----------- End Test Case -----------");
        }catch (Exception e){ }
    }

    /**
     * Generate a random string containing any letter between "a" and "z"
     * size fixed to 10 chars
     * @return
     */
    public static String generateRandomString(){
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }
}

package fr.viveris.jnidbus.test.promise;

import fr.viveris.jnidbus.message.Promise;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PromiseTest {

    @Test
    public void awaitAllTest() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);

        final Promise<Boolean>[] promises = new Promise[]{
                new Promise<Boolean>(),
                new Promise<Boolean>(),
                new Promise<Boolean>(),
                new Promise<Boolean>()
        };

        for(final Promise<Boolean> p : promises){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        p.resolve(true);
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
        }

        Promise.awaitAll(promises).then(new Promise.Callback<Promise[]>() {
            @Override
            public void value(Promise[] value, Exception e) {
                assertSame(promises,value);
                barrier.countDown();
            }
        });

        assertTrue(barrier.await(1, TimeUnit.SECONDS));

    }
}

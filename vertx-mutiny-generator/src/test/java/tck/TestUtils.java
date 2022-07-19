package tck;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestUtils {

    public static <T> void subscribe(Publisher<T> obs, TestSubscriber<T> sub) {
        obs.subscribe(new Subscriber<T>() {
            boolean unsubscribed;

            @Override
            public void onSubscribe(Flow.Subscription s) {
                sub.onSubscribe(new TestSubscriber.Subscription() {
                    @Override
                    public void fetch(long val) {
                        if (val > 0) {
                            s.request(val);
                        }
                    }

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                        s.cancel();
                    }

                    @Override
                    public boolean isUnsubscribed() {
                        return unsubscribed;
                    }
                });

            }

            @Override
            public void onNext(T buffer) {
                sub.onNext(buffer);
            }

            @Override
            public void onError(Throwable t) {
                unsubscribed = true;
                sub.onError(t);
            }

            @Override
            public void onComplete() {
                unsubscribed = true;
                sub.onCompleted();
            }
        });
    }
}

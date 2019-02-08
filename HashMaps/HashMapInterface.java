package HashMaps;

import java.util.concurrent.locks.Lock;

public interface HashMapInterface<T> {


    public boolean add(T x);

    public boolean remove(T x);

    public boolean contains(T x);
}

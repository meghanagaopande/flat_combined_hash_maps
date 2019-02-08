package HashMaps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FCBaseHashSetStriped<T> {
    public List<T>[] table;
    public int size;
    AtomicBoolean tableResizing;
    ThreadLocal<Boolean> tableResize;

    public FCBaseHashSetStriped(int capacity) {
        //size = 0;
        size = 32;
        table = (List<T>[]) new List[capacity];
        for (int i = 0; i < capacity; i++) {
            table[i] = new ArrayList<T>();
        }

        tableResizing = new AtomicBoolean(false);

        tableResize = new ThreadLocal(){
            protected Boolean initialValue(){
                return false;
            }
        };
    }

    public boolean contains(T x) {

        try {
            int myBucket = Math.abs(x.hashCode() % table.length);
            return table[myBucket].contains(x);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean add(T x) {
        boolean result = false;
        try {
            int myBucket = Math.abs(x.hashCode() % table.length);
            result = table[myBucket].add(x);
            size = result ? size + 1 : size;
        }
        catch (Exception e)
        {

        }
        if (policy())
        {
            if(tableResizing.compareAndSet(false, true))
            {
              Boolean r = tableResize.get();
              r = true;
            }
            tableResizing.set(false);
            //resize();
        }
        return result;
    }

    public boolean remove(T x) {

        try {
            int myBucket = Math.abs(x.hashCode() % table.length);
            boolean result = table[myBucket].remove(x);
            size = result ? size - 1 : size;
            return result;
        }
        catch (Exception e)
        {
            return false;
        }
    }



    public abstract void resize();

    public abstract boolean policy();

}

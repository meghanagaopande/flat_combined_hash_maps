package HashMaps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Threads.*;
import java.util.Timer;

public class FlatCombinedHashSetStriped<T> extends FCBaseHashSet<T> implements HashMapInterface<T>
{
    final ReentrantLock[] lock;
    ThreadLocal<Request> request;
    //    String add = "add";
//    String remove = "remove";
//    String contains = "contains";
//    final hashLock lock;
//    ArrayList<ThreadLocal<Record> > list;
//    ThreadLocal<Record> myRecord;
    int pass_count;
    int num_threads;
    volatile Request head;
    int FREQUENCY = 100;
    int MAX_ROUNDS;

    final private static AtomicReferenceFieldUpdater list_head_updater =
            AtomicReferenceFieldUpdater.newUpdater(FlatCombinedHashSetStriped.class, Request.class, "head");

    public FlatCombinedHashSetStriped(int capacity, int number_of_threads) {
        super(capacity);
        num_threads = number_of_threads;
        //lock  = new hashLock();
        request = new ThreadLocal<Request>() {
            protected Request<T> initialValue() {
                return new Request<T>();
            }

        };
        //list = new ArrayList<ThreadLocal<Record> >(number_of_threads);
//        list = new ArrayList(number_of_threads);
//
//        for(int i = 0; i < number_of_threads; i++)
//        {
//            list.add(null);
//        }
        pass_count = 0;
        lock = new ReentrantLock[capacity];
        for(int i = 0; i < lock.length; i++){
            lock[i] = new ReentrantLock();
        }
        MAX_ROUNDS = 1;
    }

    private void link_in_combining(Request R)
    {
        while (true)
        {
            // snapshot the list head
            Request cur_head = head;
            R.next = cur_head;

            // try to insert the node
            if (head == cur_head)
            {
                if (list_head_updater.compareAndSet(this, R.next, R))
                {
                    return;
                }
            }
        }
    }

    public boolean add(T x)
    {
        Request R = (Request)request.get();
        int myBucket = Math.abs(x.hashCode() % super.table.length);
        R.opcode = 1;
        R.value = x;
        R.bucket = myBucket;

        R.completed = false;

        if(R.active == false)
        {
            R.active = true;
            link_in_combining(R);
        }

//
        return processRecord(myBucket);
    }

    public boolean remove(T x)
    {
      Request R = (Request)request.get();
      int myBucket = Math.abs(x.hashCode() % super.table.length);
      R.bucket = myBucket;
      R.opcode = 2;
      R.value = x;

      R.completed = false;

      if(R.active == false)
      {
          R.active = true;
          link_in_combining(R);
      }


      return processRecord(myBucket);

    }

    public boolean contains(T x)
    {
      Request R = (Request)request.get();
      int myBucket = Math.abs(x.hashCode() % super.table.length);
      R.opcode = 3;
      R.value = x;
      R.bucket = myBucket;

      R.completed = false;

      if(R.active == false)
      {
          R.active = true;
          link_in_combining(R);
      }

      return processRecord(myBucket);
    }

    public boolean processRecord(int bucket)
    {
        Request R = (Request)request.get();
        int count = 0;
        while (count < 100000000) {
            if(count % 100 == 0) {
                //if(!fc_lock.get())
                if(!lock[bucket].isLocked())
                {
                  if(lock[bucket].tryLock())
                  //if(fc_lock.compareAndSet(false, true))
                  {
                      scanCombineApply(bucket);
                      //fc_lock.set(false);
                      lock[bucket].unlock();
                  }
                }
            }


            if(count % 1000 == 0)
            {
              if (!request.get().active)
              {
                  request.get().active = true;
                  link_in_combining(request.get());
              }
            }

            if(R.completed)
            {
                //If completed, let the completed field remain true

                return R.response;
            }

            count++;

        }
        return false;
    }

    public void scanCombineApply(int bucket) {
        pass_count++;
        int rounds = 0;
        T v;

        while (rounds < MAX_ROUNDS) {
            Request curr = head;
            Request prev = head;
            Request nextRec;

            boolean turn = (pass_count % FREQUENCY == 0);

            while (curr != null) {
                if (curr.completed) {
                    nextRec = curr.next;
                    if (turn && (curr != head) && (pass_count - curr.age > 10000)) {
                        curr.active = false;
                        prev.next = nextRec;
                    }
                    curr = nextRec;
                    continue;
                }

                curr.age = pass_count;

                if(curr.bucket == bucket)
                {
                    v = (T) curr.value;

                    if (curr.opcode == 1) {
                        curr.response = super.add(v);


                    } else if (curr.opcode == 2) {
                        curr.response = super.remove(v);

                    } else if (curr.opcode == 3) {
                        curr.response = super.contains(v);

                    }

                    curr.completed = true;
                }
                curr = curr.next;
            }
            rounds++;
        }
    }



    public void resize() {
        int oldCapacity = table.length;

        if (oldCapacity != table.length) {
            return; // someone beat us to it
        }
        int newCapacity  = 2 * oldCapacity;
        List<T>[] oldTable = table;
        table = (List<T>[]) new List[newCapacity];
        for (int i = 0; i < newCapacity; i++)
            table[i] = new ArrayList<T>();
        for (List<T> bucket : oldTable) {
            for (T x : bucket) {
                int myBucket = Math.abs(x.hashCode() % table.length);
                table[myBucket].add(x);
            }
        }

    }

    public boolean policy() {
        return size / table.length > 4;
    }

    static class Request<T> {
        int opcode = 0;
        T value;
        boolean response;
        volatile boolean completed = true;
        volatile boolean active = false;
        Request next = null;
        int age = 0;
        int bucket;
    }
}

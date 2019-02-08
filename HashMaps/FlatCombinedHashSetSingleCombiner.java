package HashMaps;

import com.sun.prism.impl.Disposer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import Threads.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FlatCombinedHashSetSingleCombiner<T> extends FCBaseHashSet<T> implements HashMapInterface<T>
{
    final ReentrantLock lock;

    //AtomicBoolean fc_lock;
    ThreadLocal<Request> myRequest;
    int pass_count;

    int threads_num;

    volatile Request list_head;
    int FREQUENCY = 1000;
    int MAX_ROUNDS;
    // For compareAndSet on the _req_list_head
    final private static AtomicReferenceFieldUpdater list_head_updater =
            AtomicReferenceFieldUpdater.newUpdater(FlatCombinedHashSetSingleCombiner.class, Request.class, "list_head");

    public FlatCombinedHashSetSingleCombiner(int capacity, int number_of_threads) {
        super(capacity);
        //fc_lock  = new AtomicBoolean(false);
        threads_num = number_of_threads;

        myRequest = new ThreadLocal<Request>(){
          protected Request<T> initialValue(){
            return new Request<T>();
          }
        };

        pass_count = 0;
        lock  = new ReentrantLock();
        //MAX_ROUNDS = threads_num * threads_num * threads_num ;
        MAX_ROUNDS = 100;
        //MAX_ROUNDS = 32;
    }

    private void link_in_combining(Request R)
    {
        while (true)
        {
            // snapshot the list head
            Request cur_head = list_head;
            R.next = cur_head;

            // try to insert the node
            if (list_head == cur_head)
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
        Request R = (Request)myRequest.get();
        R.opcode = 1;
        R.value = x;

        R.completed = false;

        if(R.active == false)
        {
            R.active = true;
            link_in_combining(R);
        }

        return processRecord();
    }

    public boolean remove(T x)
    {
      Request R = (Request)myRequest.get();
      R.opcode = 2;
      R.value = x;

      R.completed = false;

      if(R.active == false)
      {
          R.active = true;
          link_in_combining(R);
      }
        return processRecord();

    }

    public boolean contains(T x)
    {
      Request R = (Request)myRequest.get();
      R.opcode = 3;
      R.value = x;

      R.completed = false;

      if(R.active == false)
      {
          R.active = true;
          link_in_combining(R);
      }

      return processRecord();
    }

    public boolean processRecord()
    {

        Request R = (Request)myRequest.get();

        int count = 0;

        while (count < 100000000) {

            if(count % 100 == 0) {

                //if(!fc_lock.get())
                if(!lock.isLocked())
                {
                  if(lock.tryLock())
                  //if(fc_lock.compareAndSet(false, true))
                  {
                      scanCombineApply();
                      //fc_lock.set(false);
                      lock.unlock();
                  }
                }
            }


            if(count % 1000 == 0)
            {
              if (!myRequest.get().active)
              {
                  myRequest.get().active = true;
                  link_in_combining(myRequest.get());
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

    public void scanCombineApply()
    {
      pass_count++;

      int rounds = 0;


      T v;
      while(rounds < MAX_ROUNDS)
      {
          Request curr = list_head;
          Request prev = list_head;
          Request nextRec;

          boolean turn = (pass_count % FREQUENCY == 0);

          while(curr != null)
          {
              if(curr.completed)
              {
                  nextRec = curr.next;
                  if (turn && (curr != list_head) && (pass_count - curr.age > 10000))
                  {

                      curr.active = false;
                      prev.next = nextRec;
                  }
                  curr = nextRec;
                  continue;
              }



              curr.age = pass_count;

              v = (T) curr.value;

              if (curr.opcode == 1) {
                  curr.response = super.add(v);


              } else if (curr.opcode == 2) {
                  curr.response = super.remove(v);

              } else if (curr.opcode == 3) {
                  curr.response = super.contains(v);

              }

              curr.completed = true;
              curr = curr.next;
        }
        turn = false;
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
    }
}

# flat_combined_hash_maps

References:
1. Flat combining and the synchronization-parallelism tradeoff, D. Hendler, I. Incze, N.
Shavit, and M. Tzafrir, Proceedings of the 22nd ACM symposium on Parallelism in
algorithms and architectures (SPAA 2010), pages 355—364, 2010,
http://mcg.cs.tau.ac.il/papers/spaa2010-fc.pdf
2. FC Java queue : http://mcg.cs.tau.ac.il/projects/projects/flat-combining
3. http://booksite.elsevier.com/9780123973375/ Chapter 13 example programs

Flat combining is a synchronization technique where a combiner thread acquires a global lock
on a structure of thread local requests, scans through these concurrent requests, and then
performs these requests of all threads, writing back responses as it does so.
The benefits of this technique are:
1. Reduced synchronization overhead
2. Reduced cache invalidation traffic
It has been shown that on certain data structures, flat combining outperforms state of the art
parallelization techniques. This is illustrated for queues, stacks, priority queues by D. Hendler et
al.The authors state that an interesting line of research is the use of multiple parallel instances of
flat combining to speed up concurrent structures such as hash maps.

In this project, I implemented 4 approaches to flat combining in hash maps:
1. Single flat combiner: Flat combining applied to Coarse grain locking
2. Multiple, static flat combiners: Flat combining applied to striped hashmaps (Number of combiner threads do not increase with resizing)
3. Multiple, dynamic flat combiners: Flat combining applied to Refined locking
4. Multiple, dynamic flat combiners: Flat combining applied to refined locking with
incremental resize as lock free hash maps

Use case:
java HashMapTest <Type_of_hash_map> <number_of_threads> <duration> <capacity> <fraction_of_contains>

1. fraction_of_contains should be between 0 and 1.
2. duration should be in seconds.
3. capacity is the capacity of the hashmap. This is the maximum it will resize to.
4. Type_of_hash_map can be one of the following:
CoarseHashSet
StripedHashSet
LockFreeHashSet
FlatCombinedHashSetSingleCombiner
FlatCombinedHashSetRefinable
FlatCombinedHashSetStriped
FlatCombinedHashSetLockfree

The output will be number of operations/second.

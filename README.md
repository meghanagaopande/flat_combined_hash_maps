# flat_combined_hash_maps

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

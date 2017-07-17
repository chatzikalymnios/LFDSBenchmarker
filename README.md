# LFDSBenchmarker

The LFDSBenchmarker project contains a set of lock-free data structure implementations and
a basic infrastructure to test and compare them.

## Data structures

### Stacks

                                                Name                                                   |      Reference
-------------------------------------------------------------------------------------------------------|--------------------
[Elimination-Backoff Stack](src/main/java/io/github/chatzikalymnios/lfds/EliminationBackoffStack.java) | [[1]](#Hendler2004)

## References

1. <a href="Hendler2004"></a>Danny Hendler, Nir Shavit, and Lena Yerushalmi. 2004. A scalable lock-free stack algorithm. In
Proceedings of the sixteenth annual ACM symposium on Parallelism in algorithms and architectures (SPAA '04). ACM, New York,
NY, USA, 206-215.

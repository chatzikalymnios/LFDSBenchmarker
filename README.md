# LFDSBenchmarker

The LFDSBenchmarker project contains a set of lock-free data structure implementations and
a basic infrastructure to test and compare them.

## Data structures

### Stacks

| Name | Reference |
|------|-----------|
| [Elimination-Backoff Stack](src/main/java/io/github/chatzikalymnios/lfds/EliminationBackoffStack.java) | [[1]](#Hendler2004) |

### Queues

| Name | Reference |
|------|-----------|
| [Michael-Scott Queue](src/main/java/io/github/chatzikalymnios/lfds/MichaelScottQueue.java) | [[2]](#Michael1996) |

## Usage
In order to use the tool, clone this reposiroty and run the following.

```sh
$ cd LFDSBenchmarker
$ ./gradlew run -Pargs="options"                        # compile and run with provided options
```

Alternatively, you can build a distribution with Gradle and run it on its own.

```sh
$ cd LFDSBenchmarker
$ ./gradlew build                                       # compiles and builds the distributions

# the distributions are available under ./build/distributions
```

##### Options
The available options are:

```sh
 -d <datastructure>   # data structure to benchmark [EBStack, MSQueue]

 -h                   # print this message
 
 -i <num>             # number of items to insert and remove
 
 -s <nanoseconds>     # amount of time to wait for elimination partner in
                      # nanoseconds (applicable to EBStack) [default: 100000]
 
 -t <num>             # number of threads to use
 
 -w <microseconds>    # concurrent workload in microseconds
```

## References

1. <a href="Hendler2004"></a>Danny Hendler, Nir Shavit, and Lena Yerushalmi. 2004. A scalable lock-free stack algorithm. In
Proceedings of the sixteenth annual ACM symposium on Parallelism in algorithms and architectures (SPAA '04). ACM, New York,
NY, USA, 206-215.

2. <a href="Michael1996"></a>Maged M. Michael and Michael L. Scott. 1996. Simple, fast, and practical non-blocking and
blocking concurrent queue algorithms. In Proceedings of the fifteenth annual ACM symposium on Principles of distributed
computing (PODC '96). ACM, New York, NY, USA, 267-275.

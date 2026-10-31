# FSStatLib — Java Virtual Threads

An asynchronous filesystem statistics library built with **Java Virtual Threads**.  

---

## Overview

`FSStatLib` traverses a target directory hierarchy to compute aggregated statistics and file size distributions. The implementation leverages Java's **thread-per-task** concurrency paradigm with virtual threads, enabling high concurrency without the overhead typically associated with native operating system threads.

---

## Problem Analysis

Traversing a file system and reading metadata is an inherently **I/O-bound** workload:

- **Blocking I/O:** Standard filesystem operations (such as `Files.isDirectory` and `Files.size`) block the executing thread while awaiting completion from the underlying operating system.
- **OS Thread Limits:** In classical Java concurrency, assigning one OS (platform) thread per directory traversal rapidly exhausts system resources. Platform threads incur heavy memory footprints (1 MB+ per stack) and severe context-switching overhead under thousands of concurrent workers.
- **Virtual Thread Efficiency:** Virtual threads decouple the Java thread abstraction from underlying OS carrier threads. When a virtual thread encounters a blocking I/O call, the JVM automatically **unmounts** it, freeing the carrier thread to execute other virtual threads. This enables thousands of concurrent directory traversals without saturating system resources.

---

## Architecture & Design

The library adopts a **recursive fork-join pattern** orchestrated by a virtual thread executor.

```
                              getFSReport(...)
                                     │
                        (Root Virtual Thread / FutureTask)
                                     │
                   ┌─────────────────┴─────────────────┐
                   ▼                                   ▼
        computeDirectory(dir_A)             computeDirectory(dir_B)
         [Virtual Thread Worker]             [Virtual Thread Worker]
                   │                                   │
         ┌─────────┴─────────┐                         ▼
         ▼                   ▼                   Local FSReport
   Process Files    computeDirectory(dir_A1)
  (Local FSReport)   [Virtual Thread Worker]
         │                   │
         └─────────┬─────────┘
                   ▼
          FSReport.merge(...)
```

### 1. Thread-per-Task Execution
An `ExecutorService` is initialized with `Executors.newVirtualThreadPerTaskExecutor()` for the task's lifecycle. For every subdirectory encountered during recursive scanning, a new asynchronous subtask is dispatched to the executor. Directory branches are discovered and analyzed concurrently.

### 2. Lock-Free Aggregation
Shared mutable state and explicit locking mechanisms (such as `synchronized` blocks or explicit locks) are deliberately avoided:
- Each directory processing task instantiates its own isolated `FSReport`, initialized with an empty size distribution histogram and local file counters.
- Regular files within the directory are processed sequentially by the task, modifying only local state.
- Subdirectory tasks return a `Future<FSReport>`. The parent task waits for completion using `task.get()` and calls `FSReport.merge()` to combine the resulting records.
- This recursive fork-join structure guarantees complete thread safety without contention or lock overhead, building the overall report bottom-up.

### 3. Asynchronous Non-Blocking Entry Point
To avoid blocking the calling thread while coordinating tree exploration, the root task is encapsulated inside a `FutureTask` and initiated on a virtual thread:

```java
public static Future<FSReport> getFSReport(String d, long maxFS, int nb) {
    FutureTask<FSReport> rootTask = new FutureTask<>(() -> {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return computeDirectory(Path.of(d), maxFS, nb, executor);
        } // The executor implicitly awaits completion of all subtasks before closing
    });
    Thread.ofVirtual().start(rootTask);
    return rootTask;
}
```

The caller immediately receives the `FutureTask<FSReport>` handle to monitor or await completion asynchronously.

---

## Project Structure

```text
.
├── README.md
├── .gitignore
└── src/
    └── virtualthread/
        ├── Test.java                 # Entry point and test harness
        ├── lib/
        │   ├── FSReport.java         # Statistics & histogram model with merge()
        │   └── FSStatLib.java        # Core library with virtual thread executor
        └── testFolder/               # Sample nested directory structure for testing
```

---

## Getting Started

### Prerequisites
- **Java Development Kit (JDK) 21 or higher** (requires Virtual Threads / Project Loom support).

### Compilation & Execution

Compile the source files:
```bash
javac -d out src/virtualthread/lib/*.java src/virtualthread/Test.java
```

Run the test suite:
```bash
java -cp out virtualthread.Test
```
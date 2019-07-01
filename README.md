# cLARVA
Residual analysis for properties as symbolic automata

cLARVA (= [clara](https://github.com/Sable/clara) + [LARVA](http://www.cs.um.edu.mt/svrg/Tools/LARVA/)) is a tool aimed to reduce some of the overheads associated with runtime verifiying Java programs with *Dynamic Automata with Events and Timers* (DATEs), and variants thereof. The tool attempts to find parts of the program that can be ignored soundly for verification, and to remove transitions in a DATE that are not needed to identify a violation at runtime. [Soot](https://github.com/Sable/soot), a Java bytecode analyser, is used to support this analysis.

**Limitations**:

1. Only properties about sequential parts of the program will be analysed soundly. You can still analyse programs with threads, but the results will only apply intra-thread, and no guarantees are given for any interleaving behaviour.
2. Only LARVA scripts with one DATE will be processed.
3. Only control-flow static analysis is used, ignoring any data-oriented concerns of a DATE.
4. Tool is still under development. It has been manually verified to give correct results under some case studies, however this does not ensure that other bugs are not present.

**Input**: 
1. *mode* of the analysis --
    (a) *fast*: uses a Class Hierarchy Analysis (CHA) to create a more or less sound callgraph, while ignoring java.*, jdk.*, and sun.* packages, giving a quick (should take seconds) analysis; and
    (b) *intensive* uses [SPARK](https://link.springer.com/content/pdf/10.1007%2F3-540-36579-6_12.pdf), creating a more precise callgraph and pointer-analysis by considering also library files, leading to a longer processing time (should take some minutes);
2. *filePath to* DATE property --- an automata specifying the violating traces of the previous program (through AspectJ notions) with transitions over events (method calls) and conditions encoding data state (see [Larva Manual](http://www.cs.um.edu.mt/svrg/Tools/LARVA/LARVA-manual.pdf) for the syntax of DATEs).
3. *root directory of* compiled Java program (in eclipse, the bin directory); and
4. *Canonical name* of the Main class of program (of the form \<package-name\>.\<class-name\>).

Example call: **-jar clarva.jar fast java "./date.lrv" "./bin" main.Main**

**Output**: 
1. A DATE that is a pruned version of Input (2), with which it is enough to monitor Input (3).
2. Class files that are optimally instrumented with respect to the DATE.

Note that instrumentation is optimised by creating a new class with methods for each method call to be instrumented, and by instrumented the bytecode of the original Java program by invoking these new methods for events that have been classed as needed for sound and complete runtime verification. The DATE produced then is transformed to match events occuring in the new class, instead of in the whole program.

The output class files should then be weaved with the output DATE using the LARVA compiler.

**Tips**

1. One may wish to analyse a program with multiple entry-points. A way to do this would be to create a mock main class that can access each entry-point in a random manner, and use this as the main class for the analysis.

See technical report CS-2017-01 in this [series](https://www.um.edu.mt/ict/cs/research/technical_reports) for a detailed version of the theory behind this tool, which will be published in the proceedings of [PrePost 2017](http://staff.um.edu.mt/afra1/prepost17/), in the [EPCTS series](http://eptcs.web.cse.unsw.edu.au/content.cgi?PrePost17), and available on [arXiv](https://arxiv.org/abs/1708.07230).

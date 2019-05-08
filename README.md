# cLARVA
Residual analysis for properties as symbolic automata

cLARVA (= [clara](https://github.com/Sable/clara) + [LARVA](http://www.cs.um.edu.mt/svrg/Tools/LARVA/)) is a tool aimed to reduce some of the overheads associated with runtime verifiying Java programs with *Dynamic Automata with Events and Timers* (DATEs), and variants thereof. The tool attempts to find parts of the program that can be ignored soundy for verification, and to remove transitions in a DATE that can never match a program execution step at runtime.  [Soot](https://github.com/Sable/soot), a Java bytecode analyser, is used to support this analysis.

**Limitations**:

1. Only properties about sequential parts of the program will be analysed soundly. You can still analyse programs with threads, but the results will only apply intra-thread, and no guarantees are given for any interleaving behaviour.
2. Only LARVA scripts with one DATE will be processed.
3. The pointer-analysis employed is SPARK, as provided by Soot, and is configured to ignore Java libraries (to reduce running time). Therefore the soundness of the analysis is limited to properties about the application where the control-flow within calls to Java libraries is irrelevant to the property and to the creation of sound abstraction of the program.
4. Tool is still under development. It has been manually verified to give correct results under some case studies, however this does not ensure that other bugs are not present.

**Input**: 
1. *language name* of program to be analysed -- currently only *java* is supported;
2. *filePath to* DATE property --- an automata specifying the violating traces of the previous program (through AspectJ notions) with transitions over events (method calls) and conditions encoding data state (see [Larva Manual](http://www.cs.um.edu.mt/svrg/Tools/LARVA/LARVA-manual.pdf) for the syntax of DATEs).
3. *root directory of* compiled Java program (in eclipse, the bin directory); 
4. *Canonical name* of the Main class of program (of the form \<package-name\>.\<class-name\>); and
4. *Entry-point methods* of the program other then the main method in the main class (e.g. a list of API methods), note that if your program uses threads then you must include your *run* method here.

Example call: **java -jar clarva.jar java "./date.lrv" "./bin" main.Main server.threads.UserThread.run server.threads.AdminThread.run**
      
**Output**: 
1. A DATE that is a pruned version of Input (2), with which it is enough to monitor Input (3).
2. Class files that are optimally instrumented with respect to the DATE.

Note that instrumentation is optimised by creating a new class with methods for each method call to be instrumented, and by instrumented the bytecode of the original Java program by invoking these new methods for events that have been classed as needed for sound and complete runtime verification. The DATE produced then is transformed to match events occuring in the new class, instead of in the whole program.

See technical report CS-2017-01 in this [series](https://www.um.edu.mt/ict/cs/research/technical_reports) for a detailed version of the theory behind this tool, which will be published in the proceedings of [PrePost 2017](http://staff.um.edu.mt/afra1/prepost17/), in the [EPCTS series](http://eptcs.web.cse.unsw.edu.au/content.cgi?PrePost17), and available on [arXiv](https://arxiv.org/abs/1708.07230).

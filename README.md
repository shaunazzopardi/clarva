# cLARVA
Residual analysis for properties as symbolic automata

cLARVA (= [clara](https://github.com/Sable/clara) + [LARVA](http://www.cs.um.edu.mt/svrg/Tools/LARVA/)) is a tool aimed to reduce some of the overheads associated with runtime verification.

**Input**: 
1. (filePath to) DATE property --- an automata specifying the violating traces of the previous program (through AspectJ notions) with transitions over events (method calls) and conditions encoding data state.
2. (root directory of) Java program; and
3. (name of) Main class of program;
      
**Output**: A DATE that is a pruned version of Input (1), with which it is enough to monitor Input (2).

See this report CS-2017-01 in this [series](https://www.um.edu.mt/ict/cs/research/technical_reports) for a detailed version of the theory behind this tool, which will be published in the proceedings of [PrePost 2017](http://staff.um.edu.mt/afra1/prepost17/), in the EPCTS series.

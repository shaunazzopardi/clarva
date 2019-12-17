# modelsward-2020 

See ''pre-print.pdf'' for high-level details of experiment.

## Experiment Set-up
1. **Problem**: 
    1. A transaction system (''source-files\system'' or ''compiled-files\vanilla.jar'') must satisfy a specification (''source-files\specifications\original.lrv''), which we want to check using runtime verification.
    2. Larva instruments the system with the specification to produce ''compiled-files\monitored.jar''.
    3. Running Clarva reduces the specification to (''source-files\specifications\residual.lrv''), and resulting in the system in ''=compiled-files\residual.jar''.
    4. Does Clarva give any benefits? How does ''compiled-files\residual.jar'' compare to the other jar files given the same input?
2. We separate the process of testing for this in two parts:
    1. Generating a text file that contains a list of randomly generated user actions (using 'source-files\TransactionSystemVanilla\src\test\MainCreateActions''); and
    2. Running the system given a list of actions as input (''source-files\TransactionSystemVanilla\src\test\Main'').
3. **Running the experiment**:
    1. We generated action files for 500 to 1000 users, in increments of 100 (this may take a very long time, use smaller values, e.g. 50-100 in increments of 10, for quick experiments).
    2. We ran ''compiled-files\{vanilla | monitored | residual}.jar'' with each of these files, on an Ubuntu machine (with 8gb RAM, Intel 17-4500U with 1.80 GHZ dual core CPU), using the ''usr\bin\time'' command.

## Results

See ''pre-print.pdf'' for results.

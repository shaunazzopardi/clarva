package clarva.analysis.cfg;


import java.util.Set;

public class CFGState<St> {
    public St statement;

    public CFGState() {
    }

    public CFGState(St statement) {
        this.statement = statement;
    }

    @Override
    public boolean equals(Object cfgState) {
        if (cfgState.getClass().equals(this.getClass())) {
            if (((CFGState) cfgState).statement.equals(this.statement)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return statement.hashCode();
    }
}


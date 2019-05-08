package clarva.matching;

import clarva.analysis.cfg.CFGEvent;

public interface Aliasing<T extends CFGEvent> {
    boolean mayAlias(T cfgEvent1, T cfgEvent2);

    boolean mustAlias(T cfgEvent1, T cfgEvent2);
}

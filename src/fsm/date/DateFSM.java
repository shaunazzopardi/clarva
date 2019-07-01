package fsm.date;

import com.google.common.collect.Sets;
import compiler.EventCollection;
import compiler.Property;
import compiler.Token;
import compiler.Trigger;
import fsm.Event;
import fsm.FSM;
import fsm.State;
import fsm.Transition;
import fsm.date.events.ChannelEvent;
import fsm.date.events.ClockEvent;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;

import java.util.*;
import java.util.Map.Entry;

//import compiler.Event;
//import lsm.LazyStateMachine;

public class DateFSM extends FSM<String, DateLabel> {

    public static Map<Trigger, DateEvent> triggerToDateEvent = new HashMap<Trigger, DateEvent>();
    public Property property;
    public String propertyName;
//	public lsm.LazyStateMachine<String, DateLabel> lsm;
//	public DeterministicFSM<String, DateLabel> forwardFSM;
//	public DeterministicFSM<Set<fsm.State<String, DateLabel>>, DateLabel> backwardFSM;
    public String variableDeclarations;
    //public Set<fsm.State<String, MethodCall>> states;
    public Set<State<String, DateLabel>> acceptingStates;
    public Set<State<String, DateLabel>> badStates;
    public State<String, DateLabel> startingState;
//	public Set<MethodCall> actions;
    //public Set<Transition> transitions;
    public Map<String, Set<String>> stateHoareTripleMethod = new HashMap<String, Set<String>>();
    public Map<DateLabel, DateEvent> eventUsedInGuardedCommand;
    public List<String> forEachVariables;
    public List<String> forEachVariableTypes;

    public DateFSM(Property property, List<compiler.Variable> variables) {
        super();
        this.property = property;

        this.propertyName = property.name;

        forEachVariables = new ArrayList<String>();
        forEachVariableTypes = new ArrayList<String>();

        for (compiler.Variable variable : variables) {
            forEachVariables.add(variable.getVariableName());
            forEachVariableTypes.add(variable.getVariableType());
        }

        states = new HashSet<State<String, DateLabel>>();
        acceptingStates = new HashSet<State<String, DateLabel>>();
        finalStates = new HashSet<State<String, DateLabel>>();
        badStates = new HashSet<State<String, DateLabel>>();
        transitions = new HashSet<Transition<String, DateLabel>>();

        eventUsedInGuardedCommand = new HashMap<>();

        compiler.States larvaStates = property.states;
        compiler.Transitions larvaTransitions = property.transitions;

        if (larvaStates.starting.size() != 1) {
            neverFails = true;

            return;
        }

        ArrayList<compiler.State> allStates = new ArrayList<compiler.State>();

        allStates.addAll(0, larvaStates.accepting);
        allStates.addAll(0, larvaStates.bad);
        allStates.addAll(0, larvaStates.normal);
        allStates.addAll(0, larvaStates.starting);

        for (compiler.State state : allStates) {
            this.getOrAddState(state.name.text);
        }

        startingState = this.labelToState.get(larvaStates.starting.get(0).name.text);

        initial.add(startingState);

        for (compiler.State state : larvaStates.bad) {
            badStates.add(this.labelToState.get(state.name.text));
            finalStates.add(this.labelToState.get(state.name.text));
        }

        for (compiler.State state : larvaStates.accepting) {
            acceptingStates.add(this.labelToState.get(state.name.text));
            finalStates.add(this.labelToState.get(state.name.text));
        }

        //	actions = new HashSet<MethodCall>();

        HashMap<compiler.State, compiler.Transition> stateTransition = larvaTransitions.transitions;

        Set<Entry<compiler.State, compiler.Transition>> entrySet = stateTransition.entrySet();

        for (Entry<compiler.State, compiler.Transition> entry : entrySet) {

            State<String, DateLabel> source = this.getOrAddState(entry.getKey().name.text);

            Set<String> methods = new HashSet<String>();

            if (entry.getKey().code != null) {
                for (Token s : entry.getKey().code) {
                    if (!s.toString().contains("returnThis")
                            && !s.toString().equals("\"")
                            && !s.toString().equals("(")
                            && !s.toString().equals(")")) {
                        methods.addAll(Arrays.asList(s.toString().split(",")));
                    }

                }
            }
            stateHoareTripleMethod.put(source.label, methods);

            for (compiler.Arrow arrow : entry.getValue().arrows) {

                State<String, DateLabel> destination = this.getOrAddState(arrow.destination.name.text);

                compiler.Trigger trigger = arrow.trigger;

                String condition = "";
                String action = "";

                for (int i = 0; i < arrow.condition.size(); i++) {
                    condition += arrow.condition.get(i).toString();
                }

                for (int i = 0; i < arrow.action.size(); i++) {
                    action += arrow.action.get(i).toString();
                }

                if (trigger.getClass().equals(EventCollection.class)) {
                    Iterator<Trigger> iterator = ((EventCollection) trigger).events.iterator();

                    while (iterator.hasNext()) {
                        compiler.Trigger next = iterator.next();

                        if (next.getClass().equals(EventCollection.class)) {
                            compiler.EventCollection events = (compiler.EventCollection) next;

                            for (compiler.Trigger trigg : events.events) {
                                if (trigg.getClass().equals(compiler.Event.class)) {
                                    DateEvent event;

                                    compiler.Event larvaEvent = ((compiler.Event) trigg);

                                    if (larvaEvent.type == compiler.Event.EventType.channel) {
                                        String parameterType = "";
                                        String parameterName = "";

                                        if (larvaEvent.args.size() >= 1) {
                                            parameterName = larvaEvent.args.get(0).toString();
                                            parameterType = larvaEvent.variables.get(parameterName).getVariableType();
                                        }


                                        event = new ChannelEvent(larvaEvent.getName().toString(),
                                                larvaEvent.channelName.toString(), parameterType, parameterName, trigger.whereClause);
                                    } else if (larvaEvent.type == compiler.Event.EventType.clock) {
                                        event = new ClockEvent(larvaEvent.getName().toString(), false, larvaEvent.clockAmount);
                                    } else if (larvaEvent.type == compiler.Event.EventType.clockCycle) {
                                        event = new ClockEvent(larvaEvent.getName().toString(), true, larvaEvent.clockAmount);
                                    } else {
                                        event = new MethodCall((compiler.Event) trigg, events, this.forEachVariables, this.forEachVariableTypes, trigger.whereClause);
                                    }

                                    triggerToDateEvent.put(trigg, event);
                                    DateLabel dateLabel = new DateLabel(event, condition, action);

                                    this.addTransition(source, getOrAddEqualAction(dateLabel), destination);

//									fsm.Transition<String, MethodCall> transition = new fsm.Transition<String, MethodCall>(source, destination, getOrAddEqualAction(action));
//
//									if(transition != null){
//										transitions.add(transition);
//
//										//actions.add(new Action((Event) trigg));
//									}
                                }
                            }
                        } else if (next.getClass().equals(compiler.Event.class)) {
                            DateEvent event;

                            compiler.Event larvaEvent = ((compiler.Event) next);

                            if (larvaEvent.type == compiler.Event.EventType.channel) {
                                String parameterType = "";
                                String parameterName = "";

                                if (larvaEvent.args.size() >= 1) {
                                    parameterName = larvaEvent.args.get(0).toString();
                                    parameterType = larvaEvent.variables.get(parameterName).getVariableType();
                                }

                                event = new ChannelEvent(larvaEvent.getName().toString(),
                                        larvaEvent.channelName.toString(), parameterType, parameterName, trigger.whereClause);
                            } else if (larvaEvent.type == compiler.Event.EventType.clock) {
                                event = new ClockEvent(larvaEvent.getName().toString(), false, larvaEvent.clockAmount);
                            } else if (larvaEvent.type == compiler.Event.EventType.clockCycle) {
                                event = new ClockEvent(larvaEvent.getName().toString(), true, larvaEvent.clockAmount);
                            } else {
                                event = new MethodCall((compiler.Event) next, this.forEachVariables, this.forEachVariableTypes, trigger.whereClause);
                            }

                            triggerToDateEvent.put(next, event);

                            DateLabel dateLabel = new DateLabel(event, condition, action);

                            this.addTransition(source, getOrAddEqualAction(dateLabel), destination);
//							fsm.Transition<String, MethodCall> transition = new Transition<String, MethodCall>(source, destination, getOrAddEqualAction(action));

//							if(transition != null){
//								transitions.add(transition);
//
//								//actions.add(new Action((Event) next));
//							}
                        }
                    }

                } else if (trigger.getClass().equals(compiler.Event.class)) {
                    DateEvent event;

                    compiler.Event larvaEvent = ((compiler.Event) trigger);

                    if (larvaEvent.type == compiler.Event.EventType.channel) {
                        String parameterType = "";
                        String parameterName = "";

                        if (larvaEvent.args.size() >= 1) {
                            parameterName = larvaEvent.args.get(0).toString();
                            parameterType = larvaEvent.variables.get(parameterName).getVariableType();
                        }

                        event = new ChannelEvent(larvaEvent.getName().toString(),
                                larvaEvent.channelName.toString(), parameterType, parameterName, trigger.whereClause);
                    } else if (larvaEvent.type == compiler.Event.EventType.clock) {
                        event = new ClockEvent(larvaEvent.getName().toString(), false, larvaEvent.clockAmount);
                    } else if (larvaEvent.type == compiler.Event.EventType.clockCycle) {
                        event = new ClockEvent(larvaEvent.getName().toString(), true, larvaEvent.clockAmount);
                    } else {
                        event = new MethodCall((compiler.Event) trigger, this.forEachVariables, this.forEachVariableTypes, trigger.whereClause);
                    }

                    //	fsm.date.events.MethodCall methodCall = new MethodCall((compiler.Event) trigger, this.forEachVariables, this.forEachVariableTypes, trigger.whereClause);
                    triggerToDateEvent.put(trigger, event);

                    DateLabel dateLabel = new DateLabel(event, condition, action);

                    this.addTransition(source, getOrAddEqualAction(dateLabel), destination);

//					fsm.Transition<String, MethodCall> transition = new Transition<String, MethodCall>(source, destination, getOrAddEqualAction(action));
//
//					if(transition != null){
//						transitions.add(transition);
//
//						//actions.add(new Action((Event) next));
//					}
                }

//				transitions.put(entry.getKey().name.text, value)
            }
        }

//		forwardFSM = new DeterministicFSM<String, DateLabel>(this);
//		backwardFSM = forwardFSM.reverse();

        this.reachabilityReduction();
    }

    public DateFSM(Set<State<String, DateLabel>> states,
                   Set<State<String, DateLabel>> acceptingStates,
                   Set<State<String, DateLabel>> badStates,
                   State<String, DateLabel> startingState,
                   Set<Transition<String, DateLabel>> transitions,
                   Set<Event<DateLabel>> alphabet, Map<String, State<String, DateLabel>> labelToState,
                   String propertyName) {
        this.propertyName = propertyName;
        this.states = states;
        this.acceptingStates = acceptingStates;
        this.badStates = badStates;
        this.startingState = startingState;
        this.finalStates = new HashSet<State<String, DateLabel>>();
        this.finalStates.addAll(this.badStates);
        this.finalStates.addAll(this.acceptingStates);

        this.transitions = transitions;
        this.alphabet = alphabet;
        this.labelToState = labelToState;
//		lsm = new LazyStateMachine<String,DateLabel>(this);

        this.reachabilityReduction();
    }

    public DateFSM(fsm.date.DateFSM fsm) {
        this.propertyName = fsm.propertyName;
        states = new HashSet<State<String, DateLabel>>();
        transitions = new HashSet<Transition<String, DateLabel>>();
        finalStates = new HashSet<State<String, DateLabel>>();
        badStates = new HashSet<State<String, DateLabel>>();
        acceptingStates = new HashSet<State<String, DateLabel>>();
        initial = new HashSet<State<String, DateLabel>>();

        eventUsedInGuardedCommand = fsm.eventUsedInGuardedCommand;

        alphabet = new HashSet<Event<DateLabel>>(fsm.alphabet);

        this.stateHoareTripleMethod = new HashMap<String, Set<String>>(fsm.stateHoareTripleMethod);
        this.forEachVariables = new ArrayList<String>(fsm.forEachVariables);
        this.forEachVariableTypes = new ArrayList<String>(fsm.forEachVariableTypes);

        for (State<String, DateLabel> s : fsm.states) {
            this.getOrAddState(s);

            if (fsm.acceptingStates.contains(s)) {
                this.addAcceptingState(s);
            }
            if (fsm.badStates.contains(s)) {
                this.addBadState(s);
            }
            if (fsm.initial.contains(s)) {
                this.addInitialState(s);
            }
        }

        this.startingState = fsm.startingState;

        for (Transition<String, DateLabel> t : fsm.transitions) {
            this.addTransition(t);
        }

        //assuming only one starting state
        Iterator<State<String, DateLabel>> iterator = this.initial.iterator();
        if (iterator.hasNext()) {
            this.startingState = iterator.next();
        } else {
            this.neverFails = true;
        }

        this.neverFails = fsm.neverFails;
    }

    public DateFSM(String name) {
        this();
        this.propertyName = name;
    }

    public DateFSM() {
        super();
        startingState = new State<String, DateLabel>("", null, this);
        acceptingStates = new HashSet<State<String, DateLabel>>();
        badStates = new HashSet<State<String, DateLabel>>();

        this.forEachVariables = new ArrayList<String>();
        this.forEachVariableTypes = new ArrayList<String>();
        this.internalFSMs = new HashSet<FSM<String, DateLabel>>();
        this.labelToState = new HashMap<String, State<String, DateLabel>>();

        states.add(startingState);
        acceptingStates.add(startingState);
        initial.add(startingState);
    }

    public static fsm.date.DateFSM deepCopy(fsm.date.DateFSM fsm) {
        return new fsm.date.DateFSM(fsm);
    }


    public void reachabilityReduction() {
        this.removeStatesNotReachableFromInitialState();

        //states reachable from initial state and from which a bad state is reachable
        //only works if states unreachable from initial state are already removed
        Set<State<String, DateLabel>> badAfterStates = new HashSet<>();

        //transition back to initial state and add states on the way
        Set<State<String, DateLabel>> current = new HashSet<>();
        current.addAll(badStates);
        current.addAll(acceptingStates);

        while (current.size() != 0) {
            badAfterStates.addAll(current);
            Set<State<String, DateLabel>> next = new HashSet<>();


            for (State<String, DateLabel> state : current) {
                if (state == null) {
                    System.out.println();
                }
                for (Event<DateLabel> event : state.incomingTransitions.keySet()) {
                    Set<String> outgoingStatesLabels = state.incomingTransitions.get(event);
                    outgoingStatesLabels.forEach(label -> next.add(state.parent.labelToState.get(label)));
                }
            }

            next.removeAll(badAfterStates);
            current = next;
        }

        Set<State<String, DateLabel>> goodEntryPoints = new HashSet<State<String, DateLabel>>();

        for (State<String, DateLabel> badAfterState : badAfterStates) {
            for (Event<DateLabel> event : badAfterState.outgoingTransitions.keySet()) {
                Set<String> outgoingStatesLabels = badAfterState.outgoingTransitions.get(event);
                outgoingStatesLabels.forEach(label -> goodEntryPoints.add(badAfterState.parent.labelToState.get(label)));

//                goodEntryPoints.addAll((Collection<? extends State<String, DateLabel>>) badAfterState.outgoingTransitions.get(key));
            }
        }

        goodEntryPoints.removeAll(badAfterStates);

        Set<State<String, DateLabel>> usefulStates = Sets.union(goodEntryPoints, badAfterStates);

        Set<String> usefulStatesLabels = new HashSet<>();

        usefulStates.forEach(u -> usefulStatesLabels.add(u.label));

        this.retainOnly(usefulStatesLabels);

        this.removeStatesNotReachableFromInitialState();

        removeUnusedEvents();
    }

    public void addAcceptingState(State<String, DateLabel> state) {
        State<String, DateLabel> thisState = this.getOrAddState(state);
        this.acceptingStates.add(thisState);
        this.finalStates.add(thisState);
    }

    public void addBadState(State<String, DateLabel> state) {
        State<String, DateLabel> thisState = this.getOrAddState(state);
        this.badStates.add(thisState);
        this.finalStates.add(thisState);
    }

    public void addAcceptingState(String label) {
        State<String, DateLabel> thisState = this.getOrAddState(label);
        this.acceptingStates.add(thisState);
        this.finalStates.add(thisState);
    }

    public void addBadState(String label) {
        State<String, DateLabel> thisState = this.getOrAddState(label);
        this.badStates.add(thisState);
        this.finalStates.add(thisState);
    }

    //to prevent duplicate actions
    //do we need this?
    public Event<DateLabel> getOrAddEqualAction(DateLabel action) {
        eventUsedInGuardedCommand.put(action, action.event);

        for (Event<DateLabel> a : alphabet) {
            if (action.equals(a.label)) {
                return a;
            }
        }

        Event<DateLabel> newAction = new Event<DateLabel>(action);

        alphabet.add(newAction);
        return newAction;
    }

    public SubsetDate alphabetReduction(Set<Event<DateLabel>> alphabetToKeep) {
        return new SubsetDate(this, alphabetToKeep);
    }

    public String toString() {
        if (neverFails || this.transitions.size() == 0) return "";

        String representation = "";

        representation += "PROPERTY " + this.propertyName + "{\n\n";

        representation += "STATES{\n";

        representation += "ACCEPTING{\n";
        for (State<String, DateLabel> state : this.acceptingStates) {
            String hoareTripleMethods = "";
            if (this.stateHoareTripleMethod.get(state.label) != null) {
                hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));
            }
            representation += "\t" + state.label + "{" + hoareTripleMethods + "}\n";
        }
        representation += "\n}\n";

        representation += "BAD{\n";
        for (State<String, DateLabel> state : this.badStates) {

            String hoareTripleMethods = "";
            if (this.stateHoareTripleMethod.get(state.label) != null) {
                hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));
            }
            representation += "\t" + state.label + "{" + hoareTripleMethods + "}\n";
        }
        representation += "\n}\n";

        representation += "NORMAL{\n";
        Set<State<String, DateLabel>> normalStates = new HashSet<State<String, DateLabel>>();
        normalStates.addAll(states);
        normalStates.removeAll(initial);
        normalStates.removeAll(badStates);
        normalStates.removeAll(acceptingStates);

        for (State<String, DateLabel> state : normalStates) {

            String hoareTripleMethods = "";
            if (this.stateHoareTripleMethod.get(state.label) != null) {
                hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));
            }
            representation += "\t" + state.label + "{" + hoareTripleMethods + "}\n";
        }
        representation += "\n}\n";

        representation += "STARTING{\n";
        for (State<String, DateLabel> state : this.initial) {
            String hoareTripleMethods = "";
            if (this.stateHoareTripleMethod.get(state.label) != null) {
                hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));
            }
            representation += "\t" + state.label + "{" + hoareTripleMethods + "}\n";
        }
        representation += "\n}\n";
        representation += "\n}\n\n";

        representation += "TRANSITIONS{\n";
        for (Transition<String, DateLabel> t : this.transitions) {
            representation += "\t" + t.source + " -> " + t.destination + " [" + t.event.label.toString() + "]\n\n";
        }
        representation += "}\n}";

        return representation;
    }

}
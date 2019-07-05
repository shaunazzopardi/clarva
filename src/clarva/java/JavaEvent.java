package clarva.java;

import clarva.analysis.cfg.CFGEvent;
import fsm.date.events.ChannelEvent;
import fsm.date.events.ClockEvent;
import fsm.date.events.MethodCall;
import fsm.helper.Pair;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static clarva.java.Matching.subStructureOf;

/// This class represents an invocation event in a Java program execution
public class JavaEvent extends CFGEvent {

    //Statement being executed
    public Stmt unit;

    //Invocation performed by statement
    //@ invariant unit.getInvokeExpr() == invocation
    public InvokeExpr invocation;

    //Object abstraction binding the DATE's foreach variables to abstract objects
    //@ invariant (this.dateEvent.getClass().equals(MethodCall.class) ==> ((MethodCall) this.dateEvent).forEachVariables.containsAll(objectBinding.keySet()))
    public Map<String, Pair<InstanceKey, PointsToSet>> objectBinding = new HashMap<String, Pair<InstanceKey, PointsToSet>>();

    //Method in which this event occurs
    //@ invariant (callingMethod == null || callingMethod.getActiveBody().getUnits().contains(unit))
    public SootMethod callingMethod;

    //Value (constant or expression) associated with each foreach variable
    public Map<String, Value> valueBinding;

    public JavaEvent() {
        this.epsilon = true;
    }

    public JavaEvent(Stmt unit) {
        epsilon = true;
        this.unit = unit;
    }

    public JavaEvent(ChannelEvent event) {
        super(event);
        this.epsilon = true;
    }

    public JavaEvent(ClockEvent event) {
        super(event);
        this.epsilon = true;
    }

    public JavaEvent(InvokeExpr invocation, Stmt unit, MethodCall event, Map<String, Value> valueBinding) {
        this.invocation = invocation;
        this.dateEvent = event;
        this.unit = unit;
        this.valueBinding = valueBinding;
    }

    @Override
    public JavaEvent abstractVersion(){
        JavaEvent ev = new JavaEvent(invocation, unit, (MethodCall) dateEvent, valueBinding);

        ev.outsideEvent = true;

        return ev;
    }

    //Checks whether the types of the object bindings of foreach vars in this event
    // and the parameter event are compatible.
    // Note how if they are not compatible then we can determine they may not alias
    //@ require typesMayMatch(this)
    public Boolean typesMayMatch(JavaEvent s){
        for(String var : this.objectBinding.keySet()){

            if(this.objectBinding.get(var).first != null
                && s.objectBinding.get(var) != null
                && s.objectBinding.get(var).first != null) {

                Type localType1 = this.objectBinding.get(var).first.getLocal().getType();
                Type localType2 = s.objectBinding.get(var).first.getLocal().getType();

                if (RefType.class.isAssignableFrom(localType1.getClass())
                        && RefType.class.isAssignableFrom(localType2.getClass())) {
                    SootClass type1Class = ((RefType) localType1).getSootClass();
                    SootClass type2Class = ((RefType) localType2).getSootClass();

                    if (!subStructureOf(type1Class, type2Class)
                            && !subStructureOf(type2Class, type1Class)) {
                        return false;
                    }
                }

                Set<Type> types1 = new HashSet<>(this.objectBinding.get(var).first.getPointsToSet().possibleTypes());

                Set<Type> types2 = new HashSet<>(s.objectBinding.get(var).first.getPointsToSet().possibleTypes());


                for (Type type1 : types1) {
                    for (Type type2 : types2) {
                        if (type1.equals(type2)) continue;

                        Type type1Here;
                        if (AnySubType.class.isAssignableFrom(type1.getClass())) {
                            type1Here = ((AnySubType) type1).getBase();
                        } else {
                            type1Here = type1;
                        }

                        Type type2Here;
                        if (AnySubType.class.isAssignableFrom(type2.getClass())) {
                            type2Here = ((AnySubType) type2).getBase();
                        } else {
                            type2Here = type2;
                        }

                        if (RefType.class.isAssignableFrom(type1Here.getClass())
                                && RefType.class.isAssignableFrom(type2Here.getClass())) {
                            SootClass type1Class = ((RefType) type1Here).getSootClass();
                            SootClass type2Class = ((RefType) type2Here).getSootClass();

                            if (subStructureOf(type1Class, type2Class)
                                    || subStructureOf(type2Class, type1Class)) {
                                continue;
                            }
                        }

                        return false;
                    }
                }
            } else if(this.objectBinding.get(var).first != null
                    && s.objectBinding.get(var) != null
                    && s.objectBinding.get(var).second != null) {

                Set<Type> types1 = new HashSet<>(this.objectBinding.get(var).first.getPointsToSet().possibleTypes());
                Type localType = this.objectBinding.get(var).first.getLocal().getType();
                types1.add(localType);

                Set<Type> types2 = s.objectBinding.get(var).second.possibleTypes();

                for (Type type1 : types1) {
                    for (Type type2 : types2) {
                        if (type1.equals(type2)) continue;

                        Type type1Here;
                        if (AnySubType.class.isAssignableFrom(type1.getClass())) {
                            type1Here = ((AnySubType) type1).getBase();
                        } else {
                            type1Here = type1;
                        }

                        Type type2Here;
                        if (AnySubType.class.isAssignableFrom(type2.getClass())) {
                            type2Here = ((AnySubType) type2).getBase();
                        } else {
                            type2Here = type2;
                        }

                        if (RefType.class.isAssignableFrom(type1Here.getClass())
                                && RefType.class.isAssignableFrom(type2Here.getClass())) {
                            SootClass type1Class = ((RefType) type1Here).getSootClass();
                            SootClass type2Class = ((RefType) type2Here).getSootClass();

                            if (subStructureOf(type1Class, type2Class)
                                    || subStructureOf(type2Class, type1Class)) {
                                continue;
                            }
                        }
                        return false;
                    }
                }

            } else if(this.objectBinding.get(var).second != null
                    && s.objectBinding.get(var) != null
                    && s.objectBinding.get(var).first != null) {

                Set<Type> types1 = new HashSet<>(s.objectBinding.get(var).first.getPointsToSet().possibleTypes());
                Type localType = s.objectBinding.get(var).first.getLocal().getType();
                types1.add(localType);

                Set<Type> types2 = this.objectBinding.get(var).second.possibleTypes();

                for (Type type1 : types1) {
                    for (Type type2 : types2) {
                        if (type1.equals(type2)) continue;

                        Type type1Here;
                        if (AnySubType.class.isAssignableFrom(type1.getClass())) {
                            type1Here = ((AnySubType) type1).getBase();
                        } else {
                            type1Here = type1;
                        }

                        Type type2Here;
                        if (AnySubType.class.isAssignableFrom(type2.getClass())) {
                            type2Here = ((AnySubType) type2).getBase();
                        } else {
                            type2Here = type2;
                        }

                        if (RefType.class.isAssignableFrom(type1Here.getClass())
                                && RefType.class.isAssignableFrom(type2Here.getClass())) {
                            SootClass type1Class = ((RefType) type1Here).getSootClass();
                            SootClass type2Class = ((RefType) type2Here).getSootClass();

                            if (subStructureOf(type1Class, type2Class)
                                    || subStructureOf(type2Class, type1Class)) {
                                continue;
                            }
                        }
                        return false;
                    }
                }
            } else if(this.objectBinding.get(var).second != null
                    && s.objectBinding.get(var) != null
                    && s.objectBinding.get(var).second != null) {
                Set<Type> types1 = new HashSet<>(this.objectBinding.get(var).second.possibleTypes());


                Set<Type> types2 = s.objectBinding.get(var).second.possibleTypes();

                for (Type type1 : types1) {
                    for (Type type2 : types2) {
                        if (type1.equals(type2)) continue;

                        Type type1Here;
                        if (AnySubType.class.isAssignableFrom(type1.getClass())) {
                            type1Here = ((AnySubType) type1).getBase();
                        } else {
                            type1Here = type1;
                        }

                        Type type2Here;
                        if (AnySubType.class.isAssignableFrom(type2.getClass())) {
                            type2Here = ((AnySubType) type2).getBase();
                        } else {
                            type2Here = type2;
                        }

                        if (RefType.class.isAssignableFrom(type1Here.getClass())
                                && RefType.class.isAssignableFrom(type2Here.getClass())) {
                            SootClass type1Class = ((RefType) type1Here).getSootClass();
                            SootClass type2Class = ((RefType) type2Here).getSootClass();

                            if (subStructureOf(type1Class, type2Class)
                                    || subStructureOf(type2Class, type1Class)) {
                                continue;
                            }
                        }
                        return false;
                    }
                }
            }
        }

        return true;
    }

    //Infers object binding associated with each foreach vars
    public void inferBinding(SootMethod method, LocalMustAliasAnalysis methodMustAlias, LocalMustNotAliasAnalysis methodMustNotAlias) {
        this.callingMethod = method;

        if (epsilon) return;

        if(dateEvent.getClass().equals(MethodCall.class)) {
            MethodCall methodCallEvent = (MethodCall) dateEvent;
            for (String var : methodCallEvent.forEachVariables) {
                Local local = (Local) valueBinding.get(var);
                Stmt stmt = (Stmt) unit;
                //we have a problem with field vars: foreachvars(Account a, User u = a.u)
                //We can't calculate a.u currently. but do we need to really?
                //In this case no, since shadow(a) != shadow(a') => shadow(u) != shadow(u')
                //But in general we can have an aribitrary a.equals() function that does not depend on a.u
                //e.g. a.equals(a') <= a.value = a'.value
                //For ppDate's, if they don't use Larva's method of instanstiating fields we don't have a problem
                //Otherwise heq, maybe Bodden's Boomerang can help

                InstanceKey key = null;
                PointsToSet objSet = null;

                if (local != null) {
                    try {
                        key = new InstanceKey(local, stmt, method, methodMustAlias, methodMustNotAlias);
                    } catch (Exception e) {
                        //this will be used with "fast" analysis
                        PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
                        objSet = pta.reachingObjects(method.context(), local);
                    }
                }
                objectBinding.put(var, new Pair<>(key, objSet));
            }
        }
    }

    public boolean mayAlias(JavaEvent s) {
        if (s.callingMethod == null) {
            SootMethod sCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.methodMakingInvocation.get(s.invocation).method();
            Unit unitOfsCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.invokeExprToUnit.get(s.invocation);

            JavaMethodIdentifier sCallingMethodID = (JavaMethodIdentifier) JavaCFGAnalysis.cfgAnalysis.statementCalledBy.get(unitOfsCallingMethod);

            s.inferBinding(sCallingMethod, JavaCFGAnalysis.cfgAnalysis.methodMustAlias.get(sCallingMethodID), JavaCFGAnalysis.cfgAnalysis.methodMustNotAlias.get(sCallingMethodID));
//			return true;
        }

        if (this.callingMethod == null) {
            SootMethod thisCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.methodMakingInvocation.get(this.invocation).method();
            Unit unitOfthisCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.invokeExprToUnit.get(this.invocation);

            JavaMethodIdentifier thisCallingMethodID = (JavaMethodIdentifier) JavaCFGAnalysis.cfgAnalysis.statementCalledBy.get(unitOfthisCallingMethod);

            s.inferBinding(thisCallingMethod, JavaCFGAnalysis.cfgAnalysis.methodMustAlias.get(thisCallingMethodID), JavaCFGAnalysis.cfgAnalysis.methodMustNotAlias.get(thisCallingMethodID));
        }

        //
        if (this.equals(s)) {
            return true;
        } else if(!typesMayMatch(s)){
            return false;
        } else if(s.outsideEvent || this.outsideEvent){
            return true;
        }

        //if shadows are in same method
//		if(s.callingMethod.equals(this.callingMethod)){
        for (String var : this.objectBinding.keySet()) {
            if (s.objectBinding.keySet().contains(var)) {
                if (s.objectBinding.get(var) == null
                        || this.objectBinding.get(var) == null) continue;

                if (s.objectBinding.get(var).equals(this.objectBinding.get(var))) {
                    continue;
                }

                if (s.objectBinding.get(var).first != null
                        && this.objectBinding.get(var).first != null) {

                    if (s.objectBinding.get(var).first.equals(this.objectBinding.get(var).first)) {
                        continue;
                    }

                    //NOTE 1
                    //I think we cannot use the commented stuff to check for non-aliasing, since the analysis local/intraprocedural
                    //while we are using a method with interprocedural reasoning
                    //if we use this, then if we have the creation of a user and the deletion of a user in different branches
                    //then these two events never may alias, since during a single execution of a method they can never occur on the same user
                    //however in our analysis we are considering that the method is re-entered, and thus one execution may be
                    //creating the user and the next execution deleting the user
//						if(s.callingMethod.equals(this.callingMethod)
//								&& s.objectBinding.get(var).first.mayNotAlias(this.objectBinding.get(var).first)){
//							return false;
//						}
                } else if (s.objectBinding.get(var).first != null
                        && this.objectBinding.get(var).second != null) {

                    //TODO should this also be removed because of #NOTE 1?
                    if (s.callingMethod.equals(this.callingMethod)
                            && !s.objectBinding.get(var).first.getPointsToSet().hasNonEmptyIntersection(this.objectBinding.get(var).second)) {
                        return false;
                    }


                } else if (s.objectBinding.get(var).second != null
                        && this.objectBinding.get(var).first != null) {

                    //TODO should this also be removed because of #NOTE 1?
                    if (s.callingMethod.equals(this.callingMethod)
                            && !this.objectBinding.get(var).first.getPointsToSet().hasNonEmptyIntersection(s.objectBinding.get(var).second)) {
                        return false;
                    }

                } else if (s.objectBinding.get(var).second != null
                        && this.objectBinding.get(var).second != null) {

                    //TODO should this also be removed because of #NOTE 1?
                    if (s.callingMethod.equals(this.callingMethod)
                            && !this.objectBinding.get(var).second.hasNonEmptyIntersection(s.objectBinding.get(var).second)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean mustAlias(JavaEvent s) {
        //an object at a certain statement does not necessarily always alias with itself
//		if(this.equals(s)) return true;
        if(outsideEvent) return false;
        if (!this.mayAlias(s)) return false;

        for (String var : this.objectBinding.keySet()) {
            if (s.objectBinding.keySet().contains(var)) {
                if (s.objectBinding.get(var) == null
                        || this.objectBinding.get(var) == null) {
                    return false;
                }

                if (s.objectBinding.get(var).first != null && this.objectBinding.get(var).first != null) {

                    if (!s.objectBinding.get(var).first.mustAlias(this.objectBinding.get(var).first)) {
                        if (s.objectBinding.get(var).first.equals(this.objectBinding.get(var).first)) {
                            //	continue;
                        } else {
                            return false;
                        }
                    }


                }
            }
        }

        return true;
    }

    public boolean equals(Object obj) {
        if (obj.getClass().equals(this.getClass())) {
            JavaEvent other = (JavaEvent) obj;
            if (other.unit != null
                    && this.unit != null) {
                if (other.unit.equals(this.unit)) {
                    if (other.dateEvent != null
                            && this.dateEvent != null) {
                        if (other.dateEvent.equals(this.dateEvent)
                                && this.invocation.equals(other.invocation)) {
                            return true;
                        }
                    }
                }
            }

            if (this.epsilon && other.epsilon)
                return true;

        }

        return false;
    }

    public String toString() {
        if (this.epsilon)
            return "epsilon";
        else if (!objectBinding.isEmpty()) {
            String toReturn = this.dateEvent.name + "(";

            for (Pair<InstanceKey, PointsToSet> pair : objectBinding.values()) {
                if (toReturn.charAt(toReturn.length() - 1) != '(') {
                    toReturn += ", ";
                }

                if (pair.first != null) {
                    toReturn += pair.first.getLocal().getType().toString();
                } else if (pair.second != null) {
                    toReturn += "[";
                    for (Type type : pair.second.possibleTypes()) {
                        if (toReturn.charAt(toReturn.length() - 1) != '[') {
                            toReturn += "|";
                        }

                        toReturn += type.toString();
                    }
                    toReturn += "]";
                }
            }

            return toReturn + ")";
        } else return this.dateEvent.name;//return invocation.toString() + " in " + unit.toString();
    }

    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder(17, 37);

        if (epsilon) return hcb.toHashCode();
        else {

            if (this.unit != null) {
                hcb.append(this.unit);
            }

            if (this.dateEvent != null) {
                hcb.append(this.unit);
            }

            if (this.invocation != null) {
                hcb.append(this.invocation);
            }
        }
        return hcb.toHashCode();
    }
}

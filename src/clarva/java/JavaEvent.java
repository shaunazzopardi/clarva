package clarva.java;

import clarva.analysis.cfg.CFGEvent;
import fsm.date.events.ChannelEvent;
import fsm.date.events.ClockEvent;
import fsm.date.events.MethodCall;
import fsm.helper.Pair;
import org.apache.commons.lang3.ClassUtils;
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

public class JavaEvent extends CFGEvent {

    public InvokeExpr invocation;
    public Map<String, Pair<InstanceKey, PointsToSet>> objectBinding = new HashMap<String, Pair<InstanceKey, PointsToSet>>();
    public Stmt unit;
    public SootMethod callingMethod;
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

    //	static int i = 0;
    public void inferBinding(SootMethod method, LocalMustAliasAnalysis methodMustAlias, LocalMustNotAliasAnalysis methodMustNotAlias) {
        this.callingMethod = method;

        if (method == null) {
            System.out.print("");
        }
        if (epsilon) return;

        MethodCall methodCallEvent = (MethodCall) dateEvent;
        for (String var : methodCallEvent.forEachVariables) {
            Local local = (Local) valueBinding.get(var);
            Stmt stmt = (Stmt) unit;
            //we have a problem with the following kind of vars: foreachvars(Account a, User u = a.u)
            //We can't calculate a.u currenty. but do we need to really?
            //In this case no, since shadow(a) != shadow(a') => shadow(u) != shadow(u')
            //But in general we can have an aribitrary a.equals() function that does not depend on a.u
            //e.g. a.equals(a') <= a.value = a'.value
            //For ppDate's, if they don't use Larva's method of instanstiating fields we don't have a problem
            //Otherwise heq, maybe Bodden's Boomerang can help
//			i++;
//			System.out.println(i);

//			////what about when local = this?
//			if(local.getName().equals("this")) {
//				objectBinding.put(var, null);
//				continue;
//			}

            InstanceKey key = null;
            PointsToSet objSet = null;

            if (local != null) {
                try {
                    key = new InstanceKey(local, stmt, method, methodMustAlias, methodMustNotAlias);
                } catch (Exception e) {
                    PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
                    objSet = pta.reachingObjects(method.context(), local);
                    // e.printStackTrace();
                }
            }
            objectBinding.put(var, new Pair<>(key, objSet));
        }
    }

    public boolean mayAlias(JavaEvent s) {

        if (s.callingMethod == null) {
            SootMethod sCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.invokeExprInMethod.get(s.invocation).method();
            Unit unitOfsCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.invokeExprToUnit.get(s.invocation);

            JavaMethodIdentifier sCallingMethodID = (JavaMethodIdentifier) JavaCFGAnalysis.cfgAnalysis.statementCalledBy.get(unitOfsCallingMethod);

            s.inferBinding(sCallingMethod, JavaCFGAnalysis.cfgAnalysis.methodMustAlias.get(sCallingMethodID), JavaCFGAnalysis.cfgAnalysis.methodMustNotAlias.get(sCallingMethodID));
//			return true;
        }

        if (this.callingMethod == null) {
            SootMethod thisCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.invokeExprInMethod.get(this.invocation).method();
            Unit unitOfthisCallingMethod = JavaCFGAnalysis.cfgAnalysis.ma.invokeExprToUnit.get(this.invocation);

            JavaMethodIdentifier thisCallingMethodID = (JavaMethodIdentifier) JavaCFGAnalysis.cfgAnalysis.statementCalledBy.get(unitOfthisCallingMethod);

            s.inferBinding(thisCallingMethod, JavaCFGAnalysis.cfgAnalysis.methodMustAlias.get(thisCallingMethodID), JavaCFGAnalysis.cfgAnalysis.methodMustNotAlias.get(thisCallingMethodID));
        }

        if (this.equals(s)) {
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

                    {
                        Set<Type> types1 = new HashSet<>(this.objectBinding.get(var).first.getPointsToSet().possibleTypes());
                        Type localType1 = this.objectBinding.get(var).first.getLocal().getType();
                        types1.add(localType1);

                        Set<Type> types2 = new HashSet<>(s.objectBinding.get(var).first.getPointsToSet().possibleTypes());
                        Type localType2 = s.objectBinding.get(var).first.getLocal().getType();
                        types2.add(localType2);

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

                                    if (!subStructureOf(type1Class, type2Class)
                                            && !subStructureOf(type2Class, type1Class)) {
                                        return false;
                                    }
                                }
                            }
                        }

                    }
                } else if (s.objectBinding.get(var).first != null
                        && this.objectBinding.get(var).second != null) {

                    //TODO should this also be removed because of #NOTE 1?
                    if (s.callingMethod.equals(this.callingMethod)
                            && !s.objectBinding.get(var).first.getPointsToSet().hasNonEmptyIntersection(this.objectBinding.get(var).second)) {
                        return false;
                    }

                    {
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

                                    if (!subStructureOf(type1Class, type2Class)
                                            && !subStructureOf(type2Class, type1Class)) {
                                        return false;
                                    }
                                }
                            }
                        }

                    }
                } else if (s.objectBinding.get(var).second != null
                        && this.objectBinding.get(var).first != null) {

                    //TODO should this also be removed because of #NOTE 1?
                    if (s.callingMethod.equals(this.callingMethod)
                            && !this.objectBinding.get(var).first.getPointsToSet().hasNonEmptyIntersection(s.objectBinding.get(var).second)) {
                        return false;
                    }

                    {
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

                                    if (!subStructureOf(type1Class, type2Class)
                                            && !subStructureOf(type2Class, type1Class)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                } else if (s.objectBinding.get(var).second != null
                        && this.objectBinding.get(var).second != null) {

                    //TODO should this also be removed because of #NOTE 1?
                    if (s.callingMethod.equals(this.callingMethod)
                            && !this.objectBinding.get(var).second.hasNonEmptyIntersection(s.objectBinding.get(var).second)) {
                        return false;
                    }

                    {
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

                                    if (!subStructureOf(type1Class, type2Class)
                                            && !subStructureOf(type2Class, type1Class)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//		}
//		else{
//
//		}

        return true;
    }

    public boolean classesNonIntersecting(String type1Name, String type2Name) {
        try {
//			Hierarchy activeHierarchy = Scene.v().getActiveHierarchy();

            Class<?> type1Class = ClassUtils.getClass(type1Name);
            Class<?> type2Class = ClassUtils.getClass(type2Name);

            if (!type1Name.equals(type2Name)
                    && !type1Class.isAssignableFrom(type2Class)
                    && !type2Class.isAssignableFrom(type1Class)) {
                return false;
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean mustAlias(JavaEvent s) {
        //an object at a certain statement does not necessarily always alias with itself
//		if(this.equals(s)) return true;
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
//				else if(s.objectBinding.get(var).second != null && this.objectBinding.get(var).second != null){
//					if(s.objectBinding.get(var).second.getClass().equals(FullObjectSet.class)
//						&& this.objectBinding.get(var).second.getClass().equals(FullObjectSet.class)){
//
//
//
////								((FullObjectSet) this.objectBinding.get(var).second).hasNonEmptyIntersection(((FullObjectSet) s.objectBinding.get(var).second))
////										&& ((FullObjectSet) s.objectBinding.get(var).second).hasNonEmptyIntersection(((FullObjectSet) this.objectBinding.get(var).second));
//					}
//					//TODO what about when one is an instance key and the other not?
//				}
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

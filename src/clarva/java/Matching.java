package clarva.java;

import fsm.date.events.MethodCall;
import org.apache.commons.lang3.ClassUtils;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Matching {

    public MethodsAnalysis ma;

    public Matching(MethodsAnalysis ma) {
        this.ma = ma;
    }


    public static void loadProgramClasses(File root) throws IOException {
        if (root.isDirectory()) {
            File[] files = root.listFiles();

            for (File f : files) {
                loadProgramClasses(f);
            }
        } else {
            addURL(root.toURI().toURL());
        }
    }

    //from https://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime
    private static void addURL(URL u) throws IOException {

        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }//end try catch

    }//end method


    public static Boolean matches(InvokeExpr expr, MethodCall methodEvent) {

        SootMethod sootMethod = expr.getMethodRef().resolve();

        return matchesMethod(sootMethod, methodEvent);

//		if(matchesMethod(sootMethod, larvaEvent)){
//		//	Boolean exprMatches = false;
//			
//			//we need to check that if there are any constraints
//			//imposed on a method call, e.g. {r.method(id)} where {id = o.id;}
//			//so that an invoke expression o.method(id) matches the method call
//			//if id = o.id
//			
//			if(larvaEvent.whereMap.size() == 0){
//				return true;
//			}
//			else{
//				//check if assignment of global variable only
//				//we are not checking whether o.method(a) {where o = a;}
//				//
//				for(String s : larvaEvent.whereMap.values()){
//					if(s.contains("(")){
//						return false; //maybe rather
//					}
//					else if(s.contains(".")){
//						expr.
//						
//						return false;
//					}
//				}
//				
//				return true;
//			}
//			
//			
//		//	return exprMatches;
//		}
//		else{
//			return false;
//		}
    }

    private static String getFullClassName(String className) {
        final Package[] packages = Package.getPackages();

        for (final Package p : packages) {
            final String pack = p.getName();
            final String tentative = pack + "." + className;
            try {
                Class.forName(tentative);
            } catch (final ClassNotFoundException e) {
                continue;
            }
            return tentative;
        }

        //check if primitive
        try {
            ClassUtils.getClass(className);
            return className;
        } catch (Exception e) {
            return "";
        }

    }

    public static SootClass getSootClass(String className) {
        SootClass sootClass = Scene.v().getSootClass(className);
        //proper classes have java.lang.Object as a superclass
        if (!sootClass.hasSuperclass()) {
            Chain<SootClass> classes = Scene.v().getClasses();

            List<SootClass> matching = new ArrayList<>();

            for (SootClass sootClass1 : classes) {
                if (sootClass1.getShortJavaStyleName().equals(className)) {
                    matching.add(sootClass1);
                }
            }

            if (matching.size() == 1) return matching.get(0);
            else {
                //for some reason sometimes there is an empty class corresponding to a full sootclass
                matching.remove(sootClass);

                if (matching.size() == 1) {
                    return matching.get(0);
                } else {
                    return null;
                }
            }

        } else {
            return sootClass;
        }
    }

    public static Boolean subStructureOf(SootClass child, SootClass parent) {
        if (child.equals(parent)) return true;

        if (!child.isInterface() && !parent.isInterface()
                && Scene.v().getActiveHierarchy().isClassSubclassOf(child, parent)) {
            return true;
        }

        if (child.isInterface() && parent.isInterface()
                && Scene.v().getActiveHierarchy().isInterfaceSubinterfaceOf(child, parent)) {
            return true;
        }

        if (child.getInterfaces().contains(parent)) {
            return true;
        }

        //anything else to do?

        return false;
    }

    public static Boolean typeMatches(String larvaType, SootClass sootClass) {
        boolean classTypeMatches = true;

        if (larvaType.equals("*")
                || larvaType.replaceAll("\\+", "").equals(sootClass.getName())
                || sootClass.getName().endsWith("." + larvaType.replaceAll("\\+", ""))
                || sootClass.getType().getClassName().equals("Object")) {
            classTypeMatches = true;
        } else {
            //this will not work if package name is not in class name
            String className = larvaType.replaceAll("\\+", "");

            try {

                SootClass sootClass1 = getSootClass(className);
                if (sootClass1 == null) {
                    return true;
                }

                if (!subStructureOf(sootClass, sootClass1)) {
                    return false;
                }

//				if(!sootClass1.equals(sootClass){
//					if(!sootClass.isInterface() && !sootClass1.isInterface()
//							&& !Scene.v().getActiveHierarchy().isClassSubclassOf(sootClass, sootClass1)) {
//						if(sootClass1.isInterface() && sootClass.isInterface()
//								&& !Scene.v().getActiveHierarchy().isInterfaceSubinterfaceOf(sootClass, sootClass1)) {
//							if(!sootClass.getInterfaces().contains(sootClass1)) {
//								return false;
//							}
//						}
//					}
//				}
//				else if(!sootClass1.equals(sootClass)
//                        && sootClass1.isInterface()
//                        && !Scene.v().getActiveHierarchy().isInterfaceSubinterfaceOf(sootClass, sootClass1)){
//					classTypeMatches = true;
//				}
            } catch (Exception e) {

                return true;
//				try {
//					Class sootMethodType = ClassUtils.getClass(sootClass.getType().getClassName());
//
//					Class superClass = sootMethodType.getSuperclass();
//					do {
//						if(superClass.getName().endsWith(className)){
//							classTypeMatches = true;
//							superClass = Object.class;
//						} else{
//							superClass = sootMethodType.getSuperclass();
//						}
//					} while(!superClass.equals(Object.class));
//
//					if(!classTypeMatches) return false;
//
//				} catch (ClassNotFoundException ex) {
//					//just in case, don t return false
//					classTypeMatches = true;
//				}
//
            }
        }

        return classTypeMatches;
    }


    public static Boolean matchesMethod(MethodOrMethodContext method, MethodCall methodEvent) {

        MethodCall larvaEvent = methodEvent;

        SootMethod sootMethod = method.method();

        //static or not both
//        Boolean methodTypeMatches = false;

        //TODO when are larva events marked as static? are they ever?
//        if ((methodEvent.staticMethod && method.method().isStatic())
//                || (!methodEvent.staticMethod && !method.method().isStatic())) methodTypeMatches = true;
//        else return false;

        Boolean classTypeMatches = false;
        Boolean methodNameMatches = false;
        Boolean argTypeMatches = false;
        Boolean returnTypeMatches = false;

        //Method Name Match
        if (larvaEvent.name.equals(sootMethod.getName())
                || (larvaEvent.name.equals("new") && sootMethod.isConstructor())) {
            methodNameMatches = true;
        } else return false;

        classTypeMatches = typeMatches(larvaEvent.objectType, sootMethod.getDeclaringClass());

        //Parameter Type Match
        List<Type> sootMethodTypes = sootMethod.getParameterTypes();
        ArrayList<String> larvaActionTypes = larvaEvent.argTypes;

        if (sootMethodTypes.size() == larvaActionTypes.size()
                || larvaActionTypes.contains("*")) {

            for (int j = 0, i = 0; i < larvaActionTypes.size(); i++, j++) {

                //this takes care of when * is used as a larva action type to match multiple events

                if (larvaActionTypes.size() != sootMethodTypes.size()
                        && larvaActionTypes.get(i).equals("*")) {

                    //Assumption: here we are assuming * is only used once
                    assert !larvaActionTypes.subList(i + 1, larvaActionTypes.size()).contains("*");

                    j = (sootMethodTypes.size()) - ((larvaActionTypes.size()) - i);
                    continue;
                }

//				Class<?> sootMethodType;
//				Class<?> larvaActionType;
                try {

//					String fullClassName = getFullClassName(larvaActionTypes.get(i).toString());
//
//					sootMethodType = ClassUtils.getClass(sootMethodTypes.get(j).toString());
//					larvaActionType = ClassUtils.getClass(fullClassName);
//
//					//or other way round?
//					if(!larvaActionType.isAssignableFrom(sootMethodType)
//							&& !ClassUtils.isAssignable(larvaActionType, sootMethodType)){
//						return false;
//					}

                    if (!larvaActionTypes.get(i).equals("*")) {

                        if (!sootMethodTypes.get(j).toString().equals(larvaActionTypes.get(i))
                                && !sootMethodTypes.get(j).toString().endsWith(larvaActionTypes.get(i))
                                && !larvaActionTypes.get(i).endsWith(sootMethodTypes.get(j).toString())
                                && !sootMethod.getDeclaringClass().getType().getClassName().equals("Object")) {

                            if (RefType.class.isAssignableFrom(sootMethodTypes.get(i).getClass())
                                    && !typeMatches(larvaActionTypes.get(i), ((RefType) sootMethodTypes.get(j)).getSootClass())) {

                                return false;
                            }
                        }
                    }


                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    //	e.printStackTrace();
                    //This handles when .getClass fails (e.g. when .getClass(String) instead of .getClass(java.lang.String)
                    //in this case we just check whether the two strings are equal, or one of them ends with the other
                    //e.g. since java.lang.String ends with String then we assume that they will match
                }


                //else taghtiex kaz
            }

            argTypeMatches = true;

//				//To account for subtypes
//				try {
//					String actionTypeString = larvaActionTypes.get(i).toString();
//					Class<?> actionType = Class.forName("Integer");
//					ClassUtils.convertClassNamesToClasses(arg0)
//					switch(actionTypeString){
//						case "int" : actionType = int.class; break;
//						case "boolean" : actionType = boolean.class; break;
//						case "byte" : actionType = byte.class; break;
//						case "char" : actionType = char.class; break;
//						case "short" : actionType = short.class; break;
//						case "long" : actionType = long.class; break;
//						case "float" : actionType = float.class; break;
//						case "double" : actionType = double.class; break;
//						case "String" : actionType = String.class; break;
//						default : actionType = Class.forName(actionTypeString); break;
//					}
//					
//
//					String methodTypeString = sootMethodTypes.get(i).toString();
//					Class<?> methodParameterType;
//					
//					switch(methodTypeString){
//						case "int" : methodParameterType = int.class; break;
//						case "boolean" : methodParameterType = boolean.class; break;
//						case "byte" : methodParameterType = byte.class; break;
//						case "char" : methodParameterType = char.class; break;
//						case "short" : methodParameterType = short.class; break;
//						case "long" : methodParameterType = long.class; break;
//						case "float" : methodParameterType = float.class; break;
//						case "double" : methodParameterType = double.class; break;
//						default : methodParameterType = Class.forName(methodTypeString); break;
//					}


//					if(!actionType.isAssignableFrom(methodParameterType)){
//						return false;
//					}
//					
//				} catch (ClassNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					
//					//return false???
//				}

        } else if (larvaActionTypes.size() == 0)
            argTypeMatches = true; //if no parameters then matches any overloaded method in larva
        else return false;

        //Return Type Match
        if (larvaEvent.returnType.equals("*")
                || larvaEvent.returnType.toLowerCase().equals(sootMethod.getReturnType().toString().toLowerCase())) {
            returnTypeMatches = true;
        } else if (larvaEvent.name.equals("new") && sootMethod.isConstructor()) {
            returnTypeMatches = true;
        } else {
            String[] splitted = sootMethod.getReturnType().toString().split("\\.");

            if (larvaEvent.returnType.equals(splitted[splitted.length - 1])) {
                returnTypeMatches = true;
            } else return false;
        }

        return classTypeMatches && methodNameMatches && argTypeMatches && returnTypeMatches;
    }


    //// Used for Orphans Analysis ////
    public Boolean flowInsensitiveCompatible(JavaEvent shadow, JavaEvent otherShadow) {
        PointsToAnalysis p2a = Scene.v().getPointsToAnalysis();

//		Unit unit = ma.invokeExprToUnit.get(method);
//		Unit otherUnit = ma.invokeExprToUnit.get(method);
//						
//		for(Shadow shadow : ma.unitShadows.get(unit)){
//			for(Shadow otherShadow : ma.unitShadows.get(otherUnit)){
//			
//				if(!shadow.mayAlias(otherShadow)){
//					return false;
//				}
//			}
//		}
//		
//		return true;
//		
        //below code uses less precise flow-insensitive points-to analysis
        //consider changing back to it if above code is slow on large programs

        //for(Map<String, Value> methodVarValue : shadow.valueBinding){
        //for(Map<String, Value> otherVarValue : ma.shadowBindings.get(otherMethod)){

        //get common vars
        Set<String> commonVars = new HashSet<String>(shadow.valueBinding.keySet());
        commonVars.retainAll(otherShadow.valueBinding.keySet());

        boolean allVarsHereCompatible = true;
        //for each common var
        for (String var : commonVars) {
            //get the points to set for the var for the first shadow
            Context c1 = shadow.unit;
            PointsToSet firstP2S = p2a.reachingObjects(c1, (Local) shadow.valueBinding.get(var));

            //get the points to set for the var for the second shadow
            //when does this return an empty points to set??
            Context c2 = otherShadow.unit;
            PointsToSet secondP2S = p2a.reachingObjects(c2, (Local) otherShadow.valueBinding.get(var));

//					if(firstP2S.hasNonEmptyIntersection(secondP2S))
//						System.out.println("hello");
//
//					if(firstP2S.hasNonEmptyIntersection(firstP2S))
//						System.out.println("hello");
//
//					if(secondP2S.hasNonEmptyIntersection(secondP2S))
//						System.out.println("hello");
//
//					if(firstP2S.equals(secondP2S))
//						System.out.println("hello");

            if ((!firstP2S.hasNonEmptyIntersection(secondP2S) && !(shadow.equals(otherShadow)))
                    && !(firstP2S instanceof EmptyPointsToSet || secondP2S instanceof EmptyPointsToSet)) {
                allVarsHereCompatible = false;
                break;
            }

        }

        return allVarsHereCompatible;
        //	return flowInsensitiveCompatibleClass(method, otherMethod) && flowInsensitiveCompatibleArgs(method, otherMethod);
    }

    //we can use this when disabling certain method calls
//								
//								
//								if(methodArgVal.getClass() == Local.class
//										&& otherMethodArgVal.getClass() == Local.class){
//									
//									PointsToSet firstP2S = p2a.reachingObjects(method.getMethodRef().resolve().context(), 
//											(Local) methodArgVal);
//									
//									PointsToSet secondP2S = p2a.reachingObjects(otherMethod.getMethodRef().resolve().context(), 
//											(Local) otherMethodArgVal);
//									
//									
//									if(!firstP2S.hasNonEmptyIntersection(secondP2S)){
//										return false;
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		
//		return true;
//	}
//
//	
//	public Map<String, Set<Object>> match(DateLabel advice, SootMethod method){
//
//		if(matchesMethod(method, advice)){
//			//get variables of method and return them
//
//			return new HashMap<String, Set<Object>>();
//
//		}
//
//		return null;
//	}


//	public Map<String, Set<Object>> depMatch(DateLabel advice, SootMethod method){
//
//		if(matchesMethod(method, advice)){
//			//get variables of method and return them
//
//			return new HashMap<String, Set<Object>>();
//
//		}
//
//		return null;
//	}
//	
    //	public Boolean matches(SootMethod sootMethod, Event dateEvent){
    //		Boolean classNameMatches = false;
    //		Boolean methodNameMatches = false;
    //		Boolean parameterTypeMatches = false;
    //		Boolean returnTypeMatches = false;
    //
    //		//Declaring Class Name Match
    //		if(dateEvent.target.text == "*"
    //				|| dateEvent.target.text == sootMethod.getDeclaringClass().getName()){
    //			classNameMatches = true;
    //		}
    //
    //		//Method Name Match
    //		if(dateEvent.methodName.text == sootMethod.getName()){
    //			methodNameMatches = true;
    //		}
    //
    //		//Parameter Type Match
    //
    //		//Return Type Match
    //		if(dateEvent.returned == null && sootMethod.getReturnType() == null){
    //			returnTypeMatches = true;
    //		}
    //		else if(dateEvent.returned.text == sootMethod.getReturnType().toString()){
    //			returnTypeMatches = true;
    //		}
    //		else
    //		{
    //			String[] splitted = sootMethod.getReturnType().toString().split("\\.");
    //
    //			if(dateEvent.returned.text == splitted[splitted.length - 1]){
    //				returnTypeMatches = true;
    //			}
    //		}
    //
    //		return classNameMatches && methodNameMatches && parameterTypeMatches && returnTypeMatches;
    //	}
//
//	public static Boolean matches(SootMethod sootMethod, Trigger trigger){
//		if(trigger.getClass().equals(EventCollection.class))
//		{
//			ArrayList<Trigger> eventCollection = ((EventCollection) trigger).events;
//
//			Iterator<Trigger> iterator = eventCollection.iterator();
//
//			while(iterator.hasNext()){
//				if(trigger.getClass().equals(Event.class))
//				{
//					if(matches(sootMethod, (Event) trigger)){
//						return true;
//					}
//				}
//				else return false;
//			}
//
//			return false;
//		}
//		else return false;
//	}
//
//	public static Boolean matches(SootMethod sootMethod, Event dateEvent){
//		Boolean classNameMatches = false;
//		Boolean methodNameMatches = false;
//		Boolean parameterTypeMatches = false;
//		Boolean returnTypeMatches = false;
//
//		//Declaring Class Name Match
//		if(dateEvent.target.text == "*"
//				|| dateEvent.target.text == sootMethod.getDeclaringClass().getName()){
//			classNameMatches = true;
//		}
//
//		//Method Name Match
//		if(dateEvent.methodName.text == sootMethod.getName()){
//			methodNameMatches = true;
//		}
//
//		//Parameter Type Match
//
//		//Return Type Match
//		if(dateEvent.returned == null && sootMethod.getReturnType() == null){
//			returnTypeMatches = true;
//		}
//		else if(dateEvent.returned.text == sootMethod.getReturnType().toString()){
//			returnTypeMatches = true;
//		}
//		else
//		{ 
//			String[] splitted = sootMethod.getReturnType().toString().split("\\.");
//
//			if(dateEvent.returned.text == splitted[splitted.length - 1]){
//				returnTypeMatches = true;
//			}
//		}
//
//		return classNameMatches && methodNameMatches && parameterTypeMatches && returnTypeMatches;
//	}

}

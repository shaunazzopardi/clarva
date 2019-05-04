package clarva.java;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import compiler.ParseException;
import fsm.Event;
import fsm.date.DateFSM;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.options.Options;
import soot.util.Chain;
import soot.util.Cons;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.*;

public class ProgramModifier {

	//TODO implement this.
	// Maybe create monitored versions of functions in larva monitored system
	//	and have any instrumented calls use those functions
//	public static boolean createUnmonitoredCopy(SootMethod method) throws IOException {
//
//		SootClass declaringClass = method.getDeclaringClass();
//
//		SootMethod monitoredMethod = new SootMethod(method.getName() + "Monitored", method.getParameterTypes(), method.getReturnType());
//
//		JimpleBody body = Jimple.v().newBody(monitoredMethod);
//		monitoredMethod.setActiveBody(body);
//
//		declaringClass.addMethod(monitoredMethod);
//		monitoredMethod.setDeclaringClass(declaringClass);
//
//		Local arg = Jimple.v().newLocal("l0", method.getReturnType());
//		body.getLocals().add(arg);
//
//		if(!declaringClass.isStatic()
//				&& !method.isStatic()){
//
//			ThisRef thisRef = new ThisRef(method.getDeclaringClass().getType());
//
//			Local thisLocal = Jimple.v().newLocal("ths", thisRef.getType());
//			body.getLocals().add(thisLocal);
//
//			body.getUnits().add(Jimple.v().newIdentityStmt(thisLocal,
//			            Jimple.v().newThisRef((RefType)thisRef.getType())));
//
//		    body.getUnits().add(Jimple.v().newInvokeStmt
//		    	        (Jimple.v().newVirtualInvokeExpr
//		    	           (thisLocal, method.makeRef(), body.getParameterLocals())));
//
//
//
//		//method.setName(method.getName() + "Unmonitored");
//		}
//		else{
//		    body.getUnits().add(Jimple.v().newInvokeStmt
//		    	        (Jimple.v().newStaticInvokeExpr
//		    	           (method.makeRef(), body.getParameterLocals())));
//		}
//
//		if(method.getReturnType().toString() != "void"){
//			body.getUnits().add(Jimple.v().newReturnStmt(arg));
//		}
//
//
//		String fileName = SourceLocator.v()
//				.getFileNameFor(declaringClass, Options.output_format_class);
//		OutputStream streamOut = new JasminOutputStream(
//				new FileOutputStream(fileName));
//		PrintWriter writerOut = new PrintWriter(
//				new OutputStreamWriter(streamOut));
//		AbstractJasminClass jasminClass = new soot.baf.JasminClass(declaringClass);
//		jasminClass.print(writerOut);
//		writerOut.flush();
//		streamOut.close();
//
//
//		return true;
//	}

	static SootClass eventClass;

	public static void createInstrumentedActionsClass() {
		eventClass = new SootClass("EventMonitoredInterface", Modifier.PUBLIC);

		eventClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));

		Scene.v().addClass(eventClass);
	}


	//TODO, smarter instrumentation: augment calls with boolean monitored, where if true then the monitored calls are used, and if false the unmonitored calls are used
	public static void instrument() throws Exception {
		createInstrumentedActionsClass();

		Set<Event<JavaEvent>> events = new HashSet<>();

		for(Set<Event<JavaEvent>> eventss : PropertyTransformer.eventsToMonitorFor.values()){
			events = Sets.union(events, eventss);
		}

		for (Event<JavaEvent> event : events) {
			if (event.label.epsilon) continue;

			SootMethod sootMethod = event.label.callingMethod;

			Body sootMethodBody = sootMethod.getActiveBody();

			Chain units = sootMethodBody.getUnits();

			Iterator stmtIt = units.snapshotIterator();

			while (stmtIt.hasNext()) {
				Stmt stmt = (Stmt) stmtIt.next();

				if (event.label.unit.equals(stmt)) {
					if (InvokeStmt.class.isAssignableFrom(stmt.getClass())) {

						InvokeStmt invokeStmt = new JInvokeStmt(callandCreateInstrumentedMethod(stmt.getInvokeExpr(), VoidType.v()));
						units.insertBefore(invokeStmt, stmt);
						units.remove(stmt);

					} else if (AssignStmt.class.isAssignableFrom(stmt.getClass())) {
						InvokeExpr invokeExpr = callandCreateInstrumentedMethod(stmt.getInvokeExpr(), ((AssignStmt) stmt).getLeftOp().getType());
						JAssignStmt jAssignStmt = new JAssignStmt(((AssignStmt) stmt).getLeftOp(), invokeExpr);
						units.insertBefore(jAssignStmt, stmt);
						units.remove(stmt);

					} else{
						throw new Exception("Event triggerring statement not handled.");
					}

//					units.remove(stmt);

				}
			}
		}

//		for (Event<JavaEvent> event : events) {
//			SootClass c = event.label.callingMethod.getDeclaringClass();
//
//			for (SootMethod method : c.getMethods()) {
//				method.retrieveActiveBody();
//			}
//		}

		printEventClass();
	}

	public static InvokeExpr callandCreateInstrumentedMethod(InvokeExpr invoke, Type returnType) {
//		if (AbstractVirtualInvokeExpr.class.isAssignableFrom(invoke.getClass())
//				|| ) {

			List<Value> args = new ArrayList<>();
			String newMethodName;


			if (AbstractVirtualInvokeExpr.class.isAssignableFrom(invoke.getClass())) {
				newMethodName = invoke.getMethod().getName() + "Monitored";
//				newMethodName = "virtual" + invoke.getMethod().getName() + "Monitored";

				AbstractVirtualInvokeExpr invokeExpr = (AbstractVirtualInvokeExpr) invoke;
				args.add(invokeExpr.getBase());

				args.addAll(invokeExpr.getArgs());

			} else if (AbstractInstanceInvokeExpr.class.isAssignableFrom(invoke.getClass())) {
				newMethodName = invoke.getMethod().getName() + "Monitored";
//				newMethodName = "instance" + invoke.getMethod().getName() + "Monitored";

				AbstractInstanceInvokeExpr invokeExpr = (AbstractInstanceInvokeExpr) invoke;
				args.add(invokeExpr.getBase());

				args.addAll(invokeExpr.getArgs());
			} else {//(AbstractStaticInvokeExpr.class.isAssignableFrom(invoke.getClass())){
				newMethodName = invoke.getMethod().getName() + "Monitored";
				args.addAll(invoke.getArgs());
			}

			int varNo = 0;

			List<Type> inputTypes = new ArrayList<>();

			for (int i = 0; i < args.size(); i++) {
				inputTypes.add(args.get(i).getType());
			}

			if (!eventClass.declaresMethod(newMethodName, inputTypes)) {
				SootMethod method = new SootMethod(newMethodName,
						inputTypes,
						returnType, Modifier.STATIC | Modifier.PUBLIC);

				JimpleBody body = Jimple.v().newBody(method);
				method.setActiveBody(body);

				for(int i = 0; i < args.size(); i++){
//					if(Constant.class.isAssignableFrom(args.get(i).getClass())){
						Local newLocal = Jimple.v().newLocal("r" + varNo,(args.get(i)).getType());
						varNo++;

						body.getLocals().add(newLocal);
						body.getUnits().add(Jimple.v().newIdentityStmt(newLocal,
								Jimple.v().newParameterRef(newLocal.getType(), i)));
//					}
				}

				InvokeExpr newInvoke;

				List<Local> locals = body.getParameterLocals();

				if(JStaticInvokeExpr.class.equals(invoke.getClass())){
					newInvoke = new JStaticInvokeExpr(invoke.getMethodRef(), locals);
				} else if(JSpecialInvokeExpr.class.equals(invoke.getClass())){
					newInvoke = new JSpecialInvokeExpr((Local) locals.get(0), invoke.getMethodRef(), locals.subList(1, locals.size()));
				} else if(JInterfaceInvokeExpr.class.equals(invoke.getClass())){
					newInvoke = new JInterfaceInvokeExpr((Local) locals.get(0), invoke.getMethodRef(), locals.subList(1, locals.size()));
				} else if(JVirtualInvokeExpr.class.equals(invoke.getClass())){
					newInvoke = new JVirtualInvokeExpr((Local) locals.get(0), invoke.getMethodRef(), locals.subList(1, locals.size()));
				} else{
					//TODO this may cause errors
					newInvoke = invoke;
				}
//				TODO this isn t handled
//				TODO see http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.590.624&rep=rep1&type=pdf
//				else if(JDynamicInvokeExpr.class.equals(invoke.getClass())){
//
//				}

				if(returnType.equals(VoidType.v())) {
					body.getUnits().add(new JInvokeStmt(newInvoke));
					body.getUnits().add(Jimple.v().newReturnVoidStmt());
				} else{
					Local newLocal = Jimple.v().newLocal("r" + varNo, returnType);
					varNo++;

					body.getLocals().add(newLocal);

					body.getUnits().add(new JAssignStmt(newLocal, newInvoke));
					body.getUnits().add(Jimple.v().newReturnStmt(newLocal));
				}

				eventClass.addMethod(method);
			}

			JStaticInvokeExpr jStaticInvokeExpr = new JStaticInvokeExpr(eventClass.getMethod(newMethodName, inputTypes).makeRef(), args);

			return jStaticInvokeExpr;

//		} else if (AbstractInstanceInvokeExpr.class.isAssignableFrom(invoke.getClass())) {
//			AbstractInstanceInvokeExpr invokeExpr = (AbstractInstanceInvokeExpr) invoke;
//
////						ThisRef thisRef = new ThisRef(sootMethod.getDeclaringClass().getType());
////
////						Local base = Jimple.v().newLocal("ths", thisRef.getType());
////						event.label.callingMethod.retrieveActiveBody().getLocals().add(base);
//			Value base = invokeExpr.getBase();
//
//			List<Value> args = invokeExpr.getArgs();
//			args = invokeExpr.getArgs();

//		} else if () {
//			AbstractStaticInvokeExpr invokeExpr = (AbstractStaticInvokeExpr) invoke;
//
//			List<Value> args = invokeExpr.getArgs();
//			args = invokeExpr.getArgs();
//
////			String newMethodName = "static" + invoke.getMethod().getName() + "Monitored";
//			String newMethodName = invoke.getMethod().getName() + "Monitored";
//
//			List<Type> inputTypes = new ArrayList<>();
//
//			for (int i = 0; i < args.size(); i++) {
//				inputTypes.add(args.get(i).getType());
//			}

//			if (!eventClass.declaresMethod(newMethodName, inputTypes)) {
//				SootMethod method = new SootMethod(newMethodName,
//						inputTypes,
//						returnType, Modifier.STATIC | Modifier.PUBLIC);
//
//				JimpleBody body = Jimple.v().newBody(method);
//				method.setActiveBody(body);
//
//				for(int i = 0; i < args.size(); i++){
//					body.getLocals().add((Local) args.get(i));
//					body.getUnits().add(Jimple.v().newIdentityStmt((Local)args.get(i),
//							Jimple.v().newParameterRef(args.get(i).getType(), i )));
//				}
//
//				if(returnType.equals(VoidType.v())) {
//					body.getUnits().add(new JInvokeStmt(invoke));
//					body.getUnits().add(Jimple.v().newReturnVoidStmt());
//				} else{
//					body.getUnits().add(Jimple.v().newReturnStmt(invoke));
//				}
//
//				eventClass.addMethod(method);
//			}

//			List<Value> newArgs = new ArrayList<>();
//			newArgs.addAll(args);
//
//			JStaticInvokeExpr jStaticInvokeExpr = new JStaticInvokeExpr(eventClass.getMethod(newMethodName, inputTypes).makeRef(), newArgs);
//
//			return jStaticInvokeExpr;

//		}
//
//		return invoke;
	}

	public static void printEventClass() throws IOException {
		String fileName = SourceLocator.v().getFileNameFor(eventClass, Options.output_format_class);

		Files.createParentDirs(new File(fileName));

		OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
		PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

		JasminClass jasminClass = new soot.jimple.JasminClass(eventClass);
		jasminClass.print(writerOut);
		writerOut.flush();
		streamOut.close();

	}
}
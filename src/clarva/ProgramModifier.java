package clarva;

import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.ThisRef;

public class ProgramModifier{

	public boolean createUnmonitoredCopy(SootMethod method){
		
		SootClass declaringClass = method.getDeclaringClass();
		
		SootMethod monitoredMethod = new SootMethod(method.getName() + "Monitored", method.getParameterTypes(), method.getReturnType());
				
		JimpleBody body = Jimple.v().newBody(monitoredMethod);
		monitoredMethod.setActiveBody(body);
		
		Local arg = Jimple.v().newLocal("l0", method.getReturnType());
		body.getLocals().add(arg);
		
		if(!declaringClass.isStatic()
				&& !method.isStatic()){

			ThisRef thisRef = new ThisRef(method.getDeclaringClass().getType());
			
			Local thisLocal = Jimple.v().newLocal("ths", thisRef.getType());   
			body.getLocals().add(thisLocal);
			
			body.getUnits().add(Jimple.v().newIdentityStmt(thisLocal,
			            Jimple.v().newThisRef((RefType)thisRef.getType())));
			
		    body.getUnits().add(Jimple.v().newInvokeStmt
		    	        (Jimple.v().newVirtualInvokeExpr
		    	           (thisLocal, method.makeRef(), body.getParameterLocals())));
	
			
		
		//method.setName(method.getName() + "Unmonitored");
		}
		else{
		    body.getUnits().add(Jimple.v().newInvokeStmt
		    	        (Jimple.v().newStaticInvokeExpr
		    	           (method.makeRef(), body.getParameterLocals())));
		}
		
		if(method.getReturnType().toString() != "void"){
			body.getUnits().add(Jimple.v().newReturnStmt(arg));
		}
		
		declaringClass.addMethod(monitoredMethod);
		monitoredMethod.setDeclaringClass(declaringClass);
		
		return true;
	}
}
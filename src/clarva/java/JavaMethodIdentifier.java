package clarva.java;

import clarva.matching.MethodIdentifier;
import soot.MethodOrMethodContext;

import java.util.HashMap;

public class JavaMethodIdentifier extends MethodIdentifier {

    public static HashMap<MethodOrMethodContext, JavaMethodIdentifier> respectiveIdentifier = new HashMap<>();
    public MethodOrMethodContext methodOrMethodContext;

    private JavaMethodIdentifier(MethodOrMethodContext methodOrMethodContext) {
        this.methodOrMethodContext = methodOrMethodContext;
    }

    public static JavaMethodIdentifier get(MethodOrMethodContext method) {
        if (respectiveIdentifier.entrySet().contains(method)) {
            return respectiveIdentifier.get(method);
        } else {
            return new JavaMethodIdentifier(method);
        }
    }

    @Override
    public boolean equals(Object javaMethodIdentifier) {
        if (javaMethodIdentifier.getClass().equals(JavaMethodIdentifier.class)) {
            return methodOrMethodContext.equals(((JavaMethodIdentifier) javaMethodIdentifier).methodOrMethodContext);
        } else return false;
    }

    @Override
    public int hashCode() {
        if (methodOrMethodContext == null) return -1;

        return methodOrMethodContext.hashCode();
    }

    @Override
    public String toString() {
        return methodOrMethodContext.toString();
    }
}

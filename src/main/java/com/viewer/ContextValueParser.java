package com.viewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.frame.XStackFrame;

import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class ContextValueParser {

    private static final String TARGET_VARIABLE_NAME = "context";

    private static final Set<String> PRIMITIVE_WRAPPER_TYPES = new HashSet<>();
    private static final Map<String, String> UNBOXING_METHODS = new HashMap<>();
    private static final Map<String, String> UNBOXING_SIGNATURES = new HashMap<>();

    static {
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Integer");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Long");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Short");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Byte");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Float");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Double");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Character");
        PRIMITIVE_WRAPPER_TYPES.add("java.lang.Boolean");

        UNBOXING_METHODS.put("java.lang.Integer", "intValue");
        UNBOXING_SIGNATURES.put("java.lang.Integer", "()I");

        UNBOXING_METHODS.put("java.lang.Long", "longValue");
        UNBOXING_SIGNATURES.put("java.lang.Long", "()J");

        UNBOXING_METHODS.put("java.lang.Short", "shortValue");
        UNBOXING_SIGNATURES.put("java.lang.Short", "()S");

        UNBOXING_METHODS.put("java.lang.Byte", "byteValue");
        UNBOXING_SIGNATURES.put("java.lang.Byte", "()B");

        UNBOXING_METHODS.put("java.lang.Float", "floatValue");
        UNBOXING_SIGNATURES.put("java.lang.Float", "()F");

        UNBOXING_METHODS.put("java.lang.Double", "doubleValue");
        UNBOXING_SIGNATURES.put("java.lang.Double", "()D");

        UNBOXING_METHODS.put("java.lang.Character", "charValue");
        UNBOXING_SIGNATURES.put("java.lang.Character", "()C");

        UNBOXING_METHODS.put("java.lang.Boolean", "booleanValue");
        UNBOXING_SIGNATURES.put("java.lang.Boolean", "()Z");
    }

    /**
     * 解析当前调试帧中的 context 变量
     */
    public static void parseContext(Project project) {
        if (project == null) return;

        XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
        if (session == null) return;

        XStackFrame currentFrame = session.getCurrentStackFrame();
        if (!(currentFrame instanceof JavaStackFrame)) return;

        JavaStackFrame javaFrame = (JavaStackFrame) currentFrame;
        StackFrameProxyImpl stackFrameProxy = javaFrame.getStackFrameProxy();

        try {
            LocalVariableProxyImpl targetVar = stackFrameProxy.visibleVariableByName(TARGET_VARIABLE_NAME);
            if (targetVar == null) {
                System.out.println("在当前栈帧中未找到变量: " + TARGET_VARIABLE_NAME);
                return;
            }

            Value mapValue = stackFrameProxy.getValue(targetVar);

            if (!(mapValue instanceof ObjectReference)) {
                System.out.println("变量 '" + TARGET_VARIABLE_NAME + "' 不是一个对象。");
                return;
            }

            JsonElement jsonTree = convertJdiValueToJson((ObjectReference) mapValue, stackFrameProxy, new HashSet<>());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(jsonTree);

            System.out.println("--- " + TARGET_VARIABLE_NAME + " 的 JSON 对象树 ---");
            System.out.println(jsonString);
            System.out.println("--- 结束 ---");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private static JsonElement convertJdiValueToJson(Value value, StackFrameProxyImpl frameProxy, Set<Long> visitedIds) throws Exception {
        if (value == null) {
            return JsonNull.INSTANCE;
        }

        if (value instanceof StringReference) {
            return new JsonPrimitive(((StringReference) value).value());
        }
        if (value instanceof PrimitiveValue) {
            if (value instanceof BooleanValue) return new JsonPrimitive(((BooleanValue) value).booleanValue());
            if (value instanceof CharValue) return new JsonPrimitive(((CharValue) value).charValue());
            if (value instanceof ByteValue) return new JsonPrimitive(((ByteValue)value).value());
            if (value instanceof ShortValue) return new JsonPrimitive(((ShortValue)value).value());
            if (value instanceof IntegerValue) return new JsonPrimitive(((IntegerValue)value).value());
            if (value instanceof LongValue) return new JsonPrimitive(((LongValue)value).value());
            if (value instanceof FloatValue) return new JsonPrimitive(((FloatValue)value).value());
            if (value instanceof DoubleValue) return new JsonPrimitive(((DoubleValue)value).value());
            return new JsonPrimitive(value.toString());
        }

        if (!(value instanceof ObjectReference)) {
            return new JsonPrimitive(value.toString());
        }

        ObjectReference objRef = (ObjectReference) value;
        long uniqueID = objRef.uniqueID();
        if (visitedIds.contains(uniqueID)) {
            return new JsonPrimitive("<<CIRCULAR_REFERENCE_TO_ID:" + uniqueID + ">>");
        }
        visitedIds.add(uniqueID);

        ReferenceType type = objRef.referenceType();
        String typeName = type.name();
        ThreadReference threadRef = frameProxy.threadProxy().getThreadReference();

        try {
            // --- NEW: Handle primitive wrapper types by unboxing them ---
            if (PRIMITIVE_WRAPPER_TYPES.contains(typeName)) {
                String methodName = UNBOXING_METHODS.get(typeName);
                String methodSignature = UNBOXING_SIGNATURES.get(typeName);
                // Remotely call the unboxing method (e.g., intValue(), booleanValue())
                Value unboxedValue = invokeRemoteMethod(objRef, threadRef, methodName, methodSignature, Collections.emptyList());
                // Recursively call this function with the now-primitive value
                return convertJdiValueToJson(unboxedValue, frameProxy, visitedIds);
            }
            // --- End of new logic block ---

            if (isAssignable(type, "java.util.Collection")) {
                JsonArray jsonArray = new JsonArray();
                ObjectReference iterator = (ObjectReference) invokeRemoteMethod(objRef, threadRef, "iterator", "()Ljava/util/Iterator;", Collections.emptyList());
                while (((BooleanValue) invokeRemoteMethod(iterator, threadRef, "hasNext", "()Z", Collections.emptyList())).value()) {
                    Value element = invokeRemoteMethod(iterator, threadRef, "next", "()Ljava/lang/Object;", Collections.emptyList());
                    jsonArray.add(convertJdiValueToJson(element, frameProxy, visitedIds));
                }
                return jsonArray;
            }

            if (isAssignable(type, "java.util.Map")) {
                JsonObject jsonObject = new JsonObject();
                ObjectReference entrySet = (ObjectReference) invokeRemoteMethod(objRef, threadRef, "entrySet", "()Ljava/util/Set;", Collections.emptyList());
                ObjectReference iterator = (ObjectReference) invokeRemoteMethod(entrySet, threadRef, "iterator", "()Ljava/util/Iterator;", Collections.emptyList());

                while (((BooleanValue) invokeRemoteMethod(iterator, threadRef, "hasNext", "()Z", Collections.emptyList())).value()) {
                    ObjectReference entry = (ObjectReference) invokeRemoteMethod(iterator, threadRef, "next", "()Ljava/lang/Object;", Collections.emptyList());
                    Value key = invokeRemoteMethod(entry, threadRef, "getKey", "()Ljava/lang/Object;", Collections.emptyList());
                    Value val = invokeRemoteMethod(entry, threadRef, "getValue", "()Ljava/lang/Object;", Collections.emptyList());
                    String keyStr = (key instanceof StringReference) ? ((StringReference) key).value() : key.toString();
                    jsonObject.add(keyStr, convertJdiValueToJson(val, frameProxy, visitedIds));
                }
                return jsonObject;
            }

            if (objRef instanceof ArrayReference) {
                JsonArray jsonArray = new JsonArray();
                ArrayReference arrayRef = (ArrayReference) objRef;
                for (Value element : arrayRef.getValues()) {
                    jsonArray.add(convertJdiValueToJson(element, frameProxy, visitedIds));
                }
                return jsonArray;
            }

            JsonObject jsonObject = new JsonObject();
            for (Field field : type.allFields()) {
                if (field.isStatic()) continue;
                String fieldName = field.name();
                Value fieldValue = objRef.getValue(field);
                jsonObject.add(fieldName, convertJdiValueToJson(fieldValue, frameProxy, visitedIds));
            }
            return jsonObject;

        } finally {
            visitedIds.remove(uniqueID);
        }
    }

    // ... (invokeRemoteMethod, isAssignable, and update methods remain unchanged)
    private static Value invokeRemoteMethod(ObjectReference instance, ThreadReference thread, String methodName, String methodSignature, List<? extends Value> args) throws Exception {
        List<Method> methods = instance.referenceType().methodsByName(methodName, methodSignature);
        if (methods.isEmpty()) {
            throw new NoSuchMethodException("在 " + instance.referenceType().name() + " 中未找到方法 " + methodName + " 签名 " + methodSignature);
        }
        Method methodToInvoke = methods.get(0);
        return instance.invokeMethod(thread, methodToInvoke, args, ObjectReference.INVOKE_SINGLE_THREADED);
    }

    private static boolean isAssignable(ReferenceType type, String targetTypeName) throws ClassNotLoadedException {
        // ...
        VirtualMachine vm = type.virtualMachine();
        List<ReferenceType> targetTypes = vm.classesByName(targetTypeName);
        if (targetTypes.isEmpty()) {
            return false;
        }
        ReferenceType targetType = targetTypes.get(0);
        Queue<ReferenceType> queue = new LinkedList<>();
        queue.add(type);
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            ReferenceType current = queue.poll();
            if (current == null || !visited.add(current.name())) continue;
            if (current.equals(targetType) || current.name().equals(targetTypeName)) {
                return true;
            }
            if (current instanceof ClassType) {
                for (InterfaceType interfaceType : ((ClassType) current).allInterfaces()) {
                    if (isAssignable(interfaceType, targetTypeName)) return true;
                }
                queue.add(((ClassType) current).superclass());
            } else if (current instanceof InterfaceType) {
                queue.addAll(((InterfaceType) current).superinterfaces());
            }
        }
        return false;
    }

}

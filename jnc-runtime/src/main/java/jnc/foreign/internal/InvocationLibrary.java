package jnc.foreign.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jnc.foreign.LoadOptions;
import jnc.foreign.Pointer;
import jnc.foreign.abi.CallingMode;
import jnc.foreign.annotation.CallingConvention;
import jnc.foreign.annotation.Typedef;

class InvocationLibrary {

    private final Library library;
    private final CallingMode callingMode;
    private final ConcurrentMap<Method, InvocationHandler> map = new ConcurrentHashMap<>(4);

    InvocationLibrary(Class<?> interfaceClass, Library library, LoadOptions loadOptions) {
        this.library = library;
        CallingConvention callingConvention = AnnotationUtil.getClassAnnotation(interfaceClass, CallingConvention.class);
        this.callingMode = callingConvention != null ? callingConvention.value() : loadOptions.getCallingMode();
    }

    InvocationHandler findMethodInvoker(Method method) {
        InvocationHandler handler = map.get(method);
        if (handler != null) {
            return handler;
        }
        if (method.isDefault()) {
            if (DefaultMethodInvoker.isAvailable()) {
                handler = DefaultMethodInvoker.getInstance(method);
            } else {
                throw new UnsupportedOperationException("Default method");
            }
        } else if (method.getDeclaringClass() == Object.class) {
            handler = (proxy, m, args) -> {
                InvocationHandler ih = Proxy.getInvocationHandler(proxy);
                switch (m.getName()) {
                    case "equals":
                        Object another = args[0];
                        return proxy == another
                                || another != null
                                && Proxy.isProxyClass(another.getClass())
                                && ih == Proxy.getInvocationHandler(another);
                    case "hashCode":
                        return System.identityHashCode(ih);
                    case "toString":
                        return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(ih));
                }
                throw new AssertionError();
            };
        } else if (method.isVarArgs()) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            String name = method.getName();
            long function = library.dlsym(name);
            InternalType retType = TypeHandlerRegistry.findReturnType(method.getReturnType(), AnnotationUtil.getMethodAnnotation(method, Typedef.class));
            Class<?>[] parameterTypes = method.getParameterTypes();
            Annotation[][] annotations = method.getParameterAnnotations();
            int len = parameterTypes.length;
            InternalType[] ptypes = new InternalType[len];
            Invoker<?> invoker = TypeHandlerRegistry.getInvoker(method.getReturnType());
            @SuppressWarnings("rawtypes")
            ParameterHandler<?>[] handlers = new ParameterHandler[len];
            for (int i = 0; i < len; ++i) {
                Class<?> type = parameterTypes[i];
                Typedef aliasA = AnnotationUtil.getAnnotation(annotations[i], Typedef.class);
                ptypes[i] = TypeHandlerRegistry.findParameterType(type, aliasA);
                handlers[i] = TypeHandlerRegistry.getParameterHandler(type);
            }
            CallingConvention annotation = AnnotationUtil.getMethodAnnotation(method, CallingConvention.class);
            ffi_cif cif = new ffi_cif(annotation != null ? annotation.value() : callingMode, retType, ptypes);
            handler = (Object proxy, Method m, Object[] args) -> {
                @SuppressWarnings("unchecked")
                ParameterHandler<Object>[] h = (ParameterHandler<Object>[]) handlers;
                int length = h.length;
                if (length != 0) {
                    CallContext context = cif.newCallContext();
                    for (int i = 0; i < length; i++) {
                        h[i].handle(context, i, args[i]);
                    }
                    Object result = invoker.invoke(cif.address(), function, context.address());
                    context.finish();
                    return result;
                } else {
                    return invoker.invoke(cif.address(), function, NoParameter.NOMEMORY.address());
                }
            };
        }
        InvocationHandler ih = map.putIfAbsent(method, handler);
        return ih != null ? ih : handler;
    }

    private interface NoParameter {

        Pointer NOMEMORY = AllocatedMemory.allocate(0);

    }

}

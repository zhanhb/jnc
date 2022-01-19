/*
 * Copyright 2019 zhanhb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jnc.provider;

import jnc.foreign.LoadOptions;
import jnc.foreign.annotation.DefaultConvention;
import jnc.foreign.annotation.Stdcall;
import jnc.foreign.enums.CallingConvention;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zhanhb
 */
@SuppressWarnings("unused")
public class InvocationLibraryTest {

    private static Library DUMMY_LIB;
    private static TypeFactory typeFactory;
    private static TypeHandlerFactory THR;

    @BeforeAll
    public static void setUpClass() throws NoSuchMethodException {
        Method dlsym = Library.class.getMethod("dlsym", String.class);
        ProxyBuilder builder = ProxyBuilder.builder();
        DUMMY_LIB = builder.customize(dlsym, (proxy, args) -> 0L)
                .newInstance(Library.class);
        typeFactory = DefaultForeign.INSTANCE.getTypeFactory();
        THR = DefaultForeign.INSTANCE.getTypeHandlerFactory();
    }

    private <T> InvocationLibrary<T> newInvocationLibrary(Class<T> iface, LoadOptions loadOptions) {
        return new InvocationLibrary<>(iface, DUMMY_LIB, loadOptions, typeFactory, THR);
    }

    private <T> void test(Class<T> klass, LoadOptions options, Consumer<Function<String, MethodInvocation>> consumer) {
        InvocationLibrary<T> library = newInvocationLibrary(klass, options);
        consumer.accept(name -> {
            try {
                return library.find(klass.getMethod(name));
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        });
    }

    /**
     * Test of find method, of class InvocationLibrary.
     */
    @Test
    public void testFind() {
        LoadOptions stdcallOptions = LoadOptions.builder().stdcall().build();
        LoadOptions defaultCallingConventionOptions = LoadOptions.builder().convention(CallingConvention.DEFAULT).build();

        {
            test(NoConvention.class, defaultCallingConventionOptions, function -> {
                assertEquals(CallingConvention.STDCALL, function.apply("stdcall").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("cdecl").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("noConvention").getCallingConvention());
            });

            test(StdcallConvention.class, defaultCallingConventionOptions, function -> {
                assertEquals(CallingConvention.STDCALL, function.apply("stdcall").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("cdecl").getCallingConvention());
                assertEquals(CallingConvention.STDCALL, function.apply("noConvention").getCallingConvention());
            });

            test(DefaultConventionLib.class, defaultCallingConventionOptions, function -> {
                assertEquals(CallingConvention.STDCALL, function.apply("stdcall").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("cdecl").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("noConvention").getCallingConvention());
            });
        }

        {
            test(NoConvention.class, stdcallOptions, function -> {
                assertEquals(CallingConvention.STDCALL, function.apply("stdcall").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("cdecl").getCallingConvention());
                assertEquals(CallingConvention.STDCALL, function.apply("noConvention").getCallingConvention());
            });

            test(StdcallConvention.class, stdcallOptions, function -> {
                assertEquals(CallingConvention.STDCALL, function.apply("stdcall").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("cdecl").getCallingConvention());
                assertEquals(CallingConvention.STDCALL, function.apply("noConvention").getCallingConvention());
            });

            test(DefaultConventionLib.class, stdcallOptions, function -> {
                assertEquals(CallingConvention.STDCALL, function.apply("stdcall").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("cdecl").getCallingConvention());
                assertEquals(CallingConvention.DEFAULT, function.apply("noConvention").getCallingConvention());
            });
        }
    }

    private interface NoConvention {

        @Stdcall
        void stdcall();

        @DefaultConvention
        void cdecl();

        void noConvention();

    }

    @DefaultConvention
    private interface DefaultConventionLib {

        @Stdcall
        void stdcall();

        @DefaultConvention
        void cdecl();

        void noConvention();

    }

    @Stdcall
    private interface StdcallConvention {

        @Stdcall
        void stdcall();

        @DefaultConvention
        void cdecl();

        void noConvention();

    }

}

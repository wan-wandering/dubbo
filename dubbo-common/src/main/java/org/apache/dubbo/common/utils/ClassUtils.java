/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;


import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.utils.ArrayUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.ofSet;

public class ClassUtils {
    /**
     * Suffix for array class names: "[]"
     */
    public static final String ARRAY_SUFFIX = "[]";
    /**
     * Simple Types including:
     * <ul>
     *     <li>{@link Void}</li>
     *     <li>{@link Boolean}</li>
     *     <li>{@link Character}</li>
     *     <li>{@link Byte}</li>
     *     <li>{@link Integer}</li>
     *     <li>{@link Float}</li>
     *     <li>{@link Double}</li>
     *     <li>{@link String}</li>
     *     <li>{@link BigDecimal}</li>
     *     <li>{@link BigInteger}</li>
     *     <li>{@link Date}</li>
     *     <li>{@link Object}</li>
     * </ul>
     *
     * @see javax.management.openmbean.SimpleType
     * @since 2.7.6
     */
    public static final Set<Class<?>> SIMPLE_TYPES = ofSet(
            Void.class,
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class,
            BigDecimal.class,
            BigInteger.class,
            Date.class,
            Object.class
    );
    /**
     * Prefix for internal array class names: "[L"
     */
    private static final String INTERNAL_ARRAY_PREFIX = "[L";
    /**
     * Map with primitive type name as key and corresponding primitive type as
     * value, for example: "int" -> "int.class".
     */
    private static final Map<String, Class<?>> PRIMITIVE_TYPE_NAME_MAP = new HashMap<String, Class<?>>(32);
    /**
     * Map with primitive wrapper type as key and corresponding primitive type
     * as value, for example: Integer.class -> int.class.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_TYPE_MAP = new HashMap<Class<?>, Class<?>>(16);
    private static final char PACKAGE_SEPARATOR_CHAR = '.';

    static {
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Character.class, char.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Double.class, double.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Float.class, float.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Long.class, long.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Short.class, short.class);

        Set<Class<?>> primitiveTypeNames = new HashSet<>(32);
        primitiveTypeNames.addAll(PRIMITIVE_WRAPPER_TYPE_MAP.values());
        primitiveTypeNames.addAll(Arrays
                .asList(boolean[].class, byte[].class, char[].class, double[].class,
                        float[].class, int[].class, long[].class, short[].class));
        for (Class<?> primitiveTypeName : primitiveTypeNames) {
            PRIMITIVE_TYPE_NAME_MAP.put(primitiveTypeName.getName(), primitiveTypeName);
        }
    }

    public static Class<?> forNameWithThreadContextClassLoader(String name)
            throws ClassNotFoundException {
        return forName(name, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?> forNameWithCallerClassLoader(String name, Class<?> caller)
            throws ClassNotFoundException {
        return forName(name, caller.getClassLoader());
    }

    public static ClassLoader getCallerClassLoader(Class<?> caller) {
        return caller.getClassLoader();
    }

    /**
     * get class loader
     *
     * @param clazz
     * @return class loader
     */

   /** 一、从Java 虚拟机的角度来讲，只存在两种不同的类加载器：一种是启动类加载器(Bootstrap ClassLoader），
            它C＋＋语言实现，是虚拟机自身的一部分：另一种就是所有其他的类加载器，这些类加载器都出Java 语言实现，独立于虚拟机外部，
            并且全都继承自抽象类java.lang.ClassLoader。
    二、从发开人员的角度来看，类加载器有下面3种。
            （一）、启动类加载器 (BootstrapClassLoader)：存放在<JAVA_HOM/lib>目录中的，或者被－Xbootclasspath 参数所指定的路径中的，并且是虚拟机识别的类库加载到虚拟机内存中（一般是java.lang下的类，和java.io下的类）。启动类加载器无法被Java 程序直接引用，用户在编写自定义类加载器时，如果需要把加载请求委派给引导类加载器，直接使用null 代替。
            （二）、扩展类加载器（ExtensionClassloader）：由sun.misc.Launcher$ExtClassLoader 实现，负责加载＜JAVA_HOME>/lib/ext 目录中的，或者被java.ext.dirs 系统变量所指定的路径中的所有类库，开发者可以直接使用扩展类加器。
            （三）、应用程序类加载器(ApplicationClassLoader)： 这个类加载器由sun.misc.Launcher$AppClassLoader 来实现． 由于这个类加载器是ClassLoader 中的getSystemClassLoader()方法的返回值，所以一般也称它为系统类加载器．负责加载用户类路径经（ ClassPath ）上所指定的类库，
                    开发者可以直接使用这个类加载器，如果应用程序中没有自定义过自己的类加载器，就是程序中默认的类加载器。

    三、双亲委派模型
            类加载器之间的层次关系，称为类加载器的双亲委派模型（Parents Delegation Model）。除了顶层的启动类加载器外，其余的类加载器都有父类加载器．
            工作过程是： 如果一个类加载器收到了类加载的请求，先不会自己去加载这个类，而是把这个请求委派给父类加载器去完成，每一个层次的类加载器都是如此，所有的加载请求最终都会传送到顶层的启动类加载器中，只有当父加器无法加载这个类时，子载加载器才自己去加载。

    四、类加载器的三个特性
            类加载器有三个特性，分别为委派，可见性和单一性，其他文章上对这三个特性的介绍如下：
                * 委托机制是指将加载一个类的请求交给父类加载器，如果这个父类加载器不能够找到或者加载这个类，那么再加载它。
                * 可见性的原理是子类的加载器可以看见所有的父类加载器加载的类，而父类加载器看不到子类加载器加载的类。
                * 单一性原理是指仅加载一个类一次，这是由委托机制确保子类加载器不会再次加载父类加载器加载过的类。
            其中，委派机制是基础，在其他资料中也把这种机制叫做类加载器的双亲委派模型，其实说的是同一个意思。可加性和单一性是依赖于委派机制的。

    五、破坏双亲委派模型
            第一次“被破坏”发生在双亲委派模型出现之前。亲委派模型在JDK 1.2 之后才被引入，类加载器和抽象类Java. lang. ClassLoader 则在JDK 1 .0 时代就已经存在，对已经存在的用户自定义类加载器的实现代码， Java 设计者们引入双亲委派模型时做出了一些妥协。
    第二次“被破坏”是由这个模型自身的缺陷所导致的，双亲委派很好地解决了各个类加载器的基础类的统一问题（越基础的类由越上层的类加载器进行加载），
        基础类之所以被称为“基础”，是因为它们总是作为被用户代码词用的API。但是基础类有时又要调用用户代码，如JNDI。
    此时就引入了线程上下文类加器。
            第三次“被破坏”是由于用户对程序动态性的追求而导致的。代码热替换（HotSwap）、模块热部署（Hot Deployment）等*/
    public static ClassLoader getClassLoader(Class<?> clazz) {
        /*类的加载器有两种
            1、Java虚拟机自带的加载器
            2、 用户自定义类加载器
        而Java虚拟机自带的加载器又包括3种类加载器
            根类加载器（Bootstrap）
            扩展类加载器（Extension）
            系统类加载器（Ststem）
        系统类加载器又称为应用类加载器
        其中扩展类加载器和系统类加载器是使用Java实现的。而根加载器是使用C++实现的，JVM的API也没有暴露根类加载器，程序员无法在Java代码中获取根加载器。
        用户自定义类加载器是用户自己写的类加载器，但是必须继承java.lang.ClassLoader这个类，用户可以自定义类的加载方式！
        */

        /*补充知识点：
        而线程上下文类加载器破坏了“双亲委派模型”，可以在执行线程中抛弃双亲委派加载链模式，使程序可以逆向使用类加载器。
　　     线程上下文类加载器（context class loader）是从 JDK 1.2 开始引入的。
        类 java.lang.Thread中的方法 getContextClassLoader()和 setContextClassLoader(ClassLoader cl)用来获取和设置线程的上下文类加载器。
        如果没有通过 setContextClassLoader(ClassLoader cl)方法进行设置的话，线程将继承其父线程的上下文类加载器。
        Java 应用运行的初始线程的上下文类加载器是系统类加载器。在线程中运行的代码可以通过此类加载器来加载类和资源。
        */
        ClassLoader cl = null;
        try {//线程上下文加载器
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {//自定义类加载器
            // No thread context class loader -> use class loader of this class.
            cl = clazz.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {//系统类加载器\applicationClassLoader
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                            /**for author:  I can't live with null! ---HanLei */
                }
            }
        }

        return cl;
    }

    /**
     * Return the default ClassLoader to use: typically the thread context
     * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
     * class will be used as fallback.
     * <p>
     * Call this method if you intend to use the thread context ClassLoader in a
     * scenario where you absolutely need a non-null ClassLoader reference: for
     * example, for class path resource loading (but not necessarily for
     * <code>Class.forName</code>, which accepts a <code>null</code> ClassLoader
     * reference as well).
     *
     * @return the default ClassLoader (never <code>null</code>)
     * @see java.lang.Thread#getContextClassLoader()
     */
    public static ClassLoader getClassLoader() {
        return getClassLoader(ClassUtils.class);
    }

    /**
     * Same as <code>Class.forName()</code>, except that it works for primitive
     * types.
     */
    public static Class<?> forName(String name) throws ClassNotFoundException {
        return forName(name, getClassLoader());
    }

    /**
     * Replacement for <code>Class.forName()</code> that also returns Class
     * instances for primitives (like "int") and array class names (like
     * "String[]").
     *
     * @param name        the name of the Class
     * @param classLoader the class loader to use (may be <code>null</code>,
     *                    which indicates the default class loader)
     * @return Class instance for the supplied name
     * @throws ClassNotFoundException if the class was not found
     * @throws LinkageError           if the class file could not be loaded
     * @see Class#forName(String, boolean, ClassLoader)
     */
    public static Class<?> forName(String name, ClassLoader classLoader)
            throws ClassNotFoundException, LinkageError {

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz != null) {
            return clazz;
        }

        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[Ljava.lang.String;" style arrays
        int internalArrayMarker = name.indexOf(INTERNAL_ARRAY_PREFIX);
        if (internalArrayMarker != -1 && name.endsWith(";")) {
            String elementClassName = null;
            if (internalArrayMarker == 0) {
                elementClassName = name
                        .substring(INTERNAL_ARRAY_PREFIX.length(), name.length() - 1);
            } else if (name.startsWith("[")) {
                elementClassName = name.substring(1);
            }
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = getClassLoader();
        }
        return classLoaderToUse.loadClass(name);
    }

    /**
     * Resolve the given class name as primitive class, if appropriate,
     * according to the JVM's naming rules for primitive classes.
     * <p>
     * Also supports the JVM's internal class names for primitive arrays. Does
     * <i>not</i> support the "[]" suffix notation for primitive arrays; this is
     * only supported by {@link #forName}.
     *
     * @param name the name of the potentially primitive class
     * @return the primitive class, or <code>null</code> if the name does not
     * denote a primitive class or primitive array class
     */
    public static Class<?> resolvePrimitiveClassName(String name) {
        Class<?> result = null;
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if (name != null && name.length() <= 8) {
            // Could be a primitive - likely.
            result = (Class<?>) PRIMITIVE_TYPE_NAME_MAP.get(name);
        }
        return result;
    }

    public static String toShortString(Object obj) {
        if (obj == null) {
            return "null";
        }
        return obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj);

    }

    public static String simpleClassName(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        String className = clazz.getName();
        final int lastDotIdx = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR);
        if (lastDotIdx > -1) {
            return className.substring(lastDotIdx + 1);
        }
        return className;
    }


    /**
     * The specified type is primitive type or simple type
     *
     * @param type the type to test
     * @return
     * @deprecated as 2.7.6, use {@link Class#isPrimitive()} plus {@link #isSimpleType(Class)} instead
     */
    public static boolean isPrimitive(Class<?> type) {
        return type != null && (type.isPrimitive() || isSimpleType(type));
    }

    /**
     * The specified type is simple type or not
     *
     * @param type the type to test
     * @return if <code>type</code> is one element of {@link #SIMPLE_TYPES}, return <code>true</code>, or <code>false</code>
     * @see #SIMPLE_TYPES
     * @since 2.7.6
     */
    public static boolean isSimpleType(Class<?> type) {
        return SIMPLE_TYPES.contains(type);
    }

    public static Object convertPrimitive(Class<?> type, String value) {
        if (value == null) {
            return null;
        } else if (type == char.class || type == Character.class) {
            return value.length() > 0 ? value.charAt(0) : '\0';
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(value);
        }
        try {
            if (type == byte.class || type == Byte.class) {
                return Byte.valueOf(value);
            } else if (type == short.class || type == Short.class) {
                return Short.valueOf(value);
            } else if (type == int.class || type == Integer.class) {
                return Integer.valueOf(value);
            } else if (type == long.class || type == Long.class) {
                return Long.valueOf(value);
            } else if (type == float.class || type == Float.class) {
                return Float.valueOf(value);
            } else if (type == double.class || type == Double.class) {
                return Double.valueOf(value);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return value;
    }


    /**
     * We only check boolean value at this moment.
     *
     * @param type
     * @param value
     * @return
     */
    public static boolean isTypeMatch(Class<?> type, String value) {
        if ((type == boolean.class || type == Boolean.class)
                && !("true".equals(value) || "false".equals(value))) {
            return false;
        }
        return true;
    }

    /**
     * Get all super classes from the specified type
     *
     * @param type         the specified type
     * @param classFilters the filters for classes
     * @return non-null read-only {@link Set}
     * @since 2.7.6
     */
    public static Set<Class<?>> getAllSuperClasses(Class<?> type, Predicate<Class<?>>... classFilters) {

        Set<Class<?>> allSuperClasses = new LinkedHashSet<>();

        Class<?> superClass = type.getSuperclass();
        while (superClass != null) {
            // add current super class
            allSuperClasses.add(superClass);
            superClass = superClass.getSuperclass();
        }

        return unmodifiableSet(filterAll(allSuperClasses, classFilters));
    }

    /**
     * Get all interfaces from the specified type
     *
     * @param type             the specified type
     * @param interfaceFilters the filters for interfaces
     * @return non-null read-only {@link Set}
     * @since 2.7.6
     */
    public static Set<Class<?>> getAllInterfaces(Class<?> type, Predicate<Class<?>>... interfaceFilters) {
        if (type == null || type.isPrimitive()) {
            return emptySet();
        }

        Set<Class<?>> allInterfaces = new LinkedHashSet<>();
        Set<Class<?>> resolved = new LinkedHashSet<>();
        Queue<Class<?>> waitResolve = new LinkedList<>();

        resolved.add(type);
        Class<?> clazz = type;
        while (clazz != null) {

            Class<?>[] interfaces = clazz.getInterfaces();

            if (isNotEmpty(interfaces)) {
                // add current interfaces
                Arrays.stream(interfaces)
                        .filter(resolved::add)
                        .forEach(cls -> {
                            allInterfaces.add(cls);
                            waitResolve.add(cls);
                        });
            }

            // add all super classes to waitResolve
            getAllSuperClasses(clazz)
                    .stream()
                    .filter(resolved::add)
                    .forEach(waitResolve::add);

            clazz = waitResolve.poll();
        }

        return filterAll(allInterfaces, interfaceFilters);
    }

    /**
     * Get all inherited types from the specified type
     *
     * @param type        the specified type
     * @param typeFilters the filters for types
     * @return non-null read-only {@link Set}
     * @since 2.7.6
     */
    public static Set<Class<?>> getAllInheritedTypes(Class<?> type, Predicate<Class<?>>... typeFilters) {
        // Add all super classes
        Set<Class<?>> types = new LinkedHashSet<>(getAllSuperClasses(type, typeFilters));
        // Add all interface classes
        types.addAll(getAllInterfaces(type, typeFilters));
        return unmodifiableSet(types);
    }


    /**
     * the semantics is same as {@link Class#isAssignableFrom(Class)}
     *
     * @param superType  the super type
     * @param targetType the target type
     * @return see {@link Class#isAssignableFrom(Class)}
     * @since 2.7.6
     */
    public static boolean isAssignableFrom(Class<?> superType, Class<?> targetType) {
        // any argument is null
        if (superType == null || targetType == null) {
            return false;
        }
        // equals
        if (Objects.equals(superType, targetType)) {
            return true;
        }
        // isAssignableFrom
        return superType.isAssignableFrom(targetType);
    }

    /**
     * Test the specified class name is present in the {@link ClassLoader}
     *
     * @param className   the name of {@link Class}
     * @param classLoader {@link ClassLoader}
     * @return If found, return <code>true</code>
     * @since 2.7.6
     */
    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            forName(className, classLoader);
        } catch (Throwable ignored) { // Ignored
            return false;
        }
        return true;
    }

    /**
     * Resolve the {@link Class} by the specified name and {@link ClassLoader}
     *
     * @param className   the name of {@link Class}
     * @param classLoader {@link ClassLoader}
     * @return If can't be resolved , return <code>null</code>
     * @since 2.7.6
     */
    public static Class<?> resolveClass(String className, ClassLoader classLoader) {
        Class<?> targetClass = null;
        try {
            targetClass = forName(className, classLoader);
        } catch (Throwable ignored) { // Ignored
        }
        return targetClass;
    }

    /**
     * Is generic class or not?
     *
     * @param type the target type
     * @return if the target type is not null or <code>void</code> or Void.class, return <code>true</code>, or false
     * @since 2.7.6
     */
    public static boolean isGenericClass(Class<?> type) {
        return type != null && !void.class.equals(type) && !Void.class.equals(type);
    }
}

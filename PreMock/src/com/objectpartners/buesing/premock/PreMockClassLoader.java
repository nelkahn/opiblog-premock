package com.objectpartners.buesing.premock;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PreMockClassLoader extends ClassLoader {

    private static List<String> classes = new ArrayList<String>();

    private final ClassPool pool;

    public static void setClasses(final List<String> set) {
        // Add the classes to the List, have to be added or only the last
        // loaded PreMock will be taken into account
        classes.addAll(set);
    }

    public PreMockClassLoader(final ClassLoader parent) {
        super(parent);
        this.pool = ClassPool.getDefault();

        /* When loaded inside the ant classloader the classpath does not get passed through the ClassPool
         * so it is necessary to exract the classpath elements from the classloader and add to the ClassPool.
         */
        String classLoaderString = this.pool.getClassLoader().toString();
        if (classLoaderString.startsWith("AntClassLoader")) {

            // extract semicolon separated list which is between []
            int startIndex = classLoaderString.indexOf('[');
            startIndex++;
            final int endIndex = classLoaderString.indexOf(']');
            classLoaderString = classLoaderString.substring(startIndex, endIndex);

            final String[] pathItems = classLoaderString.split(";");

            // add extracted path elements to the ClassPool
            try {
                for (final String pathItem : pathItems) {
                    this.pool.appendClassPath(pathItem);
                }
            } catch (final NotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        // no mater what, do not allow certain classes to be loaded by this
        // class loader. change this as you see fit (and are able to).
        if (name.startsWith("java.")) {
            return super.loadClass(name);
        } else if (name.startsWith("javax.")) {
            return super.loadClass(name);
        } else if (name.startsWith("sun.")) {
            return super.loadClass(name);
        } else if (name.startsWith("org.junit.")) {
            return super.loadClass(name);
        } else if (name.startsWith("org.mockito.")) {
            return super.loadClass(name);
        } else if (name.startsWith("com.objectpartners.buesing.premock.")) {
            return super.loadClass(name);
        } else {
            if (classes.contains(name)) {
                // only load the classes specified with the class loader,
                // otherwise leave it up to the parent.
                return findClass(name);
            } else {
                return findClassNoModify(name);
//                return super.loadClass(name);
            }
        }
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
//            System.err.println("[PreMockClassLoader] findClass - Removing final modifiers for :: " + name);
            final CtClass cc = this.pool.get(name);

            // remove final modifier from the class
            if (Modifier.isFinal(cc.getModifiers())) {
                cc.setModifiers(cc.getModifiers() & ~Modifier.FINAL);
            }

            // remove final modifiers from all methods
            final CtMethod[] methods = cc.getDeclaredMethods();
            for (final CtMethod method : methods) {
                if (Modifier.isFinal(method.getModifiers())) {
                    method.setModifiers(method.getModifiers() & ~Modifier.FINAL);
                }
            }

            final byte[] b = cc.toBytecode();

            final Class<?> result = defineClass(name, b, 0, b.length);

            return result;
        } catch (final NotFoundException e) {
            throw new ClassNotFoundException("NotFoundException : " + name + " :: " + this.pool, e);
        } catch (final IOException e) {
            throw new ClassNotFoundException("IOException : " + name, e);
        } catch (final CannotCompileException e) {
            throw new ClassNotFoundException("CannotCompileException : " + name, e);
        }
    }

    private Class<?> findClassNoModify(final String name) throws ClassNotFoundException {
        try {
//            System.err.println("[PreMockClassLoader] findClass - Loading without change :: " + name);
            final CtClass cc = this.pool.get(name);

            final byte[] b = cc.toBytecode();

            final Class<?> result = defineClass(name, b, 0, b.length);

            return result;
        } catch (final NotFoundException e) {
            throw new ClassNotFoundException("NotFoundException : " + name + " :: " + this.pool, e);
        } catch (final IOException e) {
            throw new ClassNotFoundException("IOException : " + name, e);
        } catch (final CannotCompileException e) {
            throw new ClassNotFoundException("CannotCompileException : " + name, e);
        }
    }
}

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
            throw new ClassNotFoundException();
        } catch (final IOException e) {
            throw new ClassNotFoundException();
        } catch (final CannotCompileException e) {
            throw new ClassNotFoundException();
        }
    }

    private Class<?> findClassNoModify(final String name) throws ClassNotFoundException {
        try {
//            System.err.println("[PreMockClassLoader] findClass - Removing final modifiers for :: " + name);
            final CtClass cc = this.pool.get(name);

            final byte[] b = cc.toBytecode();

            final Class<?> result = defineClass(name, b, 0, b.length);

            return result;
        } catch (final NotFoundException e) {
            throw new ClassNotFoundException();
        } catch (final IOException e) {
            throw new ClassNotFoundException();
        } catch (final CannotCompileException e) {
            throw new ClassNotFoundException();
        }
    }
}

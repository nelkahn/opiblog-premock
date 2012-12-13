package com.objectpartners.buesing.premock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.mockito.MockitoAnnotations;

public class PreMockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    private static final ClassLoader classLoader = new PreMockClassLoader(PreMockJUnit4ClassRunner.class.getClassLoader());

    public PreMockJUnit4ClassRunner(final Class<?> unitTestClass) throws InitializationError {
        super(unitTestClass);

        PreMockClassLoader.setClasses(getClassesToAlter(unitTestClass));

        try {
            // Important, we must load our test class with our class loader,
            // and then replace the TestClass in the BaseJUnit4ClassRunner,
            // with our own.
            final Class<?> clazz = classLoader.loadClass(unitTestClass.getName());
            final TestClass testClass = new TestClass(clazz);
            replaceTestClass(testClass);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("ClassNotFoundException for name : " + unitTestClass.getName(), e);
        }
    }

    private List<String> getClassesToAlter(final Class<?> unitTestClass) {
        final List<String> set = new ArrayList<String>();

        // The actual JUnit test must be loaded with the UnitTestClassLoader
        set.add(unitTestClass.getName());

        final PreMock annotation = unitTestClass.getAnnotation(PreMock.class);

        if (annotation != null) {
            for (final Class<?> c : annotation.value()) {
                set.add(c.getName());
            }
        }

        return set;
    }

    /**
     * This takes the test class and replaces it in the BlockJUnit4ClassRunner.
     * This is to allow a TestClass to be constructed with a class loaded by our
     * class loader, instead of the default class loader. This way any class
     * referenced by our unit test can be loaded by our class loader.
     */
    private void replaceTestClass(final TestClass testClass) {
        // Since member fTestClass, and method validate are private in
        // BlockJunit4ClassRunner, using reflection to replace fTestClass
        // with one loaded by our class-loader. We need to then call
        // validate to mimic behavior done by the construction of
        // BlockJunit4ClassRunner.
        //
        // This is done avoid duplicating code in BlockJunit4ClassRunner.
        // If you wanted to be less tricking or prone to future changes in
        // BlockJunit4ClassRunner, you would extend ParentRunner and
        // redo/duplicate what BlockJunit4ClassRunner is doing.
        try {
            for (final Field field : ParentRunner.class.getDeclaredFields()) {
                if ("fTestClass".equals(field.getName())) {
                    field.setAccessible(true);
                    field.set(this, testClass);
                    break;
                }
            }

            for (final Method method : ParentRunner.class.getDeclaredMethods()) {
                if ("validate".equals(method.getName())) {
                    method.setAccessible(true);
                    method.invoke(this, new Object[] {});
                    break;
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Borrowed from MockitoJUnitRunner so @Mock and @InjectMocks will work.
    @SuppressWarnings("deprecation")
    @Override
    protected Statement withBefores(final FrameworkMethod method, final Object target, final Statement statement) {
        MockitoAnnotations.initMocks(target);
        return super.withBefores(method, target, statement);
    }

}

package org.mockito.configuration;

/**
 * 
 * Added to solve the bug :
 * org.mockito.exceptions.base.MockitoException:
 * ClassCastException occurred when creating the proxy.
 * You might experience classloading issues, disabling the Objenesis cache *might* help (see MockitoConfiguration)
 * 
 */
public class MockitoConfiguration extends DefaultMockitoConfiguration {

    @Override
    public boolean enableClassCache() {
        return false;
    }

}

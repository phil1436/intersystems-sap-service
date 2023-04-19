package com.intersystems.dach.sap.utils;

import java.util.HashMap;
import java.util.Properties;

import com.sap.conn.jco.ext.DataProviderException;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;

/**
 * Server Data provider implementation for SAP JCo.
 * 
 * @author Philipp Bonin, Andreas Schütz
 * @version 1.0
 * 
 */
public class ServerDataProviderImpl implements ServerDataProvider{
    /**
     * From these properties all necessary destination
     * data are gathered.
     */
    private static ServerDataProviderImpl singletonInstance = null;

    private HashMap<String,Properties> serverProperties = new HashMap<String, Properties>();
    private ServerDataEventListener serverDataEventListener = null;

    // Make this a singleton class
    private ServerDataProviderImpl() { }

    /**
     * Registers the server data handler instance with the SAP JCo environment.
     * @return
     */
    public static boolean tryRegister() {
        TraceManager.traceMessage("1");
        if (!Environment.isServerDataProviderRegistered()) {
            TraceManager.traceMessage("2");
            if (singletonInstance == null) {
                TraceManager.traceMessage("3");
                singletonInstance = new ServerDataProviderImpl();
            }
            Environment.registerServerDataProvider(singletonInstance);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Unregisters the server data handler instance from the SAP JCo environment.
     * @return True if the handler was unregistered, false if not.
     */
    public static boolean unregister() {
        if (Environment.isServerDataProviderRegistered()) {
            Environment.unregisterServerDataProvider(singletonInstance);
            while (Environment.isServerDataProviderRegistered()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { }
            }
            return true;
        }
        return false;
    }

    /**
     * Adds properties for the specified server name to the data provider.
     * @param serverName server name whose properties are to be removed from the provider.
     * @param properties properties to be added to this dat provider.
     * @throws IllegalStateException thrown if data provider has not been initialized or properties for the specified server name aleady exist.
     */
    public static void setProperties(String serverName, Properties properties) throws IllegalStateException{
        TraceManager.traceMessage("4");

        if (singletonInstance == null) {
            throw new IllegalStateException("ServerDataHandler is not initialized.");
        }

        if (singletonInstance.serverProperties.containsKey(serverName)) {
            throw new IllegalStateException("Properties for server '" + serverName + "' already set.");
        }
        TraceManager.traceMessage("5");
        singletonInstance.serverProperties.put(serverName, properties);
        TraceManager.traceMessage("6");
        if (singletonInstance.serverDataEventListener != null) {
            singletonInstance.serverDataEventListener.updated(serverName);
        }
    }

    /**
     * Removes properties for the specified server name from the data provider if present.
     * @param serverName server names whose properties are to be removed from the provider.
     * @return the previous properties associated with server name, or null if there were no properties for the server name.
     * @throws IllegalStateException thrown if data provider has not been initialized or properties for the specified server name aleady exist.
     */
    public static Properties deleteProperties(String serverName) throws IllegalStateException {
        if (singletonInstance == null) {
            throw new IllegalStateException("ServerDataHandler is not initialized.");
        }

        Properties properties = singletonInstance.serverProperties.remove(serverName);
        if (singletonInstance.serverDataEventListener != null) {
            singletonInstance.serverDataEventListener.deleted(serverName);
        }
        if (singletonInstance.serverProperties.isEmpty()) {
            unregister();
        }
        return properties;
    }


    @Override
    public Properties getServerProperties(String serverName) throws DataProviderException {
        TraceManager.traceMessage("ServerDataProvider getServerProperties called with param:" + serverName);
        if (serverProperties.containsKey(serverName)) {
            return serverProperties.get(serverName);
        } else {
            throw new DataProviderException(DataProviderException.Reason.INTERNAL_ERROR, new IllegalArgumentException("Server data for '" + serverName + "' not found."));
        }  
    }

    @Override
    public void setServerDataEventListener(ServerDataEventListener listener) {
        serverDataEventListener = listener;
     }

    @Override
    public boolean supportsEvents() {
        return true;
    }
    
}

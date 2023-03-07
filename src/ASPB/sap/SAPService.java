package ASPB.sap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.intersystems.enslib.pex.ClassMetadata;
import com.intersystems.enslib.pex.FieldMetadata;
import com.intersystems.gateway.GatewayContext;
import com.intersystems.jdbc.IRIS;
import com.intersystems.jdbc.IRISObject;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;

import ASPB.tests.TestServer;
import ASPB.utils.Buffer;
import ASPB.utils.Logger;
import ASPB.utils.ServiceManager;
import ASPB.utils.annotations.JCOPropertyAnnotation;
import ASPB.utils.annotations.NotForRealUse;
import ASPB.utils.interfaces.Callback;
import ASPB.utils.interfaces.MyServer;
import ASPB.utils.interfaces.MyService;

/**
 * A Service to receive messages from a SAP system.
 * 
 * @author Philipp Bonin
 * @version 1.0
 */
@NotForRealUse
@ClassMetadata(Description = "A Service to receive messages from a SAP system.", InfoURL = "https://github.com/phil1436/intersystems-sap-service")
public class SAPService extends com.intersystems.enslib.pex.BusinessService
        implements Callback<String>, MyService {

    /**
     * *****************************
     * *** Service configuration ***
     * *****************************
     */

    // SAP Service
    @FieldMetadata(Category = "SAP Service Settings", IsRequired = true, Description = "REQUIRED<br>Set the buisness partner in the production. The outgoing messages will be directed here.")
    public static String BusinessPartner = "";

    @FieldMetadata(Category = "SAP Service Settings", Description = "If enabled the service will return a JSON object instead of a XML object")
    public static boolean ToJSON = false;

    @FieldMetadata(Category = "SAP Service Settings", Description = "Set the burst size. If the buffer size is greater than the burst size, the burst size will be used. If the burst size is set to 0, the buffer size will be used.")
    public static int BurstSize = 10;

    @FieldMetadata(Category = "SAP Service Settings", Description = "Set the maximum buffer size. It descripes how many elements can be added to the buffer. If the maximum buffer size is set to 0, the buffer size will not be limited.")
    public static int MaxBufferSize = 100;

    // Server Connection
    @JCOPropertyAnnotation(jcoName = ServerDataProvider.JCO_GWHOST)
    @FieldMetadata(Category = "SAP Server Connection", IsRequired = true, Description = "REQUIRED<br>Set the gateway host address. The gateway host address is used to connect to the SAP system.")
    public static String GatewayHost = "";

    @JCOPropertyAnnotation(jcoName = ServerDataProvider.JCO_GWSERV)
    @FieldMetadata(Category = "SAP Server Connection", IsRequired = true, Description = "REQUIRED<br>Set the gateway service. The gateway service is used to connect to the SAP system. Usually 'sapgwNN' whereas NN is the instance number.")
    public static String GatewayService = "";

    @JCOPropertyAnnotation(jcoName = ServerDataProvider.JCO_PROGID)
    @FieldMetadata(Category = "SAP Server Connection", IsRequired = true, Description = "REQUIRED<br>Set the programm ID. The programm ID is used to identify the service in the SAP system.")
    public static String ProgrammID = "";

    @JCOPropertyAnnotation(jcoName = ServerDataProvider.JCO_CONNECTION_COUNT)
    @FieldMetadata(Category = "SAP Server Connection", IsRequired = true, Description = "REQUIRED<br>Set the connection count. The connection count is used to connect to the SAP system.")
    public static int ConnectionCount = 1;

    @JCOPropertyAnnotation(jcoName = ServerDataProvider.JCO_REP_DEST)
    @FieldMetadata(Category = "SAP Server Connection", Description = "Set the repository destination. The repository destination is used to connect to the SAP system. Usually 'SAP' or 'SAP_TEST")
    public static String Repository = "";

    // Client Connection
    @JCOPropertyAnnotation(jcoName = DestinationDataProvider.JCO_ASHOST)
    @FieldMetadata(Category = "SAP Client Connection", IsRequired = true, Description = "REQUIRED<br>Set the host address. The host address is used to connect to the SAP system.")
    public static String HostAddress = "";

    @JCOPropertyAnnotation(jcoName = DestinationDataProvider.JCO_CLIENT)
    @FieldMetadata(Category = "SAP Client Connection", IsRequired = true, Description = "REQUIRED<br>Set the client ID. The client ID is used to connect to the SAP system.")
    public static String ClientID = "";

    @JCOPropertyAnnotation(jcoName = DestinationDataProvider.JCO_SYSNR)
    @FieldMetadata(Category = "SAP Client Connection", IsRequired = true, Description = "REQUIRED<br>Set the system number. The system number is used to connect to the SAP system.")
    public static String SystemNumber = "";

    @JCOPropertyAnnotation(jcoName = DestinationDataProvider.JCO_USER)
    @FieldMetadata(Category = "SAP Client Connection", IsRequired = true, Description = "REQUIRED<br>Set the username. The username is used to connect to the SAP system.")
    public static String Username = "";

    @JCOPropertyAnnotation(jcoName = DestinationDataProvider.JCO_PASSWD)
    @FieldMetadata(Category = "SAP Client Connection", IsRequired = true, Description = "REQUIRED<br>Set the password. The password is used to connect to the SAP system.")
    public static String Password = "";

    @JCOPropertyAnnotation(jcoName = DestinationDataProvider.JCO_LANG)
    @FieldMetadata(Category = "SAP Client Connection", IsRequired = true, Description = "REQUIRED<br>Set the language. The language is used to connect to the SAP system.")
    public static String Language = "";

    // Logging
    @FieldMetadata(Category = "Logging", Description = "Set the log file path. If the log file path is set to an empty string, there will be no log file created. The log file path must refer to an already existing file.")
    public static String LogFilePath = "";

    @FieldMetadata(Category = "Logging", Description = "If enabled the log file will be cleared on restart.")
    public static boolean ClearLogOnRestart = false;

    /**
     * ******************
     * *** Attributes ***
     * ******************
     */

    // The buffer
    private static Buffer<String> buffer = new Buffer<String>(MaxBufferSize);

    // The iris connection
    private IRIS iris;

    // The server
    private MyServer server;

    @Override
    public void OnInit() throws Exception {
        // register at the ServiceManager
        ServiceManager.registerService(this);

        // get iris connection
        iris = GatewayContext.getIRIS();

        // set the log file path
        Logger.setFilePath(LogFilePath);
        // clear the log file
        if (ClearLogOnRestart)
            Logger.clear();

        // start the server
        server = new TestServer();
        server.registerCallback(this);

        // Set connection properties
        this.setJcoProperties();

        if (!server.checkIfAllPropertiesAreSet()) {
            Logger.error("SAPService could not be started");
            LOGERROR("SAPService could not be started");
            throw new RuntimeException();
        }

        if (server.start()) {
            Logger.log("SAPService started");
            LOGINFO("SAPService started");
        } else {
            Logger.error("SAPService could not be started");
            LOGERROR("SAPService could not be started");
        }
    }

    @Override
    public Object OnProcessInput(Object msg) throws Exception {
        // set the burst size
        int burst = buffer.size();
        if (burst > BurstSize && BurstSize > 0)
            burst = BurstSize;

        boolean result = true;

        // send
        for (int i = 0; i < burst; i++) {
            String s = buffer.poll();
            IRISObject request = (IRISObject) iris.classMethodObject("Ens.StringRequest", "%New",
                    s + "/" + buffer.size());
            try {
                this.SendRequestAsync(SAPService.BusinessPartner, request);
            } catch (Exception e) {
                Logger.error("Error while sending request: " + e.getMessage());
                result = false;
            }
        }

        return result;
    }

    @Override
    public void OnTearDown() throws Exception {

        // TODO
        // was passiert mit dem buffer wenn der service beendet wird???
        // buffer geht verloren!!!
        if (server.stop()) {
            Logger.log("SAPService stopped");
            LOGINFO("SAPService stopped");
        } else {
            Logger.log("SAPService could not be stopped");
            LOGERROR("SAPService could not be stopped");
        }

        iris.close();
        ServiceManager.unregisterService();
    }

    @Override
    public boolean call(String arg0) {
        return buffer.add(arg0);
    }

    /**
     * Add all attributes with the {@link JCOPropertyAnnotation} to the server
     */
    private void setJcoProperties() {

        if (server == null)
            return;

        for (Field f : this.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(JCOPropertyAnnotation.class)) {
                JCOPropertyAnnotation jcoProperty = f.getAnnotation(JCOPropertyAnnotation.class);
                try {
                    if (f.get(this) != null)
                        server.setProperty(jcoProperty.jcoName(), f.get(this).toString());
                } catch (Exception e) {
                    Logger.error("Error while setting JCO property: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public IRIS getConnection() {
        return iris;
    }

    @Override
    public Field getSetting(String name) {
        for (Field f : this.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(FieldMetadata.class)) {
                if (f.getName().equals(name))
                    return f;
            }
        }
        return null;
    }

    @Override
    public Field[] getAllSettings() {
        List<Field> fields = new ArrayList<Field>();
        for (Field f : this.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(FieldMetadata.class)) {
                fields.add(f);
            }
        }
        return fields.toArray(new Field[fields.size()]);
    }

    @Override
    public void logInfo(String message) {
        LOGINFO(message);
    }

    @Override
    public void logError(String message) {
        LOGERROR(message);
    }

    public void dispatchOnInit(com.intersystems.jdbc.IRISObject hostObject) throws java.lang.Exception {
        _dispatchOnInit(hostObject);
    }

}

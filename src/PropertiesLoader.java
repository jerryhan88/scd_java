import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class PropertiesLoader {
    public static Properties properties;

    public PropertiesLoader(File f) {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(f));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public String getProperty(String propName) {
        return properties.get(propName).toString().trim();
    }
}

package j2arduino.util;

import java.io.*;
import java.util.Properties;

public class J2ArduinoSettings{

/** Name of the properties file where different settings like selected kinds are stored. */
public static final String J2ARDUINO_PROPERTIES = "j2arduino.properties";

private final static Properties properties;

private J2ArduinoSettings(){
}

static{
	properties = loadProperties();
}

private static Properties loadProperties(){
	return loadPropertiesFromFile(J2ARDUINO_PROPERTIES);
}

/**
 Loads a ".properties"-file and returns a Property loaded with the content of that file.
 <p/>
 It first tries to deduce the name of the file to load from 3 sources:
 <ul>
 <li>First it tries to get an environment variable named {@code name.toUpperCase().replace('.', '_');}.</li>
 <li>Second it tries to get a system property named {@code name}.</li>
 <li>Finally it just uses {@code name} as the name of the file.</li>
 </ul>

 @param name see above.
 @return a Property loaded with the contents of the file or null if it could not be found. */
public static Properties loadPropertiesFromFile(String name){
	String fileName = System.getenv(getEnvFromProperty(name));
	if(fileName == null){
		fileName = System.getProperty(name);
		if(fileName == null){
			// default
			fileName = name;
		}
	}

	// load the properties file
	InputStream stream = J2ArduinoSettings.class.getClassLoader().getResourceAsStream(fileName);
	if(stream == null){
		return null;
	}

	try{
		Properties properties = new Properties();
		properties.load(stream);
		if(properties.isEmpty())
			return null;
		else
			return properties;
	} catch(IOException e){
		throw new RuntimeException("Error while reading configuration file '" + fileName + '\'', e);
	} finally{
		try{
			stream.close();
		} catch(IOException e){
			// ignore
		}
	}
}

private static String getEnvFromProperty(String propertyName){
	return propertyName.toUpperCase().replace('.', '_');
}

/**
 Returns the property value corresponding to the key given.
 <p/>
 The method tries to retrieve the value from different sources, namely:
 <lu>
 <li>Environment variables (after replacing '.' in the key with '_' and converting it to upper case,</li>
 <li>System properties,</li>
 <li>Properties loaded by the static initializer.</li>
 </lu>
 */
public static String getSetting(String key){
	return getSetting(key, null);
}

/**
 Returns the property value corresponding to the key given.
 <p/>
 The method tries to retrieve the value from different sources, namely:
 <lu>
 <li>Environment variables (after replacing '.' in the key with '_' and converting it to upper case,</li>
 <li>System properties,</li>
 <li>Properties loaded by the static initializer.</li>
 </lu>
 */
public static String getSetting(String key, String defaultValue){
	String s = null;

	try{
		s = System.getenv(getEnvFromProperty(key));
	} catch(SecurityException e){
		// ignore
	}
	if(s != null)
		return s;

	try{
		s = System.getProperty(key);
	} catch(SecurityException e){
		// ignore
	}
	if(s != null)
		return s;

	if(properties != null)
		s = properties.getProperty(key);
	if(s != null)
		return s;

	return defaultValue;
}

/**
 Adds a key:value pair to the settings.
 <p/>
 If a setting with the given key exists, it will be overwritten.

 @param key   The key.
 @param value They value.
 @return if there was a previous setting with the given key. */
public static boolean addSetting(String key, String value){
	return properties.setProperty(key, value) != null;
}

public static boolean containsSetting(String key){
	return getSetting(key) != null;
}
}

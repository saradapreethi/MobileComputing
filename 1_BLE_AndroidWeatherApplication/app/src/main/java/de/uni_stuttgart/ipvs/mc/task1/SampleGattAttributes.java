package de.uni_stuttgart.ipvs.mc.task1;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes
{
    private static HashMap<String, String> attributes = new HashMap();
    public static String TEMPERATURE_MEASUREMENT = "00002A1C-0000-1000-8000-00805F9B34FB";
    public static String HUMIDITY = "00002A6F-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805F9B34FB";
    static
    {
        // BLE Weather Services.
        attributes.put("00000002-0000-0000-FDFD-FDFDFDFDFDFD", "BLE Weather Service");
        // BLE Weather Characteristics.
        attributes.put(TEMPERATURE_MEASUREMENT, "Temperature Measurement");
        attributes.put(HUMIDITY, "Humidity");
    }

    public static String lookup(String uuid, String defaultName)
    {
        String name = attributes.get(uuid.toUpperCase());
        return name == null ? defaultName : name;
    }
}

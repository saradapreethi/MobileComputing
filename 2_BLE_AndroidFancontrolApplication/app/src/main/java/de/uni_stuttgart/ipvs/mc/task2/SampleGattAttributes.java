package de.uni_stuttgart.ipvs.mc.task2;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes
{
    private static HashMap<String, String> attributes = new HashMap();
    public static String INTENSITY_MEASURE = "10000001-0000-0000-fdfd-fdfdfdfdfdfd";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static
    {
        // BLE Fan Control Services.
        attributes.put("00000001-0000-0000-fdfd-fdfdfdfdfdfd", "BLE Fan Control Service");
        // BLE Weather Characteristics.
        attributes.put(INTENSITY_MEASURE, "Intensity Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName)
    {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
package se.eterna.ingest.util;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class StringAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(String v) {
        return v;
    }

    @Override
    public String marshal(String v) {
        if (v != null) {
            // Removes whitespace from start and end of the string
            v = v.trim();
            // Removes ASCII control characters (0-31, 127)
            return v.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        }
        return v;
    }
}
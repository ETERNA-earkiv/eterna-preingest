package se.eterna.commons.sip;

import java.io.InputStream;

public record SipFile(String filename, InputStream content) {}

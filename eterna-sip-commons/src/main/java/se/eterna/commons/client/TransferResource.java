package se.eterna.commons.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferResource(String id, String name, String relativePath, String uuid) {

    public String transferId() {
        return uuid != null ? uuid : id;
    }
}

@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters({
        @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value = StringAdapter.class, type = String.class)
})
@XmlSchema(
        namespace = "http://ead3.archivists.org/schema/",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix="", namespaceURI="http://ead3.archivists.org/schema/")
        }
)

package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import se.eterna.ingest.util.StringAdapter;

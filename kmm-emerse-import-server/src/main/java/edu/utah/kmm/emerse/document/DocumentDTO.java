package edu.utah.kmm.emerse.document;

import edu.utah.kmm.emerse.database.BaseDTO;
import org.hl7.fhir.dstu3.model.DocumentReference;

import java.util.Map;

/**
 * DTO encapsulating document attributes.
 */
public class DocumentDTO extends BaseDTO {

    public DocumentDTO(DocumentReference document, Map<String, Object> additionalParams) {
        super(additionalParams);
        String id = document.getIdElement().getIdPart();
        map.put("ID", id);
        map.put("RPT_ID", id);
        map.put("RPT_DATE", document.getCreated());
        map.put("SOURCE", "source1");
    }

}

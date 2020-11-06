package edu.utah.kmm.emerse.document;

import edu.utah.kmm.emerse.solr.BaseSolrDTO;
import org.hl7.fhir.dstu3.model.DocumentReference;

import java.util.Map;

/**
 * DTO encapsulating document attributes.
 */
public class DocumentDTO extends BaseSolrDTO {

    public DocumentDTO(
            DocumentReference document,
            Map<String, Object> additionalParams) {
        super(additionalParams, null);
        String id = document.getIdElement().getIdPart();
        map.put("ID", id);
        map.put("RPT_ID", id);
        map.put("RPT_DATE", document.getCreated());
        map.put("DOC_TYPE", document.getType().getText());
    }

}

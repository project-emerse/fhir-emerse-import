package edu.utah.kmm.emerse.patient;

import edu.utah.kmm.emerse.database.BaseDTO;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.truncate;

/**
 * DTO encapsulating patient attributes.
 */
public class PatientDTO extends BaseDTO {

    public PatientDTO(Patient patient, Map<String, Object> additionalParams) {
        super(additionalParams);
        HumanName name = patient.getNameFirstRep();
        boolean deceased = patient.hasDeceasedDateTimeType() || (patient.hasDeceasedBooleanType() && patient.getDeceasedBooleanType().getValue());
        map.put("FIRST_NAME", truncate(name.getGivenAsSingleString(), 65));
        map.put("MIDDLE_NAME", null);
        map.put("LAST_NAME", truncate(name.getFamily(), 75));
        map.put("BIRTH_DATE", patient.getBirthDate());
        map.put("SEX_CD", truncate(patient.hasGender() ? patient.getGender().toCode() : null, 50));
        map.put("DECEASED_FLAG", deceased ? 1 : 0);
        map.put("LANGUAGE_CD", truncate(patient.getLanguage(), 50));
        map.put("RACE_CD", null);
        map.put("ETHNICITY_CD", null);
        map.put("MARITAL_STATUS_CD", truncate(patient.hasMaritalStatus() ? patient.getMaritalStatus().getCodingFirstRep().getCode() : null, 50));
        map.put("RELIGION_CD", null);
        map.put("ZIP_CD", truncate(patient.hasAddress() ? patient.getAddressFirstRep().getPostalCode() : null, 10));
    }
}

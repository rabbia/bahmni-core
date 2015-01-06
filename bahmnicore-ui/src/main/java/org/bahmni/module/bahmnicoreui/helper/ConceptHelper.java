package org.bahmni.module.bahmnicoreui.helper;

import org.bahmni.module.bahmnicoreui.contract.ConceptDetails;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;

import java.util.*;
import org.openmrs.module.bahmniemrapi.encountertransaction.mapper.ETObsToBahmniObsMapper;
import org.openmrs.module.emrapi.utils.HibernateLazyLoader;

public  class ConceptHelper {


    private ConceptService conceptService;

    public ConceptHelper(ConceptService conceptService) {
        this.conceptService = conceptService;
    }


    public List<Concept> getConceptsForNames(Collection<String> conceptNames){
        List<Concept> concepts = new ArrayList<>();
        if(conceptNames!= null){
            for (String conceptName : conceptNames) {
                concepts.add(conceptService.getConceptByName(conceptName.replaceAll("%20", " ")));
            }
        }
        return concepts;
    }

    public Set<ConceptDetails> getLeafConceptDetails(List<String> obsConcepts) {
        if(obsConcepts != null && !obsConcepts.isEmpty()){
            Set<ConceptDetails> leafConcepts = new LinkedHashSet<>();
            for (String conceptName : obsConcepts) {
                Concept concept = conceptService.getConceptByName(conceptName);
                addLeafConcepts(concept, null, leafConcepts);
            }
            return leafConcepts;
        }
        return Collections.EMPTY_SET;
    }

    protected void addLeafConcepts(Concept rootConcept, Concept parentConcept, Set<ConceptDetails> leafConcepts) {
        if(rootConcept != null){
            if(rootConcept.isSet()){
                for (Concept setMember : rootConcept.getSetMembers()) {
                    addLeafConcepts(setMember,rootConcept,leafConcepts);
                }
            }
            else if(!shouldBeExcluded(rootConcept)){
                Concept conceptToAdd = rootConcept;
                if(parentConcept != null){
                    if(ETObsToBahmniObsMapper.CONCEPT_DETAILS_CONCEPT_CLASS.equals(parentConcept.getConceptClass().getName())){
                        conceptToAdd = parentConcept;
                    }
                }
                leafConcepts.add(createConceptDetails(conceptToAdd));
            }
        }
    }

    private ConceptDetails createConceptDetails(Concept conceptToAdd) {
        Concept concept = new HibernateLazyLoader().load(conceptToAdd);

        String fullName = getConceptName(concept, ConceptNameType.FULLY_SPECIFIED);
        String shortName = getConceptName(concept, ConceptNameType.SHORT);
        ConceptDetails conceptDetails = new ConceptDetails();
        conceptDetails.setName(shortName == null ? fullName : shortName);
        if (concept.isNumeric()){
            ConceptNumeric numericConcept = (ConceptNumeric) concept;
            conceptDetails.setUnits(numericConcept.getUnits());
            conceptDetails.setHiNormal(numericConcept.getHiNormal());
            conceptDetails.setLowNormal(numericConcept.getLowNormal());
        }
        return conceptDetails;
    }

    protected String getConceptName(Concept rootConcept, ConceptNameType conceptNameType) {
        String conceptName = null;
        ConceptName name = rootConcept.getName(Context.getLocale(), conceptNameType, null);
        if(name != null){
            conceptName  = name.getName();
        }
        return conceptName;
    }

    protected boolean shouldBeExcluded(Concept rootConcept) {
        return ETObsToBahmniObsMapper.ABNORMAL_CONCEPT_CLASS.equals(rootConcept.getConceptClass().getName()) ||
                ETObsToBahmniObsMapper.DURATION_CONCEPT_CLASS.equals(rootConcept.getConceptClass().getName());
    }

    public Set<ConceptDetails> getConceptDetails(List<String> conceptNames) {
        LinkedHashSet<ConceptDetails> conceptDetails = new LinkedHashSet<>();
        for (String conceptName : conceptNames) {
            Concept conceptByName = conceptService.getConceptByName(conceptName);
            if (conceptByName != null){
                conceptDetails.add(createConceptDetails(conceptByName));
            }
        }
        return conceptDetails;
    }
}

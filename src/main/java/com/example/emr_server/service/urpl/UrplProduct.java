package com.example.emr_server.service.urpl;

public class UrplProduct {
    private Long id;
    private String specimenType;
    private String medicinalProductName;
    private String commonName;
    private String pharmaceuticalFormName;
    private String medicinalProductPower;
    private String activeSubstanceName;
    private String subjectMedicinalProductName;
    private String registryNumber;
    private String procedureTypeName;
    private String expirationDateString;
    private String atcCode;
    private String targetSpecies;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSpecimenType() { return specimenType; }
    public void setSpecimenType(String specimenType) { this.specimenType = specimenType; }
    public String getMedicinalProductName() { return medicinalProductName; }
    public void setMedicinalProductName(String medicinalProductName) { this.medicinalProductName = medicinalProductName; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getPharmaceuticalFormName() { return pharmaceuticalFormName; }
    public void setPharmaceuticalFormName(String pharmaceuticalFormName) { this.pharmaceuticalFormName = pharmaceuticalFormName; }
    public String getMedicinalProductPower() { return medicinalProductPower; }
    public void setMedicinalProductPower(String medicinalProductPower) { this.medicinalProductPower = medicinalProductPower; }
    public String getActiveSubstanceName() { return activeSubstanceName; }
    public void setActiveSubstanceName(String activeSubstanceName) { this.activeSubstanceName = activeSubstanceName; }
    public String getSubjectMedicinalProductName() { return subjectMedicinalProductName; }
    public void setSubjectMedicinalProductName(String subjectMedicinalProductName) { this.subjectMedicinalProductName = subjectMedicinalProductName; }
    public String getRegistryNumber() { return registryNumber; }
    public void setRegistryNumber(String registryNumber) { this.registryNumber = registryNumber; }
    public String getProcedureTypeName() { return procedureTypeName; }
    public void setProcedureTypeName(String procedureTypeName) { this.procedureTypeName = procedureTypeName; }
    public String getExpirationDateString() { return expirationDateString; }
    public void setExpirationDateString(String expirationDateString) { this.expirationDateString = expirationDateString; }
    public String getAtcCode() { return atcCode; }
    public void setAtcCode(String atcCode) { this.atcCode = atcCode; }
    public String getTargetSpecies() { return targetSpecies; }
    public void setTargetSpecies(String targetSpecies) { this.targetSpecies = targetSpecies; }
}


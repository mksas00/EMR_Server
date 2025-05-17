package service;

import entity.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.PatientRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Override
    public List<Patient> getPatientsByFirstName(String firstName) {
        return patientRepository.findByFirstName(firstName);
    }

    @Override
    public List<Patient> getPatientsByLastName(String lastName) {
        return patientRepository.findByLastName(lastName);
    }

    @Override
    public List<Patient> getPatientsByFirstNameAndLastName(String firstName, String lastName) {
        return patientRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    @Override
    public Optional<Patient> getPatientByPesel(String pesel) {
        return patientRepository.findByPesel(pesel);
    }

    @Override
    public List<Patient> getPatientsByDateOfBirthBetween(LocalDate startDate, LocalDate endDate) {
        return patientRepository.findByDateOfBirthBetween(startDate, endDate);
    }

    @Override
    public List<Patient> getPatientsByCreatedBy(UUID createdById) {
        return patientRepository.findByCreatedBy_Id(createdById);
    }

    @Override
    public List<Patient> getPatientsByAddress(String addressFragment) {
        return patientRepository.findByAddressContaining(addressFragment);
    }

    @Override
    public List<Patient> getPatientsByGender(String gender) {
        return patientRepository.findByGender(gender);
    }

    @Override
    public List<Patient> getPatientsByNameFragment(String nameFragment) {
        return patientRepository.findByFirstNameContainingOrLastNameContaining(nameFragment, nameFragment);
    }

    @Override
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Override
    public void deletePatientById(UUID patientId) {
        patientRepository.deleteById(patientId);
    }
}
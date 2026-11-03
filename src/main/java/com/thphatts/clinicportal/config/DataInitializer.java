package com.thphatts.clinicportal.config;

import com.thphatts.clinicportal.modules.appointment.entity.Checkup;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import com.thphatts.clinicportal.modules.user.entity.User;
import com.thphatts.clinicportal.modules.patient.repository.PatientRepository;
import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import com.thphatts.clinicportal.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initUsers();
        initPatients();
    }

    private void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            log.info("Khởi tạo tài khoản mẫu Admin, Doctor, Receptionist...");

            User admin = User.builder()
                    .person(person("Quản Trị Viên", "0900000001", "admin@clinic.com", "Phòng Khám Trung Tâm"))
                    .username("admin")
                    .email("admin@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ROLE_ADMIN)
                    .build();

            User doctor = User.builder()
                    .person(person("Bác Sĩ Nguyễn Văn A", "0900000002", "doctor@clinic.com", "Khoa Nội - Phòng Khám"))
                    .username("doctor")
                    .email("doctor@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ROLE_DOCTOR)
                    .build();

            User receptionist = User.builder()
                    .person(person("Lễ Tân Lê Thị B", "0900000003", "receptionist@clinic.com", "Quầy Lễ Tân"))
                    .username("receptionist")
                    .email("receptionist@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ROLE_RECEPTIONIST)
                    .build();

            User patientUser = User.builder()
                    .person(person("Bệnh Nhân Mẫu", "0900000004", "patient@clinic.com", "TP. Hồ Chí Minh"))
                    .username("patient")
                    .email("patient@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ROLE_PATIENT)
                    .build();

            userRepository.saveAll(List.of(admin, doctor, receptionist, patientUser));
            log.info("Khởi tạo thành công các tài khoản mẫu!");
        }
    }

    private void initPatients() {
        if (patientRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu mẫu cho Bệnh nhân...");

            Patient p1 = samplePatient("Nguyễn Văn An", "038099123456", "0987654321", "nguyenvanan@gmail.com",
                    LocalDate.of(1990, 5, 15), "M", "123 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh", "PT-DEV-0001");

            Checkup c1 = new Checkup();
            c1.setDiagnoses("Cảm cúm thông thường, viêm họng nhẹ");
            c1.setPatient(p1);

            Checkup c2 = new Checkup();
            c2.setDiagnoses("Rối loạn tiêu hóa cấp tính");
            c2.setPatient(p1);

            p1.getCheckups().add(c1);
            p1.getCheckups().add(c2);

            Patient p2 = samplePatient("Trần Thị Bích", "038099234567", "0912345678", "tranthibich@gmail.com",
                    LocalDate.of(1995, 8, 20), "F", "456 Lê Lợi, Quận 3, TP. Hồ Chí Minh", "PT-DEV-0002");

            Checkup c3 = new Checkup();
            c3.setDiagnoses("Viêm xoang mãn tính tái phát");
            c3.setPatient(p2);
            p2.getCheckups().add(c3);

            Patient p3 = samplePatient("Lê Hoàng Nam", "038099345678", "0903123456", "lehoangnam@gmail.com",
                    LocalDate.of(1988, 12, 10), "M", "789 Trần Hưng Đạo, Quận 5, TP. Hồ Chí Minh", "PT-DEV-0003");

            patientRepository.saveAll(List.of(p1, p2, p3));
            log.info("Tạo dữ liệu mẫu hoàn tất! Đã lưu {} bệnh nhân.", patientRepository.count());
        }
    }

    private Person person(String name, String phone, String email, String address) {
        Person person = new Person();
        person.setFullName(name);
        person.setPhoneNumber(phone);
        person.setEmail(email);
        person.setAddress(address);
        return personRepository.save(person);
    }

    private Patient samplePatient(String name, String citizenId, String phone, String email, LocalDate dob,
                                  String gender, String address, String code) {
        Person person = person(name, phone, email, address);
        person.setCitizenId(citizenId);
        person.setBirthdate(dob);
        person.setGender(gender);
        personRepository.save(person);
        return Patient.builder().person(person).patientCode(code).build();
    }
}


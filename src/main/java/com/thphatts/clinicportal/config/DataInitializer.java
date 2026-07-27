package com.thphatts.clinicportal.config;

import com.thphatts.clinicportal.entity.Checkup;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.Role;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
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
                    .name("Quản Trị Viên")
                    .username("admin")
                    .email("admin@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("0900000001")
                    .address("Phòng Khám Trung Tâm")
                    .role(Role.ROLE_ADMIN)
                    .build();

            User doctor = User.builder()
                    .name("Bác Sĩ Nguyễn Văn A")
                    .username("doctor")
                    .email("doctor@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("0900000002")
                    .address("Khoa Nội - Phòng Khám")
                    .role(Role.ROLE_DOCTOR)
                    .build();

            User receptionist = User.builder()
                    .name("Lễ Tân Lê Thị B")
                    .username("receptionist")
                    .email("receptionist@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("0900000003")
                    .address("Quầy Lễ Tân")
                    .role(Role.ROLE_RECEPTIONIST)
                    .build();

            User patientUser = User.builder()
                    .name("Bệnh Nhân Mẫu")
                    .username("patient")
                    .email("patient@clinic.com")
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("0900000004")
                    .address("TP. Hồ Chí Minh")
                    .role(Role.ROLE_PATIENT)
                    .build();

            userRepository.saveAll(List.of(admin, doctor, receptionist, patientUser));
            log.info("Khởi tạo thành công các tài khoản mẫu!");
        } else {
            // Đồng bộ mật khẩu admin thành password123
            userRepository.findByUsername("admin").ifPresent(admin -> {
                admin.setPassword(passwordEncoder.encode("password123"));
                userRepository.save(admin);
                log.info("Đã cập nhật lại mật khẩu admin thành password123");
            });
        }
    }

    private void initPatients() {
        if (patientRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu mẫu cho Bệnh nhân...");

            Patient p1 = Patient.builder()
                    .fullName("Nguyễn Văn An")
                    .citizenId("038099123456")
                    .phone("0987654321")
                    .email("nguyenvanan@gmail.com")
                    .dob(LocalDate.of(1990, 5, 15))
                    .gender("NAM")
                    .address("123 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh")
                    .build();

            Checkup c1 = new Checkup();
            c1.setDiagnoses("Cảm cúm thông thường, viêm họng nhẹ");
            c1.setPatient(p1);

            Checkup c2 = new Checkup();
            c2.setDiagnoses("Rối loạn tiêu hóa cấp tính");
            c2.setPatient(p1);

            p1.getCheckups().add(c1);
            p1.getCheckups().add(c2);

            Patient p2 = Patient.builder()
                    .fullName("Trần Thị Bích")
                    .citizenId("038099234567")
                    .phone("0912345678")
                    .email("tranthibich@gmail.com")
                    .dob(LocalDate.of(1995, 8, 20))
                    .gender("NỮ")
                    .address("456 Lê Lợi, Quận 3, TP. Hồ Chí Minh")
                    .build();

            Checkup c3 = new Checkup();
            c3.setDiagnoses("Viêm xoang mãn tính tái phát");
            c3.setPatient(p2);
            p2.getCheckups().add(c3);

            Patient p3 = Patient.builder()
                    .fullName("Lê Hoàng Nam")
                    .citizenId("038099345678")
                    .phone("0903123456")
                    .email("lehoangnam@gmail.com")
                    .dob(LocalDate.of(1988, 12, 10))
                    .gender("NAM")
                    .address("789 Trần Hưng Đạo, Quận 5, TP. Hồ Chí Minh")
                    .build();

            patientRepository.saveAll(List.of(p1, p2, p3));
            log.info("Tạo dữ liệu mẫu hoàn tất! Đã lưu {} bệnh nhân.", patientRepository.count());
        }
    }
}


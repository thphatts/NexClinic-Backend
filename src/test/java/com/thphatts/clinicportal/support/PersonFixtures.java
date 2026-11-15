package com.thphatts.clinicportal.support;

import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.user.entity.User;

public final class PersonFixtures {
    private PersonFixtures() {}

    public static Person person(String name, String phone, String citizenId, String userId) {
        Person person = new Person();
        person.setFullName(name);
        person.setPhoneNumber(phone);
        person.setCitizenId(citizenId);
        if (userId != null) {
            User user = User.builder().id(userId).person(person).build();
            person.setUser(user);
        }
        return person;
    }
}

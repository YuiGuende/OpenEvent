
package com.group02.openevent.service.impl;

import com.group02.openevent.dto.UserDto;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserValidationServiceImpl {

    @Autowired
    private IAccountRepo accountRepo;

    @Autowired
    private IUserRepo userRepo;

    private static final Pattern GMAIL_PATTERN = Pattern.compile(".*@gmail\\.com$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZàáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵđĐ\\s]+$");

    public List<String> validateUserDto(UserDto userDto) {
        List<String> errors = new ArrayList<>();

        // Validate email
        validateEmail(userDto.getEmail(), errors);

        // Validate phone number
        validatePhoneNumber(userDto.getPhoneNumber(), errors);

        // Validate name
        validateName(userDto.getName(), errors);

        // Validate password
        validatePassword(userDto.getPassword(), errors);

        return errors;
    }

    private void validateEmail(String email, List<String> errors) {
        if (email == null || email.trim().isEmpty()) {
            errors.add("Email không được để trống");
            return;
        }

        email = email.trim();

        // Check gmail format
        if (!GMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Email phải có định dạng @gmail.com");
        }

        // Check if email exists in database
        if (accountRepo.existsByEmail(email)) {
            errors.add("Email đã tồn tại trong hệ thống");
        }
    }

    private void validatePhoneNumber(String phoneNumber, List<String> errors) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            errors.add("Số điện thoại không được để trống");
            return;
        }

        phoneNumber = phoneNumber.trim();

        // Check phone format (only numbers, 10-11 digits)
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            errors.add("Số điện thoại phải là dãy số từ 10-11 chữ số, không chứa ký tự");
        }

        // Check if phone exists in database
        if (userRepo.existsByPhoneNumber(phoneNumber)) {
            errors.add("Số điện thoại đã tồn tại trong hệ thống");
        }
    }

    private void validateName(String name, List<String> errors) {
        if (name == null || name.trim().isEmpty()) {
            errors.add("Tên không được để trống");
            return;
        }

        name = name.trim();

        // Check name format (no numbers or special characters)
        if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add("Tên không được chứa số và ký tự đặc biệt");
        }
    }

    private void validatePassword(String password, List<String> errors) {
        if (password == null || password.trim().isEmpty()) {
            errors.add("Mật khẩu không được để trống");
        } else if (password.length() < 6) {
            errors.add("Mật khẩu phải có ít nhất 6 ký tự");
        }
    }
}
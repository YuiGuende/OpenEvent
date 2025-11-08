# Migration to User Entity - Refactoring Summary

## Overview
Đã refactor hệ thống để tách User ra khỏi Account, tạo cấu trúc:
- **Account**: Chỉ phục vụ authentication (email, password, role)
- **User**: Profile chung (name, phone, avatar, timestamps)
- **Role Entities**: Customer, Host, Admin, Department map với User

## Completed Changes

### 1. Entities Created/Updated
- ✅ **User.java**: New entity với profile fields
- ✅ **Account.java**: Updated để có relationship với User (OneToOne)
- ✅ **Customer.java**: Updated để map với User thay vì Account
- ✅ **Host.java**: Updated để map với User (và giữ customer_id optional)
- ✅ **Admin.java**: Updated để map với User
- ✅ **Department.java**: Updated để map với User (và đổi primary key từ account_id sang user_id)

### 2. Repositories
- ✅ **IUserRepo.java**: Created cho User entity
- ✅ **ICustomerRepo.java**: Updated methods để query qua User

### 3. Services
- ✅ **UserService.java**: New service interface
- ✅ **UserServiceImpl.java**: Implementation với helper methods
- ✅ **CustomerServiceImpl.java**: Updated để sử dụng User

### 4. Controllers
- ✅ **OrderController.java**: Updated để sử dụng UserService

### 5. Database Migration
- ✅ **migrate_to_user_table.sql**: Complete migration script

## Remaining Work

### High Priority
1. **Authentication Logic**:
   - Update `CustomUserDetailsService.java` (vẫn sử dụng Account.getRole(), có thể giữ tạm)
   - Update `CustomAuthenticationSuccessHandler.java` nếu cần

2. **Controllers** cần update:
   - `HomeController.java`: Uses `customerRepo.findByAccount_AccountId`
   - `EventFormController.java`: Uses Customer/Account
   - `PaymentController.java`: Uses Customer/Account
   - `LoginController.java`: Check authentication flow
   - Các controller khác sử dụng `findByAccount_AccountId`

3. **Services** cần update:
   - `AuthServiceImpl.java`
   - `OrderServiceImpl.java`: Check if uses Customer.getAccount()
   - `EventFormServiceImpl.java`
   - `VolunteerServiceImpl.java`: Update để sử dụng User

4. **Other Entities/Repositories**:
   - `VolunteerApplication.java`: Đang map với Customer, có thể giữ hoặc map với User
   - Các repository khác sử dụng Account relationships

### Medium Priority
5. **DTOs**: Update các DTO có chứa customerName, customerEmail để lấy từ User
6. **Templates**: Check xem có cần update không
7. **Tests**: Update unit tests và integration tests

### Low Priority
8. **Cleanup**:
   - Xóa các field duplicate trong Customer (name, email, phone_number) sau khi migration xong
   - Xóa role field khỏi Account (sau khi đã migrate xong và test)
   - Xóa account_id column khỏi customer, admin, department tables

## Migration Steps

1. **Backup database** trước khi chạy migration
2. **Run migration script**: `migrate_to_user_table.sql`
3. **Update code** theo các file đã được updated
4. **Test thoroughly**: Authentication, order creation, customer creation
5. **Deploy** và monitor

## Notes

- Account.role field được giữ lại tạm thời để backward compatibility
- Customer, Admin, Department vẫn có thể có account_id column trong DB (để migration period)
- UserService.getOrCreateUser() tự động tạo User nếu chưa có
- Tất cả role entities (Customer, Host, Admin, Department) giờ map với User thay vì Account


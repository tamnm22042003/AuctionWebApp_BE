package vn.webapp.backend.auction.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.webapp.backend.auction.dto.RegisterAccountRequest;
import vn.webapp.backend.auction.enums.AccountState;
import vn.webapp.backend.auction.enums.Role;
import vn.webapp.backend.auction.exception.ResourceNotFoundException;
import vn.webapp.backend.auction.exception.UserAlreadyExistsException;
import vn.webapp.backend.auction.model.ErrorMessages;
import vn.webapp.backend.auction.model.User;
import vn.webapp.backend.auction.repository.AuctionRegistrationRepository;
import vn.webapp.backend.auction.repository.UserRepository;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuctionRegistrationRepository auctionRegistrationRepository;

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @Override
    public User findUserByUsernameOrEmail(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Người dùng với username hoặc email: " + username + " không tồn tại. Vui lòng đăng ký tài khoản mới.")));
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @Override
    public List<User> getAllStaff() {
            return userRepository.findAllByRole(Role.STAFF);
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public void setAccountState(Integer id, String state) {
        var existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        existingUser.setState(AccountState.valueOf(state));
    }

    @Override
    public User updateUser(User user) {
        var existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        existingUser.setAddress(user.getAddress());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setDistrict(user.getDistrict());
        existingUser.setWard(user.getWard());
        existingUser.setCity(user.getCity());
        existingUser.setAvatar(user.getAvatar());
        existingUser.setPhone(user.getPhone());
        existingUser.setYob(user.getYob());
        existingUser.setBankAccountNumber(user.getBankAccountNumber());
        existingUser.setBankAccountName(user.getBankAccountName());
        existingUser.setBank(user.getBank());
        return existingUser;
    }

    @Override
    public Page<User> getMemberByFullNameContainingAndState(
            String fullName,
            AccountState state,
            Pageable pageable) {
        return userRepository.findByFullNameContainingAndRoleAndState(fullName, Role.MEMBER, state, pageable);
    }

    @Override
    public Page<User> getStaffByFullNameContainingAndRoleAndState(
            String fullName,
            Role role,
            AccountState state,
            Pageable page) {

        // Default to MEMBER if role is null
        if (role == null) {
            role = Role.MEMBER;
        }

        // Perform the query based on role
        switch (role) {
            case MEMBER:
                return userRepository.findByFullNameContainingAndRoleAndState(fullName, Role.MEMBER, state, page);
            case MANAGER:
                return userRepository.findByFullNameContainingAndRoleAndState(fullName, Role.MANAGER, state, page);
            case STAFF:
                return userRepository.findByFullNameContainingAndRoleAndState(fullName, Role.STAFF, state, page);
            default:
                // Handle any other roles or default case
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }


    @Override
    public User getLatestUserInAuctionHistoryByAuctionId(Integer auctionId) {
        return userRepository.findLatestUserInAuctionHistoryByAuctionId(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @Override
    public User registerStaff(RegisterAccountRequest request) {
        userRepository.findByUsername(request.username())
                .ifPresent(existingUser -> {
                    throw new UserAlreadyExistsException(ErrorMessages.USER_ALREADY_EXIST);
                });
        userRepository.findByEmail(request.email())
                .ifPresent(existingUser -> {
                    throw new UserAlreadyExistsException(ErrorMessages.USER_ALREADY_EXIST);
                });
        var user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .state(AccountState.ACTIVE)
                .district(request.district())
                .ward(request.ward())
                .city(request.city())
                .yob(request.yob())
                .phone(request.phone())
                .address(request.address())
                .CCCD(request.CCCD())
                .build();
        return userRepository.save(user);
    }



}

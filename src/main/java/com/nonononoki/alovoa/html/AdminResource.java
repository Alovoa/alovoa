package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.service.AdminService;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Controller
public class AdminResource {

    public static final String URL = "/admin";
    @Autowired
    private UserReportRepository userReportRepo;
    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;

    @GetMapping(URL)
    public ModelAndView admin()
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {

        adminService.checkRights();
        ModelAndView mav = new ModelAndView("admin");

        userReportRepo.deleteAll(userReportRepo.findByUserToIsNull());
        List<UserReport> reports = userReportRepo.findTop20ByOrderByDateAsc();

        for (UserReport r : reports) {
            r.setUserToIdEnc(UserDto.encodeId(r.getUserTo().getId(), textEncryptor));
        }

        List<Contact> contacts = contactRepository.findTop20ByHiddenFalse();

        mav.addObject("reports", reports);
        mav.addObject("contacts", contacts);
        User user = authService.getCurrentUser(true);
        mav.addObject("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(true)
                .currentUser(user).user(user).textEncryptor(textEncryptor).userService(userService).build()));

        return mav;
    }

    /*
    @GetMapping(path ="/admin/profile/view/{id}", produces= MediaType.APPLICATION_JSON_VALUE)
    public UserDto viewProfile(@PathVariable String id) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, UnsupportedEncodingException, BadPaddingException,
            NoSuchAlgorithmException, InvalidKeyException {
        return adminService.viewProfile(id);
    }
     */

    @GetMapping("/admin/profile/view/{id}/media")
    public ModelAndView viewProfileMedia(@PathVariable String id) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, UnsupportedEncodingException, BadPaddingException,
            NoSuchAlgorithmException, InvalidKeyException {
        adminService.checkRights();
        ModelAndView mav = new ModelAndView("admin-user-media");
        User user = userService.encodedIdToUser(id);
        mav.addObject("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(true)
                .currentUser(user).user(user).textEncryptor(textEncryptor).userService(userService).build()));
        return mav;
    }
}

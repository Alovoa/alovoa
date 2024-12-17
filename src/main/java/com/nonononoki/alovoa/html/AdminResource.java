package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.service.AdminService;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;

@Controller
public class AdminResource {

    public static final String URL = "/admin";
    @Autowired
    private UserReportRepository userReportRepo;
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
            r.setToUuid(Tools.getUserUUID(r.getUserTo(), userService));
        }

        mav.addObject("reports", reports);
        User user = authService.getCurrentUser(true);
        mav.addObject("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(true)
                .currentUser(user).user(user).userService(userService).build()));

        return mav;
    }

    @GetMapping("/admin/profile/view/{uuid}/media")
    public ModelAndView viewProfileMedia(@PathVariable UUID uuid) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, UnsupportedEncodingException, BadPaddingException,
            NoSuchAlgorithmException, InvalidKeyException {
        adminService.checkRights();
        ModelAndView mav = new ModelAndView("admin-user-media");
        User user = userService.findUserByUuid(uuid);
        mav.addObject("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(true)
                .currentUser(user).user(user).userService(userService).build()));
        return mav;
    }
}

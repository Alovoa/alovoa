package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

	@Autowired
	private RegisterService registerService;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserReportRepository userReportRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Value("${app.age.min}")
	private int minAge;

	@Value("${app.message.size}")
	private int maxMessageSize;

	@Value("${app.first-name.length-max}")
	private int firstNameLengthMax;

	@Value("${app.first-name.length-min}")
	private int firstNameLengthMin;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private MailService mailService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private ImprintService imprintService;

	private List<User> testUsers;

	@BeforeEach
	void before() throws Exception {
		when(mailService.sendMail(Mockito.any(String.class), any(String.class), any(String.class),
				any(String.class))).thenReturn(true);
		testUsers = RegisterServiceTest.getTestUsers(captchaService, registerService, firstNameLengthMax,
				firstNameLengthMin);
	}

	@AfterEach
	void after() throws Exception {
		RegisterServiceTest.deleteAllUsers(userService, authService, captchaService, conversationRepo, userRepo);
	}

	@Test
	void test() throws Exception {

		List<User> allUsers = userRepo.findAll();
		User adminUser = allUsers.get(0);
		User user1 = testUsers.get(0);
		User user2 = testUsers.get(1);
		User user3 = testUsers.get(2);

		when(authService.getCurrentUser()).thenReturn(adminUser);
		when(authService.getCurrentUser(true)).thenReturn(adminUser);

		MailDto mailDto = new MailDto();
		String mailBodyAll = "mailBodyAll";
		String mailBodySingle = "mailBodySingle";
		String mailSubject = "mailSubject";
		mailDto.setBody(mailBodyAll);
		mailDto.setSubject(mailSubject);

		adminService.sendMailAll(mailDto);
		mailDto.setEmail(user1.getEmail());
		mailDto.setBody(mailBodySingle);
		adminService.sendMailSingle(mailDto);

		assertEquals(0, userReportRepo.count());
		reportTest(user3, user2, adminUser);
		assertEquals(1, userReportRepo.count());
		UserReport report = reportTest(user1, user2, adminUser);
		user2 = userRepo.findByEmail(user2.getEmail());
		adminService.deleteReport(report.getId());
		assertEquals(0, userReportRepo.count());

		adminService.banUser(user1.getUuid());
		User bannedUser = userRepo.findById(user1.getId()).get();
		assertTrue(bannedUser.isDisabled());
		assertTrue(adminService.userExists(URLDecoder.decode(user2.getEmail(), StandardCharsets.UTF_8)));

		double donationAmount = 10.00;
		adminService.addDonation(URLDecoder.decode(user2.getEmail(), StandardCharsets.UTF_8), donationAmount);
		assertEquals(donationAmount, user2.getTotalDonations());
		assertEquals(1, user2.getDonations().size());
	}

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteInvalidUsers() throws AlovoaException {

        User user1 = mock(User.class);
        when(user1.getEmail()).thenReturn("test+test@test.com");
        when(user1.getId()).thenReturn(1L);
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(2L);
        when(user2.getEmail()).thenReturn(null);
        User user3 = mock(User.class);
        when(user3.getId()).thenReturn(3L);
        when(user3.getEmail()).thenReturn("invalid-at-invalid-com");
        Page<User> userSlice = mock(Page.class);
        UserRepository userRepoMock = mock(UserRepository.class);
        AdminService adminService = spy(new AdminService());
        ReflectionTestUtils.setField(adminService, "userRepo", userRepoMock);
        when(userSlice.hasNext()).thenReturn(false);
        when(userSlice.getContent()).thenReturn(List.of(user1, user2, user3));
        when(userRepoMock.findAll(any(PageRequest.class))).thenReturn(userSlice);
        doNothing().when(adminService).deleteAccount(anyLong());
        doNothing().when(adminService).checkRights();

        AdminService.DeleteInvalidUsersResult result = adminService.deleteInvalidUsers();

        verify(adminService, times(2)).deleteAccount(anyLong());
        assertEquals(2L, result.getUsersToBeDeleted().get(0));
        assertEquals(3L, result.getUsersToBeDeleted().get(1));
        assertEquals(2L, result.getUsersDeleted().get(0));
        assertEquals(3L, result.getUsersDeleted().get(1));
        assertTrue(result.getUsersThatCouldNotBeDeleted().isEmpty());
    }

	private UserReport reportTest(User user1, User user2, User adminUser) throws Exception {
		when(authService.getCurrentUser()).thenReturn(user1);
		when(authService.getCurrentUser(true)).thenReturn(user1);
		UserReport report = userService.reportUser(user2.getUuid(), "report");
		when(authService.getCurrentUser()).thenReturn(adminUser);
		when(authService.getCurrentUser(true)).thenReturn(adminUser);
		return report;
	}

}
